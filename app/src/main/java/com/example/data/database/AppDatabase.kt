package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        Vehicle::class,
        ServiceLog::class,
        FuelLog::class,
        Expense::class,
        Document::class,
        Reminder::class,
        User::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceLogDao(): ServiceLogDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun documentDao(): DocumentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Template Migration from Database Version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // If there were any schema updates in version 2, Room runs this custom SQL blocks.
                // For example, if you added a new table:
                // db.execSQL("CREATE TABLE IF NOT EXISTS `new_table` ...")
                // Or if you added a column:
                // db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `new_column` TEXT DEFAULT NULL")
            }
        }

        // Template Migration from Version 2 to 3 (for future updates)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // When you want to add/modify database fields, increment the database version to 3
                // and define the SQL statement to execute here without wiping any existing user data:
                // db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `notes_field` TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "manavahana_database"
                )
                // PREVENT DESTRUCTIVE DATA LOSS:
                // By default, fallbackToDestructiveMigration() wipes out the entire database when the version increments.
                // In production, we comment out or remove fallbackToDestructiveMigration() and instead specify .addMigrations(...)
                // fallbackToDestructiveMigration() // -> REMOVED to protect user data from being wiped during updates!
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade() // Safe: will only clear data if downgrading below current version 
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
