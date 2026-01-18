package com.chrislentner.coach.planner

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

object LoadLogic {
    private val NUMBER_REGEX = Regex("(\\d+(\\.\\d+)?)")

    fun hasNumericComponent(text: String): Boolean {
        return NUMBER_REGEX.containsMatchIn(text)
    }

    fun adjustLoad(text: String, increment: Boolean): String {
        val match = NUMBER_REGEX.find(text) ?: return text

        val valueStr = match.value
        val value = valueStr.toDoubleOrNull() ?: return text

        if (!increment && value <= 0) {
            return text
        }

        // Determine step size
        // Asymmetrical boundaries:
        // Increment: >= 50 (5), >= 15 (2.5), else (1)
        // Decrement: > 50 (5), > 15 (2.5), else (1)
        val step = if (increment) {
            when {
                value >= 50 -> 5.0
                value >= 15 -> 2.5
                else -> 1.0
            }
        } else {
            when {
                value > 50 -> 5.0
                value > 15 -> 2.5
                else -> 1.0
            }
        }

        val quotient = value / step
        // Check if value is effectively a multiple of step
        // We use a small epsilon for floating point comparison
        val isAligned = abs(quotient - round(quotient)) < 1e-6

        val newValueRaw = if (isAligned) {
            if (increment) value + step else value - step
        } else {
            // Snap to grid
            if (increment) {
                // Next grid point (ceil)
                ceil(quotient) * step
            } else {
                // Previous grid point (floor)
                floor(quotient) * step
            }
        }

        // Clamp to 0
        val clampedValue = max(0.0, newValueRaw)

        // Clean up floating point noise (e.g. 17 * 2.5 = 42.5 is exact, but good practice)
        val cleanedValue = round(clampedValue * 100) / 100.0

        // If the result is a whole number, print as integer (e.g. 105).
        // If it has a decimal part (42.5), print as decimal.
        val newValueStr = if (cleanedValue % 1.0 == 0.0) {
            cleanedValue.toInt().toString()
        } else {
            cleanedValue.toString()
        }

        return text.replaceRange(match.range, newValueStr)
    }
}
