package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.ManaVahanaViewModel
import com.example.ui.pdf.PdfGenerator

@Composable
fun SettingsScreen(
    viewModel: ManaVahanaViewModel
) {
    val context = LocalContext.current

    val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()
    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsState()

    val currentUserState by viewModel.currentUserState.collectAsState()

    // Database states gathered for exports
    val vehicles by viewModel.vehicles.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val fuelLogs by viewModel.allFuelLogs.collectAsState()
    val serviceLogs by viewModel.allServiceLogs.collectAsState()
    val reminders by viewModel.allReminders.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()

    var pendingImportJsonString by remember { mutableStateOf("") }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = viewModel.exportBackupJsonString(context)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Backup JSON file saved successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save backup file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    if (jsonString.isNotBlank()) {
                        pendingImportJsonString = jsonString
                        showImportConfirmDialog = true
                    } else {
                        Toast.makeText(context, "Selected backup file is empty.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to read backup file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var showPdfSuccessDialog by remember { mutableStateOf(false) }
    var generatedPdfUri by remember { mutableStateOf<Uri?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "అమరికలు (Settings)",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Configure your offline-first security & local database storage preferences.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Security Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Security Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Secure PIN Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Ask for PIN on app startup", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Switch(
                            checked = isPinEnabled,
                            onCheckedChange = {
                                if (it) {
                                    viewModel.updatePin("1234") // Set default easily toggleable
                                    Toast.makeText(context, "PIN is configured to default '1234'. Change in update panel.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updatePin(null)
                                }
                            },
                            modifier = Modifier.testTag("pin_lock_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Biometric Authentication", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Enable fingerprint scan unlock", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Switch(
                            checked = isFingerprintEnabled,
                            onCheckedChange = { viewModel.setFingerprintEnabled(it) },
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }
                }
            }
        }

        // Visual Appearance (Theme mode selection)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Visual Appearance",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Set your visual preference for ManaVahana. Pick Standard Light, Eye-Safe Dark, or follow System Settings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    val themeMode by viewModel.themeMode.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            label = "Light Mode",
                            icon = Icons.Default.WbSunny,
                            isSelected = themeMode == "light",
                            onClick = { viewModel.setThemeMode("light") },
                            modifier = Modifier.weight(1f).testTag("theme_btn_light")
                        )

                        ThemeOptionButton(
                            label = "Dark Mode",
                            icon = Icons.Default.NightsStay,
                            isSelected = themeMode == "dark",
                            onClick = { viewModel.setThemeMode("dark") },
                            modifier = Modifier.weight(1f).testTag("theme_btn_dark")
                        )

                        ThemeOptionButton(
                            label = "System",
                            icon = Icons.Default.Settings,
                            isSelected = themeMode == "system",
                            onClick = { viewModel.setThemeMode("system") },
                            modifier = Modifier.weight(1f).testTag("theme_btn_system")
                        )
                    }
                }
            }
        }

        // Backup, Restore & PDF Reports Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Backup & Export Tools", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    Text("Local Backup & Restore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "ManaVahana is 100% offline-first. Save all your vehicles, logs, expenses, docs, and reminders as a local JSON file, or restore data instantly by choosing a local backup file.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    exportJsonLauncher.launch("manavahana_backup.json")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Storage picker unavailable. Failed to launch backup.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup")
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    importJsonLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Storage picker unavailable. Failed to launch restore.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup")
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                val jsonString = viewModel.exportBackupJsonString(context)
                                val backupDir = java.io.File(context.cacheDir, "backups")
                                if (!backupDir.exists()) backupDir.mkdirs()
                                val backupFile = java.io.File(backupDir, "manavahana_backup.json")
                                backupFile.writeText(jsonString, Charsets.UTF_8)
                                val backupUri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    backupFile
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, backupUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooserIntent = Intent.createChooser(intent, "మిత్రులతో పంచుకోండి (Share Backup File)").apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(chooserIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Failed to share backup file: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Backup File Directly")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                    Text("PDF Status Report", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Generate a comprehensive, printable A4 PDF status report of registered vehicles, cumulative fuel consumption values, financial expenses, and scheduled renewal alerts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Button(
                        onClick = {
                            val uri = PdfGenerator.generatePdfReport(
                                context = context,
                                vehicles = vehicles,
                                expenses = expenses,
                                fuelLogs = fuelLogs,
                                serviceLogs = serviceLogs,
                                reminders = reminders
                            )
                            if (uri != null) {
                                generatedPdfUri = uri
                                showPdfSuccessDialog = true
                                Toast.makeText(context, "PDF Report generated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to generate PDF Report.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate & Share PDF Report")
                    }
                }
            }
        }

        // Telugu Design Aesthetics Credit Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Telugu Cultural Aesthetics",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "ManaVahana (మన వాహనం) meaning 'Our Vehicle' features standard traditional textures and tones:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TeluguBullet("కుంకుమ బొట్టు (Kumkuma) Red", "Represents protection, visual highlight, and festive identity.")
                        TeluguBullet("పసుపు తోరణం (Turmeric) Gold", "Represents auspicious beginnings, longevity, and bright visual safety.")
                        TeluguBullet("మామిడి ఆకు (Mango Leaf) Green", "Represents purity, freshness, and the vehicle blessing celebrations.")
                    }
                }
            }
        }

        // Security Configuration Card
        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                pendingImportJsonString = ""
            },
            title = { Text("Confirm Data Overwrite") },
            text = {
                Text(
                    "WARNING: Restoring this local JSON file will completely replace all currently registered vehicles, fuel logs, service details, secure documents, and active reminders on this device! This offline operation cannot be undone.\n\nAre you sure you want to proceed and overwrite everything?",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.importBackupJson(context, pendingImportJsonString)
                        if (success) {
                            Toast.makeText(context, "Database Restored Successfully Offline!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Error: Invalid JSON/Data backup structure or corrupted file.", Toast.LENGTH_LONG).show()
                        }
                        showImportConfirmDialog = false
                        pendingImportJsonString = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirmDialog = false
                        pendingImportJsonString = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }



    if (showPdfSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            title = { Text("PDF Generated Successfully") },
            text = {
                Text("Your A4 formatted offline executive report has been compiled and is ready for sharing or printing.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfSuccessDialog = false
                        generatedPdfUri?.let { uri ->
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Status Report PDF"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "No app available to handle PDF sharing.", Toast.LENGTH_LONG).show()
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
                TextButton(onClick = { showPdfSuccessDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun TeluguBullet(title: String, desc: String) {
    Column {
        Text("• $title", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Text(desc, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
fun ThemeOptionButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
