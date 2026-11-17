package com.manavahana

import com.manavahana.data.model.Document
import com.manavahana.data.model.Expense
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class DocumentRenewalUnitTest {

    @Test
    fun testDocumentExpiryCalculations() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        // Expired document (10 days ago)
        val expiredDoc = Document(
            id = 1,
            vehicleId = 101,
            docType = "Insurance",
            title = "Car Insurance",
            expiryDate = now - (10 * dayMs),
            documentPath = "/data/user/0/com.manavahana/files/old_insurance.jpg"
        )
        val daysUntilExpired = ((expiredDoc.expiryDate!! - now) / dayMs).toInt()
        assertTrue("Document should be expired", expiredDoc.expiryDate!! < now)
        assertEquals(-10, daysUntilExpired)

        // Active document (60 days left)
        val activeDoc = Document(
            id = 2,
            vehicleId = 101,
            docType = "Pollution Certificate",
            title = "PUC Certificate",
            expiryDate = now + (60 * dayMs),
            documentPath = "/data/user/0/com.manavahana/files/old_puc.pdf"
        )
        val daysUntilActiveDoc = ((activeDoc.expiryDate!! - now) / dayMs).toInt()
        assertTrue("Document should be active", activeDoc.expiryDate!! > now)
        assertEquals(60, daysUntilActiveDoc)
    }

    @Test
    fun testDocumentRenewalWithReplacedPaper() {
        val initialDoc = Document(
            id = 5,
            vehicleId = 202,
            docType = "Insurance",
            title = "Comprehensive Insurance",
            expiryDate = 1700000000000L, // past date
            documentPath = "/data/user/0/com.manavahana/files/old_insurance_paper.jpg"
        )

        val newCal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.YEAR, 1)
        }
        val newExpiry = newCal.timeInMillis
        val newPaperPath = "/data/user/0/com.manavahana/files/renewed_doc_img_2026.jpg"

        // Perform renewal copy
        val renewedDoc = initialDoc.copy(
            title = "Comprehensive Insurance (2026-2027)",
            expiryDate = newExpiry,
            documentPath = newPaperPath,
            isEncrypted = true
        )

        assertEquals("Comprehensive Insurance (2026-2027)", renewedDoc.title)
        assertEquals(newExpiry, renewedDoc.expiryDate)
        assertEquals(newPaperPath, renewedDoc.documentPath)
        assertNotEquals(initialDoc.documentPath, renewedDoc.documentPath)

        // Expense logging test
        val renewalCost = 3450.0
        val expense = Expense(
            vehicleId = renewedDoc.vehicleId,
            expenseDate = System.currentTimeMillis(),
            category = "Insurance",
            amount = renewalCost,
            notes = "Renewed Insurance: ${renewedDoc.title}"
        )

        assertEquals(202, expense.vehicleId)
        assertEquals(3450.0, expense.amount, 0.001)
        assertEquals("Insurance", expense.category)
    }

    @Test
    fun testDirectedAlertSuppressedAfterRenewal() {
        val now = 1_750_000_000_000L
        val dayMs = 24 * 60 * 60 * 1000L
        val oldExpiry = now + dayMs // tomorrow
        val renewedExpiry = now + (365 * dayMs) // next year

        // Before renewal: 1_day_before alert matching expectedExpiry should trigger
        assertTrue(
            com.manavahana.worker.ReminderWorker.shouldTriggerDirectedDocAlert(
                expiryDate = oldExpiry,
                alertType = "1_day_before",
                expectedExpiry = oldExpiry,
                currentTime = now
            )
        )

        // After renewal: stale work carrying old expectedExpiry must NOT trigger
        assertFalse(
            com.manavahana.worker.ReminderWorker.shouldTriggerDirectedDocAlert(
                expiryDate = renewedExpiry,
                alertType = "1_day_before",
                expectedExpiry = oldExpiry,
                currentTime = now
            )
        )

        // Even without expectedExpiry, a renewed expiry 365 days out must NOT trigger
        assertFalse(
            com.manavahana.worker.ReminderWorker.shouldTriggerDirectedDocAlert(
                expiryDate = renewedExpiry,
                alertType = "7_days_before",
                expectedExpiry = -1L,
                currentTime = now
            )
        )
    }

    @Test
    fun testResolveExpiriesAndStaleReminderDetection() {
        val now = 1_750_000_000_000L
        val dayMs = 24 * 60 * 60 * 1000L
        val oldExpiredDate = now - (5 * dayMs)
        val renewedFutureDate = now + (360 * dayMs)

        val vehicleWithStaleExpiry = com.manavahana.data.model.Vehicle(
            id = 1,
            vehicleName = "Creta",
            vehicleNumber = "TS09AB1234",
            brand = "Hyundai",
            model = "SX",
            vehicleType = "Car",
            fuelType = "Petrol",
            purchaseDate = now - (400 * dayMs),
            insuranceExpiry = oldExpiredDate,
            pollutionExpiry = oldExpiredDate
        )

        val renewedInsuranceDoc = Document(
            id = 10,
            vehicleId = 1,
            docType = "Insurance",
            title = "Comprehensive Policy 2026",
            expiryDate = renewedFutureDate,
            documentPath = "/files/ins_2026.pdf"
        )
        val expiredOldInsuranceDoc = Document(
            id = 11,
            vehicleId = 1,
            docType = "Insurance",
            title = "Comprehensive Policy 2025",
            expiryDate = oldExpiredDate,
            documentPath = "/files/ins_2025.pdf"
        )
        val stillExpiredPucDoc = Document(
            id = 12,
            vehicleId = 1,
            docType = "Pollution Certificate",
            title = "PUC 2025",
            expiryDate = oldExpiredDate,
            documentPath = "/files/puc_2025.pdf"
        )

        val resolution = com.manavahana.worker.ReminderWorker.resolveExpiries(
            vehicles = listOf(vehicleWithStaleExpiry),
            documents = listOf(renewedInsuranceDoc, expiredOldInsuranceDoc, stillExpiredPucDoc)
        )

        // Renewed Insurance Doc heals effective vehicle insurance expiry
        assertEquals(renewedFutureDate, resolution.effectiveVehicleInsuranceExpiry[1])
        // PUC wasn't renewed yet, stays expired
        assertEquals(oldExpiredDate, resolution.effectiveVehiclePollutionExpiry[1])

        // Old insurance document is superseded by the renewed one
        assertTrue(
            com.manavahana.worker.ReminderWorker.isDocumentSuperseded(
                expiredOldInsuranceDoc,
                resolution.latestDocExpiryByVehicleAndCat,
                now
            )
        )
        // Renewed insurance doc itself is NOT superseded
        assertFalse(
            com.manavahana.worker.ReminderWorker.isDocumentSuperseded(
                renewedInsuranceDoc,
                resolution.latestDocExpiryByVehicleAndCat,
                now
            )
        )

        // Auto-created stale vehicle insurance renewal reminder is detected as stale
        val staleVehicleInsReminder = com.manavahana.data.model.Reminder(
            id = 101,
            vehicleId = 1,
            title = "Creta - Insurance Renewal",
            description = "Insurance is expiring. Renew with insurance provider.",
            reminderDate = oldExpiredDate - (2 * dayMs),
            category = "Insurance"
        )
        assertTrue(
            com.manavahana.worker.ReminderWorker.isStalePreRenewalReminder(
                staleVehicleInsReminder,
                resolution.effectiveExpiryByVehicleAndCat,
                now
            )
        )

        // Current-cycle renewed reminder is NOT marked stale
        val currentInsReminder = com.manavahana.data.model.Reminder(
            id = 102,
            vehicleId = 1,
            title = "Comprehensive Policy 2026 Expiry",
            description = "Document Comprehensive Policy 2026 is expiring. Renew in time.",
            reminderDate = renewedFutureDate,
            category = "Insurance"
        )
        assertFalse(
            com.manavahana.worker.ReminderWorker.isStalePreRenewalReminder(
                currentInsReminder,
                resolution.effectiveExpiryByVehicleAndCat,
                now
            )
        )

        // Unrenewed PUC reminder is NOT marked stale (still needs alerting)
        val activePucReminder = com.manavahana.data.model.Reminder(
            id = 103,
            vehicleId = 1,
            title = "Creta - PUC Renewal",
            description = "Pollution Certificate is expiring. Visit nearest emission center.",
            reminderDate = oldExpiredDate - dayMs,
            category = "Pollution"
        )
        assertFalse(
            com.manavahana.worker.ReminderWorker.isStalePreRenewalReminder(
                activePucReminder,
                resolution.effectiveExpiryByVehicleAndCat,
                now
            )
        )
    }
}
