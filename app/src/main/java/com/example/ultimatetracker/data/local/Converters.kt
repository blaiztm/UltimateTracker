package com.example.ultimatetracker.data.local

import androidx.room.TypeConverter
import com.example.ultimatetracker.data.model.WatchCategory

class Converters {
    @TypeConverter fun fromCategory(value: WatchCategory) = value.name
    @TypeConverter fun toCategory(value: String) = WatchCategory.valueOf(value)
    @TypeConverter fun fromStringList(value: List<String>) = value.joinToString(SEPARATOR)
    @TypeConverter fun toStringList(value: String) = value.split(SEPARATOR).filter(String::isNotBlank)

    private companion object { const val SEPARATOR = "\u001F" }
}
