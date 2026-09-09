package app.darkroom.android.di

import android.content.Context
import androidx.room.Room
import app.darkroom.android.data.catalog.MIGRATION_1_2
import app.darkroom.android.data.catalog.MIGRATION_2_3
import app.darkroom.android.data.catalog.MIGRATION_3_4
import app.darkroom.android.data.catalog.MIGRATION_4_5
import app.darkroom.android.data.catalog.PhotoDatabase
import app.darkroom.android.data.progress.PrefsProgressStats
import app.darkroom.android.data.progress.ProgressStats
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PhotoDatabase =
        Room.databaseBuilder(context, PhotoDatabase::class.java, "darkroom.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun progressStats(@ApplicationContext context: Context): ProgressStats = PrefsProgressStats(context)
}
