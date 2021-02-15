package me.din0s.commands

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

abstract class Command(
    val name: String,
    val usage: String = "",
    val alias: Array<String> = arrayOf(),
    val minArgs: Int = 0,
    val maxArgs: Int = Int.MAX_VALUE,
    val devOnly: Boolean = false
) {
    abstract fun invoke(event: GuildMessageReceivedEvent, args: List<String>)
}
