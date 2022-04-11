package dev.zieger.candleproxy.db

import dev.zieger.bybitapi.dto.enumerations.Interval
import dev.zieger.bybitapi.dto.rest.IKline
import dev.zieger.bybitapi.dto.rest.Kline
import dev.zieger.candleproxy.db.Db.db
import dev.zieger.utils.time.ITimeStamp
import dev.zieger.utils.time.UTC
import dev.zieger.utils.time.toTime
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchReplace
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import dev.zieger.bybitapi.dto.enumerations.Symbol as SymbolEnum


object Candles : LongIdTable() {

    val symbol = reference("symbol", Symbols.pair)
    val interval = enumeration("interval", Interval::class)
    val timeStamp = timestamp("timeStamp")
    val open = double("open")
    val high = double("high")
    val close = double("close")
    val low = double("low")
    val volume = long("volume")
    val isUpdate = bool("isUpdate")
    val isComplete = bool("isComplete")

    fun List<Kline>.replace(): List<Kline> = transaction(db) {
        batchReplace(this@replace) {
            this[symbol] = it.symbol.pair
            this[interval] = it.interval
            this[timeStamp] = (it.openTime * 1000).toTime()
            this[open] = it.open
            this[high] = it.high
            this[close] = it.close
            this[low] = it.low
            this[volume] = it.volume
            this[isUpdate] = it.isUpdate
            this[isComplete] = it.isComplete
        }
        this@replace
    }

    fun read(symbol: SymbolEnum, interval: Interval, range: ClosedRange<ITimeStamp>) = transaction(db) {
        Candle.find {
            Candles.symbol eq symbol.pair and (
                    timeStamp greaterEq range.start) and (
                    timeStamp lessEq range.endInclusive) and (
                    Candles.interval eq interval)
        }.sortedBy { timeStamp }
            .map { it.toKline() }
    }
}

class Candle(id: EntityID<Long>) : LongEntity(id), IKline<Kline> {
    companion object : LongEntityClass<Candle>(Candles) {

        fun IKline<*>.insert(): IKline<Kline> = transaction(db) {
            find {
                Candles.interval eq interval and
                        (Candles.timeStamp eq timeStamp.toTime()) and
                        (Candles.symbol eq symbol.pair)
            }.firstOrNull() ?: new {
                _symbol = this@insert.symbol.pair
                interval = this@insert.interval
                _timeStamp = this@insert.timeStamp.toTime()
                open = this@insert.open
                high = this@insert.high
                close = this@insert.close
                low = this@insert.low
                volume = this@insert.volume
                isUpdate = this@insert.isUpdate
                isComplete = this@insert.isComplete
            }
        }
    }

    internal var _symbol by Candles.symbol
    override val symbol: SymbolEnum
        get() = SymbolEnum.values().first { it.pair == _symbol }
    override var interval by Candles.interval
    internal var _timeStamp by Candles.timeStamp
    override val timeStamp
        get() = _timeStamp.timeStamp
    override val openTime: Long
        get() = _timeStamp.timeStamp.toLong() / 1000
    override var open by Candles.open
    override var high by Candles.high
    override var close by Candles.close
    override var low by Candles.low
    override var volume by Candles.volume
    override var isComplete by Candles.isComplete
    override var isUpdate by Candles.isUpdate

    override fun compareTo(other: ITimeStamp): Int = (other.millisLong - minutesLong).toInt()

    override val zone: TimeZone = UTC

    override fun copy(
        symbol: SymbolEnum,
        interval: Interval,
        openTime: Long,
        open: Double,
        high: Double,
        low: Double,
        close: Double,
        volume: Long,
        isUpdate: Boolean
    ): Kline = transaction {
        Kline(symbol, interval, openTime, open, high, low, close, volume, "")
    }
}