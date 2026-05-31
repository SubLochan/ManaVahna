package com.example.ui.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Expense
import com.example.data.model.FuelLog
import com.example.data.model.Reminder
import com.example.data.model.ServiceLog
import com.example.data.model.Vehicle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

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

    fun generateVehicleMonthlyReport(
        context: Context,
        vehicle: Vehicle,
        expenses: List<Expense>,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        reminders: List<Reminder>
    ): Uri? {
        try {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas

            val primaryPaint = Paint().apply {
                color = Color.parseColor("#B45309") // Terracotta Red / Kumkuma
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                color = Color.parseColor("#065F46") // Mango green / Leaf
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val greenPaint = Paint().apply {
                color = Color.parseColor("#065F46")
                textSize = 11f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val subTitlePaint = Paint().apply {
                color = Color.parseColor("#475569") // Slate Gray
                textSize = 10f
                isAntiAlias = true
            }
            val headerPaint = Paint().apply {
                color = Color.parseColor("#B45309")
                textSize = 11f
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
                color = Color.parseColor("#FEF3C7") // Turmeric gold highlight
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
            val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
            val currentMonthStr = SimpleDateFormat("MM-yyyy", Locale.US).format(Date())
            canvas.drawText("మాస వాహన నివేదిక / Monthly Vehicle Report — $currentMonthName", 45f, 75f, subTitlePaint)

            y = 115f

            // Vehicle Identity Section
            canvas.drawText("1. VEHICLE PROFILE / వాహన వివరాలు", 30f, y, titlePaint)
            y += 6f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            // Draw Vehicle Details Metadata
            canvas.drawRect(30f, y - 10f, 565f, y + 42f, Paint().apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            })
            canvas.drawText("Name: ${vehicle.vehicleName}", 40f, y, boldPaint)
            canvas.drawText("Number: ${vehicle.vehicleNumber.uppercase()}", 210f, y, boldPaint)
            canvas.drawText("Type: ${vehicle.vehicleType} (${vehicle.fuelType})", 380f, y, boldPaint)
            y += 18f
            canvas.drawText("Brand: ${vehicle.brand}", 40f, y, normalPaint)
            canvas.drawText("Model: ${vehicle.model}", 210f, y, normalPaint)
            val purchaseSdf = SimpleDateFormat("dd MMMM yyyy", Locale.US)
            canvas.drawText("Bought: ${purchaseSdf.format(Date(vehicle.purchaseDate))}", 380f, y, normalPaint)
            y += 18f
            
            // Filter lists
            val vehicleExpenses = expenses.filter { it.vehicleId == vehicle.id }
            val vehicleFuelLogs = fuelLogs.filter { it.vehicleId == vehicle.id }
            val vehicleServiceLogs = serviceLogs.filter { it.vehicleId == vehicle.id }
            val vehicleReminders = reminders.filter { it.vehicleId == vehicle.id }

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

            canvas.drawText("Last Odometer: ${String.format("%,.1f", lastOdo)} km", 40f, y, boldPaint)
            canvas.drawText("Estimated Mileage: " + (if (mileage > 0) "${String.format("%.2f", mileage)} km/L" else "Need 2+ fuel logs"), 210f, y, greenPaint)
            
            y += 26f

            // Section 2: This Month's Metrics
            canvas.drawText("2. THIS MONTH'S FINANCIAL SUMMARY / ఈ నెల నివేదిక", 30f, y, titlePaint)
            y += 6f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            val thisMonthExpenses = vehicleExpenses.filter {
                SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.expenseDate)) == currentMonthStr
            }
            val thisMonthFuel = vehicleFuelLogs.filter {
                SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.fuelDate)) == currentMonthStr
            }
            val thisMonthServices = vehicleServiceLogs.filter {
                SimpleDateFormat("MM-yyyy", Locale.US).format(Date(it.serviceDate)) == currentMonthStr
            }

            val totalMonthSpends = thisMonthExpenses.sumOf { it.amount }
            val totalMonthFuelCost = thisMonthFuel.sumOf { it.totalAmount }
            val totalMonthFuelLiters = thisMonthFuel.sumOf { it.litersFilled }

            canvas.drawText("Total Spends logged this month:  ₹${String.format("%,.2f", totalMonthSpends)}", 35f, y, boldPaint)
            y += 14f
            canvas.drawText("Fuel Filled this month:  ${String.format("%.2f", totalMonthFuelLiters)} Liters | Cost: ₹${String.format("%,.2f", totalMonthFuelCost)}", 35f, y, normalPaint)
            y += 14f
            canvas.drawText("Service Repairs this month:  ${thisMonthServices.size} entries", 35f, y, normalPaint)
            
            y += 24f

            // Section 3: Recent Log Entries
            canvas.drawText("3. DETAILED EXPENSE & FUEL RECENT LOGS / లాగ్‌లు", 30f, y, titlePaint)
            y += 6f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            if (vehicleExpenses.isEmpty()) {
                canvas.drawText("No log entries present for this vehicle.", 45f, y, normalPaint)
                y += 20f
            } else {
                canvas.drawRect(30f, y - 10f, 565f, y + 6f, headerBg)
                canvas.drawText("Category", 35f, y, boldPaint)
                canvas.drawText("Date", 130f, y, boldPaint)
                canvas.drawText("Amount (INR)", 230f, y, boldPaint)
                canvas.drawText("Notes / Transaction Specific details", 340f, y, boldPaint)
                y += 6f
                canvas.drawLine(30f, y, 565f, y, linePaint)
                y += 14f

                var rowIdx = 0
                for (exp in vehicleExpenses.sortedByDescending { it.expenseDate }.take(15)) {
                    if (y > 750f) {
                        pdf.finishPage(page)
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (rowIdx % 2 == 1) {
                        canvas.drawRect(30f, y - 10f, 565f, y + 4f, oddRowBg)
                    }
                    canvas.drawText(exp.category, 35f, y, normalPaint)
                    val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(exp.expenseDate))
                    canvas.drawText(dateStr, 130f, y, normalPaint)
                    canvas.drawText("₹${String.format("%,.2f", exp.amount)}", 230f, y, normalPaint)
                    val notesDetail = if (exp.notes.length > 40) exp.notes.take(38) + "..." else exp.notes
                    canvas.drawText(notesDetail, 340f, y, normalPaint)
                    y += 16f
                    rowIdx++
                }
            }

            y += 15f

            // Section 4: Upcoming Reminders
            if (y > 700f) {
                pdf.finishPage(page)
                val nextInfo = PdfDocument.PageInfo.Builder(595, 842, 3).create()
                page = pdf.startPage(nextInfo)
                canvas = page.canvas
                y = 45f
            }

            canvas.drawText("4. ALERTS & REGULATORY REMINDERS / అలర్ట్లు", 30f, y, titlePaint)
            y += 6f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            val activeReminders = vehicleReminders.filter { !it.isCompleted }
            if (activeReminders.isEmpty()) {
                canvas.drawText("Vehicular compliances are up-to-date. Safe routes ahead!", 45f, y, greenPaint)
                y += 20f
            } else {
                canvas.drawRect(30f, y - 10f, 565f, y + 6f, headerBg)
                canvas.drawText("Reminder Title", 35f, y, boldPaint)
                canvas.drawText("Scheduled Date", 250f, y, boldPaint)
                canvas.drawText("Alert Type", 380f, y, boldPaint)
                canvas.drawText("Status / Action", 480f, y, boldPaint)
                y += 6f
                canvas.drawLine(30f, y, 565f, y, linePaint)
                y += 14f

                var remIdx = 0
                for (rem in activeReminders.take(6)) {
                    if (y > 780f) {
                        pdf.finishPage(page)
                        val nextInfo = PdfDocument.PageInfo.Builder(595, 842, 4).create()
                        page = pdf.startPage(nextInfo)
                        canvas = page.canvas
                        y = 45f
                    }
                    if (remIdx % 2 == 1) {
                        canvas.drawRect(30f, y - 10f, 565f, y + 4f, oddRowBg)
                    }
                    canvas.drawText(rem.title, 35f, y, normalPaint)
                    val rDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(rem.reminderDate))
                    canvas.drawText(rDateStr, 250f, y, normalPaint)
                    canvas.drawText(rem.category, 380f, y, normalPaint)
                    canvas.drawText(if (rem.reminderDate < System.currentTimeMillis()) "🔴 OVERDUE" else "⏳ Scheduled", 480f, y, normalPaint)
                    y += 16f
                    remIdx++
                }
            }

            // Draw clean footer notes
            y = 810f
            canvas.drawLine(30f, y - 10f, 565f, y - 10f, linePaint)
            canvas.drawText("సదా మీ క్షేమమే మా ఆకాంక్ష - మన వాహన మాస నివేదిక | Wishing you auspicious journeys — ManaVahana.", 45f, y, subTitlePaint)

            pdf.finishPage(page)

            // Cache file output
            val outputFolder = File(context.cacheDir, "reports")
            if (!outputFolder.exists()) outputFolder.mkdirs()
            val reportFile = File(outputFolder, "manavahana_monthly_${vehicle.id}.pdf")
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
}

