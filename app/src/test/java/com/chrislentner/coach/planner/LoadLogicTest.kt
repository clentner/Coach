package com.chrislentner.coach.planner

import org.junit.Assert.assertEquals
import org.junit.Test

class LoadLogicTest {

    @Test
    fun `adjustLoad increments by 5 when value is greater than or equal to 50`() {
        assertEquals("55", LoadLogic.adjustLoad("50", true))
        assertEquals("105", LoadLogic.adjustLoad("100", true))
        assertEquals("105 lbs", LoadLogic.adjustLoad("100 lbs", true))
    }

    @Test
    fun `adjustLoad decrements by 5 when value is greater than or equal to 50`() {
        assertEquals("45", LoadLogic.adjustLoad("50", false)) // 50 >= 50, so step is 5. 50-5=45.
        assertEquals("95 lbs", LoadLogic.adjustLoad("100 lbs", false))
    }

    @Test
    fun `adjustLoad increments by 2 point 5 when value is between 15 and 50`() {
        assertEquals("42.5", LoadLogic.adjustLoad("40", true))
        assertEquals("17.5", LoadLogic.adjustLoad("15", true))
        assertEquals("42.5 kgs", LoadLogic.adjustLoad("40 kgs", true))
    }

    @Test
    fun `adjustLoad decrements by 2 point 5 when value is between 15 and 50`() {
        assertEquals("37.5", LoadLogic.adjustLoad("40", false))
        assertEquals("12.5", LoadLogic.adjustLoad("15", false))
    }

    @Test
    fun `adjustLoad increments by 1 when value is less than 15`() {
        assertEquals("11", LoadLogic.adjustLoad("10", true))
        assertEquals("15", LoadLogic.adjustLoad("14", true))
    }

    @Test
    fun `adjustLoad decrements by 1 when value is less than 15`() {
        assertEquals("9", LoadLogic.adjustLoad("10", false))
        assertEquals("13", LoadLogic.adjustLoad("14", false))
    }

    @Test
    fun `adjustLoad handles decimals correctly`() {
        // 42.5 is >= 15 and < 50. Step 2.5. 42.5 + 2.5 = 45.
        assertEquals("45", LoadLogic.adjustLoad("42.5", true))

        // 12.5 is < 15. Step 1. 12.5 + 1 = 13.5
        assertEquals("13.5", LoadLogic.adjustLoad("12.5", true))
    }

    @Test
    fun `adjustLoad handles empty or invalid text`() {
        assertEquals("", LoadLogic.adjustLoad("", true))
        assertEquals("abc", LoadLogic.adjustLoad("abc", true))
    }
}
