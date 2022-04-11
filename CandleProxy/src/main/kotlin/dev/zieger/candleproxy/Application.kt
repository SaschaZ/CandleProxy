package dev.zieger.candleproxy

import dev.zieger.bybitapi.ByBitExchange
import kotlinx.coroutines.runBlocking

object Application {

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val port = args.indexOf("-p").takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }?.toIntOrNull() ?: 8080
        CandleProxy(ByBitExchange(this, storeUncompressed = true), port).start()
    }
}