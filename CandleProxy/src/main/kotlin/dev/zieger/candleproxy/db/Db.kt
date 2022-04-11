package dev.zieger.candleproxy.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object Db {

    val db by lazy {
        val directory = File(".cache")
        directory.mkdirs()
        Database.connect(
            "jdbc:sqlite:${
                File(
                    directory.absolutePath + File.separatorChar
                            + "CandleProxy.db"
                ).absolutePath
            }", "org.sqlite.JDBC"
        ).also {
            TransactionManager.defaultDatabase = it

            transaction {
//                SchemaUtils.drop(Candles, Trades, Currencies, Symbols)
                SchemaUtils.create(Candles, Trades, Currencies, Symbols)
            }
        }
    }
}