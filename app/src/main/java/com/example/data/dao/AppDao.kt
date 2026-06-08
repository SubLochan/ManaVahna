package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY id DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun getVehicleById(id: Int): Flow<Vehicle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)
}

@Dao
interface ServiceLogDao {
    @Query("SELECT * FROM service_logs WHERE vehicleId = :vehicleId ORDER BY serviceDate DESC")
    fun getLogsForVehicle(vehicleId: Int): Flow<List<ServiceLog>>

    @Query("SELECT * FROM service_logs ORDER BY serviceDate DESC")
    fun getAllServiceLogs(): Flow<List<ServiceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceLog(log: ServiceLog): Long

    @Update
    suspend fun updateServiceLog(log: ServiceLog)

    @Delete
    suspend fun deleteServiceLog(log: ServiceLog)
}

@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY fuelDate DESC")
    fun getFuelLogsForVehicle(vehicleId: Int): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs ORDER BY fuelDate DESC")
    fun getAllFuelLogs(): Flow<List<FuelLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(log: FuelLog): Long

    @Update
    suspend fun updateFuelLog(log: FuelLog)

    @Delete
    suspend fun deleteFuelLog(log: FuelLog)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY expenseDate DESC")
    fun getExpensesForVehicle(vehicleId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId ORDER BY id DESC")
    fun getDocumentsForVehicle(vehicleId: Int): Flow<List<Document>>

    @Query("SELECT * FROM documents ORDER BY id DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: Document): Long

    @Update
    suspend fun updateDocument(doc: Document)

    @Delete
    suspend fun deleteDocument(doc: Document)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY reminderDate ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY reminderDate ASC")
    fun getPendingReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long
}

