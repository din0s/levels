package me.din0s

import me.din0s.listeners.ActivityListener
import me.din0s.listeners.MessageListener
import me.din0s.sql.Database
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.apache.logging.log4j.LogManager

fun main() {
    val token = System.getenv("BOT_TOKEN")
    JDABuilder.createDefault(token)
        .addEventListeners(Ready())
        .addEventListeners(ActivityListener(), MessageListener())
        .setActivity(Activity.watching("you \uD83D\uDC40"))
        .build()
}

class Ready: ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        LogManager.getLogger().info("Client READY")
        Database.init()
    }
}
