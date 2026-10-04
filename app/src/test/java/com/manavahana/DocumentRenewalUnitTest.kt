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
}
