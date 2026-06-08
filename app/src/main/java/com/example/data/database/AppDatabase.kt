package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "manavahana_database"
                )
                .fallbackToDestructiveMigration() // Simple for sandbox environments
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
