package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.ManaVahanaViewModel
import com.example.ui.pdf.PdfGenerator
import coil.compose.AsyncImage
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: ManaVahanaViewModel,
    onNavigateToAddVehicle: () -> Unit,
    onNavigateToVehicleDetails: (Int) -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToAddService: () -> Unit,
    onNavigateToAddFuel: () -> Unit
) {
    val context = LocalContext.current
    var generatedVehiclePdfUri by remember { mutableStateOf<Uri?>(null) }
    var showVehiclePdfSuccessDialog by remember { mutableStateOf(false) }
    var reportingVehicleName by remember { mutableStateOf("") }

    val vehicles by viewModel.vehicles.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allReminders by viewModel.pendingReminders.collectAsState()
    val allFuelLogs by viewModel.allFuelLogs.collectAsState()

    val totalVehicles = vehicles.size
    val currentMonthExpenses = remember(allExpenses) {
        val currentMonth = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        allExpenses.filter {
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(it.expenseDate)) == currentMonth
        }.sumOf { it.amount }
    }

    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val mileageValue by viewModel.selectedVehicleMileage.collectAsState(initial = 0.0)

    val allServiceLogs by viewModel.allServiceLogs.collectAsState()
    val lastOdometer = remember(selectedVehicle, allFuelLogs, allServiceLogs) {
        val fuelOdo = allFuelLogs.filter { it.vehicleId == selectedVehicle?.id }.maxOfOrNull { it.odometerReading } ?: 0.0
        val serviceOdo = allServiceLogs.filter { it.vehicleId == selectedVehicle?.id }.maxOfOrNull { it.odometerReading } ?: 0.0
        maxOf(fuelOdo, serviceOdo)
    }

    val selectedVehicleMonthExpenses = remember(selectedVehicle, allExpenses) {
        val currentMonth = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        allExpenses.filter {
            it.vehicleId == selectedVehicle?.id &&
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(it.expenseDate)) == currentMonth
        }.sumOf { it.amount }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logged-in User Profile Header
            item {
                val currentUser by viewModel.currentUserState.collectAsState()
                if (currentUser != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_user_header"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Dynamic User Avatar
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = currentUser!!.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "నమస్కారం, ${currentUser!!.name}!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = currentUser!!.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.70f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Welcoming Card & Core Telemetry vs Selected Primary Vehicle Card
            item {
            val currentVehicle = selectedVehicle
            if (currentVehicle != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToVehicleDetails(currentVehicle.id) }
                        .testTag("primary_vehicle_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Canvas(
                            modifier = Modifier
                                .size(120.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 20.dp, y = (-20).dp)
                        ) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.07f),
                                radius = size.minDimension / 2
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Primary Vehicle",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentVehicle.vehicleName,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        lineHeight = 30.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = currentVehicle.vehicleNumber.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!currentVehicle.vehicleImage.isNullOrBlank()) {
                                        AsyncImage(
                                            model = PathUtils.getResolutionFile(context, currentVehicle.vehicleImage) ?: currentVehicle.vehicleImage,
                                            contentDescription = "Vehicle Logo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = getVehicleIcon(currentVehicle.vehicleType),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "Last Odometer",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = if (lastOdometer > 0) "${String.format("%,.0f", lastOdometer)} km" else "0 km",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Monthly Expense",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "₹${String.format("%,.0f", selectedVehicleMonthExpenses)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "మాస నివేదిక / Monthly Sheet",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        val currentVehicle = selectedVehicle
                                        if (currentVehicle != null) {
                                            reportingVehicleName = currentVehicle.vehicleName
                                            val uri = PdfGenerator.generateVehicleMonthlyReport(
                                                context = context,
                                                vehicle = currentVehicle,
                                                expenses = allExpenses,
                                                fuelLogs = allFuelLogs,
                                                serviceLogs = allServiceLogs,
                                                reminders = allReminders
                                            )
                                            if (uri != null) {
                                                generatedVehiclePdfUri = uri
                                                showVehiclePdfSuccessDialog = true
                                                Toast.makeText(context, "Monthly PDF Report ready!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to generate vehicle PDF Report.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimary,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp).testTag("vehicle_report_btn_${currentVehicle.id}")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PDF Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "నమస్కారం! (Namaskaram)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Manage your vehicles elegantly with Telugu style.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Total Vehicles",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.61f)
                                )
                                Text(
                                    "$totalVehicles",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Column {
                                Text(
                                    "Expenses (This Month)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.61f)
                                )
                                Text(
                                    "₹${String.format("%.0f", currentMonthExpenses)}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Vehicle Selector
        item {
            Column {
                Text(
                    text = "Select Vehicle to Track",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (vehicles.isEmpty()) {
                    Button(
                        onClick = onNavigateToAddVehicle,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Your First Vehicle")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (vehicle in vehicles) {
                            val isSelected = selectedVehicle?.id == vehicle.id
                            val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            Card(
                                onClick = { viewModel.selectVehicle(vehicle.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .width(150.dp)
                                    .testTag("select_vehicle_${vehicle.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!vehicle.vehicleImage.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = PathUtils.getResolutionFile(context, vehicle.vehicleImage) ?: vehicle.vehicleImage,
                                                    contentDescription = "Vehicle Grid Image",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = getVehicleIcon(vehicle.vehicleType),
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                reportingVehicleName = vehicle.vehicleName
                                                val uri = PdfGenerator.generateVehicleMonthlyReport(
                                                    context = context,
                                                    vehicle = vehicle,
                                                    expenses = allExpenses,
                                                    fuelLogs = allFuelLogs,
                                                    serviceLogs = allServiceLogs,
                                                    reminders = allReminders
                                                )
                                                if (uri != null) {
                                                    generatedVehiclePdfUri = uri
                                                    showVehiclePdfSuccessDialog = true
                                                    Toast.makeText(context, "${vehicle.vehicleName} PDF Report ready!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to generate vehicle PDF Report.", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = "PDF Report",
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = vehicle.vehicleName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = vehicle.vehicleNumber,
                                        fontSize = 11.sp,
                                        color = contentColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick action row
        item {
            Column {
                Text(
                    text = "Quick Actions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionChip(
                        icon = Icons.Default.DirectionsCar,
                        label = "Add Vehicle",
                        onClick = onNavigateToAddVehicle,
                        tag = "qa_add_vehicle"
                    )
                    QuickActionChip(
                        icon = Icons.Default.Payments,
                        label = "Add Expense",
                        onClick = onNavigateToAddExpense,
                        enabled = vehicles.isNotEmpty(),
                        tag = "qa_add_expense"
                    )
                    QuickActionChip(
                        icon = Icons.Default.Build,
                        label = "Add Service",
                        onClick = onNavigateToAddService,
                        enabled = vehicles.isNotEmpty(),
                        tag = "qa_add_service"
                    )
                    QuickActionChip(
                        icon = Icons.Default.LocalGasStation,
                        label = "Fill Fuel",
                        onClick = onNavigateToAddFuel,
                        enabled = vehicles.isNotEmpty(),
                        tag = "qa_add_fuel"
                    )
                }
            }
        }

        // Selected vehicle statistics (Mileage & analytics)
        if (selectedVehicle != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "${selectedVehicle?.vehicleName} Efficiency",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Based on local fuel logs",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (mileageValue > 0) "${String.format("%.2f", mileageValue)} km/L" else "N/A",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Avg Mileage",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Pie Chart Section for Expenses
        if (allExpenses.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Expense Analytics (Categorywise Distribution)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ExpensePieChartCard(expenses = allExpenses)
                }
            }
        }

        // Line Graph section for fuel prices / monthly logs
        if (allFuelLogs.size >= 2) {
            item {
                Column {
                    Text(
                        text = "Fuel Price Trend",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FuelPriceLineChartCard(logs = allFuelLogs)
                }
            }
        }

        // Renewals checklist Section
        if (allReminders.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Upcoming Renewals & Expiries",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (reminder in allReminders.take(4)) {
                            val isUrgent = (reminder.reminderDate - System.currentTimeMillis()) < 15 * 24 * 60 * 60 * 1000L
                            val containerColor = if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                            val contentColor = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                            val iconColor = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                border = BorderStroke(1.dp, if (isUrgent) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleReminderCompleted(reminder) }
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = when (reminder.category) {
                                                "Insurance" -> Icons.Default.Shield
                                                "Pollution" -> Icons.Default.Co2
                                                "Service" -> Icons.Default.Build
                                                "EMI" -> Icons.Default.Payments
                                                else -> Icons.Default.Notifications
                                            },
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                reminder.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = contentColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                reminder.description,
                                                fontSize = 11.sp,
                                                color = contentColor.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    val simpleFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
                                    Text(
                                        simpleFormatter.format(Date(reminder.reminderDate)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVehiclePdfSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showVehiclePdfSuccessDialog = false },
            title = { Text("Vehicle PDF Generated") },
            text = {
                Text("The monthly executive offline workbook/status report for \"$reportingVehicleName\" has been generated and compiled successfully.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVehiclePdfSuccessDialog = false
                        generatedVehiclePdfUri?.let { uri ->
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share $reportingVehicleName Status Report"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "No app available to handle PDF sharing.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share/Print Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVehiclePdfSuccessDialog = false }) { Text("Close") }
            }
        )
    }
}
}

@Composable
fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tag: String
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        leadingIcon = { 
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(18.dp),
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ) 
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = MaterialTheme.colorScheme.secondary
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag(tag)
    )
}

fun getVehicleIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        "Bike" -> Icons.Default.TwoWheeler
        "Car" -> Icons.Default.DirectionsCar
        "Auto" -> Icons.Default.ElectricRickshaw
        "Truck" -> Icons.Default.LocalShipping
        else -> Icons.Default.ElectricCar
    }
}

// Custom built Pie Chart
@Composable
fun ExpensePieChartCard(expenses: List<Expense>) {
    val categoryMap = remember(expenses) {
        expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val totalAmount = categoryMap.values.sum()

    // Assign lovely custom colours to categories
    val colors = listOf(
        Color(0xFFE53935), // Red - Repairs
        Color(0xFF1E88E5), // Blue - Fuel
        Color(0xFFFFB300), // Amber - Insurance
        Color(0xFF43A047), // Green - Washing
        Color(0xFF8E24AA), // Purple - Accessories
        Color(0xFFD81B60), // Pink - Parking
        Color(0xFF00ACC1), // Cyan - Toll
        Color(0xFFF4511E)  // Orange - Miscellaneous
    )

    val expenseCategories = listOf("Repairs", "Fuel", "Insurance", "Washing", "Accessories", "Parking", "Toll", "Miscellaneous")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Draw Pie Segment
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .weight(1.2f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    categoryMap.forEach { (cat, amt) ->
                        val index = expenseCategories.indexOf(cat).coerceAtLeast(0) % colors.size
                        val sweepAngle = ((amt / totalAmount) * 360f).toFloat()
                        drawArc(
                            color = colors[index],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            size = Size(size.width, size.height),
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("₹${String.format("%.0f", totalAmount)}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Graph Legend panel
            Column(
                modifier = Modifier.weight(1.8f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categoryMap.entries.sortedByDescending { it.value }.take(5).forEach { (cat, amt) ->
                    val index = expenseCategories.indexOf(cat).coerceAtLeast(0) % colors.size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(colors[index], CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(cat, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            "₹${String.format("%.0f", amt)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// Custom built Line Chart representing fuel trends
@Composable
fun FuelPriceLineChartCard(logs: List<FuelLog>) {
    val sortedLogs = remember(logs) {
        logs.sortedBy { it.fuelDate }.takeLast(7)
    }

    val maxPrice = sortedLogs.maxOf { it.pricePerLiter }.toFloat()
    val minPrice = sortedLogs.minOf { it.pricePerLiter }.toFloat()
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Price Per Liter (Last 7 fills)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Min: ₹${String.format("%.1f", minPrice)} - Max: ₹${String.format("%.1f", maxPrice)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val graphWidth = size.width
                val graphHeight = size.height
                val pointsCount = sortedLogs.size

                val stepX = graphWidth / (pointsCount - 1).coerceAtLeast(1)
                val points = sortedLogs.mapIndexed { i, log ->
                    val x = i * stepX
                    val ratio = (log.pricePerLiter.toFloat() - minPrice) / priceRange
                    val y = graphHeight - (ratio * (graphHeight - 20f)) - 10f
                    Offset(x, y)
                }

                // Draw horizontal guidelines
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, 10f),
                    end = Offset(graphWidth, 10f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, graphHeight - 10f),
                    end = Offset(graphWidth, graphHeight - 10f),
                    strokeWidth = 1f
                )

                // Plot connection paths
                val path = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFFE65100),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw coordinate node anchors
                points.forEach { pt ->
                    drawCircle(
                        color = Color(0xFFFFB300),
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}
