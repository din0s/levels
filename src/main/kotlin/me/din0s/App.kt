package me.din0s

import me.din0s.activity.ActivityListener
import me.din0s.activity.ActivityManager
import me.din0s.sql.Database
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy

fun main() {
    val token = System.getenv("BOT_TOKEN")
    JDABuilder.createDefault(token)
        .addEventListeners(Ready(), Shutdown())
        .addEventListeners(ActivityListener(), MessageListener())
        .enableIntents(GatewayIntent.GUILD_MEMBERS)
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .setActivity(Activity.watching("you \uD83D\uDC40"))
        .build()
}

class Ready: ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        Database.init()
    }
}

class Shutdown: ListenerAdapter() {
    override fun onShutdown(event: ShutdownEvent) {
        ActivityManager.endAll()
    }
}
