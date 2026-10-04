package com.manavahana.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manavahana.data.model.Expense
import com.manavahana.data.model.FuelLog
import com.manavahana.data.model.Reminder
import com.manavahana.data.model.ServiceLog
import com.manavahana.data.model.Vehicle
import com.manavahana.ui.Localizer
import com.manavahana.ui.pdf.PdfGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class MonthInfo(
    val monthNumber: Int, // 1..12
    val shortEn: String,
    val fullEn: String,
    val shortTe: String,
    val fullTe: String
)

private val MONTHS = listOf(
    MonthInfo(1, "Jan", "January", "జన", "జనవరి"),
    MonthInfo(2, "Feb", "February", "ఫిబ్ర", "ఫిబ్రవరి"),
    MonthInfo(3, "Mar", "March", "మార్చి", "మార్చి"),
    MonthInfo(4, "Apr", "April", "ఏప్రి", "ఏప్రిల్"),
    MonthInfo(5, "May", "May", "మే", "మే"),
    MonthInfo(6, "Jun", "June", "జూన్", "జూన్"),
    MonthInfo(7, "Jul", "July", "జూలై", "జూలై"),
    MonthInfo(8, "Aug", "August", "ఆగ", "ఆగస్టు"),
    MonthInfo(9, "Sep", "September", "సెప్టెం", "సెప్టెంబర్"),
    MonthInfo(10, "Oct", "October", "అక్టో", "అక్టోబర్"),
    MonthInfo(11, "Nov", "November", "నవం", "నవంబర్"),
    MonthInfo(12, "Dec", "December", "డిసెం", "డిసెంబర్")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadOldReportDialog(
    vehicles: List<Vehicle>,
    initialVehicleId: Int?,
    expenses: List<Expense>,
    fuelLogs: List<FuelLog>,
    serviceLogs: List<ServiceLog>,
    reminders: List<Reminder>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentCal = remember { Calendar.getInstance() }
    val currentYear = remember { currentCal.get(Calendar.YEAR) }
    val currentMonth = remember { currentCal.get(Calendar.MONTH) + 1 }

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var selectedVehicleId by remember { mutableStateOf<Int?>(initialVehicleId) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<PdfGenerator.GeneratedReportResult?>(null) }
    var savedDownloadUri by remember { mutableStateOf<Uri?>(null) }
    var savedFileName by remember { mutableStateOf("") }
    var showDownloadedSuccess by remember { mutableStateOf(false) }

    val selectedVehicle = remember(selectedVehicleId, vehicles) {
        vehicles.find { it.id == selectedVehicleId }
    }

    // Determine min year from logs
    val minYearFromLogs = remember(expenses, fuelLogs, serviceLogs, vehicles) {
        val expenseMin = expenses.mapNotNull {
            val c = Calendar.getInstance().apply { timeInMillis = it.expenseDate }
            c.get(Calendar.YEAR)
        }.minOrNull()
        val fuelMin = fuelLogs.mapNotNull {
            val c = Calendar.getInstance().apply { timeInMillis = it.fuelDate }
            c.get(Calendar.YEAR)
        }.minOrNull()
        val srvMin = serviceLogs.mapNotNull {
            val c = Calendar.getInstance().apply { timeInMillis = it.serviceDate }
            c.get(Calendar.YEAR)
        }.minOrNull()
        val vehMin = vehicles.mapNotNull {
            val c = Calendar.getInstance().apply { timeInMillis = it.purchaseDate }
            c.get(Calendar.YEAR)
        }.minOrNull()
        listOfNotNull(expenseMin, fuelMin, srvMin, vehMin).minOrNull() ?: (currentYear - 3)
    }
    val minAllowedYear = (minYearFromLogs - 1).coerceAtMost(currentYear - 4)

    // Compute month activity map for current year & selected vehicle
    val monthActivityMap = remember(selectedYear, selectedVehicleId, expenses, fuelLogs, serviceLogs) {
        val map = mutableMapOf<Int, MonthActivity>()
        for (m in 1..12) {
            val monthKey = String.format(Locale.US, "%02d-%04d", m, selectedYear)

            val mExpenses = expenses.filter {
                (selectedVehicleId == null || it.vehicleId == selectedVehicleId) &&
                        SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.expenseDate)) == monthKey
            }
            val mFuel = fuelLogs.filter {
                (selectedVehicleId == null || it.vehicleId == selectedVehicleId) &&
                        SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.fuelDate)) == monthKey
            }
            val mServices = serviceLogs.filter {
                (selectedVehicleId == null || it.vehicleId == selectedVehicleId) &&
                        SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.serviceDate)) == monthKey
            }

            val totalSpent = mExpenses.sumOf { it.amount } + mFuel.sumOf { it.totalAmount } + mServices.sumOf { it.cost }
            val count = mExpenses.size + mFuel.size + mServices.size
            val fuelLiters = mFuel.sumOf { it.litersFilled }

            map[m] = MonthActivity(
                recordCount = count,
                totalSpent = totalSpent,
                fuelCost = mFuel.sumOf { it.totalAmount },
                fuelLiters = fuelLiters,
                serviceCost = mServices.sumOf { it.cost },
                serviceCount = mServices.size,
                expenseCost = mExpenses.sumOf { it.amount },
                expenseCount = mExpenses.size
            )
        }
        map
    }

    val currentSelectedActivity = monthActivityMap[selectedMonth] ?: MonthActivity(0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 0)
    val selectedMonthInfo = MONTHS.find { it.monthNumber == selectedMonth } ?: MONTHS[0]

    fun triggerGenerateReport(downloadImmediately: Boolean, previewImmediately: Boolean, shareImmediately: Boolean) {
        isGenerating = true
        showDownloadedSuccess = false
        try {
            val result: PdfGenerator.GeneratedReportResult? = if (selectedVehicle != null) {
                PdfGenerator.generateVehicleMonthlyReport(
                    context = context,
                    vehicle = selectedVehicle,
                    expenses = expenses,
                    fuelLogs = fuelLogs,
                    serviceLogs = serviceLogs,
                    reminders = reminders,
                    targetYear = selectedYear,
                    targetMonth = selectedMonth
                )
            } else {
                PdfGenerator.generateAllVehiclesMonthlyReport(
                    context = context,
                    vehicles = vehicles,
                    expenses = expenses,
                    fuelLogs = fuelLogs,
                    serviceLogs = serviceLogs,
                    reminders = reminders,
                    targetYear = selectedYear,
                    targetMonth = selectedMonth
                )
            }

            if (result != null) {
                generatedResult = result

                if (downloadImmediately) {
                    val publicUri = PdfGenerator.savePdfToDownloads(context, result.file, result.fileName)
                    savedDownloadUri = publicUri ?: result.uri
                    savedFileName = result.fileName
                    showDownloadedSuccess = true
                    Toast.makeText(
                        context,
                        "Saved to Downloads/ManaVahana/${result.fileName}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (previewImmediately) {
                    openPdfFile(context, result.uri)
                }

                if (shareImmediately) {
                    sharePdfFile(context, result.uri, result.title)
                }
            } else {
                Toast.makeText(context, "Failed to generate report for ${selectedMonthInfo.fullEn} $selectedYear", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            isGenerating = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("download_old_report_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Reports",
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Monthly Reports (మాస నివేదికలు)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Download past months' PDF statements",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Main Scrollable Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Vehicle Scope Selection
                    Column {
                        Text(
                            text = "1. Select Vehicle / వాహనం ఎంచుకోండి",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedVehicleId == null,
                                onClick = { selectedVehicleId = null },
                                label = { Text("All Garage (గ్యారేజ్)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("chip_all_vehicles")
                            )

                            vehicles.forEach { veh ->
                                FilterChip(
                                    selected = selectedVehicleId == veh.id,
                                    onClick = { selectedVehicleId = veh.id },
                                    label = { Text(veh.vehicleName) },
                                    leadingIcon = if (selectedVehicleId == veh.id) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.testTag("chip_vehicle_${veh.id}")
                                )
                            }
                        }
                    }

                    // 2. Year Selector with Navigator
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedYear > minAllowedYear) {
                                        selectedYear--
                                    }
                                },
                                enabled = selectedYear > minAllowedYear
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Year",
                                    tint = if (selectedYear > minAllowedYear) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$selectedYear సంవత్సరం",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (selectedYear < currentYear + 1) {
                                        selectedYear++
                                    }
                                },
                                enabled = selectedYear < currentYear + 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next Year",
                                    tint = if (selectedYear < currentYear + 1) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }

                    // 3. Quick Presets Row
                    Column {
                        Text(
                            text = "Quick Presets / సులభమైన ఎంపికలు",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                onClick = {
                                    selectedYear = currentYear
                                    selectedMonth = currentMonth
                                },
                                label = { Text("This Month (ఈ నెల)") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (selectedYear == currentYear && selectedMonth == currentMonth)
                                        MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            )

                            val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                            val lastMonthYear = lastMonthCal.get(Calendar.YEAR)
                            val lastMonthVal = lastMonthCal.get(Calendar.MONTH) + 1
                            SuggestionChip(
                                onClick = {
                                    selectedYear = lastMonthYear
                                    selectedMonth = lastMonthVal
                                },
                                label = { Text("Last Month (గత నెల)") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (selectedYear == lastMonthYear && selectedMonth == lastMonthVal)
                                        MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            )

                            val twoMonthsAgoCal = Calendar.getInstance().apply { add(Calendar.MONTH, -2) }
                            val twoMonthsYear = twoMonthsAgoCal.get(Calendar.YEAR)
                            val twoMonthsVal = twoMonthsAgoCal.get(Calendar.MONTH) + 1
                            SuggestionChip(
                                onClick = {
                                    selectedYear = twoMonthsYear
                                    selectedMonth = twoMonthsVal
                                },
                                label = { Text("2 Months Ago") }
                            )

                            val threeMonthsAgoCal = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }
                            val threeMonthsYear = threeMonthsAgoCal.get(Calendar.YEAR)
                            val threeMonthsVal = threeMonthsAgoCal.get(Calendar.MONTH) + 1
                            SuggestionChip(
                                onClick = {
                                    selectedYear = threeMonthsYear
                                    selectedMonth = threeMonthsVal
                                },
                                label = { Text("3 Months Ago") }
                            )
                        }
                    }

                    // 4. 12-Month Interactive Grid (4 rows x 3 columns)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Select Month / నెలను ఎంచుకోండి",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Showing 12 Months",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Grid: 4 chunks of 3 months each
                        MONTHS.chunked(3).forEach { rowMonths ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowMonths.forEach { mInfo ->
                                    val isSelected = (selectedMonth == mInfo.monthNumber)
                                    val activity = monthActivityMap[mInfo.monthNumber] ?: MonthActivity(0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 0)
                                    val hasActivity = activity.recordCount > 0
                                    val isFuture = (selectedYear > currentYear) || (selectedYear == currentYear && mInfo.monthNumber > currentMonth)

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(86.dp)
                                            .clickable {
                                                selectedMonth = mInfo.monthNumber
                                                showDownloadedSuccess = false
                                            }
                                            .testTag("month_item_${mInfo.monthNumber}"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                hasActivity -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                                isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                else -> MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                hasActivity -> Color(0xFF065F46)
                                                else -> MaterialTheme.colorScheme.outlineVariant
                                            }
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = mInfo.shortEn,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )

                                                if (hasActivity) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                if (isSelected) Color.White else Color(0xFF10B981),
                                                                CircleShape
                                                            )
                                                    )
                                                }
                                            }

                                            Text(
                                                text = mInfo.shortTe,
                                                fontSize = 11.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            // Subtitle: Activity or Spend
                                            if (hasActivity) {
                                                Text(
                                                    text = "₹${formatCompactAmount(activity.totalSpent)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else Color(0xFFB45309)
                                                )
                                            } else {
                                                Text(
                                                    text = if (isFuture) "Future" else "0 logs",
                                                    fontSize = 9.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                                    else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Selected Month Live Breakdown Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF3C7).copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${selectedMonthInfo.fullEn} $selectedYear",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF78350F)
                                    )
                                    Text(
                                        text = "${selectedMonthInfo.fullTe} $selectedYear నివేదిక సారాంశం",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Grand Outlay",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", currentSelectedActivity.totalSpent)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF59E0B).copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "⛽ Fuel Refills",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF78350F)
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.0f", currentSelectedActivity.fuelCost)} (${String.format(Locale.US, "%.1f", currentSelectedActivity.fuelLiters)} L)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF451A03)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "🔧 Maintenance",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF78350F)
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.0f", currentSelectedActivity.serviceCost)} (${currentSelectedActivity.serviceCount} srv)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF451A03)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "💸 Other Spends",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF78350F)
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.0f", currentSelectedActivity.expenseCost)} (${currentSelectedActivity.expenseCount} items)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF451A03)
                                    )
                                }
                            }

                            if (currentSelectedActivity.recordCount == 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF92400E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "No logs for this month. You can still generate a clean offline status record.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }

                    // 6. Downloaded Success Banner (if saved)
                    AnimatedVisibility(visible = showDownloadedSuccess) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFECFDF5)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF065F46),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Saved to Downloads Folder!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF065F46)
                                        )
                                        Text(
                                            text = "Downloads/ManaVahana/$savedFileName",
                                            fontSize = 11.sp,
                                            color = Color(0xFF047857),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    savedDownloadUri?.let { uri ->
                                        TextButton(
                                            onClick = { openPdfFile(context, uri) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Open PDF")
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        TextButton(
                                            onClick = { sharePdfFile(context, uri, savedFileName) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary: Download Report Button
                    Button(
                        onClick = {
                            triggerGenerateReport(
                                downloadImmediately = true,
                                previewImmediately = false,
                                shareImmediately = false
                            )
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("download_month_report_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB45309) // Terracotta Gold
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating PDF Report...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download ${selectedMonthInfo.shortEn} $selectedYear Report (డౌన్‌లోడ్)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Secondary: Preview & Share Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                triggerGenerateReport(
                                    downloadImmediately = false,
                                    previewImmediately = true,
                                    shareImmediately = false
                                )
                            },
                            enabled = !isGenerating,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("preview_month_report_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "View",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview (చూడండి)", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                triggerGenerateReport(
                                    downloadImmediately = false,
                                    previewImmediately = false,
                                    shareImmediately = true
                                )
                            },
                            enabled = !isGenerating,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("share_month_report_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share (షేర్)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class MonthActivity(
    val recordCount: Int,
    val totalSpent: Double,
    val fuelCost: Double,
    val fuelLiters: Double,
    val serviceCost: Double,
    val serviceCount: Int,
    val expenseCost: Double,
    val expenseCount: Int
)

private fun formatCompactAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format(Locale.US, "%.1fL", amount / 100000.0)
        amount >= 1000 -> String.format(Locale.US, "%.1fk", amount / 1000.0)
        else -> String.format(Locale.US, "%.0f", amount)
    }
}

private fun openPdfFile(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open Monthly Report PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
        // If view fails, try share
        sharePdfFile(context, uri, "ManaVahana Monthly Report")
    }
}

private fun sharePdfFile(context: Context, uri: Uri, title: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Monthly Report PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not open share chooser: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
