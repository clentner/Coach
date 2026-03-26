package com.chrislentner.coach.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.lang.reflect.Modifier
import java.util.Locale

object HealthConnectLabels {

    private val exerciseTypeNames: Map<Int, String> by lazy {
        ExerciseSessionRecord::class.java.fields
            .asSequence()
            .filter { field ->
                field.type == Int::class.javaPrimitiveType &&
                    Modifier.isStatic(field.modifiers) &&
                    Modifier.isPublic(field.modifiers) &&
                    field.name.startsWith("EXERCISE_TYPE_")
            }
            .mapNotNull { field ->
                runCatching {
                    val value = field.getInt(null)
                    value to prettifyConstantName(field.name.removePrefix("EXERCISE_TYPE_"))
                }.getOrNull()
            }
            .toMap()
    }

    fun exerciseTypeLabel(type: Int): String {
        return exerciseTypeNames[type] ?: "Unknown"
    }

    fun exerciseTypeLabelWithCode(type: Int): String {
        return "${exerciseTypeLabel(type)} ($type)"
    }

    private fun prettifyConstantName(name: String): String {
        return name
            .lowercase(Locale.US)
            .split('_')
            .joinToString(" ") { word ->
                word.replaceFirstChar { c -> c.titlecase(Locale.US) }
            }
    }
}
