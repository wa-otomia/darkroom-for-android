package app.darkroom.android.core

import java.math.BigInteger
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private val RNG = SecureRandom()

fun encryptEcb(key: ByteArray, data: ByteArray): ByteArray {
    val pad = (16 - (data.size % 16)) % 16
    val padded = if (pad == 0) data else data + ByteArray(pad)
    val cipher = Cipher.getInstance("AES/ECB/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
    return cipher.doFinal(padded)
}

fun decryptEcb(key: ByteArray, data: ByteArray, stripZeros: Boolean = false): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
    val out = cipher.doFinal(data)
    if (!stripZeros) return out
    var end = out.size
    while (end > 0 && out[end - 1] == 0.toByte()) end--
    return out.copyOf(end)
}

fun handleTailZero(buf: ByteArray): ByteArray {
    val z = buf.indexOf(0)
    return if (z == -1) buf else buf.copyOf(z)
}

fun bytesToInt(buf: ByteArray): BigInteger {
    val hex = String(buf, Charsets.ISO_8859_1)
    if (hex.isEmpty()) return BigInteger.ZERO
    return BigInteger(hex, 16)
}

fun intToBytes(n: BigInteger): ByteArray =
    n.toString(16).uppercase().toByteArray(Charsets.US_ASCII)

fun pack16Front(src: ByteArray): ByteArray {
    val out = ByteArray(16) { 0x30 }
    System.arraycopy(src, 0, out, 16 - src.size, src.size)
    return out
}

fun pack16End(buf: ByteArray): ByteArray {
    val out = ByteArray(16)
    System.arraycopy(buf, 0, out, 0, minOf(16, buf.size))
    return out
}

fun modPow(base: BigInteger, exp: BigInteger, mod: BigInteger): BigInteger =
    base.modPow(exp, mod)

fun randomBelow(n: BigInteger): BigInteger {
    if (n <= BigInteger.valueOf(3)) error("invalid DH modulus")
    val bytes = (n.toString(16).length + 1) / 2
    while (true) {
        val raw = ByteArray(bytes)
        RNG.nextBytes(raw)
        var r = BigInteger(1, raw).mod(n)
        if (r > BigInteger.TWO && r < n) return r
    }
}

data class DhSession(
    val key: ByteArray,
    val rbField: ByteArray,
    val encP: ByteArray,
    val g: BigInteger,
    val p: BigInteger,
    val ra: BigInteger,
    val b: BigInteger,
    val rb: BigInteger,
    val k: BigInteger,
    val pField: ByteArray,
)

fun deriveSession(serverHelloBody: ByteArray, b: BigInteger? = null): DhSession {
    if (serverHelloBody.size < 36) error("server hello body must be 36 bytes")
    val gField = serverHelloBody.copyOfRange(0, 4)
    val pField = serverHelloBody.copyOfRange(4, 20)
    val raField = serverHelloBody.copyOfRange(20, 36)
    val g = bytesToInt(handleTailZero(gField))
    val p = bytesToInt(pField)
    val ra = bytesToInt(raField)
    val secret = b ?: randomBelow(p.subtract(BigInteger.TWO))
    val rb = modPow(g, secret, p)
    var rbBytes = intToBytes(rb)
    if (rbBytes.size < 16) rbBytes = pack16Front(rbBytes)
    val k = modPow(ra, secret, p)
    var kBytes = intToBytes(k)
    if (kBytes.size < 16) kBytes = pack16End(kBytes)
    val key = kBytes.copyOf(16)
    val encP = encryptEcb(key, pField).copyOf(16)
    return DhSession(
        key = key,
        rbField = rbBytes.copyOf(16),
        encP = encP,
        g = g,
        p = p,
        ra = ra,
        b = secret,
        rb = rb,
        k = k,
        pField = pField,
    )
}
