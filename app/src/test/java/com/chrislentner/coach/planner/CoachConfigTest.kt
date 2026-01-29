package com.chrislentner.coach.planner

import com.chrislentner.coach.planner.model.CoachConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class CoachConfigTest {

    @Test
    fun `real coach yaml parses successfully`() {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val file = File("src/main/assets/coach.yaml")
        val inputStream = if (file.exists()) {
             FileInputStream(file)
        } else {
             FileInputStream("app/src/main/assets/coach.yaml")
        }

        val config = mapper.readValue(inputStream, CoachConfig::class.java)

        assertNotNull(config)
        assertNotNull(config.targets)
        assertNotNull(config.priorities)

        // Verify Library
        assertNotNull(config.library)
        val nordicSki = config.library["nordic_ski"]
        assertNotNull("nordic_ski should exist in library", nordicSki)

        val block = nordicSki!!.blocks.firstOrNull()
        assertNotNull(block)

        // Check defaults are working (sizeMinutes empty)
        assertNotNull(block!!.sizeMinutes)
        assert(block.sizeMinutes.isEmpty())

        val prescription = block.prescription.firstOrNull()
        assertNotNull(prescription)

        // Check distance
        assertEquals("1 mile", prescription!!.distance)

        // Check fatigue
        val kneeFatigue = prescription.fatigueLoads["knee"]
        assertEquals(0.5, kneeFatigue)

        println("Successfully parsed ${config.targets.size} targets, ${config.priorities.size} priority groups, and ${config.library.size} library entries.")
    }
}
