package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ManaVahanaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class ManaVahanaViewModel(
    private val repository: ManaVahanaRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Onboarding and Security PIN Preferences
    val isOnboardingCompleted = preferencesRepository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val savedSecurityPin = preferencesRepository.securityPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPinLockEnabled = preferencesRepository.isPinLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isFingerprintEnabled = preferencesRepository.isFingerprintEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    // Verification state for current session
    private val _isPinVerified = MutableStateFlow(false)
    val isPinVerified: StateFlow<Boolean> = _isPinVerified.asStateFlow()

    fun verifyPin(enteredPin: String): Boolean {
        val correctPin = savedSecurityPin.value
        val isValid = correctPin == null || enteredPin == correctPin
        if (isValid) {
            _isPinVerified.value = true
        }
        return isValid
    }

    fun bypassPinVerification() {
        _isPinVerified.value = true
    }

    fun logout() {
        _isPinVerified.value = false
    }

    fun completeOnboarding(pin: String?) {
        viewModelScope.launch {
            if (!pin.isNullOrBlank()) {
                preferencesRepository.savePin(pin)
            }
            preferencesRepository.setOnboardingCompleted(true)
            _isPinVerified.value = true
        }
    }

    fun updatePin(newPin: String?) {
        viewModelScope.launch {
            if (newPin.isNullOrBlank()) {
                preferencesRepository.clearPin()
            } else {
                preferencesRepository.savePin(newPin)
            }
        }
    }

    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setFingerprintEnabled(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    // Selected vehicle state
    private val _selectedVehicleId = MutableStateFlow<Int?>(null)
    val selectedVehicleId: StateFlow<Int?> = _selectedVehicleId.asStateFlow()

    fun selectVehicle(id: Int?) {
        _selectedVehicleId.value = id
    }

    // Reactive Data Sources
    val vehicles = repository.allVehicles
        .onEach { list ->
            if (_selectedVehicleId.value == null && list.isNotEmpty()) {
                _selectedVehicleId.value = list.first().id
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicleId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getVehicleById(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allServiceLogs = repository.allServiceLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedVehicleServiceLogs: StateFlow<List<ServiceLog>> = _selectedVehicleId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getLogsForVehicle(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFuelLogs = repository.allFuelLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedVehicleFuelLogs: StateFlow<List<FuelLog>> = _selectedVehicleId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getFuelLogsForVehicle(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedVehicleExpenses: StateFlow<List<Expense>> = _selectedVehicleId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getExpensesForVehicle(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedVehicleDocuments: StateFlow<List<Document>> = _selectedVehicleId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getDocumentsForVehicle(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pendingReminders = repository.pendingReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database CRUD wrappers
    fun addVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            val id = repository.insertVehicle(vehicle)
            // Auto add an Expense log for the purchase if needed, or simply update selected ID
            if (_selectedVehicleId.value == null) {
                _selectedVehicleId.value = id.toInt()
            }
            // Auto add reminders for Insurance and Pollution expiries
            if (vehicle.insuranceExpiry > 0) {
                repository.insertReminder(
                    Reminder(
                        vehicleId = id.toInt(),
                        title = "${vehicle.vehicleName} - Insurance Renewal",
                        description = "Insurance is expiring. Renew with insurance provider.",
                        reminderDate = vehicle.insuranceExpiry - (2 * 24 * 60 * 60 * 1000L), // 2 days before
                        category = "Insurance"
                    )
                )
            }
            if (vehicle.pollutionExpiry > 0) {
                repository.insertReminder(
                    Reminder(
                        vehicleId = id.toInt(),
                        title = "${vehicle.vehicleName} - PUC Renewal",
                        description = "Pollution Certificate is expiring. Visit nearest emission center.",
                        reminderDate = vehicle.pollutionExpiry - (1 * 24 * 60 * 60 * 1000L), // 1 day before
                        category = "Pollution"
                    )
                )
            }
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle)
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            if (_selectedVehicleId.value == vehicle.id) {
                _selectedVehicleId.value = null
            }
        }
    }

    fun addServiceLog(log: ServiceLog) {
        viewModelScope.launch {
            repository.insertServiceLog(log)
            // Log expense as well
            if (log.cost > 0) {
                repository.insertExpense(
                    Expense(
                        vehicleId = log.vehicleId,
                        expenseDate = log.serviceDate,
                        category = "Repairs",
                        amount = log.cost,
                        notes = "Service log cost: ${log.serviceType} at ${log.serviceCenter}"
                    )
                )
            }
            // Log reminder for next service if specified
            if (log.nextServiceDate > 0) {
                repository.insertReminder(
                    Reminder(
                        vehicleId = log.vehicleId,
                        title = "Next Service Due",
                        description = "Upcoming scheduled service. Logged from previous service notes.",
                        reminderDate = log.nextServiceDate,
                        category = "Service"
                    )
                )
            }
        }
    }

    fun deleteServiceLog(log: ServiceLog) {
        viewModelScope.launch {
            repository.deleteServiceLog(log)
        }
    }

    fun updateServiceLog(log: ServiceLog) {
        viewModelScope.launch {
            repository.updateServiceLog(log)
        }
    }

    fun addFuelLog(log: FuelLog) {
        viewModelScope.launch {
            repository.insertFuelLog(log)
            // Auto-create corresponding Expense record
            repository.insertExpense(
                Expense(
                    vehicleId = log.vehicleId,
                    expenseDate = log.fuelDate,
                    category = "Fuel",
                    amount = log.totalAmount,
                    notes = "Filled ${String.format("%.2f", log.litersFilled)}L fuel at ${log.fuelStationName}"
                )
            )
        }
    }

    fun deleteFuelLog(log: FuelLog) {
        viewModelScope.launch {
            repository.deleteFuelLog(log)
        }
    }

    fun updateFuelLog(log: FuelLog) {
        viewModelScope.launch {
            repository.updateFuelLog(log)
        }
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun addDocument(document: Document) {
        viewModelScope.launch {
            repository.insertDocument(document)
            // Auto add reminder if an expiry date is set
            if (document.expiryDate != null && document.expiryDate > 0) {
                repository.insertReminder(
                    Reminder(
                        vehicleId = document.vehicleId,
                        title = "${document.title} Expiry",
                        description = "Document ${document.title} is expiring. Renew in time.",
                        reminderDate = document.expiryDate,
                        category = if (document.docType == "Insurance") "Insurance" else if (document.docType == "Pollution Certificate") "Pollution" else "License"
                    )
                )
            }
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            try {
                val list = repository.allReminders.firstOrNull() ?: emptyList()
                val match = list.find { it.vehicleId == document.vehicleId && (it.title == "${document.title} Expiry" || it.title.contains(document.title)) }
                if (match != null) {
                    repository.deleteReminder(match)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun updateDocument(document: Document) {
        viewModelScope.launch {
            repository.updateDocument(document)
            try {
                val list = repository.allReminders.firstOrNull() ?: emptyList()
                val match = list.find { it.vehicleId == document.vehicleId && (it.title == "${document.title} Expiry" || it.title.contains(document.title)) }
                if (match != null) {
                    repository.deleteReminder(match)
                }
            } catch (e: Exception) {
                // Ignore
            }
            if (document.expiryDate != null && document.expiryDate > 0) {
                repository.insertReminder(
                    Reminder(
                        vehicleId = document.vehicleId,
                        title = "${document.title} Expiry",
                        description = "Document ${document.title} is expiring. Renew in time.",
                        reminderDate = document.expiryDate,
                        category = if (document.docType == "Insurance") "Insurance" else if (document.docType == "Pollution Certificate") "Pollution" else "License"
                    )
                )
            }
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.insertReminder(reminder)
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
        }
    }

    fun toggleReminderCompleted(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // Advanced analytics
    // Double = Mileage efficiency for current selected vehicle
    val selectedVehicleMileage: Flow<Double> = selectedVehicleFuelLogs.map { logs ->
        if (logs.size < 2) return@map 0.0
        // Sort logs ascending by date/odometer
        val sortedLogs = logs.sortedBy { it.odometerReading }
        val earliestOdo = sortedLogs.first().odometerReading
        val latestOdo = sortedLogs.last().odometerReading
        val totalDistance = latestOdo - earliestOdo

        // Exclude the first log fill amount since we track differences or sum of the fills for the segment
        val totalFuelConsumed = sortedLogs.drop(1).sumOf { it.litersFilled }
        if (totalFuelConsumed > 0) totalDistance / totalFuelConsumed else 0.0
    }

    private fun encryptBackup(plainText: String): String {
        return try {
            val keyBytes = "M4n4Vah4naS3cur3".toByteArray(Charsets.UTF_8)
            val ivBytes = "M4n4Vah4naIv2026".toByteArray(Charsets.UTF_8)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            plainText
        }
    }

    private fun decryptBackup(encryptedText: String): String {
        val trimmed = encryptedText.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }
        return try {
            val keyBytes = "M4n4Vah4naS3cur3".toByteArray(Charsets.UTF_8)
            val ivBytes = "M4n4Vah4naIv2026".toByteArray(Charsets.UTF_8)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decoded = Base64.decode(trimmed, Base64.DEFAULT)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback to plain text if decryption fails (e.g., if already plaintext)
            encryptedText
        }
    }

    // Backup & Restore helpers
    private fun getLocalFileFromPath(context: android.content.Context, pathString: String?): java.io.File? {
        if (pathString.isNullOrBlank()) return null
        val cleanPath = if (pathString.startsWith("file://")) pathString.substring(7) else pathString
        val file = java.io.File(cleanPath)
        if (file.exists()) {
            return file
        }
        val fileName = cleanPath.substringAfterLast('/')
        val fallback = java.io.File(context.filesDir, fileName)
        if (fallback.exists()) {
            return fallback
        }
        return null
    }

    private fun fileToBase64(file: java.io.File?): String {
        if (file == null || !file.exists()) return ""
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun base64ToFile(context: android.content.Context, base64Str: String?, originalPath: String?): String? {
        if (base64Str.isNullOrBlank() || originalPath.isNullOrBlank()) return null
        return try {
            val cleanPath = if (originalPath.startsWith("file://")) originalPath.substring(7) else originalPath
            val fileName = cleanPath.substringAfterLast('/')
            if (fileName.isBlank()) return null
            val targetFile = java.io.File(context.filesDir, fileName)
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            java.io.FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
            }
            "file://${targetFile.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getResolutionFile(context: android.content.Context, pathString: String?): java.io.File? {
        if (pathString.isNullOrBlank()) return null
        val cleanPath = if (pathString.startsWith("file://")) pathString.substring(7) else pathString
        val fileOfCleanPath = java.io.File(cleanPath)
        if (fileOfCleanPath.exists()) {
            return fileOfCleanPath
        }
        val fileName = cleanPath.substringAfterLast('/')
        val fallbackFile = java.io.File(context.filesDir, fileName)
        if (fallbackFile.exists()) {
            return fallbackFile
        }
        return null
    }

    // Backup & Restore
    fun exportBackupJsonString(context: android.content.Context): String {
        val root = org.json.JSONObject()
        root.put("appName", "ManaVahana")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        
        val vehiclesArray = org.json.JSONArray()
        vehicles.value.forEach { v ->
            val item = org.json.JSONObject()
            item.put("id", v.id)
            item.put("vehicleName", v.vehicleName)
            item.put("vehicleNumber", v.vehicleNumber)
            item.put("brand", v.brand)
            item.put("model", v.model)
            item.put("vehicleType", v.vehicleType)
            item.put("fuelType", v.fuelType)
            item.put("purchaseDate", v.purchaseDate)
            item.put("insuranceExpiry", v.insuranceExpiry)
            item.put("pollutionExpiry", v.pollutionExpiry)
            item.put("vehicleImage", v.vehicleImage ?: "")
            if (!v.vehicleImage.isNullOrBlank()) {
                val f = getLocalFileFromPath(context, v.vehicleImage)
                if (f != null && f.exists()) {
                    item.put("vehicleImage_base64", fileToBase64(f))
                }
            }
            vehiclesArray.put(item)
        }
        root.put("vehicles", vehiclesArray)

        val serviceArray = org.json.JSONArray()
        allServiceLogs.value.forEach { s ->
            val item = org.json.JSONObject()
            item.put("id", s.id)
            item.put("vehicleId", s.vehicleId)
            item.put("serviceDate", s.serviceDate)
            item.put("odometerReading", s.odometerReading)
            item.put("serviceType", s.serviceType)
            item.put("serviceCenter", s.serviceCenter)
            item.put("cost", s.cost)
            item.put("notes", s.notes)
            item.put("nextServiceDate", s.nextServiceDate)
            item.put("billPhoto", s.billPhoto ?: "")
            if (!s.billPhoto.isNullOrBlank()) {
                val f = getLocalFileFromPath(context, s.billPhoto)
                if (f != null && f.exists()) {
                    item.put("billPhoto_base64", fileToBase64(f))
                }
            }
            serviceArray.put(item)
        }
        root.put("serviceLogs", serviceArray)

        val fuelArray = org.json.JSONArray()
        allFuelLogs.value.forEach { f ->
            val item = org.json.JSONObject()
            item.put("id", f.id)
            item.put("vehicleId", f.vehicleId)
            item.put("fuelDate", f.fuelDate)
            item.put("litersFilled", f.litersFilled)
            item.put("pricePerLiter", f.pricePerLiter)
            item.put("totalAmount", f.totalAmount)
            item.put("odometerReading", f.odometerReading)
            item.put("fuelStationName", f.fuelStationName)
            fuelArray.put(item)
        }
        root.put("fuelLogs", fuelArray)

        val expenseArray = org.json.JSONArray()
        allExpenses.value.forEach { e ->
            val item = org.json.JSONObject()
            item.put("id", e.id)
            item.put("vehicleId", e.vehicleId)
            item.put("expenseDate", e.expenseDate)
            item.put("category", e.category)
            item.put("amount", e.amount)
            item.put("notes", e.notes)
            expenseArray.put(item)
        }
        root.put("expenses", expenseArray)

        val docArray = org.json.JSONArray()
        allDocuments.value.forEach { d ->
            val item = org.json.JSONObject()
            item.put("id", d.id)
            item.put("vehicleId", d.vehicleId)
            item.put("docType", d.docType)
            item.put("title", d.title)
            item.put("expiryDate", d.expiryDate ?: 0L)
            item.put("documentPath", d.documentPath ?: "")
            if (!d.documentPath.isNullOrBlank()) {
                val f = getLocalFileFromPath(context, d.documentPath)
                if (f != null && f.exists()) {
                    item.put("documentPath_base64", fileToBase64(f))
                }
            }
            item.put("isEncrypted", d.isEncrypted)
            docArray.put(item)
        }
        root.put("documents", docArray)

        val reminderArray = org.json.JSONArray()
        allReminders.value.forEach { r ->
            val item = org.json.JSONObject()
            item.put("id", r.id)
            item.put("vehicleId", r.vehicleId ?: -1)
            item.put("title", r.title)
            item.put("description", r.description)
            item.put("reminderDate", r.reminderDate)
            item.put("isCompleted", r.isCompleted)
            item.put("category", r.category)
            reminderArray.put(item)
        }
        root.put("reminders", reminderArray)

        val plainJson = root.toString(2)
        return encryptBackup(plainJson)
    }

    fun importBackupJson(context: android.content.Context, jsonString: String): Boolean {
        return try {
            val decryptedJson = decryptBackup(jsonString.trim())
            val root = org.json.JSONObject(decryptedJson)
            if (!root.has("appName") || root.getString("appName") != "ManaVahana") {
                return false
            }
            
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // Clear existing lists to avoid duplicates
                    vehicles.value.forEach { repository.deleteVehicle(it) }
                    allServiceLogs.value.forEach { repository.deleteServiceLog(it) }
                    allFuelLogs.value.forEach { repository.deleteFuelLog(it) }
                    allExpenses.value.forEach { repository.deleteExpense(it) }
                    allDocuments.value.forEach { repository.deleteDocument(it) }
                    allReminders.value.forEach { repository.deleteReminder(it) }

                    val idMap = mutableMapOf<Int, Int>()

                    // 1. Insert Vehicles
                    val vehiclesArray = root.optJSONArray("vehicles")
                    if (vehiclesArray != null) {
                        for (i in 0 until vehiclesArray.length()) {
                            val obj = vehiclesArray.getJSONObject(i)
                            val oldId = obj.optInt("id", 0)
                            val originalImg = obj.optString("vehicleImage", "")
                            val base64Img = obj.optString("vehicleImage_base64", "")
                            var restoredImgPath = originalImg
                            if (base64Img.isNotEmpty()) {
                                val newPath = base64ToFile(context, base64Img, originalImg)
                                if (newPath != null) {
                                    restoredImgPath = newPath
                                }
                            } else if (originalImg.startsWith("file://") || originalImg.contains("/files/")) {
                                val resolvedFile = getResolutionFile(context, originalImg)
                                if (resolvedFile != null) {
                                    restoredImgPath = "file://${resolvedFile.absolutePath}"
                                }
                            }
                            val v = Vehicle(
                                id = 0,
                                vehicleName = obj.optString("vehicleName", ""),
                                vehicleNumber = obj.optString("vehicleNumber", ""),
                                brand = obj.optString("brand", ""),
                                model = obj.optString("model", ""),
                                vehicleType = obj.optString("vehicleType", "Car"),
                                fuelType = obj.optString("fuelType", "Petrol"),
                                purchaseDate = obj.optLong("purchaseDate", 0L),
                                insuranceExpiry = obj.optLong("insuranceExpiry", 0L),
                                pollutionExpiry = obj.optLong("pollutionExpiry", 0L),
                                vehicleImage = if (restoredImgPath.isEmpty()) null else restoredImgPath
                            )
                            val newId = repository.insertVehicle(v).toInt()
                            if (oldId > 0) {
                                idMap[oldId] = newId
                            }
                        }
                    }

                    // 2. Insert Service Logs
                    val serviceArray = root.optJSONArray("serviceLogs")
                    if (serviceArray != null) {
                        for (i in 0 until serviceArray.length()) {
                            val obj = serviceArray.getJSONObject(i)
                            val oldVehicleId = obj.optInt("vehicleId", 0)
                            val newVehicleId = idMap[oldVehicleId] ?: oldVehicleId
                            val originalBill = obj.optString("billPhoto", "")
                            val base64Bill = obj.optString("billPhoto_base64", "")
                            var restoredBillPath = originalBill
                            if (base64Bill.isNotEmpty()) {
                                val newPath = base64ToFile(context, base64Bill, originalBill)
                                if (newPath != null) {
                                    restoredBillPath = newPath
                                }
                            } else if (originalBill.startsWith("file://") || originalBill.contains("/files/")) {
                                val resolvedFile = getResolutionFile(context, originalBill)
                                if (resolvedFile != null) {
                                    restoredBillPath = "file://${resolvedFile.absolutePath}"
                                }
                            }
                            val s = ServiceLog(
                                id = 0,
                                vehicleId = newVehicleId,
                                serviceDate = obj.optLong("serviceDate", 0L),
                                odometerReading = obj.optDouble("odometerReading", 0.0),
                                serviceType = obj.optString("serviceType", "General"),
                                serviceCenter = obj.optString("serviceCenter", ""),
                                cost = obj.optDouble("cost", 0.0),
                                notes = obj.optString("notes", ""),
                                nextServiceDate = obj.optLong("nextServiceDate", 0L),
                                billPhoto = if (restoredBillPath.isEmpty()) null else restoredBillPath
                            )
                            repository.insertServiceLog(s)
                        }
                    }

                    // 3. Insert Fuel Logs
                    val fuelArray = root.optJSONArray("fuelLogs")
                    if (fuelArray != null) {
                        for (i in 0 until fuelArray.length()) {
                            val obj = fuelArray.getJSONObject(i)
                            val oldVehicleId = obj.optInt("vehicleId", 0)
                            val newVehicleId = idMap[oldVehicleId] ?: oldVehicleId
                            val f = FuelLog(
                                id = 0,
                                vehicleId = newVehicleId,
                                fuelDate = obj.optLong("fuelDate", 0L),
                                litersFilled = obj.optDouble("litersFilled", 0.0),
                                pricePerLiter = obj.optDouble("pricePerLiter", 0.0),
                                totalAmount = obj.optDouble("totalAmount", 0.0),
                                odometerReading = obj.optDouble("odometerReading", 0.0),
                                fuelStationName = obj.optString("fuelStationName", "")
                            )
                            repository.insertFuelLog(f)
                        }
                    }

                    // 4. Insert Expenses
                    val expenseArray = root.optJSONArray("expenses")
                    if (expenseArray != null) {
                        for (i in 0 until expenseArray.length()) {
                            val obj = expenseArray.getJSONObject(i)
                            val oldVehicleId = obj.optInt("vehicleId", 0)
                            val newVehicleId = idMap[oldVehicleId] ?: oldVehicleId
                            val e = Expense(
                                id = 0,
                                vehicleId = newVehicleId,
                                expenseDate = obj.optLong("expenseDate", 0L),
                                category = obj.optString("category", "General"),
                                amount = obj.optDouble("amount", 0.0),
                                notes = obj.optString("notes", "")
                            )
                            repository.insertExpense(e)
                        }
                    }

                    // 5. Insert Documents
                    val docArray = root.optJSONArray("documents")
                    if (docArray != null) {
                        for (i in 0 until docArray.length()) {
                            val obj = docArray.getJSONObject(i)
                            val oldVehicleId = obj.optInt("vehicleId", 0)
                            val newVehicleId = idMap[oldVehicleId] ?: oldVehicleId
                            val originalDoc = obj.optString("documentPath", "")
                            val base64Doc = obj.optString("documentPath_base64", "")
                            var restoredDocPath = originalDoc
                            if (base64Doc.isNotEmpty()) {
                                val newPath = base64ToFile(context, base64Doc, originalDoc)
                                if (newPath != null) {
                                    restoredDocPath = newPath
                                }
                            } else if (originalDoc.startsWith("file://") || originalDoc.contains("/files/")) {
                                val resolvedFile = getResolutionFile(context, originalDoc)
                                if (resolvedFile != null) {
                                    restoredDocPath = "file://${resolvedFile.absolutePath}"
                                }
                            }
                            val d = Document(
                                id = 0,
                                vehicleId = newVehicleId,
                                docType = obj.optString("docType", "RC"),
                                title = obj.optString("title", ""),
                                expiryDate = obj.optLong("expiryDate", 0L).let { if (it == 0L) null else it },
                                documentPath = if (restoredDocPath.isEmpty()) null else restoredDocPath,
                                isEncrypted = obj.optBoolean("isEncrypted", false)
                            )
                            repository.insertDocument(d)
                        }
                    }

                    // 6. Insert Reminders
                    val reminderArray = root.optJSONArray("reminders")
                    if (reminderArray != null) {
                        for (i in 0 until reminderArray.length()) {
                            val obj = reminderArray.getJSONObject(i)
                            val oldVehicleId = obj.optInt("vehicleId", -1)
                            val newVehicleId = if (oldVehicleId != -1) (idMap[oldVehicleId] ?: oldVehicleId) else null
                            val r = Reminder(
                                id = 0,
                                vehicleId = newVehicleId,
                                title = obj.optString("title", ""),
                                description = obj.optString("description", ""),
                                reminderDate = obj.optLong("reminderDate", 0L),
                                isCompleted = obj.optBoolean("isCompleted", false),
                                category = obj.optString("category", "General")
                            )
                            repository.insertReminder(r)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

class ManaVahanaViewModelFactory(
    private val repository: ManaVahanaRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManaVahanaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManaVahanaViewModel(repository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
