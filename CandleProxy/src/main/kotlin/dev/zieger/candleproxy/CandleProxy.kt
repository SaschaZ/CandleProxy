@file:Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_IS_NOT_ENABLED", "OPT_IN_USAGE", "unused")

package dev.zieger.candleproxy

import dev.zieger.bybitapi.IByBitExchange
import dev.zieger.bybitapi.dto.enumerations.Interval
import dev.zieger.bybitapi.dto.enumerations.Symbol
import dev.zieger.bybitapi.dto.rest.IKline
import dev.zieger.bybitapi.dto.rest.Kline
import dev.zieger.candleproxy.db.Candles
import dev.zieger.candleproxy.db.Candles.replace
import dev.zieger.utils.time.ITimeSpan
import dev.zieger.utils.time.ITimeStamp
import dev.zieger.utils.time.TimeUnit
import dev.zieger.utils.time.toTime
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.log4j.BasicConfigurator

class CandleProxy(
    private val exchange: IByBitExchange,
    private val port: Int = 8080
) {

    suspend fun start() {
        BasicConfigurator.configure()

        embeddedServer(Netty, port) {
            install(DefaultHeaders)
            install(Compression)

            routing {
                get("/candles") {
                    val symbol = call.request.queryParameters["symbol"]
                        ?.let { runCatching { Symbol.valueOf(it) }.getOrNull() }
                    val interval = call.request.queryParameters["interval"]
                        ?.let { runCatching { Interval.valueOf(it) }.getOrNull() }
                    val start = call.request.queryParameters["start"]?.toLongOrNull()?.toTime(TimeUnit.SECOND)
                    val end = call.request.queryParameters["end"]?.toLongOrNull()?.toTime(TimeUnit.SECOND)

                    symbol?.also { sym ->
                        interval?.also { int ->
                            start?.also { s ->
                                end?.also { e ->
                                    val candles = candles(sym, int, s..e).toList()
                                    call.respond(Json.encodeToString(KlineContainerSurrogate(candles)))
                                }
                            }
                        }
                    }
                }
            }
        }.start(true)
    }

    private suspend fun candles(
        symbol: Symbol, interval: Interval, range: ClosedRange<ITimeStamp>
    ): Flow<Kline> = cachedCandles(symbol, interval, range) { missing ->
        exchange.candles(symbol, interval, missing).map { it.toKline() }
    }

    private suspend fun cachedCandles(
        symbol: Symbol,
        interval: Interval,
        range: ClosedRange<ITimeStamp>,
        onCacheMiss: suspend (range: ClosedRange<ITimeStamp>) -> Flow<Kline>
    ): Flow<Kline> {
        val normRange = range.normalize(interval.duration)
        val candlesDb = Candles.read(symbol, interval, normRange)
        val missing = candlesDb.missingRanges(interval, range)
        return (missing.flatMap { onCacheMiss(it).toList() }.storeCandles() + candlesDb)
            .sortedBy { it.openTime }.asFlow()
    }

    private fun List<IKline<Kline>>.missingRanges(
        interval: Interval,
        expected: ClosedRange<ITimeStamp>
    ): List<ClosedRange<ITimeStamp>> {
        if (isEmpty()) return listOf(expected)

        var prev: ITimeStamp = expected.start - interval.duration
        val missing = mapNotNull { kline ->
            when {
                kline - prev > interval.duration -> prev..kline
                else -> null
            }.also { prev = kline }
        }
        if (missing.lastOrNull()?.endInclusive?.let { it < expected.endInclusive } == true)
            return missing + (missing.last().endInclusive..expected.endInclusive)
        return missing
    }

    private fun ClosedRange<ITimeStamp>.normalize(duration: ITimeSpan): ClosedRange<ITimeStamp> =
        start.normalize(duration)..endInclusive.normalize(duration)

    private fun List<Kline>.storeCandles(): List<Kline> = replace()
}

@Serializable
data class KlineContainerSurrogate(
    val symbol: Symbol,
    val interval: Interval,
    val klines: List<List<Double>>
) {
    constructor(klines: List<IKline<*>>) : this(
        klines.first().symbol,
        klines.first().interval,
        klines.map { value ->
            listOf(value.timeStamp, value.open, value.high, value.close, value.low, value.volume.toDouble())
        }
    )

    fun klines(): List<Kline> = klines.map { kline ->
        Kline(symbol, interval, kline[0].toLong(), kline[1], kline[2], kline[3], kline[4], kline[5].toLong(), "")
    }
}
