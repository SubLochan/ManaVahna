package com.manavahana.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.manavahana.data.model.Document
import com.manavahana.ui.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewDocumentDialog(
    document: Document,
    vehicleName: String,
    onDismiss: () -> Unit,
    onRenewConfirmed: (updatedDoc: Document, renewalExpenseAmount: Double?, expenseNotes: String?) -> Unit
) {
    val context = LocalContext.current
    val langCode = LocalAppLanguage.current
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val now = System.currentTimeMillis()

    // Determine current expiry status
    val currentExpiry = document.expiryDate ?: 0L
    val isExpired = currentExpiry > 0 && currentExpiry < now
    val daysFromNow = if (currentExpiry > 0) ((currentExpiry - now) / (24 * 60 * 60 * 1000L)).toInt() else 0

    // Initial default calculation for new expiry:
    // If not expired yet, extend from current expiry date; if already expired, extend from today.
    val baseTimeForExtension = if (!isExpired && currentExpiry > now) currentExpiry else now

    val defaultExtensionDays = when {
        document.docType.contains("Pollution", ignoreCase = true) -> 180 // 6 months
        document.docType.contains("Insurance", ignoreCase = true) -> 365 // 1 year
        else -> 365
    }

    var newExpiryDate by remember {
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseTimeForExtension
            if (defaultExtensionDays == 180) {
                add(Calendar.MONTH, 6)
            } else {
                add(Calendar.YEAR, 1)
            }
        }
        mutableStateOf(cal.timeInMillis)
    }

    var newTitle by remember { mutableStateOf(document.title) }
    var policyNumber by remember { mutableStateOf("") }
    var renewalCost by remember { mutableStateOf("") }
    var recordAsExpense by remember { mutableStateOf(true) }

    // New replaced document path
    var newDocumentPath by remember { mutableStateOf<String?>(null) }
    var newDocumentName by remember { mutableStateOf<String?>(null) }

    // Image Picker (0-permission photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = copyUriToInternalStorage(context, uri, "renewed_doc_img")
            newDocumentPath = localPath ?: uri.toString()
            newDocumentName = "Renewed_Photo_${System.currentTimeMillis() % 10000}.jpg"
            Toast.makeText(context, if (langCode == "te") "కొత్త ఫోటో ఎంపికైంది" else "New document photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    // PDF / File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = copyUriToInternalStorage(context, uri, "renewed_doc_file")
            newDocumentPath = localPath ?: uri.toString()
            val fileName = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx != -1 && cursor.moveToFirst()) cursor.getString(idx) else null
                }
            } catch (e: Exception) {
                null
            }
            newDocumentName = fileName ?: "Renewed_Document.pdf"
            Toast.makeText(context, if (langCode == "te") "కొత్త డాక్యుమెంట్ ఎంపికైంది" else "New document attached", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_renew_document"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = if (langCode == "te") "పత్రం పునరుద్ధరణ (రెన్యూవల్)" else if (langCode == "hi") "दस्तावेज़ नवीनीकरण" else "Renew Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$vehicleName • ${document.docType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Current Expiry Status Banner
                item {
                    val bannerBg = if (isExpired) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    } else if (daysFromNow <= 15) {
                        Color(0xFFFEF3C7) // Amber
                    } else {
                        Color(0xFFECFDF5) // Emerald
                    }

                    val bannerTextColor = if (isExpired) {
                        MaterialTheme.colorScheme.error
                    } else if (daysFromNow <= 15) {
                        Color(0xFFB45309)
                    } else {
                        Color(0xFF047857)
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bannerBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpired) Icons.Default.Warning else if (daysFromNow <= 15) Icons.Default.Schedule else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = bannerTextColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                val statusHeadline = when {
                                    isExpired -> if (langCode == "te") "కాలపరిమితి ముగిసింది (${-daysFromNow} రోజుల క్రితం)" else if (langCode == "hi") "समय सीमा समाप्त हुआ (${-daysFromNow} दिन पहले)" else "Expired ${-daysFromNow} days ago"
                                    daysFromNow == 0 -> if (langCode == "te") "ఈరోజే గడువు ముగుస్తుంది" else if (langCode == "hi") "आज समाप्त हो रहा है" else "Expires Today!"
                                    daysFromNow in 1..30 -> if (langCode == "te") "${daysFromNow} రోజుల్లో గడువు ముగుస్తుంది" else if (langCode == "hi") "${daysFromNow} दिनों में समाप्त होगा" else "Expires in $daysFromNow days"
                                    else -> if (langCode == "te") "చెల్లుబాటులో ఉంది" else if (langCode == "hi") "वैध है" else "Currently Active"
                                }
                                Text(
                                    text = statusHeadline,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = bannerTextColor
                                )
                                if (currentExpiry > 0) {
                                    Text(
                                        text = "${if (langCode == "te") "మునుపటి గడువు:" else "Previous expiry:"} ${sdf.format(Date(currentExpiry))}",
                                        fontSize = 11.sp,
                                        color = bannerTextColor.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Document Paper Image / File Replacement Section
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (langCode == "te") "కొత్త పత్రం ఫోటో / ఫైల్ మార్పు" else if (langCode == "hi") "नया दस्तावेज़ फोटो / फाइल" else "New Document Paper / File",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                if (newDocumentPath != null) {
                                    TextButton(
                                        onClick = {
                                            newDocumentPath = null
                                            newDocumentName = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (langCode == "te") "రీసెట్" else "Revert", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // Show live preview of newly picked paper or current paper
                            val activeDisplayPath = newDocumentPath ?: document.documentPath
                            val isShowingNew = newDocumentPath != null

                            if (!activeDisplayPath.isNullOrBlank()) {
                                val isImg = activeDisplayPath.endsWith(".png", ignoreCase = true) ||
                                        activeDisplayPath.endsWith(".jpg", ignoreCase = true) ||
                                        activeDisplayPath.endsWith(".jpeg", ignoreCase = true) ||
                                        activeDisplayPath.endsWith(".webp", ignoreCase = true)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isShowingNew) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isShowingNew) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isImg) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            AsyncImage(
                                                model = PathUtils.getResolutionFile(context, activeDisplayPath) ?: activeDisplayPath,
                                                contentDescription = "Document Paper Preview",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isShowingNew) {
                                                if (langCode == "te") "✅ కొత్త పత్రం జతచేయబడింది" else "✅ New Paper Attached"
                                            } else {
                                                if (langCode == "te") "📄 ప్రస్తుతం ఉన్న పత్రం" else "📄 Current Document Paper"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isShowingNew) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = newDocumentName ?: (activeDisplayPath.substringAfterLast("/").take(25) + "..."),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isShowingNew) {
                                            Text(
                                                text = if (langCode == "te") "గత పత్రం స్థానంలో ఇది భద్రపరచబడుతుంది" else "Will replace previous file",
                                                fontSize = 10.sp,
                                                color = Color(0xFF047857).copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Pick buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_pick_renew_photo"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (langCode == "te") "ఫోటో / కెమెరా" else "Take / Pick Photo", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        filePickerLauncher.launch("*/*")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_pick_renew_pdf"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (langCode == "te") "PDF / ఫైల్" else "Select PDF", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 3. New Expiry Date & Quick Extension Presets
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (langCode == "te") "కొత్త గడువు తేదీ (New Expiry Date)" else "New Expiry Date",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        // Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val preset1Label = if (document.docType.contains("Pollution", ignoreCase = true)) "+6 Months" else "+1 Year"
                            val preset2Label = if (document.docType.contains("Pollution", ignoreCase = true)) "+1 Year" else "+2 Years"
                            val preset3Label = if (document.docType.contains("Pollution", ignoreCase = true)) "+2 Years" else "+3 Years"

                            SuggestionChip(
                                onClick = {
                                    val c = Calendar.getInstance().apply {
                                        timeInMillis = baseTimeForExtension
                                        if (document.docType.contains("Pollution", ignoreCase = true)) add(Calendar.MONTH, 6) else add(Calendar.YEAR, 1)
                                    }
                                    newExpiryDate = c.timeInMillis
                                },
                                label = { Text(preset1Label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("chip_preset_1")
                            )

                            SuggestionChip(
                                onClick = {
                                    val c = Calendar.getInstance().apply {
                                        timeInMillis = baseTimeForExtension
                                        if (document.docType.contains("Pollution", ignoreCase = true)) add(Calendar.YEAR, 1) else add(Calendar.YEAR, 2)
                                    }
                                    newExpiryDate = c.timeInMillis
                                },
                                label = { Text(preset2Label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("chip_preset_2")
                            )

                            SuggestionChip(
                                onClick = {
                                    val c = Calendar.getInstance().apply {
                                        timeInMillis = baseTimeForExtension
                                        if (document.docType.contains("Pollution", ignoreCase = true)) add(Calendar.YEAR, 2) else add(Calendar.YEAR, 3)
                                    }
                                    newExpiryDate = c.timeInMillis
                                },
                                label = { Text(preset3Label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("chip_preset_3")
                            )
                        }

                        // Date picker trigger field
                        val displayDate = if (newExpiryDate > 0) sdf.format(Date(newExpiryDate)) else "Select Date"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val c = Calendar.getInstance().apply {
                                        timeInMillis = if (newExpiryDate > 0) newExpiryDate else System.currentTimeMillis()
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val sel = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                                            newExpiryDate = sel.timeInMillis
                                        },
                                        c.get(Calendar.YEAR),
                                        c.get(Calendar.MONTH),
                                        c.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            OutlinedTextField(
                                value = displayDate,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text(if (langCode == "te") "పునరుద్ధరించిన గడువు తేదీ" else "Renewed Expiry Date") },
                                trailingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_renewed_expiry_date"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // 4. Document Title / Policy Number
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text(if (langCode == "te") "పత్రం శీర్షిక (Title)" else "Document Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_renewed_title"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = policyNumber,
                            onValueChange = { policyNumber = it },
                            label = { Text(if (langCode == "te") "పాలసీ / రిజిస్ట్రేషన్ సంఖ్య (ఐచ్ఛికం)" else "Policy / Certificate No. (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_renewed_policy_no"),
                            singleLine = true
                        )
                    }
                }

                // 5. Renewal Cost & Expense Logging
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (langCode == "te") "పునరుద్ధరణ ఖర్చు (Renewal Expense)" else "Renewal Expense",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            OutlinedTextField(
                                value = renewalCost,
                                onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) renewalCost = it },
                                label = { Text(if (langCode == "te") "ప్రీమియం / చెల్లించిన రుసుము (₹)" else "Fee / Premium Paid (₹)") },
                                placeholder = { Text("e.g., 2850") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_renewed_cost"),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = recordAsExpense,
                                    onCheckedChange = { recordAsExpense = it },
                                    modifier = Modifier.testTag("chk_record_expense")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = if (langCode == "te") "ఖర్చుల లాగ్‌లో చేర్చండి" else "Add to Vehicle Expenses Log",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (langCode == "te") "వాహన నెలవారీ ఖర్చులలో ఆటోమేటిక్‌గా నమోదు అవుతుంది." else "Will be recorded under ${document.docType} expenses.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    if (newTitle.isBlank()) {
                        Toast.makeText(context, if (langCode == "te") "దయచేసి శీర్షికను నమోదు చేయండి" else "Please enter document title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newExpiryDate <= 0) {
                        Toast.makeText(context, if (langCode == "te") "దయచేసి గడువు తేదీని ఎంచుకోండి" else "Please select expiry date", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val finalPath = newDocumentPath ?: document.documentPath
                    val updatedDoc = document.copy(
                        title = newTitle.trim(),
                        expiryDate = newExpiryDate,
                        documentPath = finalPath,
                        isEncrypted = true
                    )

                    val costAmount = renewalCost.toDoubleOrNull()
                    val expenseNotes = if (costAmount != null && costAmount > 0) {
                        val policyPart = if (policyNumber.isNotBlank()) " [No: ${policyNumber.trim()}]" else ""
                        "Renewed ${document.docType}: ${newTitle.trim()}$policyPart (Valid till ${sdf.format(Date(newExpiryDate))})"
                    } else null

                    onRenewConfirmed(
                        updatedDoc,
                        if (recordAsExpense) costAmount else null,
                        expenseNotes
                    )
                },
                modifier = Modifier.testTag("btn_confirm_renew_doc")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (langCode == "te") "పునరుద్ధరించి భద్రపరచండి" else "Renew & Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_renew_doc")
            ) {
                Text(if (langCode == "te") "రద్దు" else "Cancel")
            }
        }
    )
}
