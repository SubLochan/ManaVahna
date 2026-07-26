package com.manavahana.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.manavahana.ui.AppUpdateHelper
import com.manavahana.ui.UpdateStatus
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
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
    onNavigateToAddFuel: () -> Unit,
    onNavigateToVault: () -> Unit
) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val langCode = selectedLanguage ?: "en"
    var generatedVehiclePdfUri by remember { mutableStateOf<Uri?>(null) }
    var showVehiclePdfSuccessDialog by remember { mutableStateOf(false) }
    var reportingVehicleName by remember { mutableStateOf("") }
    var showRemindersDialog by remember { mutableStateOf(false) }
    var notificationBadgeEnabled by remember { mutableStateOf(true) }

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
    val allServiceLogs by viewModel.allServiceLogs.collectAsState()

    val totalVehicles = vehicles.size

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val currentMonthExpenses = remember(allExpenses, allServiceLogs) {
        val currentMonth = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        val expensesSum = allExpenses.filter {
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(it.expenseDate)) == currentMonth
        }.sumOf { it.amount }
        val servicesSum = allServiceLogs.filter {
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(it.serviceDate)) == currentMonth
        }.sumOf { it.cost }
        expensesSum + servicesSum
    }

    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val mileageValue by viewModel.selectedVehicleMileage.collectAsState(initial = 0.0)

    val lastOdometer = remember(selectedVehicle, allFuelLogs, allServiceLogs) {
        val fuelOdo = allFuelLogs.filter { it.vehicleId == selectedVehicle?.id }.maxOfOrNull { it.odometerReading } ?: 0.0
        val serviceOdo = allServiceLogs.filter { it.vehicleId == selectedVehicle?.id }.maxOfOrNull { it.odometerReading } ?: 0.0
        maxOf(fuelOdo, serviceOdo)
    }

    val selectedVehicleMonthExpenses = remember(selectedVehicle, allExpenses, allServiceLogs) {
        val currentMonth = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        val expensesSum = allExpenses.filter {
            it.vehicleId == selectedVehicle?.id &&
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(it.expenseDate)) == currentMonth
        }.sumOf { it.amount }
        val servicesSum = allServiceLogs.filter {
            it.vehicleId == selectedVehicle?.id &&
            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(it.serviceDate)) == currentMonth
        }.sumOf { it.cost }
        expensesSum + servicesSum
    }

    val isPinLockEnabled by viewModel.isPinLockEnabled.collectAsState()
    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsState()
    val isBiometricPromptShown by viewModel.isBiometricPromptShown.collectAsState()

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
                            val greeting = if (langCode == "te") "నమస్కారం!" else if (langCode == "hi") "नमस्ते!" else "Hello, Rider!"
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
                                    showRemindersDialog = true
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
                            if (notificationBadgeEnabled) {
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
                                        // Update local version preference storage to correspond to latest version
                                        viewModel.updateSimulatedAppVersion(versionStr)
                                        
                                        // Launch real Google Play Store updater flow or redirect to store details
                                        val info = status.appUpdateInfo
                                        if (activity != null) {
                                            if (info != null && !status.isSimulation) {
                                                updateHelper.launchRealUpdate(
                                                    activity = activity,
                                                    appUpdateInfo = info,
                                                    launcher = updateLauncher,
                                                    isFlexible = status.isFlexibleAllowed
                                                )
                                            } else {
                                                updateHelper.openPlayStore(activity)
                                            }
                                        }
                                        updateHelper.resetStatus()
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

            // Biometric / Fingerprint shortcut suggestion prompt
            if (isPinLockEnabled && !isFingerprintEnabled && !isBiometricPromptShown) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_suggestion_prompt_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Fingerprint shortcut suggestion",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = if (langCode == "te") "బయోమెట్రిక్ సత్వరమార్గం యాక్టివేట్ చేయండి" else "Enable Biometric Shortcuts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (langCode == "te") {
                                    "సెక్యూరిటీ పిన్‌ను టైప్ చేసే బదులు మీ వేలిముద్రతో సులభంగా మరియు వేగంగా వాహన యాప్‌ ప్రవేశించండి!"
                                } else {
                                    "Use your device's fingerprint scanning to authorize access instantly, bypassing PIN prompt screens."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { 
                                        viewModel.setBiometricPromptShown(true) 
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("biometric_skip_prompt_button")
                                ) {
                                    Text(
                                        text = if (langCode == "te") "దాటవేయి" else "Skip", 
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Button(
                                    onClick = {
                                        viewModel.setFingerprintEnabled(true)
                                        viewModel.setBiometricPromptShown(true)
                                        Toast.makeText(context, if (langCode == "te") "బయోమెట్రిక్ విజయవంతంగా లింక్ చేయబడింది!" else "Biometrics linked successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("biometric_enable_prompt_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (langCode == "te") "యాక్టివేట్ చేయి" else "Enable Shortcut", 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PRIMARY VEHICLE GRAPHIC HERO DISPLAY (Vivid Orange Card)
            item {
                val currentVeh = selectedVehicle
                if (currentVeh != null && vehicles.isNotEmpty()) {
                    val initialIndex = remember(vehicles, currentVeh) {
                        val idx = vehicles.indexOfFirst { it.id == currentVeh.id }
                        if (idx >= 0) idx else 0
                    }
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                        initialPage = initialIndex,
                        pageCount = { vehicles.size }
                    )

                    // Synchronize Page Swipes with Selected Vehicle state
                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage < vehicles.size) {
                            val activeVeh = vehicles[pagerState.currentPage]
                            if (activeVeh.id != currentVeh.id) {
                                viewModel.selectVehicle(activeVeh.id)
                            }
                        }
                    }

                    // Also synchronize programmatic selectedVehicle changes back to pagerState!
                    LaunchedEffect(currentVeh) {
                        val currentIdx = vehicles.indexOfFirst { it.id == currentVeh.id }
                        if (currentIdx >= 0 && currentIdx != pagerState.currentPage) {
                            pagerState.animateScrollToPage(currentIdx)
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            val pageVeh = vehicles[page]
                            val pageLastOdometer = remember(pageVeh, allFuelLogs, allServiceLogs) {
                                val fuelOdo = allFuelLogs.filter { it.vehicleId == pageVeh.id }.maxOfOrNull { it.odometerReading } ?: 0.0
                                val serviceOdo = allServiceLogs.filter { it.vehicleId == pageVeh.id }.maxOfOrNull { it.odometerReading } ?: 0.0
                                maxOf(fuelOdo, serviceOdo)
                            }

                            // Clickable Hero Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("primary_vehicle_card_${pageVeh.id}")
                                    .clickable { onNavigateToVehicleDetails(pageVeh.id) },
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
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Primary Vehicle",
                                                modifier = Modifier,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.Black.copy(alpha = 0.65f)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = pageVeh.vehicleName,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black,
                                                lineHeight = 28.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "${pageVeh.brand} ${pageVeh.model}\n${pageVeh.vehicleType ?: "Car"}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Mileage: ${if (mileageValue > 0 && pageVeh.id == currentVeh.id) "${String.format("%.1f", mileageValue)} km/L" else "N/A"}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black.copy(alpha = 0.6f)
                                            )
                                        }

                                        val heroFileExists = if (!pageVeh.vehicleImage.isNullOrBlank() && (pageVeh.vehicleImage.startsWith("file://") || pageVeh.vehicleImage.startsWith("/"))) {
                                            val file = PathUtils.getResolutionFile(context, pageVeh.vehicleImage)
                                            file != null && file.exists()
                                        } else {
                                            true
                                        }

                                        if (!pageVeh.vehicleImage.isNullOrBlank() && heroFileExists) {
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 150.dp, height = 150.dp) // Noticeably INCREASED image size!
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color.White.copy(alpha = 0.25f))
                                                    .border(1.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                            ) {
                                                AsyncImage(
                                                    model = PathUtils.getResolutionUriString(context, pageVeh.vehicleImage),
                                                    contentDescription = "Vehicle Image",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Interactive action chevron
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.35f))
                                                .clickable { onNavigateToVehicleDetails(pageVeh.id) },
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

                                    Spacer(modifier = Modifier.height(16.dp))

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
                                                imageVector = getVehicleIcon(pageVeh.vehicleType),
                                                contentDescription = null,
                                                tint = Color.Black.copy(alpha = 0.8f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = pageVeh.vehicleNumber.uppercase(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black.copy(alpha = 0.8f)
                                            )
                                        }

                                        Text(
                                            text = if (pageLastOdometer > 0) "${String.format("%,.0f", pageLastOdometer)} km \u2022 Odo" else "No Odo yet",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sync indicator dots representing all vehicles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            vehicles.forEachIndexed { index, veh ->
                                val isSelected = index == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isSelected) 10.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFFFA000) else Color.DarkGray)
                                        .clickable {
                                            viewModel.selectVehicle(veh.id)
                                        }
                                )
                            }
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
                                .height(130.dp)
                                .clickable {
                                    if (selectedVehicle != null) {
                                        Toast.makeText(context, "Generating Monthly PDF Report...", Toast.LENGTH_SHORT).show()
                                        val uri = PdfGenerator.generateVehicleMonthlyReport(
                                            context = context,
                                            vehicle = selectedVehicle!!,
                                            expenses = allExpenses,
                                            fuelLogs = allFuelLogs,
                                            serviceLogs = allServiceLogs,
                                            reminders = allReminders
                                        )
                                        if (uri != null) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/pdf")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Open Monthly Expenses Report"))
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                try {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/pdf"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Monthly Expenses Report"))
                                                } catch (ex: Exception) {
                                                    ex.printStackTrace()
                                                    Toast.makeText(context, "No app available to open or share PDF.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Failed to generate PDF Report.", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Please add or select a vehicle first.", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .testTag("monthly_expenses_report_card"),
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
                            
                            val borderStroke = if (isSelected) {
                                BorderStroke(
                                    2.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFA000).copy(alpha = glowAlpha),
                                            Color(0xFFFFB300),
                                            Color(0xFFFFD54F).copy(alpha = glowAlpha)
                                        )
                                    )
                                )
                            } else {
                                BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.5f))
                            }
                            
                            val cardBgColor = if (isSelected) {
                                Color(0xFF2E261F)
                            } else {
                                Color(0xFF1C1C1E)
                            }
                            
                            val cardModifier = if (isSelected) {
                                Modifier
                                    .width(150.dp)
                                    .height(130.dp)
                                    .shadow(
                                        elevation = (8 * glowAlpha).dp,
                                        shape = RoundedCornerShape(18.dp),
                                        clip = false,
                                        ambientColor = Color(0xFFFFA000),
                                        spotColor = Color(0xFFFFA000)
                                    )
                                    .clickable { viewModel.selectVehicle(veh.id) }
                                    .testTag("select_vehicle_${veh.id}")
                            } else {
                                Modifier
                                    .width(150.dp)
                                    .height(130.dp)
                                    .clickable { viewModel.selectVehicle(veh.id) }
                                    .testTag("select_vehicle_${veh.id}")
                            }

                            Card(
                                modifier = cardModifier,
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                border = borderStroke
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
                                            .background(
                                                if (isSelected) Color(0xFFFFA000).copy(alpha = 0.15f)
                                                else Color.White.copy(alpha = 0.05f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val resolvedUri = PathUtils.getResolutionUriString(context, veh.vehicleImage)
                                        val fileExists = if (!veh.vehicleImage.isNullOrBlank() && (veh.vehicleImage.startsWith("file://") || veh.vehicleImage.startsWith("/"))) {
                                            val file = PathUtils.getResolutionFile(context, veh.vehicleImage)
                                            file != null && file.exists()
                                        } else {
                                            true
                                        }

                                        if (!veh.vehicleImage.isNullOrBlank() && fileExists) {
                                            AsyncImage(
                                                model = resolvedUri,
                                                contentDescription = "Vehicle Grid Image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = getVehicleIcon(veh.vehicleType),
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFFFFA000) else Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = veh.vehicleName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFFFFA000) else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = veh.vehicleNumber.uppercase(),
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color(0xFFFFD54F).copy(alpha = 0.8f) else Color.Gray
                                        )
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
                        // Action 1: Fill Fuel
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B1B1D))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .clickable { onNavigateToAddFuel() }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA000).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = "Fill Fuel",
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (langCode == "te") "ఇంధనం పూరించండి" else "Fill Fuel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (langCode == "te") "మైలేజ్ మరియు ఇంధన ఖర్చులు" else "Log fuel logs & mileage efficiency",
                                fontSize = 8.5.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Action 2: Add Service
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B1B1D))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .clickable { onNavigateToAddService() }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA000).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Add Service Log",
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (langCode == "te") "సర్వీస్ రికార్డ్" else "Add Service",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (langCode == "te") "ఇంజిన్ ఆయిల్ మరియు రిపేర్లు" else "Track oil, filters & parts logs",
                                fontSize = 8.5.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Action 3: Boost Documents
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B1B1D))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .clickable { onNavigateToVault() }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA000).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Vault Docs",
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (langCode == "te") "డాక్యుమెంట్ వాల్ట్" else "Vault Docs",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (langCode == "te") "RC, ఇన్సూరెన్స్ సురక్షితం" else "Securely store RC, PUC cards",
                                fontSize = 8.5.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
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

    if (showRemindersDialog) {
        AlertDialog(
            onDismissRequest = {
                notificationBadgeEnabled = false
                showRemindersDialog = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications Icon",
                        tint = Color(0xFFFFA000)
                    )
                    Text(
                        text = "Missed Notifications",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    val displayedNotifications = remember(allReminders) {
                        if (allReminders.isEmpty()) {
                            listOf(
                                Reminder(
                                    title = "Periodic Service Overdue",
                                    description = "Your primary vehicle is overdue for periodic engine oil filter replacement.",
                                    reminderDate = System.currentTimeMillis() - 12 * 3600 * 1000L,
                                    category = "Service",
                                    isCompleted = false,
                                    vehicleId = null
                                ),
                                Reminder(
                                    title = "Insurance Expiry Renewal",
                                    description = "Immediate renewal recommended to prevent standard coverage lapse.",
                                    reminderDate = System.currentTimeMillis() - 2 * 24 * 3600 * 1000L,
                                    category = "Insurance",
                                    isCompleted = false,
                                    vehicleId = null
                                ),
                                Reminder(
                                    title = "PUC Pollution Check",
                                    description = "Your PUC certificate is nearing its safety emission standard threshold.",
                                    reminderDate = System.currentTimeMillis() - 3 * 24 * 3600 * 1000L,
                                    category = "Pollution",
                                    isCompleted = false,
                                    vehicleId = null
                                )
                            )
                        } else {
                            allReminders
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(displayedNotifications) { reminder ->
                            val simpleFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1D)),
                                border = BorderStroke(1.dp, Color.DarkGray),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
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
                                            tint = Color(0xFFFFA000),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reminder.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = reminder.description,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Missed: " + simpleFormatter.format(Date(reminder.reminderDate)),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Red.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        notificationBadgeEnabled = false
                        showRemindersDialog = false
                    }
                ) {
                    Text("Close")
                }
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

// Custom built interactive Pie/Donut Chart with premium Material 3 styling
@Composable
fun ExpensePieChartCard(expenses: List<Expense>) {
    val categoryMap = remember(expenses) {
        expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val totalAmount = categoryMap.values.sum()
    if (totalAmount <= 0) return

    val colors = listOf(
        Color(0xFFEF5350), // Red - Service
        Color(0xFF42A5F5), // Blue - Fuel
        Color(0xFFFFCA28), // Amber - Insurance
        Color(0xFF66BB6A), // Green - Washing
        Color(0xFFAB47BC), // Purple - Accessories
        Color(0xFFEC407A), // Pink - Parking
        Color(0xFF26C6DA), // Cyan - Toll
        Color(0xFFFF7043)  // Orange - Miscellaneous
    )

    val expenseCategories = listOf("Service", "Fuel", "Insurance", "Washing", "Accessories", "Parking", "Toll", "Miscellaneous")

    // Retrieve active language code or fallback to English
    val context = LocalContext.current
    val activity = context as? Activity
    val intentLang = activity?.intent?.getStringExtra("lang") ?: "en"

    // Component tab switcher
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Donut Visualizer, 1 for Dynamic Ledger Breakdowns
    
    // Interactive segment highlight (auto-select category with maximum expense initially)
    var selectedCategory by remember(categoryMap) { 
        mutableStateOf(categoryMap.keys.maxByOrNull { categoryMap[it] ?: 0.0 } ?: "Fuel") 
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_analytics_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0E)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Modern Header & High-level Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (intentLang == "te") "మొత్తం ఖర్చుల విశ్లేషణ" else "Expense Analytics",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₹${String.format("%,.0f", totalAmount)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Smooth styled micro Tab Indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = if (intentLang == "te") listOf("చార్ట్", "వివరాలు") else listOf("Chart", "Breakdown")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.Gray
                            )
                        }
                    }
                }
            }

            // Interactive insight chip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val highestCat = categoryMap.maxByOrNull { it.value }?.key ?: "None"
                    val highestAmt = categoryMap[highestCat] ?: 0.0
                    val highestPercentage = if (totalAmount > 0) (highestAmt / totalAmount) * 100 else 0.0
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFFA000), CircleShape)
                    )
                    Text(
                        text = if (intentLang == "te") {
                            "ప్రధాన వ్యయం: ${highestCat} (~${String.format("%.0f", highestPercentage)}%)"
                        } else {
                            "Heavy Source: ${highestCat} accounts for ${String.format("%.0f", highestPercentage)}% of budget"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.LightGray
                    )
                }
            }

            // Main Tab View switcher block
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "AnalyticsTabTransition"
            ) { targetTab ->
                if (targetTab == 0) {
                    // INTERACTIVE DONUT VIEW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glowing Donut Ring Canvas (Tap highlights active category)
                        Box(
                            modifier = Modifier
                                .size(135.dp)
                                .weight(1.2f),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                var startAngle = -90f
                                categoryMap.forEach { (cat, amt) ->
                                    val index = expenseCategories.indexOf(cat).coerceAtLeast(0) % colors.size
                                    val sweepAngle = ((amt / totalAmount) * 360f).toFloat()
                                    val isSelected = cat == selectedCategory
                                    
                                    val strokeWidth = if (isSelected) 18.dp.toPx() else 11.dp.toPx()
                                    val diameterPadding = if (isSelected) 3.dp.toPx() else 8.dp.toPx()
                                    
                                    drawArc(
                                        color = colors[index],
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        size = Size(size.width - diameterPadding, size.height - diameterPadding),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                        topLeft = Offset(diameterPadding / 2, diameterPadding / 2)
                                    )
                                    startAngle += sweepAngle
                                }
                            }

                            // Donut Center - Displays Live Highlight Data
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                val activeAmount = categoryMap[selectedCategory] ?: 0.0
                                val activePercentage = (activeAmount / totalAmount) * 100
                                
                                Text(
                                    text = selectedCategory,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format("%.0f", activeAmount)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "${String.format("%.1f", activePercentage)}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Interactive Legend matching Donut segments
                        Column(
                            modifier = Modifier.weight(1.8f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categoryMap.entries.sortedByDescending { it.value }.take(5).forEach { (cat, amt) ->
                                val index = expenseCategories.indexOf(cat).coerceAtLeast(0) % colors.size
                                val isSelected = cat == selectedCategory
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.06f) else Color.Transparent)
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(colors[index], CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cat,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "₹${String.format("%.0f", amt)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // DYNAMIC CATEGORY BUDGET PROGRESS LIST
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categoryMap.entries.sortedByDescending { it.value }.take(6).forEach { (cat, amt) ->
                            val index = expenseCategories.indexOf(cat).coerceAtLeast(0) % colors.size
                            val portion = (amt / totalAmount).toFloat()
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(colors[index], CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "₹${String.format("%,.0f", amt)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "(${String.format("%.0f", portion * 100)}%)",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Custom Gradient progress line element
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = portion)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        colors[index],
                                                        colors[index].copy(alpha = 0.6f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
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
