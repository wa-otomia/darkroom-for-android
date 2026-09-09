package app.darkroom.android.data.catalog

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.darkroom.android.core.EditRecord
import app.darkroom.android.core.PhotoMeta
import app.darkroom.android.core.PrintRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: String,
    val filename: String,
    val createdAt: String,
    val ingestedAt: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val parentId: String?,
    val generatePrompt: String?,
    val editsJson: String,
    val printsJson: String,
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun PhotoEntity.toMeta(): PhotoMeta = PhotoMeta(
    id = id,
    filename = filename,
    createdAt = createdAt,
    ingestedAt = ingestedAt,
    width = width,
    height = height,
    bytes = bytes,
    edits = runCatching { json.decodeFromString<List<EditRecord>>(editsJson) }.getOrDefault(emptyList()),
    prints = runCatching { json.decodeFromString<List<PrintRecord>>(printsJson) }.getOrDefault(emptyList()),
    parentId = parentId,
    generatePrompt = generatePrompt,
)

fun PhotoMeta.toEntity(): PhotoEntity = PhotoEntity(
    id = id,
    filename = filename,
    createdAt = createdAt,
    ingestedAt = ingestedAt,
    width = width,
    height = height,
    bytes = bytes,
    parentId = parentId,
    generatePrompt = generatePrompt,
    editsJson = json.encodeToString(edits),
    printsJson = json.encodeToString(prints),
)

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY ingestedAt DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY ingestedAt DESC")
    suspend fun list(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun get(id: String): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: String)
}

@Entity(tableName = "print_jobs")
data class PrintJobEntity(
    @PrimaryKey val id: String,
    val photoId: String,
    val source: String,
    val cropJson: String?,
    val copies: Int,
    val presetId: String?,
    val prompt: String,
    val cropImageWidth: Int?,
    val cropImageHeight: Int?,
    val rotateQuarters: Int,
    val landscape: Boolean,
    val rotationDegrees: Float,
    val placementJson: String?,
    val origin: String,
    val watermark: Boolean = false,
    val state: String,
    val phase: String?,
    val createdAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?,
    val printerJobId: Int?,
    val jobState: String?,
    val error: String?,
)

@Dao
interface PrintJobDao {
    @Query("SELECT * FROM print_jobs ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs WHERE state = 'queued' ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextQueued(): PrintJobEntity?

    @Query("SELECT * FROM print_jobs WHERE id = :id")
    suspend fun get(id: String): PrintJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrintJobEntity)

    @Query("DELETE FROM print_jobs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM print_jobs WHERE state IN (:states)")
    suspend fun deleteWhereState(states: List<String>)

    @Query("SELECT * FROM print_jobs WHERE state = 'running' ORDER BY createdAt ASC")
    suspend fun listRunning(): List<PrintJobEntity>
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE print_jobs ADD COLUMN watermark INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `print_jobs` (
                `id` TEXT NOT NULL,
                `photoId` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `cropJson` TEXT,
                `copies` INTEGER NOT NULL,
                `presetId` TEXT,
                `prompt` TEXT NOT NULL,
                `cropImageWidth` INTEGER,
                `cropImageHeight` INTEGER,
                `rotateQuarters` INTEGER NOT NULL,
                `landscape` INTEGER NOT NULL,
                `rotationDegrees` REAL NOT NULL,
                `placementJson` TEXT,
                `origin` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `phase` TEXT,
                `createdAt` INTEGER NOT NULL,
                `startedAt` INTEGER,
                `finishedAt` INTEGER,
                `printerJobId` INTEGER,
                `jobState` TEXT,
                `error` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}

@Database(entities = [PhotoEntity::class, PrintJobEntity::class], version = 3, exportSchema = false)
abstract class PhotoDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun printJobDao(): PrintJobDao
}
