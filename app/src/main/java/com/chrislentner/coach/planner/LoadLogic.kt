package com.chrislentner.coach.planner

object LoadLogic {
    private val NUMBER_REGEX = Regex("(\\d+(\\.\\d+)?)")

    fun adjustLoad(text: String, increment: Boolean): String {
        val match = NUMBER_REGEX.find(text) ?: return text

        val valueStr = match.value
        val value = valueStr.toDoubleOrNull() ?: return text

        if (!increment && value <= 0.0) {
            return text
        }

        // Determine step size
        val step = when {
            value >= 50 -> 5.0
            value >= 15 -> 2.5
            else -> 1.0
        }

        var newValueRaw = if (increment) value + step else value - step
        if (newValueRaw < 0.0) {
            newValueRaw = 0.0
        }

        // If the result is a whole number, print as integer (e.g. 105).
        // If it has a decimal part (42.5), print as decimal.
        val newValueStr = if (newValueRaw % 1.0 == 0.0) {
            newValueRaw.toInt().toString()
        } else {
            newValueRaw.toString()
        }

        return text.replaceRange(match.range, newValueStr)
    }
}
