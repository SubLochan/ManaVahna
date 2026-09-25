package com.manavahana.ui.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.manavahana.data.model.Expense
import com.manavahana.data.model.FuelLog
import com.manavahana.data.model.Reminder
import com.manavahana.data.model.ServiceLog
import com.manavahana.data.model.Vehicle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PdfGenerator {

    data class GeneratedReportResult(
        val file: File,
        val uri: Uri,
        val fileName: String,
        val title: String,
        val monthName: String,
        val year: Int,
        val month: Int,
        val totalSpent: Double,
        val recordCount: Int
    )

    fun generatePdfReport(
        context: Context,
        vehicles: List<Vehicle>,
        expenses: List<Expense>,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        reminders: List<Reminder>
    ): Uri? {
        try {
            val pdf = PdfDocument()
            // Standard A4 dimensions: 595 x 842 points (72 points = 1 inch)
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas

            val primaryPaint = Paint().apply {
                color = Color.parseColor("#B45309") // Terracotta
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val greenPaint = Paint().apply {
                color = Color.parseColor("#065F46") // Forest Green
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val subTitlePaint = Paint().apply {
                color = Color.parseColor("#475569") // Slate Gray
                textSize = 10f
                isAntiAlias = true
            }
            val headerPaint = Paint().apply {
                color = Color.parseColor("#065F46")
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val normalPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 9f
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            val headerBg = Paint().apply {
                color = Color.parseColor("#FEF3C7") // Warm pale yellow/turmeric accent
                style = Paint.Style.FILL
            }
            val oddRowBg = Paint().apply {
                color = Color.parseColor("#F8FAFC") // Soft container gray
                style = Paint.Style.FILL
            }

            var y = 45f

            // Logo Header Card
            canvas.drawRect(30f, 25f, 565f, 90f, Paint().apply {
                color = Color.parseColor("#FAF9F6")
                style = Paint.Style.FILL
            })
            canvas.drawRect(30f, 25f, 565f, 90f, Paint().apply {
                color = Color.parseColor("#B45309")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            })
            canvas.drawText("MANAVAHANA (మన వాహనం)", 45f, 55f, primaryPaint)
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            canvas.drawText("Auspicious Vehicle Companion — Generated Offline Report: ${sdf.format(Date())}", 45f, 75f, subTitlePaint)

            y = 115f

            // SECTION 1: Vehicles Directory
            canvas.drawText("1. REGISTERED VEHICLES / వాహనాలు (${vehicles.size})", 30f, y, headerPaint)
            y += 6f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            if (vehicles.isEmpty()) {
                canvas.drawText("No vehicles registered in ManaVahana databases.", 45f, y, normalPaint)
                y += 20f
            } else {
                // Table Headers row
                canvas.drawRect(30f, y - 10f, 565f, y + 6f, headerBg)
                canvas.drawText("Vehicle Name", 35f, y, boldPaint)
                canvas.drawText("Plate Number", 150f, y, boldPaint)
                canvas.drawText("Brand / Model", 250f, y, boldPaint)
                canvas.drawText("Type / Fuel", 370f, y, boldPaint)
                canvas.drawText("Purchase Date", 475f, y, boldPaint)
                y += 6f
                canvas.drawLine(30f, y, 565f, y, linePaint)
                y += 14f

                var rowCount = 0
                for (vh in vehicles) {
                    if (y > 780f) {
                        pdf.finishPage(page)
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (rowCount % 2 == 1) {
                        canvas.drawRect(30f, y - 10f, 565f, y + 4f, oddRowBg)
                    }
                    canvas.drawText(vh.vehicleName, 35f, y, normalPaint)
                    canvas.drawText(vh.vehicleNumber.uppercase(), 150f, y, normalPaint)
                    canvas.drawText("${vh.brand} ${vh.model}", 250f, y, normalPaint)
                    canvas.drawText("${vh.vehicleType} / ${vh.fuelType}", 370f, y, normalPaint)
                    val purchaseSdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    canvas.drawText(purchaseSdf.format(Date(vh.purchaseDate)), 475f, y, normalPaint)
                    y += 16f
                    rowCount++
                }
            }

            y += 15f

            // SECTION 2: Premium Cost Summary & History
            if (y > 720f) {
                pdf.finishPage(page)
                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, 3).create()
                page = pdf.startPage(nextInfo)
                canvas = page.canvas
                y = 45f
            }

            canvas.drawText("2. RECENT LOGS & CORRESPONDING EXPENSES", 30f, y, headerPaint)
            y += 6f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            val totalAllExpenses = expenses.sumOf { it.amount }
            canvas.drawText("Total Aggregated Expenses:  ₹${String.format("%,.2f", totalAllExpenses)}", 35f, y, boldPaint)
            canvas.drawText("Total Fuel Volume Refilled:  ${String.format("%.2f", fuelLogs.sumOf { it.litersFilled })} Liters", 300f, y, boldPaint)
            y += 20f

            if (expenses.isEmpty()) {
                canvas.drawText("No items recorded in expenses so far.", 45f, y, normalPaint)
                y += 20f
            } else {
                canvas.drawRect(30f, y - 10f, 565f, y + 6f, headerBg)
                canvas.drawText("Category", 35f, y, boldPaint)
                canvas.drawText("Date", 130f, y, boldPaint)
                canvas.drawText("Amount (INR)", 230f, y, boldPaint)
                canvas.drawText("Transaction / Event Log Details", 340f, y, boldPaint)
                y += 6f
                canvas.drawLine(30f, y, 565f, y, linePaint)
                y += 14f

                var expRow = 0
                for (exp in expenses.sortedByDescending { it.expenseDate }.take(18)) {
                    if (y > 780f) {
                        pdf.finishPage(page)
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, 4).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (expRow % 2 == 1) {
                        canvas.drawRect(30f, y - 10f, 565f, y + 4f, oddRowBg)
                    }
                    canvas.drawText(exp.category, 35f, y, normalPaint)
                    val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(exp.expenseDate))
                    canvas.drawText(dateStr, 130f, y, normalPaint)
                    canvas.drawText("₹${String.format("%,.2f", exp.amount)}", 230f, y, normalPaint)
                    
                    val detail = if (exp.notes.length > 40) exp.notes.take(38) + "..." else exp.notes
                    canvas.drawText(detail, 340f, y, normalPaint)
                    y += 16f
                    expRow++
                }
                if (expenses.size > 18) {
                    canvas.drawText("... representing ${expenses.size} total items logged in the system.", 35f, y, subTitlePaint)
                    y += 16f
                }
            }

            y += 15f

            // SECTION 3: Compliance & Scheduled Checkups
            if (y > 720f) {
                pdf.finishPage(page)
                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, 5).create()
                page = pdf.startPage(nextInfo)
                canvas = page.canvas
                y = 45f
            }

            canvas.drawText("3. ALERTS & PENDING REMINDERS", 30f, y, headerPaint)
            y += 6f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            val activeList = reminders.filter { !it.isCompleted }.sortedBy { it.reminderDate }
            if (activeList.isEmpty()) {
                canvas.drawText("All reminders and regulatory compliances are fully cleared!", 45f, y, greenPaint)
                y += 20f
            } else {
                canvas.drawRect(30f, y - 10f, 565f, y + 6f, headerBg)
                canvas.drawText("Alert / Renewal Focus", 35f, y, boldPaint)
                canvas.drawText("Category", 210f, y, boldPaint)
                canvas.drawText("Scheduled Date", 330f, y, boldPaint)
                canvas.drawText("Status Icon Detail", 450f, y, boldPaint)
                y += 6f
                canvas.drawLine(30f, y, 565f, y, linePaint)
                y += 14f

                var remRow = 0
                for (rem in activeList.take(12)) {
                    if (y > 780f) {
                        pdf.finishPage(page)
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, 6).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (remRow % 2 == 1) {
                        canvas.drawRect(30f, y - 10f, 565f, y + 4f, oddRowBg)
                    }
                    canvas.drawText(rem.title, 35f, y, normalPaint)
                    canvas.drawText(rem.category, 210f, y, normalPaint)
                    val remDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(rem.reminderDate))
                    canvas.drawText(remDateStr, 330f, y, normalPaint)
                    canvas.drawText(if (rem.reminderDate < System.currentTimeMillis()) "🔴 OVERDUE" else "⏳ Active Schedule", 450f, y, normalPaint)
                    y += 16f
                    remRow++
                }
            }

            // Draw clean footer notes
            y = 810f
            canvas.drawLine(30f, y - 10f, 565f, y - 10f, linePaint)
            canvas.drawText("ManaVahana (మన వాహనం) — Secure Offline Companion. Built with traditional Telugu aesthetics.", 50f, y, subTitlePaint)

            pdf.finishPage(page)

            // Cache file output
            val outputFolder = File(context.cacheDir, "reports")
            if (!outputFolder.exists()) outputFolder.mkdirs()
            val reportFile = File(outputFolder, "manavahana_report.pdf")
            val stream = FileOutputStream(reportFile)
            pdf.writeTo(stream)
            stream.close()
            pdf.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                reportFile
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun savePdfToDownloads(context: Context, reportFile: File, displayName: String): Uri? {
        try {
            val cleanName = if (displayName.endsWith(".pdf", ignoreCase = true)) displayName else "$displayName.pdf"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ManaVahana")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (downloadUri != null) {
                    resolver.openOutputStream(downloadUri)?.use { out ->
                        reportFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(downloadUri, contentValues, null, null)
                    return downloadUri
                }
            }

            // Fallback for pre-Android 10 or when MediaStore is not accessible
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appFolder = File(downloadsDir, "ManaVahana")
            if (!appFolder.exists()) appFolder.mkdirs()
            val destFile = File(appFolder, cleanName)
            reportFile.copyTo(destFile, overwrite = true)

            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("application/pdf"), null)
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            // Final fallback: copy to external files directory
            try {
                val extDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (extDownloads != null) {
                    val fallbackFile = File(extDownloads, "$displayName.pdf")
                    reportFile.copyTo(fallbackFile, overwrite = true)
                    return FileProvider.getUriForFile(context, "${context.packageName}.provider", fallbackFile)
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", reportFile)
        }
    }

    /**
     * Backward-compatible overload for current month report
     */
    fun generateVehicleMonthlyReport(
        context: Context,
        vehicle: Vehicle,
        expenses: List<Expense>,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        reminders: List<Reminder>
    ): Uri? {
        val cal = Calendar.getInstance()
        val result = generateVehicleMonthlyReport(
            context = context,
            vehicle = vehicle,
            expenses = expenses,
            fuelLogs = fuelLogs,
            serviceLogs = serviceLogs,
            reminders = reminders,
            targetYear = cal.get(Calendar.YEAR),
            targetMonth = cal.get(Calendar.MONTH) + 1
        )
        return result?.uri
    }

    /**
     * Generates a comprehensive monthly report for a specific vehicle and target month/year.
     * Supports downloading old/historical reports.
     */
    fun generateVehicleMonthlyReport(
        context: Context,
        vehicle: Vehicle,
        expenses: List<Expense>,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        reminders: List<Reminder>,
        targetYear: Int,
        targetMonth: Int // 1..12
    ): GeneratedReportResult? {
        try {
            val pdf = PdfDocument()
            var pageNumber = 1
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas

            val targetCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, targetYear)
                set(Calendar.MONTH, targetMonth - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val monthFullName = SimpleDateFormat("MMMM yyyy", Locale.US).format(targetCal.time)
            val monthKey = String.format(Locale.US, "%02d-%04d", targetMonth, targetYear)

            val primaryPaint = Paint().apply {
                color = Color.parseColor("#B45309") // Terracotta Red / Kumkuma
                textSize = 21f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                color = Color.parseColor("#065F46") // Mango green / Leaf
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val greenPaint = Paint().apply {
                color = Color.parseColor("#065F46")
                textSize = 10f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val subTitlePaint = Paint().apply {
                color = Color.parseColor("#475569") // Slate Gray
                textSize = 9.5f
                isAntiAlias = true
            }
            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8.5f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val normalPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 8.5f
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            val headerBg = Paint().apply {
                color = Color.parseColor("#FEF3C7") // Turmeric gold highlight
                style = Paint.Style.FILL
            }
            val oddRowBg = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val cardBg = Paint().apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }

            var y = 45f

            // Logo Header Card
            canvas.drawRect(30f, 25f, 565f, 90f, Paint().apply {
                color = Color.parseColor("#FAF9F6")
                style = Paint.Style.FILL
            })
            canvas.drawRect(30f, 25f, 565f, 90f, Paint().apply {
                color = Color.parseColor("#B45309")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            })
            canvas.drawText("MANAVAHANA (మన వాహనం)", 45f, 52f, primaryPaint)
            val currentGenDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("మాస వాహన నివేదిక / Monthly Vehicle Report — $monthFullName", 45f, 70f, titlePaint)
            canvas.drawText("Generated: $currentGenDate • 100% Offline Secured Storage", 45f, 82f, subTitlePaint)

            y = 110f

            // Vehicle Identity Section
            canvas.drawText("1. VEHICLE PROFILE / వాహన వివరాలు", 30f, y, titlePaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 16f

            // Draw Vehicle Details Metadata
            canvas.drawRect(30f, y - 10f, 565f, y + 42f, cardBg)
            canvas.drawText("Name: ${vehicle.vehicleName}", 40f, y, boldPaint)
            canvas.drawText("Number: ${vehicle.vehicleNumber.uppercase()}", 210f, y, boldPaint)
            canvas.drawText("Type: ${vehicle.vehicleType} (${vehicle.fuelType})", 380f, y, boldPaint)
            y += 16f
            canvas.drawText("Brand: ${vehicle.brand}", 40f, y, normalPaint)
            canvas.drawText("Model: ${vehicle.model}", 210f, y, normalPaint)
            val purchaseSdf = SimpleDateFormat("dd MMMM yyyy", Locale.US)
            canvas.drawText("Bought: ${purchaseSdf.format(Date(vehicle.purchaseDate))}", 380f, y, normalPaint)
            y += 16f

            // Filter lists for vehicle and target month
            val vehicleExpenses = expenses.filter { it.vehicleId == vehicle.id }
            val vehicleFuelLogs = fuelLogs.filter { it.vehicleId == vehicle.id }
            val vehicleServiceLogs = serviceLogs.filter { it.vehicleId == vehicle.id }
            val vehicleReminders = reminders.filter { it.vehicleId == vehicle.id }

            val thisMonthExpenses = vehicleExpenses.filter {
                SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.expenseDate)) == monthKey
            }
            val thisMonthFuel = vehicleFuelLogs.filter {
                SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.fuelDate)) == monthKey
            }
            val thisMonthServices = vehicleServiceLogs.filter {
                SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.serviceDate)) == monthKey
            }

            // Estimate Current Mileage
            val mileage = if (vehicleFuelLogs.size >= 2) {
                val sortedFuel = vehicleFuelLogs.sortedBy { it.odometerReading }
                val dist = sortedFuel.last().odometerReading - sortedFuel.first().odometerReading
                val fuelVolume = sortedFuel.drop(1).sumOf { it.litersFilled }
                if (fuelVolume > 0) dist / fuelVolume else 0.0
            } else 0.0

            val lastOdo = maxOf(
                vehicleFuelLogs.maxOfOrNull { it.odometerReading } ?: 0.0,
                vehicleServiceLogs.maxOfOrNull { it.odometerReading } ?: 0.0
            )

            canvas.drawText("Last Odometer: ${String.format(Locale.US, "%,.1f", lastOdo)} km", 40f, y, boldPaint)
            canvas.drawText("Overall Mileage: " + (if (mileage > 0) "${String.format(Locale.US, "%.2f", mileage)} km/L" else "N/A"), 210f, y, greenPaint)

            y += 24f

            // Section 2: Financial Summary for this Month
            val totalMonthSpends = thisMonthExpenses.sumOf { it.amount }
            val totalMonthFuelCost = thisMonthFuel.sumOf { it.totalAmount }
            val totalMonthFuelLiters = thisMonthFuel.sumOf { it.litersFilled }
            val totalMonthServiceCost = thisMonthServices.sumOf { it.cost }
            val totalGrandSpend = totalMonthSpends + totalMonthFuelCost + totalMonthServiceCost
            val totalRecordsCount = thisMonthExpenses.size + thisMonthFuel.size + thisMonthServices.size

            canvas.drawText("2. MONTHLY FINANCIAL SUMMARY / $monthFullName ఖర్చులు", 30f, y, titlePaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 16f

            // Summary Stats Cards Box
            canvas.drawRect(30f, y - 8f, 565f, y + 42f, headerBg)
            canvas.drawText("GRAND TOTAL OUTLAY IN $monthFullName:", 40f, y + 4f, boldPaint)
            val highlightPaint = Paint().apply {
                color = Color.parseColor("#B45309")
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("₹${String.format(Locale.US, "%,.2f", totalGrandSpend)}", 300f, y + 5f, highlightPaint)

            y += 20f
            canvas.drawText("• Fuel: ₹${String.format(Locale.US, "%,.2f", totalMonthFuelCost)} (${String.format(Locale.US, "%.2f", totalMonthFuelLiters)} L)", 40f, y, normalPaint)
            canvas.drawText("• Services: ₹${String.format(Locale.US, "%,.2f", totalMonthServiceCost)} (${thisMonthServices.size} entries)", 210f, y, normalPaint)
            canvas.drawText("• Other: ₹${String.format(Locale.US, "%,.2f", totalMonthSpends)} (${thisMonthExpenses.size} entries)", 380f, y, normalPaint)

            y += 28f

            // Section 3: Fuel Refills in target Month
            canvas.drawText("3. FUEL REFILLS / ఇంధన లాగ్‌లు ($monthFullName)", 30f, y, titlePaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            if (thisMonthFuel.isEmpty()) {
                canvas.drawText("No fuel refills logged in $monthFullName.", 40f, y, normalPaint)
                y += 18f
            } else {
                canvas.drawRect(30f, y - 9f, 565f, y + 5f, headerBg)
                canvas.drawText("Date", 35f, y, boldPaint)
                canvas.drawText("Fuel Station", 110f, y, boldPaint)
                canvas.drawText("Liters", 260f, y, boldPaint)
                canvas.drawText("Price/L", 330f, y, boldPaint)
                canvas.drawText("Total Cost", 410f, y, boldPaint)
                canvas.drawText("Odometer", 490f, y, boldPaint)
                y += 5f
                canvas.drawLine(30f, y, 565f, y, linePaint)
                y += 13f

                var rowIdx = 0
                for (fuel in thisMonthFuel.sortedByDescending { it.fuelDate }) {
                    if (y > 750f) {
                        pdf.finishPage(page)
                        pageNumber++
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (rowIdx % 2 == 1) {
                        canvas.drawRect(30f, y - 9f, 565f, y + 4f, oddRowBg)
                    }
                    val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(fuel.fuelDate))
                    canvas.drawText(dateStr, 35f, y, normalPaint)
                    val stn = if (fuel.fuelStationName.length > 22) fuel.fuelStationName.take(20) + ".." else fuel.fuelStationName.ifEmpty { "Petrol Pump" }
                    canvas.drawText(stn, 110f, y, normalPaint)
                    canvas.drawText("${String.format(Locale.US, "%.2f", fuel.litersFilled)} L", 260f, y, normalPaint)
                    canvas.drawText("₹${String.format(Locale.US, "%.1f", fuel.pricePerLiter)}", 330f, y, normalPaint)
                    canvas.drawText("₹${String.format(Locale.US, "%,.2f", fuel.totalAmount)}", 410f, y, boldPaint)
                    canvas.drawText("${String.format(Locale.US, "%,.0f", fuel.odometerReading)} km", 490f, y, normalPaint)
                    y += 15f
                    rowIdx++
                }
            }

            y += 10f

            // Section 4: Maintenance & Service Logs in target Month
            if (y > 700f) {
                pdf.finishPage(page)
                pageNumber++
                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdf.startPage(nextInfo)
                canvas = page.canvas
                y = 45f
            }

            canvas.drawText("4. SERVICE & REPAIRS / సర్వీస్ వివరాలు ($monthFullName)", 30f, y, titlePaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            if (thisMonthServices.isEmpty()) {
                canvas.drawText("No service or repair records logged in $monthFullName.", 40f, y, normalPaint)
                y += 18f
            } else {
                canvas.drawRect(30f, y - 9f, 565f, y + 5f, headerBg)
                canvas.drawText("Date", 35f, y, boldPaint)
                canvas.drawText("Service Center", 110f, y, boldPaint)
                canvas.drawText("Type", 260f, y, boldPaint)
                canvas.drawText("Cost", 360f, y, boldPaint)
                canvas.drawText("Mechanic Notes", 440f, y, boldPaint)
                y += 5f
                canvas.drawLine(30f, y, 565f, y, linePaint)
                y += 13f

                var sRow = 0
                for (srv in thisMonthServices.sortedByDescending { it.serviceDate }) {
                    if (y > 750f) {
                        pdf.finishPage(page)
                        pageNumber++
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (sRow % 2 == 1) {
                        canvas.drawRect(30f, y - 9f, 565f, y + 4f, oddRowBg)
                    }
                    val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(srv.serviceDate))
                    canvas.drawText(dateStr, 35f, y, normalPaint)
                    val sc = if (srv.serviceCenter.length > 22) srv.serviceCenter.take(20) + ".." else srv.serviceCenter.ifEmpty { "Workshop" }
                    canvas.drawText(sc, 110f, y, normalPaint)
                    canvas.drawText(srv.serviceType.take(16), 260f, y, normalPaint)
                    canvas.drawText("₹${String.format(Locale.US, "%,.2f", srv.cost)}", 360f, y, boldPaint)
                    val notes = if (srv.notes.length > 22) srv.notes.take(20) + ".." else srv.notes.ifEmpty { "General Maintenance" }
                    canvas.drawText(notes, 440f, y, normalPaint)
                    y += 15f
                    sRow++
                }
            }

            y += 10f

            // Section 5: Other Logged Expenses in target Month
            if (y > 700f) {
                pdf.finishPage(page)
                pageNumber++
                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdf.startPage(nextInfo)
                canvas = page.canvas
                y = 45f
            }

            canvas.drawText("5. OTHER EXPENSES / ఇతర ఖర్చులు ($monthFullName)", 30f, y, titlePaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            if (thisMonthExpenses.isEmpty()) {
                canvas.drawText("No additional expenses logged in $monthFullName.", 40f, y, normalPaint)
                y += 18f
            } else {
                canvas.drawRect(30f, y - 9f, 565f, y + 5f, headerBg)
                canvas.drawText("Date", 35f, y, boldPaint)
                canvas.drawText("Category", 120f, y, boldPaint)
                canvas.drawText("Amount (INR)", 240f, y, boldPaint)
                canvas.drawText("Notes & Specifics", 360f, y, boldPaint)
                y += 5f
                canvas.drawLine(30f, y, 565f, y, linePaint)
                y += 13f

                var eRow = 0
                for (exp in thisMonthExpenses.sortedByDescending { it.expenseDate }) {
                    if (y > 750f) {
                        pdf.finishPage(page)
                        pageNumber++
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (eRow % 2 == 1) {
                        canvas.drawRect(30f, y - 9f, 565f, y + 4f, oddRowBg)
                    }
                    val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(exp.expenseDate))
                    canvas.drawText(dateStr, 35f, y, normalPaint)
                    canvas.drawText(exp.category, 120f, y, normalPaint)
                    canvas.drawText("₹${String.format(Locale.US, "%,.2f", exp.amount)}", 240f, y, boldPaint)
                    val note = if (exp.notes.length > 35) exp.notes.take(33) + ".." else exp.notes
                    canvas.drawText(note, 360f, y, normalPaint)
                    y += 15f
                    eRow++
                }
            }

            y += 10f

            // Section 6: Upcoming & Regulatory Reminders
            if (y > 690f) {
                pdf.finishPage(page)
                pageNumber++
                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdf.startPage(nextInfo)
                canvas = page.canvas
                y = 45f
            }

            canvas.drawText("6. REGULATORY COMPLIANCE & ALERTS / నియంత్రణ అలర్ట్లు", 30f, y, titlePaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            val activeReminders = vehicleReminders.filter { !it.isCompleted }
            if (activeReminders.isEmpty()) {
                canvas.drawText("Vehicular compliances are fully up-to-date. Safe routes ahead!", 40f, y, greenPaint)
                y += 18f
            } else {
                for (rem in activeReminders.take(4)) {
                    if (y > 780f) {
                        pdf.finishPage(page)
                        pageNumber++
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    val rDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(rem.reminderDate))
                    val isOverdue = rem.reminderDate < System.currentTimeMillis()
                    canvas.drawText("• ${rem.title} [${rem.category}] - Due: $rDateStr ${if (isOverdue) "(🔴 Overdue)" else "(⏳ Scheduled)"}", 40f, y, normalPaint)
                    y += 14f
                }
            }

            // Draw clean footer notes
            y = 810f
            canvas.drawLine(30f, y - 10f, 565f, y - 10f, linePaint)
            canvas.drawText("సదా మీ క్షేమమే మా ఆకాంక్ష - మన వాహన మాస నివేదిక | ManaVahana Offline Secured Report", 45f, y, subTitlePaint)

            pdf.finishPage(page)

            // Cache file output
            val outputFolder = File(context.cacheDir, "reports")
            if (!outputFolder.exists()) outputFolder.mkdirs()
            val safeVehName = vehicle.vehicleName.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val safeMonth = monthFullName.replace(" ", "_")
            val fileName = "ManaVahana_Report_${safeVehName}_${safeMonth}.pdf"
            val reportFile = File(outputFolder, fileName)
            val stream = FileOutputStream(reportFile)
            pdf.writeTo(stream)
            stream.close()
            pdf.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                reportFile
            )

            return GeneratedReportResult(
                file = reportFile,
                uri = uri,
                fileName = fileName,
                title = "${vehicle.vehicleName} - $monthFullName Report",
                monthName = monthFullName,
                year = targetYear,
                month = targetMonth,
                totalSpent = totalGrandSpend,
                recordCount = totalRecordsCount
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Generates a combined garage monthly report across all vehicles for a chosen month/year.
     */
    fun generateAllVehiclesMonthlyReport(
        context: Context,
        vehicles: List<Vehicle>,
        expenses: List<Expense>,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        reminders: List<Reminder>,
        targetYear: Int,
        targetMonth: Int
    ): GeneratedReportResult? {
        try {
            val pdf = PdfDocument()
            var pageNumber = 1
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas

            val targetCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, targetYear)
                set(Calendar.MONTH, targetMonth - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val monthFullName = SimpleDateFormat("MMMM yyyy", Locale.US).format(targetCal.time)
            val monthKey = String.format(Locale.US, "%02d-%04d", targetMonth, targetYear)

            val primaryPaint = Paint().apply {
                color = Color.parseColor("#B45309")
                textSize = 21f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                color = Color.parseColor("#065F46")
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val subTitlePaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 9.5f
                isAntiAlias = true
            }
            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8.5f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val normalPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 8.5f
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            val headerBg = Paint().apply {
                color = Color.parseColor("#FEF3C7")
                style = Paint.Style.FILL
            }
            val oddRowBg = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            var y = 45f

            // Logo Header Card
            canvas.drawRect(30f, 25f, 565f, 90f, Paint().apply {
                color = Color.parseColor("#FAF9F6")
                style = Paint.Style.FILL
            })
            canvas.drawRect(30f, 25f, 565f, 90f, Paint().apply {
                color = Color.parseColor("#B45309")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            })
            canvas.drawText("MANAVAHANA (మన వాహనం)", 45f, 52f, primaryPaint)
            val currentGenDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("గ్యారేజ్ మొత్తం మాస నివేదిక / Garage Status Report — $monthFullName", 45f, 70f, titlePaint)
            canvas.drawText("Generated: $currentGenDate • Vehicles Monitored: ${vehicles.size}", 45f, 82f, subTitlePaint)

            y = 110f

            // Filter all records for target month
            val thisMonthExpenses = expenses.filter {
                SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.expenseDate)) == monthKey
            }
            val thisMonthFuel = fuelLogs.filter {
                SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.fuelDate)) == monthKey
            }
            val thisMonthServices = serviceLogs.filter {
                SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.serviceDate)) == monthKey
            }

            val totalFuelCost = thisMonthFuel.sumOf { it.totalAmount }
            val totalFuelLiters = thisMonthFuel.sumOf { it.litersFilled }
            val totalServiceCost = thisMonthServices.sumOf { it.cost }
            val totalOtherCost = thisMonthExpenses.sumOf { it.amount }
            val totalGrandSpend = totalFuelCost + totalServiceCost + totalOtherCost
            val totalRecordsCount = thisMonthFuel.size + thisMonthServices.size + thisMonthExpenses.size

            // Section 1: Financial Summary Box
            canvas.drawText("1. GARAGE FINANCIAL SUMMARY / గ్యారేజ్ ఖర్చుల సారాంశం", 30f, y, titlePaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 16f

            canvas.drawRect(30f, y - 8f, 565f, y + 42f, headerBg)
            canvas.drawText("TOTAL GARAGE EXPENDITURE IN $monthFullName:", 40f, y + 4f, boldPaint)
            val highlightPaint = Paint().apply {
                color = Color.parseColor("#B45309")
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("₹${String.format(Locale.US, "%,.2f", totalGrandSpend)}", 300f, y + 5f, highlightPaint)

            y += 20f
            canvas.drawText("• Total Fuel: ₹${String.format(Locale.US, "%,.2f", totalFuelCost)} (${String.format(Locale.US, "%.1f", totalFuelLiters)} L)", 40f, y, normalPaint)
            canvas.drawText("• Total Services: ₹${String.format(Locale.US, "%,.2f", totalServiceCost)} (${thisMonthServices.size} entries)", 220f, y, normalPaint)
            canvas.drawText("• Other Expenses: ₹${String.format(Locale.US, "%,.2f", totalOtherCost)} (${thisMonthExpenses.size} entries)", 390f, y, normalPaint)

            y += 28f

            // Section 2: Per Vehicle Breakdown
            canvas.drawText("2. VEHICLE BREAKDOWN / వాహనవారీ వివరాలు", 30f, y, titlePaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            canvas.drawRect(30f, y - 9f, 565f, y + 5f, headerBg)
            canvas.drawText("Vehicle Name", 35f, y, boldPaint)
            canvas.drawText("Plate Number", 160f, y, boldPaint)
            canvas.drawText("Fuel Spent", 270f, y, boldPaint)
            canvas.drawText("Service Spent", 370f, y, boldPaint)
            canvas.drawText("Total Spend", 470f, y, boldPaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 13f

            var vRow = 0
            for (veh in vehicles) {
                if (y > 750f) {
                    pdf.finishPage(page)
                    pageNumber++
                    val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdf.startPage(nextInfo)
                    canvas = page.canvas
                    y = 45f
                }
                if (vRow % 2 == 1) {
                    canvas.drawRect(30f, y - 9f, 565f, y + 4f, oddRowBg)
                }
                val vFuel = thisMonthFuel.filter { it.vehicleId == veh.id }.sumOf { it.totalAmount }
                val vSrv = thisMonthServices.filter { it.vehicleId == veh.id }.sumOf { it.cost }
                val vExp = thisMonthExpenses.filter { it.vehicleId == veh.id }.sumOf { it.amount }
                val vTot = vFuel + vSrv + vExp

                canvas.drawText(veh.vehicleName.take(18), 35f, y, normalPaint)
                canvas.drawText(veh.vehicleNumber.uppercase(), 160f, y, normalPaint)
                canvas.drawText("₹${String.format(Locale.US, "%,.1f", vFuel)}", 270f, y, normalPaint)
                canvas.drawText("₹${String.format(Locale.US, "%,.1f", vSrv)}", 370f, y, normalPaint)
                canvas.drawText("₹${String.format(Locale.US, "%,.2f", vTot)}", 470f, y, boldPaint)
                y += 15f
                vRow++
            }

            y += 10f

            // Section 3: Itemized Monthly Transactions
            if (y > 700f) {
                pdf.finishPage(page)
                pageNumber++
                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdf.startPage(nextInfo)
                canvas = page.canvas
                y = 45f
            }

            canvas.drawText("3. DETAILED LOGS / లాగ్‌ల జాబితా ($monthFullName)", 30f, y, titlePaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            if (totalRecordsCount == 0) {
                canvas.drawText("No log transactions found across garage in $monthFullName.", 40f, y, normalPaint)
                y += 18f
            } else {
                canvas.drawRect(30f, y - 9f, 565f, y + 5f, headerBg)
                canvas.drawText("Date", 35f, y, boldPaint)
                canvas.drawText("Vehicle", 110f, y, boldPaint)
                canvas.drawText("Type", 210f, y, boldPaint)
                canvas.drawText("Amount", 310f, y, boldPaint)
                canvas.drawText("Details / Notes", 400f, y, boldPaint)
                y += 5f
                canvas.drawLine(30f, y, 565f, y, linePaint)
                y += 13f

                var logRow = 0

                // Fuel logs
                for (fl in thisMonthFuel.sortedByDescending { it.fuelDate }) {
                    if (y > 750f) {
                        pdf.finishPage(page)
                        pageNumber++
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (logRow % 2 == 1) canvas.drawRect(30f, y - 9f, 565f, y + 4f, oddRowBg)
                    val dStr = SimpleDateFormat("dd-MM", Locale.US).format(Date(fl.fuelDate))
                    val vName = vehicles.find { it.id == fl.vehicleId }?.vehicleName?.take(14) ?: "Vehicle"
                    canvas.drawText(dStr, 35f, y, normalPaint)
                    canvas.drawText(vName, 110f, y, normalPaint)
                    canvas.drawText("Fuel", 210f, y, normalPaint)
                    canvas.drawText("₹${String.format(Locale.US, "%,.2f", fl.totalAmount)}", 310f, y, boldPaint)
                    canvas.drawText("${String.format(Locale.US, "%.1f", fl.litersFilled)}L at ${fl.fuelStationName.take(18)}", 400f, y, normalPaint)
                    y += 15f
                    logRow++
                }

                // Service logs
                for (sl in thisMonthServices.sortedByDescending { it.serviceDate }) {
                    if (y > 750f) {
                        pdf.finishPage(page)
                        pageNumber++
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (logRow % 2 == 1) canvas.drawRect(30f, y - 9f, 565f, y + 4f, oddRowBg)
                    val dStr = SimpleDateFormat("dd-MM", Locale.US).format(Date(sl.serviceDate))
                    val vName = vehicles.find { it.id == sl.vehicleId }?.vehicleName?.take(14) ?: "Vehicle"
                    canvas.drawText(dStr, 35f, y, normalPaint)
                    canvas.drawText(vName, 110f, y, normalPaint)
                    canvas.drawText("Service", 210f, y, normalPaint)
                    canvas.drawText("₹${String.format(Locale.US, "%,.2f", sl.cost)}", 310f, y, boldPaint)
                    canvas.drawText("${sl.serviceType} at ${sl.serviceCenter.take(18)}", 400f, y, normalPaint)
                    y += 15f
                    logRow++
                }

                // Expenses logs
                for (el in thisMonthExpenses.sortedByDescending { it.expenseDate }) {
                    if (y > 750f) {
                        pdf.finishPage(page)
                        pageNumber++
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (logRow % 2 == 1) canvas.drawRect(30f, y - 9f, 565f, y + 4f, oddRowBg)
                    val dStr = SimpleDateFormat("dd-MM", Locale.US).format(Date(el.expenseDate))
                    val vName = vehicles.find { it.id == el.vehicleId }?.vehicleName?.take(14) ?: "Vehicle"
                    canvas.drawText(dStr, 35f, y, normalPaint)
                    canvas.drawText(vName, 110f, y, normalPaint)
                    canvas.drawText(el.category.take(12), 210f, y, normalPaint)
                    canvas.drawText("₹${String.format(Locale.US, "%,.2f", el.amount)}", 310f, y, boldPaint)
                    canvas.drawText(el.notes.take(24), 400f, y, normalPaint)
                    y += 15f
                    logRow++
                }
            }

            // Draw clean footer notes
            y = 810f
            canvas.drawLine(30f, y - 10f, 565f, y - 10f, linePaint)
            canvas.drawText("మన వాహనం గ్యారేజ్ సమగ్ర నివేదిక — ManaVahana Complete Garage Report", 45f, y, subTitlePaint)

            pdf.finishPage(page)

            // Cache file output
            val outputFolder = File(context.cacheDir, "reports")
            if (!outputFolder.exists()) outputFolder.mkdirs()
            val safeMonth = monthFullName.replace(" ", "_")
            val fileName = "ManaVahana_Garage_Report_${safeMonth}.pdf"
            val reportFile = File(outputFolder, fileName)
            val stream = FileOutputStream(reportFile)
            pdf.writeTo(stream)
            stream.close()
            pdf.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                reportFile
            )

            return GeneratedReportResult(
                file = reportFile,
                uri = uri,
                fileName = fileName,
                title = "Garage Report - $monthFullName",
                monthName = monthFullName,
                year = targetYear,
                month = targetMonth,
                totalSpent = totalGrandSpend,
                recordCount = totalRecordsCount
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

