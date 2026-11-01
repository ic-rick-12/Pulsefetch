package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.DownloadStatus
import com.example.data.model.ExtractorType

class RoomConverters {
    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus =
        runCatching { DownloadStatus.valueOf(value) }.getOrDefault(DownloadStatus.QUEUED)

    @TypeConverter
    fun fromExtractorType(value: ExtractorType): String = value.name

    @TypeConverter
    fun toExtractorType(value: String): ExtractorType =
        runCatching { ExtractorType.valueOf(value) }.getOrDefault(ExtractorType.YTDLP_FORMAT)
}

@Database(
    entities = [DownloadTaskEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class PulseFetchDatabase : RoomDatabase() {
    abstract fun downloadTaskDao(): DownloadTaskDao

    companion object {
        @Volatile
        private var INSTANCE: PulseFetchDatabase? = null

        fun getInstance(context: Context): PulseFetchDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulseFetchDatabase::class.java,
                    "pulsefetch.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
