package io.github.wa_otomia.darkroom.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.wa_otomia.darkroom.core.StoredXaiCredential
import io.github.wa_otomia.darkroom.core.XaiTokenStorePort
import io.github.wa_otomia.darkroom.core.XaiTokens
import io.github.wa_otomia.darkroom.core.isAllowedAuthEndpoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscription tokens encrypted with a non-exportable AndroidKeyStore AES-256-GCM key, in
 * noBackupFilesDir so they never reach cloud backup or device transfer (AIPV050-R07, SYPM040-R04).
 * Any failure to read them back counts as signed out: the file is deleted, nothing is thrown and
 * nothing about the content is logged.
 */
@Singleton
class KeystoreXaiTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : XaiTokenStorePort {
    private val file: File get() = File(context.noBackupFilesDir, FILE_NAME)
    private val tmp: File get() = File(context.noBackupFilesDir, "$FILE_NAME.tmp")
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var cached: StoredXaiCredential? = null
    @Volatile private var cacheValid = false

    @Synchronized
    override fun load(): StoredXaiCredential? {
        if (cacheValid) return cached
        val result = try {
            readFile()
        } catch (_: Exception) {
            discard()
            null
        }
        cached = result
        cacheValid = true
        return result
    }

    fun hasCredential(): Boolean = load() != null

    @Synchronized
    override fun save(c: StoredXaiCredential) {
        val plain = encode(c).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
        cipher.updateAAD(AAD)
        val sealed = cipher.doFinal(plain)
        val iv = cipher.iv
        if (iv.size != IV_BYTES) throw IOException("unexpected iv")
        val dir = context.noBackupFilesDir
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("no directory")
        val target = tmp
        try {
            FileOutputStream(target).use { out ->
                out.write(byteArrayOf(VERSION))
                out.write(iv)
                out.write(sealed)
                out.flush()
                out.fd.sync()
            }
            if (!target.renameTo(file)) throw IOException("rename failed")
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        cached = c
        cacheValid = true
    }

    @Synchronized
    override fun delete() {
        file.delete()
        tmp.delete()
        cached = null
        cacheValid = true
    }

    private fun readFile(): StoredXaiCredential? {
        val f = file
        if (!f.isFile) return null
        val bytes = f.readBytes()
        if (bytes.size <= 1 + IV_BYTES || bytes[0] != VERSION) throw IOException("bad header")
        val key = key(create = false) ?: throw IOException("no key")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes, 1, IV_BYTES))
        cipher.updateAAD(AAD)
        val plain = cipher.doFinal(bytes, 1 + IV_BYTES, bytes.size - 1 - IV_BYTES)
        return decode(String(plain, Charsets.UTF_8)) ?: throw IOException("bad content")
    }

    /** Unreadable credential: drop the file and the key so the next sign-in starts clean. */
    private fun discard() {
        file.delete()
        tmp.delete()
        runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
        }
    }

    private fun key(create: Boolean): SecretKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        if (!create) return null
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encode(c: StoredXaiCredential): String = JsonObject(
        mapOf(
            "v" to JsonPrimitive(1),
            "access" to JsonPrimitive(c.tokens.accessToken),
            "refresh" to JsonPrimitive(c.tokens.refreshToken),
            "expiresAt" to (c.tokens.expiresAtMs?.let(::JsonPrimitive) ?: JsonNull),
            "account" to (c.tokens.accountLabel?.let(::JsonPrimitive) ?: JsonNull),
            "tokenEndpoint" to JsonPrimitive(c.tokenEndpoint),
            "revocationEndpoint" to (c.revocationEndpoint?.let(::JsonPrimitive) ?: JsonNull),
        ),
    ).toString()

    private fun decode(text: String): StoredXaiCredential? {
        val root = json.parseToJsonElement(text) as? JsonObject ?: return null
        fun str(key: String): String? = (root[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
        if ((root["v"] as? JsonPrimitive)?.longOrNull != 1L) return null
        val access = str("access")?.takeIf { it.isNotBlank() } ?: return null
        val refresh = str("refresh")?.takeIf { it.isNotBlank() } ?: return null
        val tokenEndpoint = str("tokenEndpoint")?.takeIf(::isAllowedAuthEndpoint) ?: return null
        val revocationRaw = str("revocationEndpoint")
        if (revocationRaw != null && !isAllowedAuthEndpoint(revocationRaw)) return null
        return StoredXaiCredential(
            tokens = XaiTokens(
                accessToken = access,
                refreshToken = refresh,
                expiresAtMs = (root["expiresAt"] as? JsonPrimitive)?.longOrNull,
                accountLabel = str("account"),
            ),
            tokenEndpoint = tokenEndpoint,
            revocationEndpoint = revocationRaw,
        )
    }

    private companion object {
        const val FILE_NAME = "xai_subscription.bin"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "darkroom_xai_tokens_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val VERSION: Byte = 0x01
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        val AAD = "darkroom-xai-v1".toByteArray(Charsets.UTF_8)
    }
}
