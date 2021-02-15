package me.din0s

import me.din0s.commands.Command
import me.din0s.commands.ShutdownCommand
import me.din0s.commands.XpCommand
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class MessageListener: ListenerAdapter() {
    private val prefix = "?"
    private val devId = System.getenv("BOT_DEV_ID")

    private val cmdMap = mutableMapOf<String, Command>()

    init {
        arrayOf(XpCommand(), ShutdownCommand())
            .forEach {
                cmdMap[it.name] = it
                it.alias.forEach { alias ->
                    cmdMap[alias] = it
                }
            }
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.isWebhookMessage || event.author.isBot) {
            return
        }

        val msg = event.message.contentDisplay
        if (!msg.startsWith(prefix)) {
            return
        }

        val args = msg.split("\\s+".toRegex())
        val lbl = args[0].substring(prefix.length).toLowerCase()
        cmdMap[lbl]?.let {
            val min = it.minArgs
            val max = it.maxArgs
            val argCount = args.size - 1
            if (argCount < min || argCount > max) {
                event.channel.sendMessage("${prefix}${lbl} ${it.usage}").queue()
                return
            }
            if (it.devOnly && event.author.id != devId) {
                return
            }

            it.invoke(event, args.drop(1))
        }
    }
}
