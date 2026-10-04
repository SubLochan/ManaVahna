package com.manavahana

import com.manavahana.data.model.FuelLog
import com.manavahana.widget.FuelWidgetUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FuelWidgetUnitTest {

    @Test
    fun testFuelLitersAndCostCalculations() {
        val totalAmount = 1000.0
        val pricePerLiter = 102.50
        val expectedLiters = totalAmount / pricePerLiter

        assertEquals(9.756, expectedLiters, 0.01)

        val litersFilled = 5.0
        val expectedTotal = litersFilled * pricePerLiter
        assertEquals(512.50, expectedTotal, 0.01)
    }

    @Test
    fun testMonthlyFuelExpenseAggregation() {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Month start
        val monthStartCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = monthStartCal.timeInMillis

        // Previous month date
        val prevMonthCal = (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        val prevMonthDate = prevMonthCal.timeInMillis

        val logs = listOf(
            FuelLog(id = 1, vehicleId = 1, fuelDate = now, litersFilled = 4.88, pricePerLiter = 102.50, totalAmount = 500.0, odometerReading = 14200.0, fuelStationName = "Indian Oil"),
            FuelLog(id = 2, vehicleId = 1, fuelDate = now - 86400000L, litersFilled = 9.76, pricePerLiter = 102.50, totalAmount = 1000.0, odometerReading = 14450.0, fuelStationName = "HP"),
            FuelLog(id = 3, vehicleId = 1, fuelDate = prevMonthDate, litersFilled = 10.0, pricePerLiter = 100.0, totalAmount = 1000.0, odometerReading = 13800.0, fuelStationName = "Shell")
        )

        val currentMonthLogs = logs.filter { it.fuelDate >= monthStart }
        val currentMonthTotal = currentMonthLogs.sumOf { it.totalAmount }

        assertEquals(2, currentMonthLogs.size)
        assertEquals(1500.0, currentMonthTotal, 0.01)
    }

    @Test
    fun testPresetAmountConstants() {
        assertEquals("extra_preset_amount", FuelWidgetUpdater.EXTRA_PRESET_AMOUNT)
        assertEquals("extra_target_vehicle_id", FuelWidgetUpdater.EXTRA_TARGET_VEHICLE_ID)
        assertEquals("com.manavahana.action.REFRESH_WIDGET", FuelWidgetUpdater.ACTION_REFRESH_WIDGET)
    }

    @Test
    fun testOdometerIncrementLogic() {
        val previousOdo = 14500.0
        val newOdo = 14750.0
        assertTrue(newOdo > previousOdo)
        assertEquals(250.0, newOdo - previousOdo, 0.01)
    }
}
