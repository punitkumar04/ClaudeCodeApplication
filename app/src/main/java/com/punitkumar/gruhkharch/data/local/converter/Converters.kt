package com.punitkumar.gruhkharch.data.local.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, Double>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringMap(value: String): Map<String, Double> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyMap()
    }
}
