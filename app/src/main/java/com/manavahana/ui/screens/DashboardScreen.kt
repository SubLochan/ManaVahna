package com.manavahana.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.manavahana.ui.AppUpdateHelper
import com.manavahana.ui.UpdateStatus
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import com.manavahana.data.model.*
import com.manavahana.ui.ManaVahanaViewModel
import com.manavahana.ui.pdf.PdfGenerator
import com.manavahana.ui.Localizer
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
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val langCode = selectedLanguage ?: "en"
    var generatedVehiclePdfUri by remember { mutableStateOf<Uri?>(null) }
    var showVehiclePdfSuccessDialog by remember { mutableStateOf(false) }
    var reportingVehicleName by remember { mutableStateOf("") }

    val updateHelper = remember { AppUpdateHelper.getInstance(context) }
    val updateStatus by updateHelper.updateStatus.collectAsState()
    val livePlayStoreVersion by updateHelper.livePlayStoreVersion.collectAsState()

    val updateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.e("DashboardScreen", "In-app update aborted or failed: ${result.resultCode}")
        }
    }

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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E)), // Premium pitch-black carbon darkness as shown in mockup
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(bottom = 96.dp, top = 20.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header Top Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Golden Crown Profile Emblem
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F1F23))
                                .border(BorderStroke(2.dp, Color(0xFFD4AF37)), CircleShape), // Ornate gold frame
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "ManaVahana Crown Emblem",
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            val userName = viewModel.currentUserState.value?.name ?: "User"
                            val greeting = if (langCode == "te") "నమస్కారం, $userName!" else if (langCode == "hi") "नमस्ते, $userName!" else "Hello, $userName!"
                            Text(
                                text = greeting,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Hyderabad, IN",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F1F23))
                                .clickable {
                                    Toast.makeText(context, "Search Filter triggered!", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Vehicles",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Notification alert bell
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F1F23))
                                .clickable {
                                    Toast.makeText(context, "Scanning for reminders...", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Upcoming Reminders",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            
                            // Glowing notification dot mapping active expirations size
                            val pinCount = if (allReminders.isNotEmpty()) allReminders.size else 1
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$pinCount",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Play Store In-App Updates
            if (updateStatus is UpdateStatus.UpdateAvailable) {
                val status = updateStatus as UpdateStatus.UpdateAvailable
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_update_notification_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = "అప్‌డేట్",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = Localizer.get("update_available_title", langCode),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val versionStr = if (livePlayStoreVersion != "Retrieving..." && livePlayStoreVersion != "Not checked yet") {
                                livePlayStoreVersion
                            } else {
                                "1.6"
                            }

                            Text(
                                text = Localizer.get("update_available_desc", langCode).replace("%1\$s", versionStr),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { updateHelper.resetStatus() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier.testTag("update_later_button")
                                ) {
                                    Text(Localizer.get("update_later", langCode), fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                val activity = context as? Activity
                                Button(
                                    onClick = {
                                        if (status.isSimulation) {
                                            activity?.let { updateHelper.openPlayStore(it) }
                                        } else {
                                            val info = status.appUpdateInfo
                                            if (activity != null && info != null) {
                                                updateHelper.launchRealUpdate(
                                                    activity = activity,
                                                    appUpdateInfo = info,
                                                    launcher = updateLauncher,
                                                    isFlexible = status.isFlexibleAllowed
                                                )
                                            } else {
                                                activity?.let { updateHelper.openPlayStore(it) }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("update_now_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Localizer.get("update_now", langCode), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // PRIMARY VEHICLE GRAPHIC HERO DISPLAY (Vivid Orange Card)
            item {
                val currentVeh = selectedVehicle
                if (currentVeh != null) {
                    // Clickable Hero Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("primary_vehicle_card")
                            .clickable { onNavigateToVehicleDetails(currentVeh.id) },
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFA000) // Beautiful bright yellow/orange exactly like screenshot
                        )
                    ) {
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
                                        color = Color.Black.copy(alpha = 0.65f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentVeh.vehicleName,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black,
                                        lineHeight = 28.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Hyundai\n${currentVeh.vehicleType ?: "Car"}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Mileage: ${if (mileageValue > 0) "${String.format("%.1f", mileageValue)} km/L" else "N/A"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black.copy(alpha = 0.6f)
                                    )
                                }

                                // Interactive action chevron
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.35f))
                                        .clickable { onNavigateToVehicleDetails(currentVeh.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Details",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = getVehicleIcon(currentVeh.vehicleType),
                                        contentDescription = null,
                                        tint = Color.Black.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = currentVeh.vehicleNumber.uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black.copy(alpha = 0.8f)
                                    )
                                }

                                Text(
                                    text = if (lastOdometer > 0) "${String.format("%,.0f", lastOdometer)} km \u2022 Odo" else "No Odo yet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pager indicator dots (clicking changes selected vehicle if multiple exist!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        vehicles.take(4).forEachIndexed { index, veh ->
                            val isSelected = veh.id == currentVeh.id
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (isSelected) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFFFA000) else Color.DarkGray)
                                    .clickable { viewModel.selectVehicle(veh.id) }
                            )
                        }
                    }
                } else {
                    // Fallback visual banner when no vehicle exists yet
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAddVehicle() },
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F23)),
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (langCode == "te") "వాహనాన్ని జోడించండి" else if (langCode == "hi") "अपना वाहन जोड़ें" else "Add Your Vehicle",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (langCode == "te") {
                                    "వేగ పరిమితులు, ఆయిల్ మెట్రిక్స్ మరియు పత్రాలను ఆఫ్‌లైన్‌లో ట్రాక్ చేయండి."
                                } else if (langCode == "hi") {
                                    "गति सीमा, तेल मेट्रिक्स और दस्तावेज़ों को ऑफ़लाइन ट्रैक करें।"
                                } else {
                                    "Track speed limits, oil metrics, and documents offline."
                                },
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // SELECT VEHICLE TO TRACK CAROUSEL SECTION
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Vehicle to Track",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        
                        Text(
                            text = "See All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFA000),
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Filtering complete list...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Custom Odometer Gauge Card
                        Card(
                            modifier = Modifier
                                .width(155.dp)
                                .height(130.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1D))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Draw beautiful Speed Gauge dial vector dynamically
                                Box(
                                    modifier = Modifier
                                        .size(45.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        // Speed dial arc tracker
                                        drawArc(
                                            color = Color.DarkGray,
                                            startAngle = 180f,
                                            sweepAngle = 180f,
                                            useCenter = false,
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                        drawArc(
                                            color = Color(0xFF4CAF50), // Green progress arc
                                            startAngle = 180f,
                                            sweepAngle = 120f,
                                            useCenter = false,
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                        // Red dial pointer needle
                                        val needleLength = 16.dp.toPx()
                                        drawLine(
                                            color = Color.Red,
                                            start = Offset(size.width / 2, size.height),
                                            end = Offset(size.width / 2 + needleLength * 0.707f, size.height - needleLength * 0.707f),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp).align(Alignment.BottomCenter)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Odometer",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = if (lastOdometer > 0) "${String.format("%,.0f", lastOdometer)} km" else "0 km",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Card 2: Custom Monthly Expense Active Tracker (Highlighted in mockup)
                        Card(
                            modifier = Modifier
                                .width(155.dp)
                                .height(130.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF242220)), // Subtle highlight tint
                            border = BorderStroke(1.dp, Color(0xFFFFA000)) // Highlight border exactly like screenshot
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFA000).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Monthly Expense",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "\u20b9${String.format("%,.0f", selectedVehicleMonthExpenses)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFA000)
                                    )
                                }
                            }
                        }

                        // Card 3: Switch Selectable Vehicles list
                        vehicles.forEach { veh ->
                            val isSelected = selectedVehicle?.id == veh.id
                            if (!isSelected) {
                                Card(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(130.dp)
                                        .clickable { viewModel.selectVehicle(veh.id) }
                                        .testTag("select_vehicle_${veh.id}"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                                    border = BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getVehicleIcon(veh.vehicleType),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = veh.vehicleName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = veh.vehicleNumber.uppercase(),
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // QUICK ACTIONS GRID SECTION
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Actions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "See All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFA000)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action 1: Add Expense
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B1B1D))
                                .clickable { onNavigateToAddExpense() }
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA000).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "Add Expense",
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Add Expense",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // Action 2: Add Service
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B1B1D))
                                .clickable { onNavigateToAddService() }
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA000).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Add Service Log",
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Add Service Log",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // Action 3: Boost Documents
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B1B1D))
                                .clickable { onNavigateToAddFuel() }
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA000).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Vault Docs",
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Boost Documents",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // EXECUTIVE OFFLINE REPORT ACTION BAR
            if (selectedVehicle != null) {
                item {
                    val activeVeh = selectedVehicle!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_report_generative_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1D)),
                        border = BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (langCode == "te") "మాస నివేదిక" else if (langCode == "hi") "मासिक शीट" else "Monthly Sheet",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Export active logs offline.",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    reportingVehicleName = activeVeh.vehicleName
                                    val uri = PdfGenerator.generateVehicleMonthlyReport(
                                        context = context,
                                        vehicle = activeVeh,
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
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFA000),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("vehicle_report_btn_${activeVeh.id}")
                            ) {
                                Text("PDF Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // UPCOMING REMINDERS & TO-DOS SECTION (mockup "Not inks")
            if (allReminders.isNotEmpty()) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Logs & Expiries",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "See All",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFA000)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (reminder in allReminders.take(4)) {
                                val isUrgent = (reminder.reminderDate - System.currentTimeMillis()) < 15 * 24 * 60 * 60 * 1000L
                                val containerColor = if (isUrgent) Color(0xFF2E191A) else Color(0xFF1B1B1D)
                                val borderStrokeColor = if (isUrgent) Color.Red.copy(alpha = 0.5f) else Color.DarkGray
                                val iconColor = if (isUrgent) Color.Red else Color(0xFFFFA000)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = containerColor),
                                    border = BorderStroke(1.dp, borderStrokeColor),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleReminderCompleted(reminder) }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.05f)),
                                                contentAlignment = Alignment.Center
                                            ) {
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
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    reminder.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    reminder.description,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        val simpleFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
                                        Text(
                                            simpleFormatter.format(Date(reminder.reminderDate)),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isUrgent) Color.Red else Color(0xFFFFA000)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ANALYTICS & PIE CHART SECTION
            if (allExpenses.isNotEmpty()) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Expense Analytics",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        ExpensePieChartCard(expenses = allExpenses)
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
