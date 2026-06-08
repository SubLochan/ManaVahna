package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.ManaVahanaViewModel
import kotlinx.coroutines.launch
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.draw.clip
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceLogsScreen(
    viewModel: ManaVahanaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val allServiceLogs by viewModel.allServiceLogs.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<ServiceLog?>(null) }

    // Form states
    var vehicleId by remember { mutableStateOf(0) }
    var serviceDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var odometerReading by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("General") }
    var serviceCenter by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var nextServiceDate by remember { mutableStateOf(0L) }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun resetFields() {
        editingLog = null
        vehicleId = selectedVehicle?.id ?: vehicles.firstOrNull()?.id ?: 0
        serviceDate = System.currentTimeMillis()
        odometerReading = ""
        serviceType = "General"
        serviceCenter = ""
        cost = ""
        notes = ""
        nextServiceDate = 0L
    }

    // Auto-populate active vehicle
    LaunchedEffect(selectedVehicle, showAddDialog) {
        if (showAddDialog && editingLog == null) {
            vehicleId = selectedVehicle?.id ?: vehicles.firstOrNull()?.id ?: 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("service_logs_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (vehicles.isEmpty()) {
                        android.widget.Toast.makeText(context, "Please add a vehicle from the Dashboard first!", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        resetFields()
                        showAddDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_service_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Service Log")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "సర్వీస్ రికార్డులు (Service History)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (vehicles.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "వాహనం లేదు (No Vehicle Added)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Please add a vehicle first on the Dashboard before tracking logs.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (allServiceLogs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No services logged. Tap + to record details.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(allServiceLogs) { log ->
                            val vehicleName = vehicles.find { it.id == log.vehicleId }?.vehicleName ?: "Vehicle"
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("$vehicleName - ${log.serviceType}", fontWeight = FontWeight.Bold)
                                        Text("Center: ${log.serviceCenter}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text("Odo: ${log.odometerReading} km | Notes: ${log.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Date: ${sdf.format(Date(log.serviceDate))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹${log.cost}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingLog = log
                                                    vehicleId = log.vehicleId
                                                    serviceDate = log.serviceDate
                                                    odometerReading = log.odometerReading.toString()
                                                    serviceType = log.serviceType
                                                    serviceCenter = log.serviceCenter
                                                    cost = log.cost.toString()
                                                    notes = log.notes
                                                    nextServiceDate = log.nextServiceDate
                                                    showAddDialog = true
                                                }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Log", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { viewModel.deleteServiceLog(log) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Log", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                resetFields()
            },
            title = { Text(if (editingLog != null) "Edit Service Detail" else "Log Service Detail") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        var expandedVeh by remember { mutableStateOf(false) }
                        Box {
                            val activeVehName = vehicles.find { it.id == vehicleId }?.vehicleName ?: "Select Vehicle"
                            OutlinedButton(onClick = { expandedVeh = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Vehicle: $activeVehName")
                            }
                            DropdownMenu(expanded = expandedVeh, onDismissRequest = { expandedVeh = false }) {
                                vehicles.forEach { veh ->
                                    DropdownMenuItem(
                                        text = { Text(veh.vehicleName) },
                                        onClick = {
                                            vehicleId = veh.id
                                            expandedVeh = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = serviceType,
                            onValueChange = { serviceType = it },
                            label = { Text("Service Type (e.g., Oil, Engine, Wash)") },
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
                            value = odometerReading,
                            onValueChange = { odometerReading = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Odometer Reading (km)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = cost,
                            onValueChange = { cost = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text("Total Service Cost (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
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
                        if (vehicleId > 0 && serviceType.isNotBlank()) {
                            val newLog = ServiceLog(
                                id = editingLog?.id ?: 0,
                                vehicleId = vehicleId,
                                serviceDate = serviceDate,
                                odometerReading = odometerReading.trim().toDoubleOrNull() ?: 0.0,
                                serviceType = serviceType,
                                serviceCenter = serviceCenter,
                                cost = cost.trim().toDoubleOrNull() ?: 0.0,
                                notes = notes,
                                nextServiceDate = nextServiceDate
                            )
                            if (editingLog != null) {
                                viewModel.updateServiceLog(newLog)
                            } else {
                                viewModel.addServiceLog(newLog)
                            }
                            showAddDialog = false
                            resetFields()
                        }
                    }
                ) {
                    Text("Save Log")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    resetFields()
                }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelLogsScreen(
    viewModel: ManaVahanaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val fuelLogs by viewModel.allFuelLogs.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingFuel by remember { mutableStateOf<FuelLog?>(null) }

    // Form builders
    var vehicleId by remember { mutableStateOf(0) }
    var fuelDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var litersFilled by remember { mutableStateOf("") }
    var pricePerLiter by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var odometerReading by remember { mutableStateOf("") }
    var fuelStationName by remember { mutableStateOf("") }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun resetFields() {
        editingFuel = null
        vehicleId = selectedVehicle?.id ?: vehicles.firstOrNull()?.id ?: 0
        fuelDate = System.currentTimeMillis()
        litersFilled = ""
        pricePerLiter = ""
        totalAmount = ""
        odometerReading = ""
        fuelStationName = ""
    }

    LaunchedEffect(selectedVehicle, showAddDialog) {
        if (showAddDialog && editingFuel == null) {
            vehicleId = selectedVehicle?.id ?: vehicles.firstOrNull()?.id ?: 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("fuel_logs_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (vehicles.isEmpty()) {
                        android.widget.Toast.makeText(context, "Please add a vehicle from the Dashboard first!", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        resetFields()
                        showAddDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_fuel_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Fuel")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ఇంధన లాగ్‌లు (Fuel Logs)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (vehicles.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "వాహనం లేదు (No Vehicle Added)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Please add a vehicle first on the Dashboard before tracking logs.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (fuelLogs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No fuel fills logged. Tap + to add logs.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    val sortedVehiclesLogs = remember(fuelLogs) {
                        fuelLogs.groupBy { it.vehicleId }.mapValues { (_, list) ->
                            list.sortedBy { it.odometerReading }
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(fuelLogs) { log ->
                            val vehicleName = vehicles.find { it.id == log.vehicleId }?.vehicleName ?: "Vehicle"
                            val vehicleLogs = sortedVehiclesLogs[log.vehicleId] ?: emptyList()
                            val idx = vehicleLogs.indexOf(log)
                            val mileage = if (idx > 0) {
                                val prev = vehicleLogs[idx - 1]
                                val dist = log.odometerReading - prev.odometerReading
                                if (log.litersFilled > 0) dist / log.litersFilled else null
                            } else null

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("$vehicleName - Filled ${log.litersFilled}L", fontWeight = FontWeight.Bold)
                                        Text("Station: ${log.fuelStationName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text("Odo: ${log.odometerReading} km", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        if (mileage != null && mileage > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("మైలేజ్ (Mileage): ${String.format("%.2f", mileage)} km/L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Date: ${sdf.format(Date(log.fuelDate))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹${log.totalAmount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingFuel = log
                                                    vehicleId = log.vehicleId
                                                    fuelDate = log.fuelDate
                                                    litersFilled = log.litersFilled.toString()
                                                    pricePerLiter = log.pricePerLiter.toString()
                                                    totalAmount = log.totalAmount.toString()
                                                    odometerReading = log.odometerReading.toString()
                                                    fuelStationName = log.fuelStationName
                                                    showAddDialog = true
                                                }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Fuel", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { viewModel.deleteFuelLog(log) }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                resetFields()
            },
            title = { Text(if (editingFuel != null) "Edit Fuel Fill" else "Log Fuel Fill") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        var expandedVeh by remember { mutableStateOf(false) }
                        Box {
                            val activeVehName = vehicles.find { it.id == vehicleId }?.vehicleName ?: "Select Vehicle"
                            OutlinedButton(onClick = { expandedVeh = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Vehicle: $activeVehName")
                            }
                            DropdownMenu(expanded = expandedVeh, onDismissRequest = { expandedVeh = false }) {
                                vehicles.forEach { veh ->
                                    DropdownMenuItem(
                                        text = { Text(veh.vehicleName) },
                                        onClick = {
                                            vehicleId = veh.id
                                            expandedVeh = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = litersFilled,
                            onValueChange = {
                                litersFilled = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text("Liters Filled") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = totalAmount,
                            onValueChange = {
                                totalAmount = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text("Total Cost (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = odometerReading,
                            onValueChange = { odometerReading = it },
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
                        DatePickerField(
                            label = "Date",
                            timestamp = fuelDate,
                            onDateSelected = { fuelDate = it },
                            context = context,
                            sdf = sdf
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vehicleId > 0 && litersFilled.isNotBlank() && totalAmount.isNotBlank() && odometerReading.isNotBlank()) {
                            val liters = litersFilled.toDoubleOrNull() ?: 0.0
                            val total = totalAmount.toDoubleOrNull() ?: 0.0
                            val calculatedPrice = if (liters > 0) total / liters else 0.0
                            val newFuelLog = FuelLog(
                                id = editingFuel?.id ?: 0,
                                vehicleId = vehicleId,
                                fuelDate = fuelDate,
                                litersFilled = liters,
                                pricePerLiter = calculatedPrice,
                                totalAmount = total,
                                odometerReading = odometerReading.toDoubleOrNull() ?: 0.0,
                                fuelStationName = fuelStationName.ifBlank { "Local Pump" }
                            )
                            if (editingFuel != null) {
                                viewModel.updateFuelLog(newFuelLog)
                            } else {
                                viewModel.addFuelLog(newFuelLog)
                            }
                            showAddDialog = false
                            resetFields()
                        }
                    }
                ) {
                    Text("Save Fuel Log")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    resetFields()
                }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ManaVahanaViewModel
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    // Form items
    var vehicleId by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf("Fuel") }
    val categories = listOf("Fuel", "Repairs", "Insurance", "Washing", "Accessories", "Parking", "Toll", "Miscellaneous")
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun resetFields() {
        editingExpense = null
        vehicleId = selectedVehicle?.id ?: vehicles.firstOrNull()?.id ?: 0
        category = "Fuel"
        amount = ""
        notes = ""
        expenseDate = System.currentTimeMillis()
    }

    LaunchedEffect(selectedVehicle, showAddDialog) {
        if (showAddDialog && editingExpense == null) {
            vehicleId = selectedVehicle?.id ?: vehicles.firstOrNull()?.id ?: 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("expense_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (vehicles.isEmpty()) {
                        android.widget.Toast.makeText(context, "Please add a vehicle from the Dashboard first!", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        resetFields()
                        showAddDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add General Expense")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ఖర్చుల నివేదిక (Expenses)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (vehicles.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "వాహనం లేదు (No Vehicle Added)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Please add a vehicle first on the Dashboard before tracking logs.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (allExpenses.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No expenses logged. Tap + to add logs.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(allExpenses) { exp ->
                            val vehicleName = vehicles.find { it.id == exp.vehicleId }?.vehicleName ?: "Vehicle"
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("$vehicleName - ${exp.category}", fontWeight = FontWeight.Bold)
                                        Text(exp.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Logged: ${sdf.format(Date(exp.expenseDate))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("₹${exp.amount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                editingExpense = exp
                                                vehicleId = exp.vehicleId
                                                category = exp.category
                                                amount = exp.amount.toString()
                                                notes = exp.notes
                                                expenseDate = exp.expenseDate
                                                showAddDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Expense", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteExpense(exp) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                resetFields()
            },
            title = { Text(if (editingExpense != null) "Edit Expense Record" else "Add Expense Record") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        var expandedVeh by remember { mutableStateOf(false) }
                        Box {
                            val activeVehName = vehicles.find { it.id == vehicleId }?.vehicleName ?: "Select Vehicle"
                            OutlinedButton(onClick = { expandedVeh = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Vehicle: $activeVehName")
                            }
                            DropdownMenu(expanded = expandedVeh, onDismissRequest = { expandedVeh = false }) {
                                vehicles.forEach { veh ->
                                    DropdownMenuItem(
                                        text = { Text(veh.vehicleName) },
                                        onClick = {
                                            vehicleId = veh.id
                                            expandedVeh = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        var expandedCat by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedCat = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Category: $category")
                            }
                            DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            expandedCat = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text("Amount (₹)") },
                            modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vehicleId > 0 && amount.isNotBlank()) {
                            val newExpense = Expense(
                                id = editingExpense?.id ?: 0,
                                vehicleId = vehicleId,
                                expenseDate = expenseDate,
                                category = category,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                notes = notes.ifBlank { "General $category expense" }
                            )
                            if (editingExpense != null) {
                                viewModel.updateExpense(newExpense)
                            } else {
                                viewModel.addExpense(newExpense)
                            }
                            showAddDialog = false
                            resetFields()
                        }
                    }
                ) {
                    Text("Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    resetFields()
                }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVaultScreen(
    viewModel: ManaVahanaViewModel
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val vehicles by viewModel.vehicles.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val pendingReminders by viewModel.pendingReminders.collectAsState()

    fun getResolutionFile(context: android.content.Context, pathString: String?): java.io.File? {
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

    fun getShareableUri(context: android.content.Context, pathString: String?): android.net.Uri? {
        if (pathString.isNullOrBlank()) return null
        return try {
            val resolvedFile = getResolutionFile(context, pathString)
            if (resolvedFile != null) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    resolvedFile
                )
            } else {
                val uri = android.net.Uri.parse(pathString)
                if (uri.scheme == "content") {
                    uri
                } else if (uri.scheme == "file") {
                    val file = java.io.File(uri.path ?: "")
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                } else {
                    val file = java.io.File(pathString)
                    if (file.exists()) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                    } else {
                        uri
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getMimeType(pathString: String?): String {
        if (pathString.isNullOrBlank()) return "*/*"
        val lowercase = pathString.lowercase(java.util.Locale.getDefault())
        return when {
            lowercase.endsWith(".pdf") -> "application/pdf"
            lowercase.endsWith(".png") -> "image/png"
            lowercase.endsWith(".jpg") || lowercase.endsWith(".jpeg") -> "image/jpeg"
            lowercase.endsWith(".webp") -> "image/webp"
            else -> "*/*"
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingDoc by remember { mutableStateOf<Document?>(null) }
    var previewingDoc by remember { mutableStateOf<Document?>(null) }

    if (previewingDoc != null) {
        val doc = previewingDoc!!
        val path = doc.documentPath ?: ""
        val isImage = path.endsWith(".png", ignoreCase = true) ||
                path.endsWith(".jpg", ignoreCase = true) ||
                path.endsWith(".jpeg", ignoreCase = true) ||
                path.endsWith(".webp", ignoreCase = true)

        AlertDialog(
            onDismissRequest = { previewingDoc = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { previewingDoc = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = getResolutionFile(context, path) ?: path,
                                contentDescription = doc.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 290.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = doc.docType,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Secure Document Format",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "ఈ డాక్యుమెంట్ స్థానిక డేటాబేస్ వాల్ట్ లో సురక్షితంగా రక్షించబడింది.\n(This document is stored securely in your private local vault.)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val contentUri = getShareableUri(context, path)
                                if (contentUri != null) {
                                    val mime = getMimeType(path)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(contentUri, mime)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "డాక్యుమెంట్ ఓపెన్ చేయండి (Open with)"))
                                } else {
                                    android.widget.Toast.makeText(context, "Error resolving path link.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "No app available to open this format.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val contentUri = getShareableUri(context, path)
                                if (contentUri != null) {
                                    val mime = getMimeType(path)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = mime
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "మిత్రులతో పంచుకోండి (Share Document)"))
                                } else {
                                    android.widget.Toast.makeText(context, "Error resolving share link.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "Failed to initiate share.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                }
            }
        )
    }

    // Form attributes
    var vehicleId by remember { mutableStateOf(0) }
    var docType by remember { mutableStateOf("RC") }
    val docTypes = listOf("RC", "Insurance", "Pollution Certificate", "License", "Service Bills")
    var title by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf(0L) }
    var documentPath by remember { mutableStateOf<String?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = copyUriToInternalStorage(context, uri, "doc")
            if (localPath != null) {
                documentPath = localPath
            } else {
                documentPath = uri.toString()
            }
            if (title.isBlank()) {
                title = "$docType Document"
            }
        }
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun resetFields() {
        editingDoc = null
        vehicleId = selectedVehicle?.id ?: vehicles.firstOrNull()?.id ?: 0
        docType = "RC"
        title = ""
        expiryDate = 0L
        documentPath = null
    }

    LaunchedEffect(selectedVehicle, showAddDialog) {
        if (showAddDialog && editingDoc == null) {
            vehicleId = selectedVehicle?.id ?: vehicles.firstOrNull()?.id ?: 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("document_vault_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (vehicles.isEmpty()) {
                        android.widget.Toast.makeText(context, "Please add a vehicle from the Dashboard first!", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        resetFields()
                        showAddDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_document_fab")
            ) {
                Icon(Icons.Default.FolderZip, contentDescription = "Add Document card")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "డాక్యుమెంట్ వాల్ట్ (Document Vault)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Store RC, Insurance cards, license files locally with secure local encryption tags.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                // Upcoming Expiries & Expiry Reminders (పరిమితి ముగింపు రిమైండర్లు)
                val currentTime = System.currentTimeMillis()
                val loomingReminders = remember(pendingReminders) {
                    pendingReminders.filter { reminder ->
                        val isDocCategory = reminder.category in listOf("Insurance", "Pollution", "License") ||
                                reminder.title.contains("Expiry", ignoreCase = true) ||
                                reminder.title.contains("Renew", ignoreCase = true)
                        val timeDiff = reminder.reminderDate - currentTime
                        isDocCategory && (timeDiff < 30L * 24 * 60 * 60 * 1000L) // Under 30 days
                    }.sortedBy { it.reminderDate }
                }

                if (loomingReminders.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("expiry_reminders_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "త్వరలో ముగిసే పరిమితులు (Expiries Near)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            loomingReminders.forEach { reminder ->
                                val daysLeft = ((reminder.reminderDate - currentTime) / (24 * 60 * 60 * 1000L)).toInt()
                                val statusText = when {
                                    daysLeft < 0 -> "🔴 కాలపరిమితి ముగిసింది (Expired ${-daysLeft} days ago)"
                                    daysLeft == 0 -> "⚠️ ఈరోజే ముగుస్తుంది (Expires today!)"
                                    daysLeft == 1 -> "⏳ రేపే ముగుస్తుంది (Expires tomorrow!)"
                                    else -> "⏳ ${daysLeft} రోజుల్లో ముగుస్తుంది (${daysLeft} days left)"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(reminder.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text(statusText, fontSize = 11.sp, color = if (daysLeft <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleReminderCompleted(reminder) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Mark Cleared",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (vehicles.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "వాహనం లేదు (No Vehicle Added)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Please add a vehicle first on the Dashboard before tracking logs.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (allDocuments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No documents saved. Tap + to store documents.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(allDocuments) { doc ->
                            val vehicleName = vehicles.find { it.id == doc.vehicleId }?.vehicleName ?: "Vehicle"
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("$vehicleName - ${doc.title}", fontWeight = FontWeight.Bold)
                                        }
                                        Text("Doc Type: ${doc.docType}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        if (doc.expiryDate != null && doc.expiryDate > 0) {
                                            Text("Expires on: ${sdf.format(Date(doc.expiryDate))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row {
                                        if (!doc.documentPath.isNullOrBlank()) {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        previewingDoc = doc; if (false) { val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(Uri.parse(doc.documentPath), contentResolver.getType(Uri.parse(doc.documentPath)) ?: "*/*") // test successful
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(intent) }
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Opening safe local file sandbox view...", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Visibility,
                                                    contentDescription = "View Doc Status",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                editingDoc = doc
                                                vehicleId = doc.vehicleId
                                                docType = doc.docType
                                                title = doc.title
                                                expiryDate = doc.expiryDate ?: 0L
                                                documentPath = doc.documentPath
                                                showAddDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Doc", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteDocument(doc) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                resetFields()
            },
            title = { Text(if (editingDoc != null) "Edit Document Securely" else "Store Document Securely") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        var expandedVeh by remember { mutableStateOf(false) }
                        Box {
                            val activeVehName = vehicles.find { it.id == vehicleId }?.vehicleName ?: "Select Vehicle"
                            OutlinedButton(onClick = { expandedVeh = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Vehicle: $activeVehName")
                            }
                            DropdownMenu(expanded = expandedVeh, onDismissRequest = { expandedVeh = false }) {
                                vehicles.forEach { veh ->
                                    DropdownMenuItem(
                                        text = { Text(veh.vehicleName) },
                                        onClick = {
                                            vehicleId = veh.id
                                            expandedVeh = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        var expandedDoc by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedDoc = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Doc Type: $docType")
                            }
                            DropdownMenu(expanded = expandedDoc, onDismissRequest = { expandedDoc = false }) {
                                docTypes.forEach { type ->
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
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Document Title (e.g., My RC Copy)") },
                            modifier = Modifier.fillMaxWidth().testTag("document_title_input")
                        )
                    }

                    item {
                        val displayExpiry = if (expiryDate > 0) sdf.format(Date(expiryDate)) else "No Expiry / Lifetime"
                        OutlinedTextField(
                            value = displayExpiry,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expiry Date") },
                            modifier = Modifier.fillMaxWidth().clickable {
                                val c = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    val sel = Calendar.getInstance().apply { set(y, m, d) }
                                    expiryDate = sel.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }
                        )
                    }

                    item {
                        val selectedFileName = remember(documentPath) {
                            if (documentPath.isNullOrEmpty()) {
                                null
                            } else {
                                try {
                                    val uri = Uri.parse(documentPath)
                                    var resolvedName: String? = null
                                    if (uri.scheme == "content") {
                                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                            if (cursor.moveToFirst()) {
                                                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                                if (nameIdx != -1) {
                                                    resolvedName = cursor.getString(nameIdx)
                                                }
                                            }
                                        }
                                    }
                                    resolvedName ?: uri.lastPathSegment
                                } catch (e: Exception) {
                                    "Selected Document"
                                }
                            }
                        }

                        Button(
                            onClick = { fileLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedFileName != null) "File: $selectedFileName 👍" else "Select PDF or Image File")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vehicleId > 0 && title.isNotBlank()) {
                            val newDoc = Document(
                                id = editingDoc?.id ?: 0,
                                vehicleId = vehicleId,
                                docType = docType,
                                title = title,
                                expiryDate = if (expiryDate > 0) expiryDate else null,
                                documentPath = documentPath ?: "content://media/external_mock_path.pdf",
                                isEncrypted = true
                            )
                            if (editingDoc != null) {
                                viewModel.updateDocument(newDoc)
                            } else {
                                viewModel.addDocument(newDoc)
                            }
                            showAddDialog = false
                            resetFields()
                        }
                    }
                ) {
                    Text("Encrypt and Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    resetFields()
                }) { Text("Cancel") }
            }
        )
    }
}
