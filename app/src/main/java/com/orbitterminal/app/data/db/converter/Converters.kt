package com.orbitterminal.app.data.db.converter

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString(",")

    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.split(",")?.filter { it.isNotEmpty() }

    @TypeConverter
    fun fromLongNullable(value: Long?): Long = value ?: -1L

    @TypeConverter
    fun toLongNullable(value: Long): Long? = if (value == -1L) null else value
}
