package dev.zieger.candleproxy.db

import dev.zieger.bybitapi.dto.enumerations.Side
import dev.zieger.bybitapi.dto.rest.ITrade
import dev.zieger.candleproxy.db.Db.db
import dev.zieger.candleproxy.db.Symbol.Companion.insert
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.batchReplace
import org.jetbrains.exposed.sql.transactions.transaction
import dev.zieger.bybitapi.dto.enumerations.Symbol as SymbolByBit

object Trades : LongIdTable() {

    val symbol = reference("symbol", Symbols.id)
    val timeStamp = timestamp("timeStamp")
    val price = double("price")
    val volume = long("volume")
    val side = enumeration("side", Side::class)
    val tradeTime = varchar("tradeTime", 128)
    val dataId = long("dataId")
    val tradeId = varchar("tradeId", 128)

    fun List<ITrade>.replace() = transaction(db) {
        batchReplace(this@replace) {
            this[symbol] = it.symbol.insert().id
            this[timeStamp] = it.time
            this[price] = it.price
            this[volume] = it.qty
            this[side] = it.side
            this[tradeTime] = it.tradeTime
            this[dataId] = it.dataId
            this[tradeId] = it.tradeId
        }.map { Trade(it[this@Trades.id]) }
    }
}

class Trade(id: EntityID<Long>) : LongEntity(id), ITrade {
    companion object : LongEntityClass<Trade>(Trades) {

        fun ITrade.insert(): ITrade = transaction(db) {
            new {
                _symbol = this@insert.symbol.insert().id
                price = this@insert.price
                qty = this@insert.qty
                time = this@insert.time
                side = this@insert.side
                tradeTime = this@insert.tradeTime
                dataId = this@insert.dataId
                tradeId = this@insert.tradeId
            }
        }
    }

    private var _symbol by Trades.symbol
    override val symbol: SymbolByBit
        get() = SymbolByBit.values().first { it.name == Symbol(_symbol).pair }
    override var price by Trades.price
    override var qty by Trades.volume
    override var time by Trades.timeStamp
    override var side by Trades.side
    override var tradeTime by Trades.tradeTime
    override var dataId by Trades.dataId
    override var tradeId by Trades.tradeId


}