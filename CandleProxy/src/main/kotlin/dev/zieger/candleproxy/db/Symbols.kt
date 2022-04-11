package dev.zieger.candleproxy.db

import dev.zieger.bybitapi.dto.enumerations.ICurrency
import dev.zieger.bybitapi.dto.enumerations.ISymbol
import dev.zieger.candleproxy.db.Currency.Companion.insert
import dev.zieger.candleproxy.db.Db.db
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object Symbols : LongIdTable() {

    val base = reference("base", Currencies)
    val counter = reference("counter", Currencies)
    val pair = varchar("pair", 32)
}

class Symbol(id: EntityID<Long>) : LongEntity(id), ISymbol {
    companion object : LongEntityClass<Symbol>(Symbols) {

        fun dev.zieger.bybitapi.dto.enumerations.Symbol.insert() = transaction(db) {
            find { Symbols.pair eq pair }.firstOrNull() ?: new {
                _base = this@insert.base.insert().id
                _counter = this@insert.counter.insert().id
                pair = this@insert.pair
            }
        }
    }

    private var _base by Symbols.base
    override val base: ICurrency
        get() = Currency(_base)
    private var _counter by Symbols.counter
    override val counter: ICurrency
        get() = Currency(_counter)
    override var pair by Symbols.pair
}