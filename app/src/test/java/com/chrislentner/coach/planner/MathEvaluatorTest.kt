package com.chrislentner.coach.planner

import org.junit.Assert.assertEquals
import org.junit.Test

class MathEvaluatorTest {

    @Test
    fun `test simple arithmetic`() {
        assertEquals(5.0, MathEvaluator.evaluate("2 + 3", emptyMap()), 0.001)
        assertEquals(1.0, MathEvaluator.evaluate("3 - 2", emptyMap()), 0.001)
        assertEquals(6.0, MathEvaluator.evaluate("2 * 3", emptyMap()), 0.001)
        assertEquals(2.0, MathEvaluator.evaluate("6 / 3", emptyMap()), 0.001)
    }

    @Test
    fun `test precedence`() {
        assertEquals(7.0, MathEvaluator.evaluate("1 + 2 * 3", emptyMap()), 0.001)
        assertEquals(9.0, MathEvaluator.evaluate("(1 + 2) * 3", emptyMap()), 0.001)
    }

    @Test
    fun `test variables`() {
        val vars = mapOf("x" to 10.0, "y" to 2.0)
        assertEquals(12.0, MathEvaluator.evaluate("x + y", vars), 0.001)
        assertEquals(20.0, MathEvaluator.evaluate("x * 2", vars), 0.001)
    }

    @Test
    fun `test complex formula`() {
        val vars = mapOf("performed_minutes" to 30.0)
        // "$performed_minutes * 0.5 / 20"
        assertEquals(0.75, MathEvaluator.evaluate("performed_minutes * 0.5 / 20", vars), 0.001)
    }

    @Test
    fun `test decimals`() {
        assertEquals(1.5, MathEvaluator.evaluate("3 / 2.0", emptyMap()), 0.001)
        assertEquals(1.5, MathEvaluator.evaluate("1.25 + 0.25", emptyMap()), 0.001)
    }
}
