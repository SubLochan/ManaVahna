package com.manavahana.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manavahana.ManaVahanaApplication
import com.manavahana.data.model.Expense
import com.manavahana.data.model.FuelLog
import com.manavahana.data.model.Vehicle
import com.manavahana.ui.theme.ManaVahanaTheme
import com.manavahana.widget.FuelWidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class QuickAddFuelActivity : ComponentActivity() {

    private var currentPresetAmount by mutableStateOf<Double?>(null)
    private var currentTargetVehicleId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val presetAmountExtra = intent.getDoubleExtra(FuelWidgetUpdater.EXTRA_PRESET_AMOUNT, -1.0)
        currentPresetAmount = if (presetAmountExtra > 0) presetAmountExtra else null
        val targetVehicleIdExtra = intent.getIntExtra(FuelWidgetUpdater.EXTRA_TARGET_VEHICLE_ID, -1)
        currentTargetVehicleId = if (targetVehicleIdExtra > 0) targetVehicleIdExtra else null

        val app = application as ManaVahanaApplication
        val repository = app.repository
        val preferencesRepository = app.userPreferencesRepository

        setContent {
            val themeMode by preferencesRepository.themeMode.collectAsState(initial = "system")
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val selectedLanguage by preferencesRepository.selectedLanguage.collectAsState(initial = "en")
            val langCode = selectedLanguage ?: "en"

            ManaVahanaTheme(themeMode = themeMode, darkTheme = isDarkTheme) {
                // Dimmed background overlay that dismisses on outside tap
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            finish()
                        }
                        .systemBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    // Prevent clicks inside the dialog card from dismissing
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume click */ }
                    ) {
                        QuickAddFuelDialogContent(
                            initialAmount = currentPresetAmount,
                            initialVehicleId = currentTargetVehicleId,
                            langCode = langCode,
                            onDismiss = { finish() },
                            onSaveSuccess = { fuelLog ->
                                val message = if (langCode == "te") {
                                    "₹${fuelLog.totalAmount.toInt()} ఇంధన ఖర్చు విజయవంతంగా నమోదు చేయబడింది!"
                                } else if (langCode == "hi") {
                                    "₹${fuelLog.totalAmount.toInt()} ईंधन खर्च दर्ज हो गया!"
                                } else {
                                    "Fuel expense of ₹${fuelLog.totalAmount.toInt()} logged!"
                                }
                                Toast.makeText(this@QuickAddFuelActivity, message, Toast.LENGTH_SHORT).show()
                                finish()
                            },
                            repository = repository
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val preset = intent.getDoubleExtra(FuelWidgetUpdater.EXTRA_PRESET_AMOUNT, -1.0)
        currentPresetAmount = if (preset > 0) preset else null
        val vId = intent.getIntExtra(FuelWidgetUpdater.EXTRA_TARGET_VEHICLE_ID, -1)
        currentTargetVehicleId = if (vId > 0) vId else null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddFuelDialogContent(
    initialAmount: Double?,
    initialVehicleId: Int?,
    langCode: String,
    onDismiss: () -> Unit,
    onSaveSuccess: (FuelLog) -> Unit,
    repository: com.manavahana.data.repository.ManaVahanaRepository
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var previousOdometer by remember { mutableStateOf<Double?>(null) }
    var lastPricePerLiter by remember { mutableStateOf(102.50) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedVehicleId by remember { mutableStateOf<Int?>(initialVehicleId) }
    var totalAmountInput by remember { mutableStateOf(if (initialAmount != null) initialAmount.toInt().toString() else "") }
    var litersFilledInput by remember { mutableStateOf("") }
    var pricePerLiterInput by remember { mutableStateOf("102.50") }
    var odometerInput by remember { mutableStateOf("") }
    var fuelStationName by remember { mutableStateOf("Indian Oil") }

    // Quick inline vehicle creation if no vehicle exists
    var newVehicleName by remember { mutableStateOf("") }
    var newVehicleNumber by remember { mutableStateOf("") }

    val amountFocusRequester = remember { FocusRequester() }

    // Load initial data from Room
    LaunchedEffect(Unit) {
        val loadedVehicles = repository.allVehicles.firstOrNull() ?: emptyList()
        vehicles = loadedVehicles
        if (selectedVehicleId == null && loadedVehicles.isNotEmpty()) {
            selectedVehicleId = loadedVehicles.first().id
        }

        // Check recent fuel log for latest price and odometer
        val fuelLogs = repository.allFuelLogs.firstOrNull() ?: emptyList()
        val latestLog = fuelLogs.maxByOrNull { it.fuelDate }
        if (latestLog != null) {
            if (latestLog.pricePerLiter > 0) {
                lastPricePerLiter = latestLog.pricePerLiter
                pricePerLiterInput = String.format(Locale.US, "%.2f", latestLog.pricePerLiter)
            }
            previousOdometer = latestLog.odometerReading
        }

        // If initial preset amount passed, calculate liters right away
        if (initialAmount != null && lastPricePerLiter > 0) {
            val liters = initialAmount / lastPricePerLiter
            litersFilledInput = String.format(Locale.US, "%.2f", liters)
        }

        isLoading = false

        // Automatically request focus on amount input for immediate typing
        delay(200)
        try {
            amountFocusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Auto-update previous odometer if selected vehicle changes
    LaunchedEffect(selectedVehicleId) {
        val vId = selectedVehicleId ?: return@LaunchedEffect
        val vehicleLogs = repository.getFuelLogsForVehicle(vId).firstOrNull() ?: emptyList()
        val latestVehicleLog = vehicleLogs.maxByOrNull { it.fuelDate }
        if (latestVehicleLog != null) {
            previousOdometer = latestVehicleLog.odometerReading
            if (latestVehicleLog.pricePerLiter > 0) {
                pricePerLiterInput = String.format(Locale.US, "%.2f", latestVehicleLog.pricePerLiter)
            }
        }
    }

    val presetAmounts = listOf(200.0, 500.0, 1000.0, 1500.0, 2000.0)
    val popularStations = listOf("Indian Oil", "HP", "Bharat Petroleum", "Shell", "Jio-bp", "Nayara")

    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .widthIn(max = 480.dp)
            .padding(10.dp)
            .testTag("quick_add_fuel_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Color(0xFFFFA000).copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dialog Header: Title, Icon & Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFA000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (langCode == "te") "ఇంధన ఖర్చు నమోదు" else if (langCode == "hi") "ईंधन खर्च दर्ज करें" else "Log Fuel Expense",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (langCode == "te") "హోమ్ స్క్రీన్ విడ్జెట్ త్వరిత నమోదు" else if (langCode == "hi") "होम स्क्रीन विजेट त्वरित प्रविष्टि" else "Widget Instant Entry",
                            fontSize = 11.5.sp,
                            color = Color(0xFFFFA000),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFA000))
                }
            } else {
                // Vehicle selection or Quick Vehicle Creation
                if (vehicles.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (langCode == "te") "త్వరిత వాహన వివరాలు" else if (langCode == "hi") "त्वरित वाहन विवरण" else "Quick Vehicle Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = newVehicleName,
                            onValueChange = { newVehicleName = it },
                            label = { Text("Vehicle Name (e.g. Splendor, Creta)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newVehicleNumber,
                            onValueChange = { newVehicleNumber = it },
                            label = { Text("Plate (e.g. AP 29 BK 1234)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (langCode == "te") "వాహనం" else if (langCode == "hi") "वाहन" else "Vehicle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        var expandedVehicleMenu by remember { mutableStateOf(false) }
                        val currentVeh = vehicles.find { it.id == selectedVehicleId } ?: vehicles.first()

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedVehicleMenu = true }
                                .testTag("vehicle_selector_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = currentVeh.vehicleName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${currentVeh.vehicleNumber} • ${currentVeh.fuelType}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = expandedVehicleMenu,
                                onDismissRequest = { expandedVehicleMenu = false }
                            ) {
                                vehicles.forEach { v ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${v.vehicleName} (${v.vehicleNumber})")
                                        },
                                        onClick = {
                                            selectedVehicleId = v.id
                                            expandedVehicleMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Amount Section with Auto Focus and Quick Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (langCode == "te") "ఖర్చు మొత్తం (₹)" else if (langCode == "hi") "कुल राशि (₹)" else "Fuel Amount (₹)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = totalAmountInput,
                        onValueChange = { newVal ->
                            totalAmountInput = newVal
                            val amt = newVal.toDoubleOrNull()
                            val price = pricePerLiterInput.toDoubleOrNull() ?: lastPricePerLiter
                            if (amt != null && price > 0) {
                                litersFilledInput = String.format(Locale.US, "%.2f", amt / price)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = {
                            Text("₹", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFFFA000))
                        },
                        trailingIcon = {
                            if (totalAmountInput.isNotBlank()) {
                                IconButton(onClick = { totalAmountInput = ""; litersFilledInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        placeholder = { Text("e.g. 500") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(amountFocusRequester)
                            .testTag("quick_amount_input")
                    )

                    // Quick Preset Chips (One-tap Fill)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        items(presetAmounts) { amt ->
                            val isSelected = totalAmountInput == amt.toInt().toString() || totalAmountInput == amt.toString()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFFFA000) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        totalAmountInput = amt.toInt().toString()
                                        val price = pricePerLiterInput.toDoubleOrNull() ?: lastPricePerLiter
                                        if (price > 0) {
                                            litersFilledInput = String.format(Locale.US, "%.2f", amt / price)
                                        }
                                    }
                            ) {
                                Text(
                                    text = "₹${amt.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Liters Filled & Price Per Liter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Liters Filled
                    OutlinedTextField(
                        value = litersFilledInput,
                        onValueChange = { newVal ->
                            litersFilledInput = newVal
                            val liters = newVal.toDoubleOrNull()
                            val price = pricePerLiterInput.toDoubleOrNull() ?: lastPricePerLiter
                            if (liters != null && price > 0) {
                                totalAmountInput = String.format(Locale.US, "%.2f", liters * price)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(if (langCode == "te") "లీటర్లు" else if (langCode == "hi") "लीटर" else "Liters") },
                        placeholder = { Text("e.g. 4.88") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_liters_input")
                    )

                    // Price per liter
                    OutlinedTextField(
                        value = pricePerLiterInput,
                        onValueChange = { newVal ->
                            pricePerLiterInput = newVal
                            val price = newVal.toDoubleOrNull()
                            val amt = totalAmountInput.toDoubleOrNull()
                            if (price != null && price > 0 && amt != null) {
                                litersFilledInput = String.format(Locale.US, "%.2f", amt / price)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(if (langCode == "te") "ధర/లీ." else if (langCode == "hi") "मूल्य/ली." else "₹ / Liter") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_price_input")
                    )
                }

                // Odometer Reading
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = odometerInput,
                        onValueChange = { odometerInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(if (langCode == "te") "ప్రస్తుత ఓడోమీటర్ (కి.మీ.)" else if (langCode == "hi") "वर्तमान ओडोमीटर (किमी)" else "Current Odometer (km)") },
                        placeholder = {
                            Text(if (previousOdometer != null) "e.g. ${(previousOdometer!! + 100).toInt()}" else "e.g. 15400")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_odometer_input")
                    )

                    if (previousOdometer != null) {
                        Text(
                            text = if (langCode == "te") "గత రీడింగ్: ${String.format(Locale.US, "%.0f", previousOdometer)} కి.మీ." else if (langCode == "hi") "पिछली रीडिंग: ${String.format(Locale.US, "%.0f", previousOdometer)} किमी" else "Previous logged: ${String.format(Locale.US, "%.0f", previousOdometer)} km",
                            fontSize = 11.sp,
                            color = Color(0xFFFFA000),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Fuel Station quick select chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (langCode == "te") "ఇంధన కేంద్రం" else if (langCode == "hi") "ईंधन स्टेशन" else "Fuel Station",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(popularStations) { station ->
                            val isSelected = fuelStationName == station
                            FilterChip(
                                selected = isSelected,
                                onClick = { fuelStationName = station },
                                label = { Text(station, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFA000).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFFFFA000)
                                )
                            )
                        }
                    }
                }

                // Action Buttons: Save Fuel Expense + Cancel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val canSave = (totalAmountInput.isNotBlank() && (totalAmountInput.toDoubleOrNull() ?: 0.0) > 0) &&
                            (vehicles.isNotEmpty() || newVehicleName.isNotBlank())

                    Button(
                        onClick = {
                            val total = totalAmountInput.toDoubleOrNull() ?: 0.0
                            val liters = litersFilledInput.toDoubleOrNull() ?: (if (lastPricePerLiter > 0) total / lastPricePerLiter else 0.0)
                            val price = pricePerLiterInput.toDoubleOrNull() ?: (if (liters > 0) total / liters else 102.50)
                            val odo = odometerInput.toDoubleOrNull() ?: (previousOdometer ?: 0.0)

                            if (total <= 0) return@Button

                            coroutineScope.launch {
                                var vId = selectedVehicleId ?: (vehicles.firstOrNull()?.id ?: 0)

                                // If no vehicle existed, auto-create it now
                                if (vId == 0 && newVehicleName.isNotBlank()) {
                                    val newVeh = Vehicle(
                                        vehicleName = newVehicleName.trim(),
                                        vehicleNumber = newVehicleNumber.trim().ifBlank { "REG" },
                                        brand = "Vehicle",
                                        model = "Model",
                                        vehicleType = "Car",
                                        fuelType = "Petrol",
                                        purchaseDate = System.currentTimeMillis(),
                                        insuranceExpiry = 0L,
                                        pollutionExpiry = 0L
                                    )
                                    val generatedVehId = repository.insertVehicle(newVeh)
                                    vId = generatedVehId.toInt()
                                }

                                val fuelLog = FuelLog(
                                    vehicleId = vId,
                                    fuelDate = System.currentTimeMillis(),
                                    litersFilled = liters,
                                    pricePerLiter = price,
                                    totalAmount = total,
                                    odometerReading = odo,
                                    fuelStationName = fuelStationName.ifBlank { "Local Pump" }
                                )
                                repository.insertFuelLog(fuelLog)
                                repository.insertExpense(
                                    Expense(
                                        vehicleId = vId,
                                        expenseDate = fuelLog.fuelDate,
                                        category = "Fuel",
                                        amount = total,
                                        notes = "Widget Fill: ${String.format(Locale.US, "%.2f", liters)}L at ${fuelStationName.ifBlank { "Fuel Pump" }}"
                                    )
                                )
                                // Refresh home screen widget immediately with committed data before finishing
                                try {
                                    FuelWidgetUpdater.updateAllWidgetsSuspend(context)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                onSaveSuccess(fuelLog)
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_quick_fuel_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFA000),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (langCode == "te") "ఇంధన ఖర్చు సేవ్ చేయండి" else if (langCode == "hi") "ईंधन खर्च सहेजें" else "Save Fuel Expense",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("cancel_quick_fuel_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (langCode == "te") "రద్దు చేయి (హోమ్ స్క్రీన్)" else if (langCode == "hi") "रद्द करें (होम स्क्रीन)" else "Cancel / Back to Home Screen",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
