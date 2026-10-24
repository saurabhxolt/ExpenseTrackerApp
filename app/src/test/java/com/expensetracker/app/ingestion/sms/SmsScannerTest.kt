package com.expensetracker.app.ingestion.sms

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SmsScannerTest {

    @Test
    fun testInitialScanTimestampCalculatesThreeMonthsAgoFirstDay() {
        // Given current date is October 25, 2026
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 25)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 15)
            set(Calendar.MILLISECOND, 500)
        }

        // When calculating initial scan timestamp (3 months back)
        val initialTimestamp = SmsScanner.getInitialScanTimestamp(monthsBack = 3, baseCalendar = calendar)

        // Then expected result is July 1, 2026 00:00:00.000 UTC
        val resultCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = initialTimestamp
        }

        assertEquals("Year should be 2026", 2026, resultCal.get(Calendar.YEAR))
        assertEquals("Month should be July (Calendar.JULY = 6)", Calendar.JULY, resultCal.get(Calendar.MONTH))
        assertEquals("Day of month should be 1st", 1, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Hour should be 0", 0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute should be 0", 0, resultCal.get(Calendar.MINUTE))
        assertEquals("Second should be 0", 0, resultCal.get(Calendar.SECOND))
        assertEquals("Millisecond should be 0", 0, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun testInitialScanTimestampCrossesYearBoundaryCorrectly() {
        // Given current date is January 15, 2026
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 10)
        }

        // When calculating initial scan timestamp (3 months back)
        val initialTimestamp = SmsScanner.getInitialScanTimestamp(monthsBack = 3, baseCalendar = calendar)

        // Then expected result is October 1, 2025 00:00:00.000 UTC
        val resultCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = initialTimestamp
        }

        assertEquals("Year should be 2025", 2025, resultCal.get(Calendar.YEAR))
        assertEquals("Month should be October (Calendar.OCTOBER = 9)", Calendar.OCTOBER, resultCal.get(Calendar.MONTH))
        assertEquals("Day of month should be 1st", 1, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Hour should be 0", 0, resultCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testInitialScanTimestampHandlesMonthEndDayClamping() {
        // Given current date is March 31, 2026
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
        }

        // When calculating initial scan timestamp (3 months back)
        val initialTimestamp = SmsScanner.getInitialScanTimestamp(monthsBack = 3, baseCalendar = calendar)

        // Then expected result is December 1, 2025 00:00:00.000 UTC
        val resultCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = initialTimestamp
        }

        assertEquals("Year should be 2025", 2025, resultCal.get(Calendar.YEAR))
        assertEquals("Month should be December (Calendar.DECEMBER = 11)", Calendar.DECEMBER, resultCal.get(Calendar.MONTH))
        assertEquals("Day of month should be 1st", 1, resultCal.get(Calendar.DAY_OF_MONTH))
    }
}
