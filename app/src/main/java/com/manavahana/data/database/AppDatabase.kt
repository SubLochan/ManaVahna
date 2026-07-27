package com.manavahana.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.manavahana.data.dao.*
import com.manavahana.data.model.*

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

        // Migration from Database Version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `vehicles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleName` TEXT NOT NULL, `vehicleNumber` TEXT NOT NULL, `brand` TEXT NOT NULL, `model` TEXT NOT NULL, `vehicleType` TEXT NOT NULL, `fuelType` TEXT NOT NULL, `purchaseDate` INTEGER NOT NULL, `insuranceExpiry` INTEGER NOT NULL, `pollutionExpiry` INTEGER NOT NULL, `vehicleImage` TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `service_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `serviceDate` INTEGER NOT NULL, `odometerReading` REAL NOT NULL, `serviceType` TEXT NOT NULL, `serviceCenter` TEXT NOT NULL, `cost` REAL NOT NULL, `notes` TEXT NOT NULL, `nextServiceDate` INTEGER NOT NULL, `billPhoto` TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `fuel_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `fuelDate` INTEGER NOT NULL, `litersFilled` REAL NOT NULL, `pricePerLiter` REAL NOT NULL, `totalAmount` REAL NOT NULL, `odometerReading` REAL NOT NULL, `fuelStationName` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `expenseDate` INTEGER NOT NULL, `category` TEXT NOT NULL, `amount` REAL NOT NULL, `notes` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `documents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `docType` TEXT NOT NULL, `title` TEXT NOT NULL, `expiryDate` INTEGER, `documentPath` TEXT, `isEncrypted` INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `reminderDate` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL DEFAULT 0, `category` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `email` TEXT NOT NULL, `name` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
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
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
