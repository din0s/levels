package me.din0s.commands

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class ShutdownCommand: Command(
    name  = "shutdown",
    devOnly = true
) {
    override fun invoke(event: GuildMessageReceivedEvent, args: List<String>) {
        event.message.addReaction(	"\u2705").queue {
            event.jda.shutdown()
        }
    }
}
