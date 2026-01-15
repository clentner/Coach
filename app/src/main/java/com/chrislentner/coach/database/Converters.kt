package com.chrislentner.coach.database

import androidx.room.TypeConverter
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromTempo(tempo: Tempo?): String? {
        if (tempo == null) return null
        val json = JSONObject()
        tempo.up?.let { json.put("up", it) }
        tempo.holdTop?.let { json.put("holdTop", it) }
        tempo.down?.let { json.put("down", it) }
        tempo.holdBottom?.let { json.put("holdBottom", it) }

        if (json.length() == 0) return null

        return json.toString()
    }

    @TypeConverter
    fun toTempo(data: String?): Tempo? {
        if (data == null) return null
        try {
            val json = JSONObject(data)
            val up = if (json.has("up")) json.getInt("up") else null
            val holdTop = if (json.has("holdTop")) json.getInt("holdTop") else null
            val down = if (json.has("down")) json.getInt("down") else null
            val holdBottom = if (json.has("holdBottom")) json.getInt("holdBottom") else null

            if (up == null && holdTop == null && down == null && holdBottom == null) {
                return null
            }

            return Tempo(up, holdTop, down, holdBottom)
        } catch (e: Exception) {
            return null
        }
    }
}
