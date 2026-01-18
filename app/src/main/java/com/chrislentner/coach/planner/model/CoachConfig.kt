package com.chrislentner.coach.planner.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

data class CoachConfig(
    val version: Int,
    val targets: List<Target>,
    @JsonProperty("fatigue_constraints")
    val fatigueConstraints: Map<String, List<FatigueConstraint>>,
    @JsonProperty("priority_order")
    val priorityOrder: List<String>,
    val priorities: Map<String, PriorityGroup>,
    val selection: SelectionStrategy
)

data class Target(
    val id: String,
    @JsonProperty("window_days")
    val windowDays: Int,
    val type: String, // "sets" or "minutes"
    val goal: Int
)

data class FatigueConstraint(
    val kind: String,
    @JsonProperty("window_hours")
    val windowHours: Int,
    val threshold: Double,
    @JsonProperty("applies_to_blocks_with_tag")
    val appliesToBlocksWithTag: List<String>,
    val reason: String
)

data class PriorityGroup(
    val blocks: List<Block>
)

data class Block(
    @JsonProperty("block_name")
    val blockName: String,
    @JsonProperty("size_minutes")
    @JsonDeserialize(using = SizeMinutesDeserializer::class)
    val sizeMinutes: List<Int>, // Always normalized to a list
    val location: String,
    val tags: List<String> = emptyList(),
    @JsonProperty("contributes_to")
    val contributesTo: List<Contribution>,
    val prescription: List<Prescription>,
    val progression: Progression? = null
)

data class Contribution(
    val target: String
)

data class Prescription(
    val exercise: String,
    val sets: Int? = null,
    val reps: Int? = null,
    val tempo: String? = null,
    val seconds: Int? = null,
    @JsonProperty("per_side")
    val perSide: Boolean = false,
    val target: String? = null, // e.g. "Z2"
    @JsonProperty("interval_protocol")
    val intervalProtocol: String? = null,
    @JsonProperty("seconds_per_rep")
    val secondsPerRep: Int? = null,
    val rpe: Int? = null,
    @JsonProperty("fatigue_loads")
    val fatigueLoads: Map<String, Any> = emptyMap() // Can be Double or String
)

data class Progression(
    val type: String,
    @JsonProperty("every_n_sessions")
    val everyNSessions: Int? = null,
    val parameters: Map<String, Any> = emptyMap()
)

data class SelectionStrategy(
    val strategy: String
)

class SizeMinutesDeserializer : JsonDeserializer<List<Int>>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<Int> {
        if (p.currentToken == JsonToken.VALUE_NUMBER_INT) {
            return listOf(p.intValue)
        }
        if (p.currentToken == JsonToken.START_ARRAY) {
             val list = mutableListOf<Int>()
             while (p.nextToken() != JsonToken.END_ARRAY) {
                 list.add(p.intValue)
             }
             return list
        }
        throw RuntimeException("Unexpected token for sizeMinutes: ${p.currentToken}")
    }
}
