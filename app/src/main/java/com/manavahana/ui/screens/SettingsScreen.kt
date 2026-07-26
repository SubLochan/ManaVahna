package com.manavahana.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.manavahana.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.manavahana.ui.ManaVahanaViewModel
import com.manavahana.ui.pdf.PdfGenerator
import com.manavahana.ui.AppUpdateHelper
import com.manavahana.ui.UpdateStatus
import com.manavahana.ui.Localizer

@Composable
fun SettingsScreen(
    viewModel: ManaVahanaViewModel
) {
    val context = LocalContext.current

    val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()
    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsState()
    val savedPin by viewModel.savedSecurityPin.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val overriddenAppVersion by viewModel.overriddenAppVersion.collectAsState()
    val overriddenPlayStoreVersion by viewModel.overriddenPlayStoreVersion.collectAsState()
    val langCode = selectedLanguage ?: "en"

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

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinDeactivateDialog by remember { mutableStateOf(false) }
    var showBiometricEnrollDialog by remember { mutableStateOf(false) }

    var pinSetupText by remember { mutableStateOf("") }
    var pinConfirmText by remember { mutableStateOf("") }
    var pinSetupError by remember { mutableStateOf("") }

    var pinDeactivateText by remember { mutableStateOf("") }
    var pinDeactivateError by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Localizer.get("settings_title", langCode),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = Localizer.get("settings_subtitle", langCode),
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
                    Text(Localizer.get("security_settings", langCode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(Localizer.get("secure_pin_lock", langCode), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = if (isPinEnabled) {
                                        if (langCode == "te") "పిన్ సక్రియంగా ఉంది (మార్చడానికి క్రింద క్లిక్ చేయండి)" else "Security PIN is active"
                                    } else {
                                        Localizer.get("pin_start_desc", langCode)
                                    },
                                    fontSize = 11.sp,
                                    color = if (isPinEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Switch(
                            checked = isPinEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (savedPin.isNullOrBlank()) {
                                        showPinSetupDialog = true
                                    } else {
                                        viewModel.updatePin(savedPin)
                                    }
                                } else {
                                    if (!savedPin.isNullOrBlank()) {
                                        showPinDeactivateDialog = true
                                    } else {
                                        viewModel.updatePin(null)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("pin_lock_switch")
                        )
                    }

                    if (isPinEnabled && !savedPin.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .clickable { showPinSetupDialog = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Password,
                                contentDescription = "Change PIN",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (langCode == "te") "సెక్యూరిటీ పిన్‌ను మార్చండి" else if (langCode == "hi") "सुरक्षा पिन बदलें" else "Change 4-Digit Security PIN",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                                Text(Localizer.get("biometric_auth", langCode), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(Localizer.get("biometric_desc", langCode), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Switch(
                            checked = isFingerprintEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showBiometricEnrollDialog = true
                                } else {
                                    viewModel.setFingerprintEnabled(false)
                                    Toast.makeText(context, if (langCode == "te") "బయోమెట్రిక్ ప్రామాణీకరణ నిలిపివేయబడింది" else "Biometrics disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }


                }
            }
        }

        // Language Selection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("language_selection_settings_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = Localizer.get("settings_lang_title", langCode),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Localizer.get("settings_lang_desc", langCode),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Localizer.LANGUAGES.forEach { option ->
                            val isSelected = langCode == option.code
                            Button(
                                onClick = {
                                    viewModel.selectLanguage(option.code)
                                    Toast.makeText(context, Localizer.get("toast_lang_changed", option.code), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("settings_lang_btn_${option.code}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(text = option.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
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
                                    val backupFileName = "ManaVahana_Backup.json"
                                    exportJsonLauncher.launch(backupFileName)
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
                                val backupFileName = "ManaVahana_Backup.json"
                                val backupFile = java.io.File(backupDir, backupFileName)
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

        // Play Store App Updates Card
        item {
            val updateHelper = remember { AppUpdateHelper.getInstance(context) }
            val updateState by updateHelper.updateStatus.collectAsState()
            val livePlayStoreVersion by updateHelper.livePlayStoreVersion.collectAsState()

            LaunchedEffect(Unit) {
                updateHelper.fetchPlayStoreVersionDirectly()
            }

            var isEditingVersions by remember { mutableStateOf(false) }
            var tempCurrentVersion by remember { mutableStateOf("") }
            var tempPlayStoreVersion by remember { mutableStateOf("") }

            val packageInfo = remember(context) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            val installedVersionName = packageInfo?.versionName ?: "1.0"
            val installedVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode ?: 1L
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode?.toLong() ?: 1L
            }

            val currentPlayStoreVersion = overriddenPlayStoreVersion ?: livePlayStoreVersion
            val dynamicCurrentVersion = remember(currentPlayStoreVersion, installedVersionName) {
                if (currentPlayStoreVersion.isNotEmpty() && 
                    currentPlayStoreVersion != "Retrieving..." && 
                    currentPlayStoreVersion != "Not checked yet") {
                    com.manavahana.ui.PlayStoreVersionFetcher.getLowerVersion(currentPlayStoreVersion)
                } else {
                    installedVersionName
                }
            }
            val currentAppVersion = overriddenAppVersion ?: dynamicCurrentVersion

            LaunchedEffect(isEditingVersions) {
                if (isEditingVersions) {
                    tempCurrentVersion = currentAppVersion
                    tempPlayStoreVersion = currentPlayStoreVersion
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App System Updates",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { isEditingVersions = !isEditingVersions },
                            modifier = Modifier.size(36.dp).testTag("edit_versions_btn")
                        ) {
                            Icon(
                                imageVector = if (isEditingVersions) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = "Edit Versions",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = "ManaVahana matches your device Play Store configuration. Keep track of immediate or flexible releases and verify schema structures are completely preserved.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    // Display current update status text
                    val currentStatusText = when (updateState) {
                        is UpdateStatus.Idle -> "No update checking has run yet."
                        is UpdateStatus.Checking -> "Checking Google Play Store for active rolls..."
                        is UpdateStatus.UpToDate -> "ManaVahana is completely up to date!"
                        is UpdateStatus.UpdateAvailable -> {
                            val info = updateState as UpdateStatus.UpdateAvailable
                            "Update Available! New Version Code: ${info.versionCode} ${if (info.isSimulation) "(SIMULATION)" else ""}"
                        }
                        is UpdateStatus.Error -> "Store check failed: ${(updateState as UpdateStatus.Error).message}"
                    }

                    if (isEditingVersions) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Configure Version Overrides",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                OutlinedTextField(
                                    value = tempCurrentVersion,
                                    onValueChange = { tempCurrentVersion = it },
                                    label = { Text("Simulate Current Version Name") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.fillMaxWidth().testTag("override_current_version_input")
                                )

                                OutlinedTextField(
                                    value = tempPlayStoreVersion,
                                    onValueChange = { tempPlayStoreVersion = it },
                                    label = { Text("Simulate Latest Play Store Version") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.fillMaxWidth().testTag("override_play_store_version_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.updateSimulatedAppVersion(tempCurrentVersion)
                                            viewModel.updateSimulatedPlayStoreVersion(tempPlayStoreVersion)
                                            isEditingVersions = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Save", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            isEditingVersions = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Cancel", fontSize = 11.sp)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.updateSimulatedAppVersion("")
                                            viewModel.updateSimulatedPlayStoreVersion("")
                                            updateHelper.fetchPlayStoreVersionDirectly()
                                            isEditingVersions = false
                                        },
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Reset Defaults", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current Version Name:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = currentAppVersion,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Latest Play Store Version:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = currentPlayStoreVersion,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (updateState) {
                                    is UpdateStatus.UpdateAvailable -> Icons.Default.NewReleases
                                    is UpdateStatus.Checking -> Icons.Default.Sync
                                    is UpdateStatus.UpToDate -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = when (updateState) {
                                    is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.tertiary
                                    is UpdateStatus.UpToDate -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = currentStatusText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { updateHelper.checkForUpdates(forceNotification = true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Updates", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { updateHelper.triggerSimulation(isFlexible = true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Duo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Release", fontSize = 11.sp)
                        }
                    }

                    if (updateState is UpdateStatus.UpdateAvailable) {
                        Button(
                            onClick = { updateHelper.resetStatus() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset Update Status Tracker", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Reminders & Document Expiration Notification Console
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("reminders_notification_console_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localizer.get("doc_expiry_system", langCode),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "ManaVahana schedules secure background tasks to parse your vehicle insurance, pollution certificates, and personal document vaults. Get warned instantly on your status bar for upcoming dates and overdue tasks completely offline.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = {
                            try {
                                val request = androidx.work.OneTimeWorkRequestBuilder<com.manavahana.worker.ReminderWorker>().build()
                                androidx.work.WorkManager.getInstance(context).enqueue(request)
                                Toast.makeText(context, "Scanning document expiry states & publishing alerts...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Expiration Scan & Alerts")
                    }
                }
            }
        }

        // Telugu Cultural Aesthetics
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

    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinSetupDialog = false 
                pinSetupText = ""
                pinConfirmText = ""
                pinSetupError = ""
            },
            title = {
                Text(
                    text = if (langCode == "te") "సెక్యూరిటీ పిన్ సెటప్" else "Set Security PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (langCode == "te") "యాప్‌ను సురక్షితంగా ఉంచడానికి దయచేసి 4 అంకెల పిన్‌ను నమోదు చేయండి." else "Enter a 4-digit PIN to secure your application offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    OutlinedTextField(
                        value = pinSetupText,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinSetupText = it
                                pinSetupError = ""
                            }
                        },
                        label = { Text(if (langCode == "te") "కొత్త పిన్ నమోదు చేయండి" else "Enter 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pin_setup_input_1")
                    )

                    OutlinedTextField(
                        value = pinConfirmText,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinConfirmText = it
                                pinSetupError = ""
                            }
                        },
                        label = { Text(if (langCode == "te") "పిన్‌ను నిర్ధారించండి" else "Confirm 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pin_setup_input_2")
                    )

                    if (pinSetupError.isNotEmpty()) {
                        Text(
                            text = pinSetupError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinSetupText.length != 4) {
                            pinSetupError = if (langCode == "te") "పిన్ కచ్చితంగా 4 అంకెలు ఉండాలి" else "PIN must be exactly 4 digits"
                        } else if (pinSetupText != pinConfirmText) {
                            pinSetupError = if (langCode == "te") "పిన్‌లు సరిపోలడం లేదు" else "PINs do not match"
                        } else {
                            viewModel.updatePin(pinSetupText)
                            Toast.makeText(context, if (langCode == "te") "సెక్యూరిటీ పిన్ సేవ్ చేయబడింది!" else "Security PIN updated successfully!", Toast.LENGTH_SHORT).show()
                            showPinSetupDialog = false
                            pinSetupText = ""
                            pinConfirmText = ""
                            pinSetupError = ""
                        }
                    },
                    modifier = Modifier.testTag("pin_setup_confirm_btn")
                ) {
                    Text(if (langCode == "te") "సేవ్ చేయి" else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPinSetupDialog = false 
                        pinSetupText = ""
                        pinConfirmText = ""
                        pinSetupError = ""
                    }
                ) {
                    Text(if (langCode == "te") "క్యాన్సిల్" else "Cancel")
                }
            }
        )
    }

    if (showPinDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinDeactivateDialog = false 
                pinDeactivateText = ""
                pinDeactivateError = ""
            },
            title = {
                Text(
                    text = if (langCode == "te") "పిన్ లాక్ నిలిపివేత" else "Deactivate PIN Lock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (langCode == "te") "పిన్ లాక్‌ను నిలిపివేయడానికి దయచేసి మీ ప్రస్తుత 4 అంకెల పిన్‌ను నమోదు చేయండి." else "Enter your current 4-digit security PIN to disable lock protection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    OutlinedTextField(
                        value = pinDeactivateText,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinDeactivateText = it
                                pinDeactivateError = ""
                            }
                        },
                        label = { Text(if (langCode == "te") "ప్రస్తుత పిన్" else "Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pin_deactivate_input")
                    )

                    if (pinDeactivateError.isNotEmpty()) {
                        Text(
                            text = pinDeactivateError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinDeactivateText == savedPin) {
                            viewModel.updatePin(null)
                            Toast.makeText(context, if (langCode == "te") "పిన్ లాక్ నిలిపివేయబడింది!" else "PIN lock deactivated successfully!", Toast.LENGTH_SHORT).show()
                            showPinDeactivateDialog = false
                            pinDeactivateText = ""
                            pinDeactivateError = ""
                        } else {
                            pinDeactivateError = if (langCode == "te") "తప్పు పిన్. మళ్లీ ప్రయత్నించండి." else "Incorrect PIN. Try again."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("pin_deactivate_confirm_btn")
                ) {
                    Text(if (langCode == "te") "నిలిపివేయి" else "Deactivate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPinDeactivateDialog = false 
                        pinDeactivateText = ""
                        pinDeactivateError = ""
                    }
                ) {
                    Text(if (langCode == "te") "క్యాన్సిల్" else "Cancel")
                }
            }
        )
    }

    if (showBiometricEnrollDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricEnrollDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (langCode == "te") "బయోమెట్రిక్ ఎన్‌రోల్‌మెంట్" else "Enroll Biometrics"
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = if (langCode == "te") "సిస్టమ్ ఫింగర్‌ప్రింట్ కోసంగా సెన్సార్‌ను ధృవీకరించడానికి కింద ఉన్న చిహ్నాన్ని తాకండి." else "Tap the fingerprint sensor icon below to authorize and enroll this device's biometrics.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable {
                                viewModel.setFingerprintEnabled(true)
                                Toast.makeText(context, if (langCode == "te") "బయోమెట్రిక్ విజయవంతంగా లింక్ చేయబడింది!" else "Biometrics linked successfully!", Toast.LENGTH_SHORT).show()
                                showBiometricEnrollDialog = false
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Tap to Scan",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Text(
                        text = if (langCode == "te") "స్కానింగ్ చేయడానికి తాకండి" else "TAP ICON TO SECURELY SCAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showBiometricEnrollDialog = false }
                ) {
                    Text(if (langCode == "te") "క్యాన్సిల్" else "Cancel")
                }
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
