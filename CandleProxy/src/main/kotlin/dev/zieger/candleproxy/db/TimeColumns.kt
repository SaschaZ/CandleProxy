package dev.zieger.candleproxy.db

import dev.zieger.utils.time.*
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Table
import java.util.*


open class TimeStampColumnType(
    private val zone: TimeZone? = null,
    private val colType: LongColumnType = LongColumnType()
) : IColumnType by colType {
    override fun valueFromDB(value: Any): Any = when (value) {
        is ITimeStamp -> value
        is String -> value.parse(zone)
        else -> colType.valueFromDB(value).toTime(zone = zone)
    }

    override fun notNullValueToDB(value: Any): Any = colType.notNullValueToDB(valueUnwrap(value))
    override fun nonNullValueToString(value: Any): String = colType.nonNullValueToString(valueUnwrap(value))
    override fun valueToDB(value: Any?): Any? = value?.let { notNullValueToDB(it) }
    override fun valueToString(value: Any?): String = value?.let { nonNullValueToString(it) } ?: "NULL"

    private fun valueUnwrap(value: Any) = (value as ITimeStamp).millisLong
}

fun Table.timestamp(name: String, zone: TimeZone? = null) =
    registerColumn<ITimeStamp>(name, TimeStampColumnType(zone))

open class TimeSpanColumnType(
    private val colType: LongColumnType = LongColumnType()
) : IColumnType by colType {
    override fun valueFromDB(value: Any): Any = when (value) {
        is ITimeSpan -> value
        else -> colType.valueFromDB(value).millis
    }

    override fun notNullValueToDB(value: Any): Any = colType.notNullValueToDB(valueUnwrap(value))
    override fun nonNullValueToString(value: Any): String = colType.nonNullValueToString(valueUnwrap(value))
    override fun valueToDB(value: Any?): Any? = value?.let { notNullValueToDB(it) }
    override fun valueToString(value: Any?): String = value?.let { nonNullValueToString(it) } ?: "NULL"
    private fun valueUnwrap(value: Any) = (value as ITimeSpan).millisLong
}

fun Table.timespan(name: String) = registerColumn<ITimeSpan>(name, TimeSpanColumnType())