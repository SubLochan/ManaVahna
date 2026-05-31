package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.ManaVahanaViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri, prefix: String = "file"): String? {
    return try {
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(uri) ?: return null
        val type = resolver.getType(uri)
        val ext = when {
            type != null && type.contains("pdf", ignoreCase = true) -> "pdf"
            type != null && type.contains("png", ignoreCase = true) -> "png"
            type != null && type.contains("jpeg", ignoreCase = true) -> "jpg"
            type != null && type.contains("jpg", ignoreCase = true) -> "jpg"
            else -> {
                val lastSeg = uri.lastPathSegment ?: ""
                val dotIdx = lastSeg.lastIndexOf('.')
                if (dotIdx != -1 && dotIdx < lastSeg.length - 1) {
                    lastSeg.substring(dotIdx + 1)
                } else {
                    "jpg"
                }
            }
        }
        val file = java.io.File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        val outputStream = java.io.FileOutputStream(file)
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
        "file://${file.absolutePath}"
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(
    viewModel: ManaVahanaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var vehicleImage by remember { mutableStateOf<String?>(null) }

    val vehicleTypes = listOf("Bike", "Car", "Auto", "Truck")
    var selectedType by remember { mutableStateOf("Car") }

    val fuelTypes = listOf("Petrol", "Diesel", "CNG", "Electric", "Hybrid")
    var selectedFuel by remember { mutableStateOf("Petrol") }

    var purchaseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var insuranceExpiry by remember { mutableStateOf(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000) }
    var pollutionExpiry by remember { mutableStateOf(System.currentTimeMillis() + 180L * 24 * 60 * 60 * 1000) }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val localPath = copyUriToInternalStorage(context, uri, "vehicle_img")
                if (localPath != null) {
                    vehicleImage = localPath
                } else {
                    vehicleImage = uri.toString()
                }
            }
        }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("add_vehicle_screen")
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Text(
                "వాహనాన్ని చేర్చండి (Add Vehicle)",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Save your motor vehicle credentials offline with high privacy",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Circular Image Selector
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (vehicleImage != null) {
                        AsyncImage(
                            model = PathUtils.getResolutionFile(context, vehicleImage) ?: vehicleImage,
                            contentDescription = "Vehicle Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "App Icon Placeholder",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Add Photo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select vehicle photo", fontSize = 13.sp)
                }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Vehicle Nickname (e.g., My Bullet, Red Swift)") },
                modifier = Modifier.fillMaxWidth().testTag("vehicle_name_input"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        }

        item {
            OutlinedTextField(
                value = number,
                onValueChange = { number = it.uppercase() },
                label = { Text("Vehicle registration Number (e.g., AP39UZ1234)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            }
        }

        item {
            Column {
                Text("Select Vehicle Type", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    for (type in vehicleTypes) {
                        val isSel = type == selectedType
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedType = type },
                            label = { Text(type) },
                            leadingIcon = { Icon(getVehicleIcon(type), contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        item {
            Column {
                Text("Select Fuel Type", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    for (fuel in fuelTypes) {
                        val isSel = fuel == selectedFuel
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedFuel = fuel },
                            label = { Text(fuel) }
                        )
                    }
                }
            }
        }

        // Date select dialog actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DatePickerField(
                    label = "Purchase Date",
                    timestamp = purchaseDate,
                    onDateSelected = { purchaseDate = it },
                    context = context,
                    sdf = sdf
                )
                DatePickerField(
                    label = "Insurance Expiry",
                    timestamp = insuranceExpiry,
                    onDateSelected = { insuranceExpiry = it },
                    context = context,
                    sdf = sdf
                )
                DatePickerField(
                    label = "Pollution Certificate (PUC) Expiry",
                    timestamp = pollutionExpiry,
                    onDateSelected = { pollutionExpiry = it },
                    context = context,
                    sdf = sdf
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (name.isNotBlank() && number.isNotBlank()) {
                        viewModel.addVehicle(
                            Vehicle(
                                vehicleName = name,
                                vehicleNumber = number,
                                brand = brand,
                                model = model,
                                vehicleType = selectedType,
                                fuelType = selectedFuel,
                                purchaseDate = purchaseDate,
                                insuranceExpiry = insuranceExpiry,
                                pollutionExpiry = pollutionExpiry,
                                vehicleImage = vehicleImage
                            )
                        )
                        onNavigateBack()
                    }
                },
                enabled = name.isNotBlank() && number.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_vehicle_button")
            ) {
                Text("Save Vehicle Record", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
}

@Composable
fun DatePickerField(
    label: String,
    timestamp: Long,
    onDateSelected: (Long) -> Unit,
    context: android.content.Context,
    sdf: SimpleDateFormat
) {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    OutlinedTextField(
        value = sdf.format(Date(timestamp)),
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        }
                        onDateSelected(selectedCal.timeInMillis)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
        trailingIcon = {
            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        }
                        onDateSelected(selectedCal.timeInMillis)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            })
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    viewModel: ManaVahanaViewModel,
    vehicleId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(vehicleId) {
        viewModel.selectVehicle(vehicleId)
    }

    val vehicle by viewModel.selectedVehicle.collectAsState()
    val services by viewModel.selectedVehicleServiceLogs.collectAsState()
    val fuels by viewModel.selectedVehicleFuelLogs.collectAsState()
    val expenses by viewModel.selectedVehicleExpenses.collectAsState()
    val documents by viewModel.selectedVehicleDocuments.collectAsState()

    var activeTab by remember { mutableStateOf(0) }

    val tabs = listOf("Profile", "Services", "Fuel Logs", "Expenses", "Vault")

    // Edit Vehicle Dialog states:
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editNumber by remember { mutableStateOf("") }
    var editBrand by remember { mutableStateOf("") }
    var editModel by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }
    var editFuel by remember { mutableStateOf("") }
    var editPurchaseDate by remember { mutableStateOf(0L) }
    var editInsuranceExpiry by remember { mutableStateOf(0L) }
    var editPollutionExpiry by remember { mutableStateOf(0L) }
    var editImage by remember { mutableStateOf<String?>(null) }

    // CRUD state variables for sub-items
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var editingServiceLog by remember { mutableStateOf<ServiceLog?>(null) }
    var serviceDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var serviceOdometer by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("General") }
    var serviceCenter by remember { mutableStateOf("") }
    var serviceCost by remember { mutableStateOf("") }
    var serviceNotes by remember { mutableStateOf("") }
    var nextServiceDate by remember { mutableStateOf(0L) }

    var showAddFuelDialog by remember { mutableStateOf(false) }
    var editingFuelLog by remember { mutableStateOf<FuelLog?>(null) }
    var fuelDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var fuelLiters by remember { mutableStateOf("") }
    var fuelPricePerLiter by remember { mutableStateOf("") }
    var fuelTotalAmount by remember { mutableStateOf("") }
    var fuelOdometer by remember { mutableStateOf("") }
    var fuelStationName by remember { mutableStateOf("") }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var expenseCategory by remember { mutableStateOf("Fuel") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseNotes by remember { mutableStateOf("") }

    var showAddDocumentDialog by remember { mutableStateOf(false) }
    var editingDocument by remember { mutableStateOf<Document?>(null) }
    var docType by remember { mutableStateOf("RC") }
    var docTitle by remember { mutableStateOf("") }
    var docExpiryDate by remember { mutableStateOf(0L) }
    var docDocumentPath by remember { mutableStateOf<String?>(null) }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun resetServiceFields() {
        editingServiceLog = null
        serviceDate = System.currentTimeMillis()
        serviceOdometer = ""
        serviceType = "General"
        serviceCenter = ""
        serviceCost = ""
        serviceNotes = ""
        nextServiceDate = 0L
    }

    fun resetFuelFields() {
        editingFuelLog = null
        fuelDate = System.currentTimeMillis()
        fuelLiters = ""
        fuelPricePerLiter = ""
        fuelTotalAmount = ""
        fuelOdometer = ""
        fuelStationName = ""
    }

    fun resetExpenseFields() {
        editingExpense = null
        expenseDate = System.currentTimeMillis()
        expenseCategory = "Fuel"
        expenseAmount = ""
        expenseNotes = ""
    }

    fun resetDocumentFields() {
        editingDocument = null
        docType = "RC"
        docTitle = ""
        docExpiryDate = 0L
        docDocumentPath = null
    }

    // Picker launcher for Editing:
    val editPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val localPath = copyUriToInternalStorage(context, uri, "vehicle_img")
                if (localPath != null) {
                    editImage = localPath
                } else {
                    editImage = uri.toString()
                }
            }
        }
    )

    val docLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val localPath = copyUriToInternalStorage(context, uri, "doc")
            if (localPath != null) {
                docDocumentPath = localPath
            } else {
                docDocumentPath = uri.toString()
            }
            if (docTitle.isBlank()) {
                docTitle = "$docType Document"
            }
        }
    }

    if (vehicle == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val activeVehicle = vehicle!!

        // Dialogs for additions / edits
        if (showAddServiceDialog) {
            val titleText = if (editingServiceLog != null) "Edit Service Detail" else "Log Service Detail"
            AlertDialog(
                onDismissRequest = {
                    showAddServiceDialog = false
                    resetServiceFields()
                },
                title = { Text(titleText) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            OutlinedTextField(
                                value = serviceType,
                                onValueChange = { serviceType = it },
                                label = { Text("Service Type (e.g., Oil change, Wash)") },
                                modifier = Modifier.fillMaxWidth().testTag("service_type_input")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = serviceCenter,
                                onValueChange = { serviceCenter = it },
                                label = { Text("Service Center Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = serviceOdometer,
                                onValueChange = { serviceOdometer = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Odometer Reading (km)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = serviceCost,
                                onValueChange = { serviceCost = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                label = { Text("Total Service Cost (₹)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = serviceNotes,
                                onValueChange = { serviceNotes = it },
                                maxLines = 3,
                                label = { Text("Additional Notes") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            val displayNext = if (nextServiceDate > 0) sdf.format(Date(nextServiceDate)) else "Not set"
                            OutlinedTextField(
                                value = displayNext,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Next Recall Date") },
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val c = Calendar.getInstance()
                                    DatePickerDialog(context, { _, y, m, d ->
                                        val sel = Calendar.getInstance().apply { set(y, m, d) }
                                        nextServiceDate = sel.timeInMillis
                                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (serviceType.isNotBlank()) {
                                val newLog = ServiceLog(
                                    id = editingServiceLog?.id ?: 0,
                                    vehicleId = activeVehicle.id,
                                    serviceDate = serviceDate,
                                    odometerReading = serviceOdometer.trim().toDoubleOrNull() ?: 0.0,
                                    serviceType = serviceType,
                                    serviceCenter = serviceCenter,
                                    cost = serviceCost.trim().toDoubleOrNull() ?: 0.0,
                                    notes = serviceNotes,
                                    nextServiceDate = nextServiceDate
                                )
                                if (editingServiceLog != null) {
                                    viewModel.updateServiceLog(newLog)
                                } else {
                                    viewModel.addServiceLog(newLog)
                                }
                                showAddServiceDialog = false
                                resetServiceFields()
                            }
                        }
                    ) {
                        Text("Save Log")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddServiceDialog = false
                        resetServiceFields()
                    }) { Text("Cancel") }
                }
            )
        }

        if (showAddFuelDialog) {
            val titleText = if (editingFuelLog != null) "Edit Fuel Fill" else "Log Fuel Fill"
            AlertDialog(
                onDismissRequest = {
                    showAddFuelDialog = false
                    resetFuelFields()
                },
                title = { Text(titleText) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            OutlinedTextField(
                                value = fuelLiters,
                                onValueChange = {
                                    fuelLiters = it
                                    val price = fuelPricePerLiter.toDoubleOrNull() ?: 0.0
                                    val liters = it.toDoubleOrNull() ?: 0.0
                                    if (price > 0 && liters > 0) {
                                        fuelTotalAmount = String.format(Locale.US, "%.2f", price * liters)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                label = { Text("Liters Filled") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = fuelPricePerLiter,
                                onValueChange = {
                                    fuelPricePerLiter = it
                                    val price = it.toDoubleOrNull() ?: 0.0
                                    val total = fuelTotalAmount.toDoubleOrNull() ?: 0.0
                                    val liters = fuelLiters.toDoubleOrNull() ?: 0.0
                                    if (price > 0 && total > 0) {
                                        fuelLiters = String.format(Locale.US, "%.2f", total / price)
                                    } else if (price > 0 && liters > 0) {
                                        fuelTotalAmount = String.format(Locale.US, "%.2f", price * liters)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                label = { Text("Price Per Liter (₹)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = fuelTotalAmount,
                                onValueChange = {
                                    fuelTotalAmount = it
                                    val total = it.toDoubleOrNull() ?: 0.0
                                    val price = fuelPricePerLiter.toDoubleOrNull() ?: 0.0
                                    if (price > 0 && total > 0) {
                                        fuelLiters = String.format(Locale.US, "%.2f", total / price)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                label = { Text("Total Cost (₹)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = fuelOdometer,
                                onValueChange = { fuelOdometer = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Odometer (km)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = fuelStationName,
                                onValueChange = { fuelStationName = it },
                                label = { Text("Fuel Center / Station") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            val displayDate = sdf.format(Date(fuelDate))
                            OutlinedTextField(
                                value = displayDate,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Date") },
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val c = Calendar.getInstance()
                                    DatePickerDialog(context, { _, y, m, d ->
                                        val sel = Calendar.getInstance().apply { set(y, m, d) }
                                        fuelDate = sel.timeInMillis
                                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (fuelLiters.isNotBlank() && fuelTotalAmount.isNotBlank() && fuelOdometer.isNotBlank()) {
                                val newFuelLog = FuelLog(
                                    id = editingFuelLog?.id ?: 0,
                                    vehicleId = activeVehicle.id,
                                    fuelDate = fuelDate,
                                    litersFilled = fuelLiters.toDoubleOrNull() ?: 0.0,
                                    pricePerLiter = fuelPricePerLiter.toDoubleOrNull() ?: 0.0,
                                    totalAmount = fuelTotalAmount.toDoubleOrNull() ?: 0.0,
                                    odometerReading = fuelOdometer.toDoubleOrNull() ?: 0.0,
                                    fuelStationName = fuelStationName.ifBlank { "Local Pump" }
                                )
                                if (editingFuelLog != null) {
                                    viewModel.updateFuelLog(newFuelLog)
                                } else {
                                    viewModel.addFuelLog(newFuelLog)
                                }
                                showAddFuelDialog = false
                                resetFuelFields()
                            }
                        }
                    ) {
                        Text("Save Fuel Log")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddFuelDialog = false
                        resetFuelFields()
                    }) { Text("Cancel") }
                }
            )
        }

        if (showAddExpenseDialog) {
            val titleText = if (editingExpense != null) "Edit Expense Record" else "Add Expense Record"
            AlertDialog(
                onDismissRequest = {
                    showAddExpenseDialog = false
                    resetExpenseFields()
                },
                title = { Text(titleText) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            var expandedCat by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expandedCat = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Category: $expenseCategory")
                                }
                                DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                                    listOf("Fuel", "Repairs", "Insurance", "Washing", "Accessories", "Parking", "Toll", "Miscellaneous").forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                expenseCategory = cat
                                                expandedCat = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = expenseAmount,
                                onValueChange = { expenseAmount = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                label = { Text("Amount (₹)") },
                                modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = expenseNotes,
                                onValueChange = { expenseNotes = it },
                                label = { Text("Notes / Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (expenseAmount.isNotBlank()) {
                                val newExpense = Expense(
                                    id = editingExpense?.id ?: 0,
                                    vehicleId = activeVehicle.id,
                                    expenseDate = expenseDate,
                                    category = expenseCategory,
                                    amount = expenseAmount.toDoubleOrNull() ?: 0.0,
                                    notes = expenseNotes.ifBlank { "General $expenseCategory expense" }
                                )
                                if (editingExpense != null) {
                                    viewModel.updateExpense(newExpense)
                                } else {
                                    viewModel.addExpense(newExpense)
                                }
                                showAddExpenseDialog = false
                                resetExpenseFields()
                            }
                        }
                    ) {
                        Text("Save Expense")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddExpenseDialog = false
                        resetExpenseFields()
                    }) { Text("Cancel") }
                }
            )
        }

        if (showAddDocumentDialog) {
            val titleText = if (editingDocument != null) "Edit Document Securely" else "Store Document Securely"
            AlertDialog(
                onDismissRequest = {
                    showAddDocumentDialog = false
                    resetDocumentFields()
                },
                title = { Text(titleText) },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            var expandedDoc by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expandedDoc = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Doc Type: $docType")
                                }
                                DropdownMenu(expanded = expandedDoc, onDismissRequest = { expandedDoc = false }) {
                                    listOf("RC", "Insurance", "Pollution Certificate", "License", "Service Bills").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                docType = type
                                                expandedDoc = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = docTitle,
                                onValueChange = { docTitle = it },
                                label = { Text("Document Title (e.g., My RC Copy)") },
                                modifier = Modifier.fillMaxWidth().testTag("document_title_input")
                            )
                        }
                        item {
                            val displayExpiry = if (docExpiryDate > 0) sdf.format(Date(docExpiryDate)) else "No Expiry / Lifetime"
                            OutlinedTextField(
                                value = displayExpiry,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Expiry Date") },
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val c = Calendar.getInstance()
                                    DatePickerDialog(context, { _, y, m, d ->
                                        val sel = Calendar.getInstance().apply { set(y, m, d) }
                                        docExpiryDate = sel.timeInMillis
                                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                }
                            )
                        }
                        item {
                            Button(
                                onClick = { docLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (docDocumentPath != null) "File: ${docDocumentPath?.substringAfterLast("/")} 👍" else "Select PDF or Image File")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (docTitle.isNotBlank()) {
                                val newDoc = Document(
                                    id = editingDocument?.id ?: 0,
                                    vehicleId = activeVehicle.id,
                                    docType = docType,
                                    title = docTitle,
                                    expiryDate = if (docExpiryDate > 0) docExpiryDate else null,
                                    documentPath = docDocumentPath ?: "content://media/external_mock_path.pdf",
                                    isEncrypted = true
                                )
                                if (editingDocument != null) {
                                    viewModel.updateDocument(newDoc)
                                } else {
                                    viewModel.addDocument(newDoc)
                                }
                                showAddDocumentDialog = false
                                resetDocumentFields()
                            }
                        }
                    ) {
                        Text("Encrypt and Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDocumentDialog = false
                        resetDocumentFields()
                    }) { Text("Cancel") }
                }
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                if (activeTab > 0) {
                    FloatingActionButton(
                        onClick = {
                            when (activeTab) {
                                1 -> {
                                    resetServiceFields()
                                    showAddServiceDialog = true
                                }
                                2 -> {
                                    resetFuelFields()
                                    showAddFuelDialog = true
                                }
                                3 -> {
                                    resetExpenseFields()
                                    showAddExpenseDialog = true
                                }
                                4 -> {
                                    resetDocumentFields()
                                    showAddDocumentDialog = true
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_subitem_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add log")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .testTag("vehicle_details_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Header: Vehicle Name and registration
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (!activeVehicle.vehicleImage.isNullOrBlank()) {
                                        AsyncImage(
                                            model = PathUtils.getResolutionFile(context, activeVehicle.vehicleImage) ?: activeVehicle.vehicleImage,
                                            contentDescription = "Vehicle Profile Photo",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            getVehicleIcon(activeVehicle.vehicleType),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(activeVehicle.vehicleName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text(activeVehicle.vehicleNumber, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    editName = activeVehicle.vehicleName
                                    editNumber = activeVehicle.vehicleNumber
                                    editBrand = activeVehicle.brand
                                    editModel = activeVehicle.model
                                    editType = activeVehicle.vehicleType
                                    editFuel = activeVehicle.fuelType
                                    editPurchaseDate = activeVehicle.purchaseDate
                                    editInsuranceExpiry = activeVehicle.insuranceExpiry
                                    editPollutionExpiry = activeVehicle.pollutionExpiry
                                    editImage = activeVehicle.vehicleImage
                                    showEditDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Vehicle details", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteVehicle(activeVehicle)
                                    onNavigateBack()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Vehicle", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Tab selectors
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEachIndexed { idx, title ->
                            val isSel = activeTab == idx
                            val colCont = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            val colText = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            Card(
                                onClick = { activeTab = idx },
                                colors = CardDefaults.cardColors(containerColor = colCont),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = title,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = colText
                                )
                            }
                        }
                    }
                }

                // Tab Content delegation
                when (activeTab) {
                    0 -> { // Profile / Info tab
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (!activeVehicle.vehicleImage.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = PathUtils.getResolutionFile(context, activeVehicle.vehicleImage) ?: activeVehicle.vehicleImage,
                                                contentDescription = "Vehicle Portrait",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        }
                                    }

                                    ProfileRow(label = "Brand / Model", value = "${activeVehicle.brand} ${activeVehicle.model}")
                                    ProfileRow(label = "Vehicle Type", value = activeVehicle.vehicleType)
                                    ProfileRow(label = "Fuel Type", value = activeVehicle.fuelType)
                                    
                                    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    ProfileRow(label = "Purchase Date", value = format.format(Date(activeVehicle.purchaseDate)))
                                    ProfileRow(label = "Insurance Renewal Date", value = format.format(Date(activeVehicle.insuranceExpiry)))
                                    ProfileRow(label = "Pollution Certificate Expiry", value = format.format(Date(activeVehicle.pollutionExpiry)))
                                }
                            }
                        }
                    }
                    1 -> { // Custom vehicle services
                        if (services.isEmpty()) {
                            item { EmptyTabMessage("No service history logged.") }
                        } else {
                            items(services) { service ->
                                ServiceLogItem(
                                    log = service,
                                    onEdit = {
                                        editingServiceLog = service
                                        serviceDate = service.serviceDate
                                        serviceOdometer = service.odometerReading.toString()
                                        serviceType = service.serviceType
                                        serviceCenter = service.serviceCenter
                                        serviceCost = service.cost.toString()
                                        serviceNotes = service.notes
                                        nextServiceDate = service.nextServiceDate
                                        showAddServiceDialog = true
                                    },
                                    onDelete = { viewModel.deleteServiceLog(service) }
                                )
                            }
                        }
                    }
                    2 -> { // Custom vehicle fuel logs
                        if (fuels.isEmpty()) {
                            item { EmptyTabMessage("No fuel logs recorded.") }
                        } else {
                            val sortedFuels = fuels.sortedBy { it.odometerReading }
                            items(fuels) { fuel ->
                                val idx = sortedFuels.indexOf(fuel)
                                val mileage = if (idx > 0) {
                                    val prev = sortedFuels[idx - 1]
                                    val dist = fuel.odometerReading - prev.odometerReading
                                    if (fuel.litersFilled > 0) dist / fuel.litersFilled else null
                                } else null
                                FuelLogItem(
                                    log = fuel,
                                    mileage = mileage,
                                    onEdit = {
                                        editingFuelLog = fuel
                                        fuelDate = fuel.fuelDate
                                        fuelLiters = fuel.litersFilled.toString()
                                        fuelPricePerLiter = fuel.pricePerLiter.toString()
                                        fuelTotalAmount = fuel.totalAmount.toString()
                                        fuelOdometer = fuel.odometerReading.toString()
                                        fuelStationName = fuel.fuelStationName
                                        showAddFuelDialog = true
                                    },
                                    onDelete = { viewModel.deleteFuelLog(fuel) }
                                )
                            }
                        }
                    }
                    3 -> { // Custom vehicle expenses
                        if (expenses.isEmpty()) {
                            item { EmptyTabMessage("No expenses recorded yet.") }
                        } else {
                            items(expenses) { exp ->
                                ExpenseItem(
                                    expense = exp,
                                    onEdit = {
                                        editingExpense = exp
                                        expenseDate = exp.expenseDate
                                        expenseCategory = exp.category
                                        expenseAmount = exp.amount.toString()
                                        expenseNotes = exp.notes
                                        showAddExpenseDialog = true
                                    },
                                    onDelete = { viewModel.deleteExpense(exp) }
                                )
                            }
                        }
                    }
                    4 -> { // Document Vault files
                        if (documents.isEmpty()) {
                            item { EmptyTabMessage("No document cards stored.") }
                        } else {
                            items(documents) { doc ->
                                DocumentItem(
                                    doc = doc,
                                    onEdit = {
                                        editingDocument = doc
                                        docType = doc.docType
                                        docTitle = doc.title
                                        docExpiryDate = doc.expiryDate ?: 0L
                                        docDocumentPath = doc.documentPath
                                        showAddDocumentDialog = true
                                    },
                                    onDelete = { viewModel.deleteDocument(doc) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Edit Vehicle Overlay Dialog
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Vehicle Specifications") },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Image picker inside edit form
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable {
                                            editPhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (editImage != null) {
                                        AsyncImage(
                                            model = PathUtils.getResolutionFile(context, editImage) ?: editImage,
                                            contentDescription = "Edit Vehicle",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = "Logo",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        editPhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Text("Change Photo", fontSize = 12.sp)
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Vehicle Nickname") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = editNumber,
                                onValueChange = { editNumber = it.uppercase() },
                                label = { Text("Registration Number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = editBrand,
                                onValueChange = { editBrand = it },
                                label = { Text("Brand") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = editModel,
                                onValueChange = { editModel = it },
                                label = { Text("Model Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        item {
                            Column {
                                Text("Vehicle Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    listOf("Bike", "Car", "Auto", "Truck").forEach { ty ->
                                        FilterChip(
                                            selected = ty == editType,
                                            onClick = { editType = ty },
                                            label = { Text(ty) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column {
                                Text("Fuel Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    listOf("Petrol", "Diesel", "CNG", "Electric", "Hybrid").forEach { fu ->
                                        FilterChip(
                                            selected = fu == editFuel,
                                            onClick = { editFuel = fu },
                                            label = { Text(fu) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            DatePickerField(
                                label = "Purchase Date",
                                timestamp = editPurchaseDate,
                                onDateSelected = { editPurchaseDate = it },
                                context = context,
                                sdf = sdf
                            )
                        }

                        item {
                            DatePickerField(
                                label = "Insurance Expiry",
                                timestamp = editInsuranceExpiry,
                                onDateSelected = { editInsuranceExpiry = it },
                                context = context,
                                sdf = sdf
                            )
                        }

                        item {
                            DatePickerField(
                                label = "PUC Expiry",
                                timestamp = editPollutionExpiry,
                                onDateSelected = { editPollutionExpiry = it },
                                context = context,
                                sdf = sdf
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editName.isNotBlank() && editNumber.isNotBlank()) {
                                val updatedVeh = activeVehicle.copy(
                                    vehicleName = editName,
                                    vehicleNumber = editNumber,
                                    brand = editBrand,
                                    model = editModel,
                                    vehicleType = editType,
                                    fuelType = editFuel,
                                    purchaseDate = editPurchaseDate,
                                    insuranceExpiry = editInsuranceExpiry,
                                    pollutionExpiry = editPollutionExpiry,
                                    vehicleImage = editImage
                                )
                                viewModel.updateVehicle(updatedVeh)
                                viewModel.selectVehicle(activeVehicle.id) // Refresh details
                                showEditDialog = false
                            }
                        }
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyTabMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
        }
    }
}

@Composable
fun ServiceLogItem(log: ServiceLog, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(log.serviceType, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Center: ${log.serviceCenter}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("Odo: ${log.odometerReading} km | Notes: ${log.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Logged: ${sdf.format(Date(log.serviceDate))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${log.cost}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Log", Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Log", Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FuelLogItem(log: FuelLog, mileage: Double? = null, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Fuel Fill | ${log.litersFilled} Liters", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Station: ${log.fuelStationName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("Price/L: ₹${log.pricePerLiter} | Odo: ${log.odometerReading} km", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                if (mileage != null && mileage > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("మైలేజ్ (Mileage): ${String.format("%.2f", mileage)} km/L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Logged: ${sdf.format(Date(log.fuelDate))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${log.totalAmount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Log", Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Log", Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.category, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                Text(expense.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(sdf.format(Date(expense.expenseDate)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹${expense.amount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Log", Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Log", Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun DocumentItem(doc: Document, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Topic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text("Category: ${doc.docType}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                if (doc.expiryDate != null && doc.expiryDate > 0) {
                    Text("Expires on: ${sdf.format(Date(doc.expiryDate))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!doc.documentPath.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val parsedUri = android.net.Uri.parse(doc.documentPath)
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(parsedUri, context.contentResolver.getType(parsedUri) ?: "*/*")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Opening safe local file sandbox view...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "View Doc Status", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Doc File", Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Doc File", Modifier.size(18.dp))
                }
            }
        }
    }
}
