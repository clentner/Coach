package com.chrislentner.coach.planner

import com.chrislentner.coach.planner.model.CoachConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.assertNotNull
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

        println("Successfully parsed ${config.targets.size} targets and ${config.priorities.size} priority groups.")
    }
}
