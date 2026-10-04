package com.manavahana.ui.screens

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manavahana.R
import com.manavahana.data.model.FuelLog
import com.manavahana.data.model.Vehicle
import com.manavahana.ui.LocalAppLanguage
import com.manavahana.ui.Localizer
import com.manavahana.ui.ManaVahanaViewModel
import com.manavahana.ui.QuickAddFuelActivity
import com.manavahana.widget.FuelExpenseWidgetProvider
import com.manavahana.widget.FuelWidgetUpdater
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreenWidgetCard(
    viewModel: ManaVahanaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val langCode = LocalAppLanguage.current
    val vehicles by viewModel.vehicles.collectAsState()
    val allFuelLogs by viewModel.allFuelLogs.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()

    var showGuideDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val activeVehicle = selectedVehicle ?: vehicles.firstOrNull()
    val latestLog = allFuelLogs.maxByOrNull { it.fuelDate }

    val currentMonthTotal = remember(allFuelLogs) {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        allFuelLogs.filter { it.fuelDate >= start }.sumOf { it.totalAmount }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_screen_widget_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.2.dp, Color(0xFFFFA000).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Icon + Title + Expand arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFA000).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = Localizer.get("widget_feature_title", langCode),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (langCode == "te") "మీ హోమ్ స్క్రీన్ నుండి నేరుగా ఇంధనం నమోదు చేయండి" else if (langCode == "hi") "होम स्क्रीन से तुरंत ईंधन दर्ज करें" else "Log fuel right from your home screen",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Description
            Text(
                text = Localizer.get("widget_feature_desc", langCode),
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                lineHeight = 17.sp
            )

            // Visual Widget Preview Box (Styled exactly like the real Home Screen Widget)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1C1A))
                    .border(1.5.dp, Color(0xFFFFA000).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Preview Header: Vehicle + Realtime Clock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA000).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = activeVehicle?.vehicleName ?: "ManaVahana Vehicle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "${activeVehicle?.vehicleNumber ?: "AP 29 BK 1234"} • ${activeVehicle?.fuelType ?: "Petrol"}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFFA000)
                                )
                            }
                        }

                        // Simulated TextClock
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                        val now = Date()
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = timeFormat.format(now),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = dateFormat.format(now),
                                fontSize = 10.sp,
                                color = Color(0xFFB0BEC5)
                            )
                        }
                    }

                    // Preview Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Last Fill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text("Last Fill", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                                Text(
                                    text = if (latestLog != null) "₹${latestLog.totalAmount.toInt()} (${String.format(Locale.US, "%.1f", latestLog.litersFilled)}L)" else "₹0",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                            }
                        }

                        // Month Fuel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text("Month Fuel", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                                Text(
                                    text = "₹${currentMonthTotal.toInt()}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB74D)
                                )
                            }
                        }

                        // Odometer
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text("Odometer", fontSize = 9.sp, color = Color(0xFF9E9E9E))
                                Text(
                                    text = if (latestLog != null) "${latestLog.odometerReading.toInt()} km" else "0 km",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }

                    // Preview Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1.5f)
                                .height(32.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFA000)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Fuel Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(0.7f)
                                .height(32.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFA000).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("+₹500", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA000))
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(0.7f)
                                .height(32.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFA000).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("+₹1000", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA000))
                            }
                        }
                    }
                }
            }

            // Action Buttons: Add Widget to Home Screen + Sync + How to Add
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add to Home Screen Button
                Button(
                    onClick = {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val providerComponent = ComponentName(context, FuelExpenseWidgetProvider::class.java)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                            val successCallback = Intent(context, FuelExpenseWidgetProvider::class.java).apply {
                                action = FuelWidgetUpdater.ACTION_REFRESH_WIDGET
                            }
                            val successPendingIntent = PendingIntent.getBroadcast(
                                context,
                                99,
                                successCallback,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            appWidgetManager.requestPinAppWidget(providerComponent, null, successPendingIntent)
                            Toast.makeText(context, "Select 'Add to Home screen' in the system prompt!", Toast.LENGTH_SHORT).show()
                        } else {
                            showGuideDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_add_widget_to_home"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA000),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localizer.get("widget_btn_add_to_home", langCode),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                // Sync / Refresh Button
                OutlinedButton(
                    onClick = {
                        FuelWidgetUpdater.updateAllWidgets(context)
                        Toast.makeText(context, "Widgets synchronized with latest vehicle data!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(44.dp).testTag("btn_sync_widget"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                }

                // Guide Button
                FilledTonalIconButton(
                    onClick = { showGuideDialog = true },
                    modifier = Modifier.size(44.dp).testTag("btn_widget_guide")
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Guide", modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (langCode == "te") "హోమ్ స్క్రీన్‌పై విడ్జెట్‌ను ఎలా ఉంచాలి?" else if (langCode == "hi") "होम स्क्रीन पर विजेट कैसे लगाएं?" else "How to add Widget to Home Screen",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val steps = if (langCode == "te") listOf(
                        "1. మీ ఫోన్ హోమ్ స్క్రీన్‌పై ఖాళీ ప్రదేశాన్ని తాకి పట్టుకోండి.",
                        "2. మెనూ నుండి 'Widgets' (విడ్జెట్లు) ఎంచుకోండి.",
                        "3. 'ManaVahana' (మనవాహన) కోసం శోధించండి.",
                        "4. 'Fuel Expense & Clock Widget' ను నొక్కి మీ హోమ్ స్క్రీన్ పైకి లాగండి!"
                    ) else if (langCode == "hi") listOf(
                        "1. अपने फ़ोन के होम स्क्रीन पर किसी खाली जगह को दबाकर रखें।",
                        "2. दिखाई देने वाले मेनू से 'Widgets' चुनें।",
                        "3. सूची में 'ManaVahana' खोजें।",
                        "4. 'Fuel Expense & Clock Widget' को दबाकर होम स्क्रीन पर खींचें!"
                    ) else listOf(
                        "1. Touch and hold any empty space on your Android home screen.",
                        "2. Tap 'Widgets' from the pop-up menu.",
                        "3. Scroll down or search for 'ManaVahana'.",
                        "4. Touch and hold the 'Fuel Expense & Clock Widget' and drag it to your preferred position!"
                    )

                    steps.forEach { step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Text(step, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000), contentColor = Color.Black)
                ) {
                    Text(if (langCode == "te") "సరే, అర్థమైంది" else if (langCode == "hi") "ठीक है" else "Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
