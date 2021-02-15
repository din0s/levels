package me.din0s.sql

import me.din0s.sql.tables.Levels
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    private val logger = LogManager.getLogger()

    fun init() {
        logger.info("Initializing DB module")
        connect()
        addDefaults()
    }

    private fun connect() {
        logger.info("Connecting to database")
        val ip = System.getenv("DB_IP")
        val port = System.getenv("DB_PORT")
        val db = System.getenv("DB_NAME")
        val user = System.getenv("DB_USERNAME")
        val pass = System.getenv("DB_PASSWORD")

        Database.connect(
            url = "jdbc:postgresql://${ip}:${port}/${db}",
            driver = "org.postgresql.Driver",
            user = user,
            password = pass
        )
    }

    private fun addDefaults() {
        logger.info("Creating default tables")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Levels)
        }
    }
}
