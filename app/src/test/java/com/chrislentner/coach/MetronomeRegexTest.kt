package com.chrislentner.coach

import org.junit.Test
import org.junit.Assert.*

class MetronomeRegexTest {

    private val regex = Regex("\\ds")

    @Test
    fun testRegexMatching() {
        assertTrue("Should match '85 lbs @ 6s/rep'", "85 lbs @ 6s/rep".contains(regex))
        assertTrue("Should match '3s'", "3s".contains(regex))
        assertTrue("Should match '10s'", "10s".contains(regex))

        assertFalse("Should not match '85 lbs'", "85 lbs".contains(regex))
        assertFalse("Should not match 'Reps only'", "Reps only".contains(regex))
        assertFalse("Should not match 'Slow'", "Slow".contains(regex))
    }
}
