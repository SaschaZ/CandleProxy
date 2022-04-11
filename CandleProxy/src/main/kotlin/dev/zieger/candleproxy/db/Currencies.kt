package dev.zieger.candleproxy.db

import dev.zieger.bybitapi.dto.enumerations.ICurrency
import dev.zieger.candleproxy.db.Db.db
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object Currencies : LongIdTable() {

    val fullName = varchar("name", 128)
    val abbreviation = varchar("abbreviation", 32)
    val isFiat = bool("isFiat")
}

class Currency(id: EntityID<Long>) : LongEntity(id), ICurrency {
    companion object : LongEntityClass<Currency>(Currencies) {

        fun dev.zieger.bybitapi.dto.enumerations.ICurrency.insert() = transaction(db) {
            find { Currencies.abbreviation eq abbreviation }.firstOrNull() ?: new {
                fullName = this@insert.fullName
                abbreviation = this@insert.abbreviation
                isFiat = this@insert.isFiat
            }
        }
    }

    override var fullName by Currencies.fullName
    override var abbreviation by Currencies.abbreviation
    override var isFiat by Currencies.isFiat
}