package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleName: String,
    val vehicleNumber: String,
    val brand: String,
    val model: String,
    val vehicleType: String, // Bike, Car, Auto, Truck, Tractor
    val fuelType: String, // Petrol, Diesel, CNG, Electric, Hybrid
    val purchaseDate: Long,
    val insuranceExpiry: Long,
    val pollutionExpiry: Long,
    val vehicleImage: String? = null // local photo path or preset avatar icon
)

@Entity(tableName = "service_logs")
data class ServiceLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val serviceDate: Long,
    val odometerReading: Double,
    val serviceType: String, // General, Engine Oil, Repair, Body Wash, Brake/Clutch
    val serviceCenter: String,
    val cost: Double,
    val notes: String,
    val nextServiceDate: Long,
    val billPhoto: String? = null
)

@Entity(tableName = "fuel_logs")
data class FuelLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val fuelDate: Long,
    val litersFilled: Double,
    val pricePerLiter: Double,
    val totalAmount: Double,
    val odometerReading: Double,
    val fuelStationName: String
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val expenseDate: Long,
    val category: String, // Fuel, Repairs, Insurance, Washing, Accessories, Parking, Toll, Miscellaneous
    val amount: Double,
    val notes: String
)

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val docType: String, // RC, Insurance, Pollution Certificate, License, Service Bills
    val title: String,
    val expiryDate: Long?,
    val documentPath: String?, // local file uri or mock encrypted string
    val isEncrypted: Boolean = false
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int?,
    val title: String,
    val description: String,
    val reminderDate: Long,
    val isCompleted: Boolean = false,
    val category: String // Insurance, Pollution, Service, EMI, License
)
