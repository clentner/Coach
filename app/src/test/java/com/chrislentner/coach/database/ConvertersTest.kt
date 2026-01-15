package com.chrislentner.coach.database

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fromTempo converts Tempo to JSON string`() {
        val tempo = Tempo(up = 1, holdTop = 2, down = 3, holdBottom = 4)
        val jsonStr = converters.fromTempo(tempo)
        assertNotNull(jsonStr)
        val json = JSONObject(jsonStr)
        assertEquals(1, json.getInt("up"))
        assertEquals(2, json.getInt("holdTop"))
        assertEquals(3, json.getInt("down"))
        assertEquals(4, json.getInt("holdBottom"))
    }

    @Test
    fun `fromTempo handles nulls`() {
        val tempo = Tempo(down = 3)
        val jsonStr = converters.fromTempo(tempo)
        val json = JSONObject(jsonStr)
        assertEquals(3, json.getInt("down"))
        assertFalse(json.has("up"))
    }

    @Test
    fun `fromTempo returns null for null input`() {
        assertNull(converters.fromTempo(null))
    }

    @Test
    fun `toTempo converts JSON string to Tempo`() {
        val jsonStr = """{"up":1,"holdTop":2,"down":3,"holdBottom":4}"""
        val tempo = converters.toTempo(jsonStr)
        assertNotNull(tempo)
        assertEquals(1, tempo?.up)
        assertEquals(2, tempo?.holdTop)
        assertEquals(3, tempo?.down)
        assertEquals(4, tempo?.holdBottom)
    }

    @Test
    fun `toTempo handles partial JSON`() {
        val jsonStr = """{"down":3}"""
        val tempo = converters.toTempo(jsonStr)
        assertNotNull(tempo)
        assertEquals(3, tempo?.down)
        assertNull(tempo?.up)
    }

    @Test
    fun `toTempo returns null for invalid JSON`() {
        assertNull(converters.toTempo("{invalid}"))
    }

    @Test
    fun `toTempo returns null for null input`() {
        assertNull(converters.toTempo(null))
    }
}
