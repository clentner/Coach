package com.chrislentner.coach.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MetronomeLogicTest {

    @Test
    fun testCalculateInterval() {
        // Verify default behavior logic
        assertEquals(1000L, Metronome.calculateInterval("3030"))
        assertEquals(1000L, Metronome.calculateInterval(null))
        assertEquals(1000L, Metronome.calculateInterval(""))
        assertEquals(1000L, Metronome.calculateInterval("20X1"))
    }
}
