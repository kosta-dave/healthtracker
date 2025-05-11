package com.kosta.healthtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HealthRecord::class],
    version = 4,
    exportSchema = false // Добавьте это для отключения экспорта схемы
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthRecordDao(): HealthRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 1. Объявите миграцию здесь (в companion object)
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем новую колонку
                database.execSQL("ALTER TABLE health_records ADD COLUMN waistSize REAL")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE health_records ADD COLUMN comment TEXT")
            }
        }
        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавьте здесь изменения схемы
                // Например, если вы добавляли/изменяли поля:
                // database.execSQL("ALTER TABLE health_records ADD COLUMN new_column TEXT")
            }
        }
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_records_db" // Имя БД должно совпадать с предыдущим!
                )
                    .addMigrations(migration1To2, migration2To3, migration3To4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}