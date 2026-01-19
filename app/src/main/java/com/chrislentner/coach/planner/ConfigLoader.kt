package com.chrislentner.coach.planner

import android.content.Context
import com.chrislentner.coach.planner.model.CoachConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.InputStream

object ConfigLoader {
    fun load(context: Context): CoachConfig {
        return loadConfig(context.assets.open("coach.yaml"))
    }

    fun loadConfig(inputStream: InputStream): CoachConfig {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(inputStream, CoachConfig::class.java)
    }
}
