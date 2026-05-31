package com.example.data.repository

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class ManaVahanaRepository(
    private val vehicleDao: VehicleDao,
    private val serviceLogDao: ServiceLogDao,
    private val fuelLogDao: FuelLogDao,
    private val expenseDao: ExpenseDao,
    private val documentDao: DocumentDao,
    private val reminderDao: ReminderDao
) {
    // Vehicles
    val allVehicles: Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    fun getVehicleById(id: Int): Flow<Vehicle?> = vehicleDao.getVehicleById(id)
    suspend fun insertVehicle(vehicle: Vehicle): Long = vehicleDao.insertVehicle(vehicle)
    suspend fun updateVehicle(vehicle: Vehicle) = vehicleDao.updateVehicle(vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.deleteVehicle(vehicle)

    // Service Logs
    val allServiceLogs: Flow<List<ServiceLog>> = serviceLogDao.getAllServiceLogs()
    fun getLogsForVehicle(vehicleId: Int): Flow<List<ServiceLog>> = serviceLogDao.getLogsForVehicle(vehicleId)
    suspend fun insertServiceLog(log: ServiceLog): Long = serviceLogDao.insertServiceLog(log)
    suspend fun updateServiceLog(log: ServiceLog) = serviceLogDao.updateServiceLog(log)
    suspend fun deleteServiceLog(log: ServiceLog) = serviceLogDao.deleteServiceLog(log)

    // Fuel Logs
    val allFuelLogs: Flow<List<FuelLog>> = fuelLogDao.getAllFuelLogs()
    fun getFuelLogsForVehicle(vehicleId: Int): Flow<List<FuelLog>> = fuelLogDao.getFuelLogsForVehicle(vehicleId)
    suspend fun insertFuelLog(log: FuelLog): Long = fuelLogDao.insertFuelLog(log)
    suspend fun updateFuelLog(log: FuelLog) = fuelLogDao.updateFuelLog(log)
    suspend fun deleteFuelLog(log: FuelLog) = fuelLogDao.deleteFuelLog(log)

    // Expenses
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    fun getExpensesForVehicle(vehicleId: Int): Flow<List<Expense>> = expenseDao.getExpensesForVehicle(vehicleId)
    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    // Documents
    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()
    fun getDocumentsForVehicle(vehicleId: Int): Flow<List<Document>> = documentDao.getDocumentsForVehicle(vehicleId)
    suspend fun insertDocument(document: Document): Long = documentDao.insertDocument(document)
    suspend fun updateDocument(document: Document) = documentDao.updateDocument(document)
    suspend fun deleteDocument(document: Document) = documentDao.deleteDocument(document)

    // Reminders
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()
    val pendingReminders: Flow<List<Reminder>> = reminderDao.getPendingReminders()
    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
}
