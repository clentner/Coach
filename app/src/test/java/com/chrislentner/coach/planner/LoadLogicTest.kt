package com.chrislentner.coach.planner

import org.junit.Assert.assertEquals
import org.junit.Test

class LoadLogicTest {

    // --- Increment Tests ---

    @Test
    fun `adjustLoad increments by 5 when value is greater than or equal to 50`() {
        assertEquals("55", LoadLogic.adjustLoad("50", true))
        assertEquals("105", LoadLogic.adjustLoad("100", true))
        assertEquals("105 lbs", LoadLogic.adjustLoad("100 lbs", true))
    }

    @Test
    fun `adjustLoad increments by 2 point 5 when value is between 15 and 50`() {
        assertEquals("42.5", LoadLogic.adjustLoad("40", true))
        assertEquals("17.5", LoadLogic.adjustLoad("15", true)) // Boundary: 15 >= 15 -> Step 2.5
        assertEquals("42.5 kgs", LoadLogic.adjustLoad("40 kgs", true))
    }

    @Test
    fun `adjustLoad increments by 1 when value is less than 15`() {
        assertEquals("11", LoadLogic.adjustLoad("10", true))
        assertEquals("15", LoadLogic.adjustLoad("14", true))
    }

    // --- Decrement Tests (Asymmetrical) ---

    @Test
    fun `adjustLoad decrements by 5 ONLY when value is STRICTLY greater than 50`() {
        assertEquals("95", LoadLogic.adjustLoad("100", false)) // 100 > 50 -> Step 5
        // 51 is > 50, so step is 5.
        // 51 is not a multiple of 5. Grid: 50, 55.
        // Decrement snaps to previous grid point: 50.
        assertEquals("50", LoadLogic.adjustLoad("51", false))
    }

    @Test
    fun `adjustLoad decrements by 2 point 5 when value is 50`() {
        assertEquals("47.5", LoadLogic.adjustLoad("50", false)) // 50 is NOT > 50 -> Step 2.5
    }

    @Test
    fun `adjustLoad decrements by 2 point 5 when value is STRICTLY greater than 15`() {
        assertEquals("37.5", LoadLogic.adjustLoad("40", false))

        // 16 > 15 -> Step 2.5.
        // 16 is not a multiple of 2.5. Grid: 15.0, 17.5.
        // Decrement snaps to previous grid point: 15.
        assertEquals("15", LoadLogic.adjustLoad("16", false))
    }

    @Test
    fun `adjustLoad decrements by 1 when value is 15`() {
        assertEquals("14", LoadLogic.adjustLoad("15", false)) // 15 is NOT > 15 -> Step 1
    }

    @Test
    fun `adjustLoad decrements by 1 when value is less than 15`() {
        assertEquals("9", LoadLogic.adjustLoad("10", false))
        assertEquals("13", LoadLogic.adjustLoad("14", false))
    }

    // --- Snap to Grid Tests ---

    @Test
    fun `adjustLoad snaps to integer grid when step is 1`() {
        // Value 12.5, Step 1.
        // Increment -> Next integer > 12.5 -> 13
        assertEquals("13", LoadLogic.adjustLoad("12.5", true))

        // Decrement -> Prev integer < 12.5 -> 12
        assertEquals("12", LoadLogic.adjustLoad("12.5", false))
    }

    @Test
    fun `adjustLoad snaps to 2 point 5 grid when step is 2 point 5`() {
        // Value 41.2, Step 2.5. Grid: 40.0, 42.5
        // Increment -> Next grid point > 41.2 -> 42.5
        assertEquals("42.5", LoadLogic.adjustLoad("41.2", true))

        // Decrement -> Prev grid point < 41.2 -> 40
        assertEquals("40", LoadLogic.adjustLoad("41.2", false))
    }

    @Test
    fun `adjustLoad snaps correctly near boundaries`() {
        // Value 16, Step 2.5 (because 16 >= 15 for Inc, 16 > 15 for Dec)
        // Grid: ... 15.0, 17.5 ...

        // Increment: 16 -> 17.5
        assertEquals("17.5", LoadLogic.adjustLoad("16", true))

        // Decrement: 16 -> 15.0
        assertEquals("15", LoadLogic.adjustLoad("16", false))

        // Value 48, Step 2.5
        // Increment: 48 -> 50
        assertEquals("50", LoadLogic.adjustLoad("48", true))

        // Decrement: 48 -> 47.5
        assertEquals("47.5", LoadLogic.adjustLoad("48", false))
    }

    @Test
    fun `adjustLoad handles empty or invalid text`() {
        assertEquals("", LoadLogic.adjustLoad("", true))
        assertEquals("abc", LoadLogic.adjustLoad("abc", true))
    }

    @Test
    fun `adjustLoad does not decrement below 0`() {
        assertEquals("0", LoadLogic.adjustLoad("0", false))
        assertEquals("0 lbs", LoadLogic.adjustLoad("0 lbs", false))

        // Test clamping (0.5 - 1.0 = -0.5 -> 0.0)
        assertEquals("0", LoadLogic.adjustLoad("0.5", false))
    }
}
