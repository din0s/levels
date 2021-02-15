package me.din0s.listeners

import me.din0s.commands.Command
import me.din0s.commands.XpCommand
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.apache.logging.log4j.LogManager

class MessageListener: ListenerAdapter() {
    private val prefix = "?"

    private val cmdMap = mutableMapOf<String, Command>()

    init {
        arrayOf(XpCommand())
            .forEach {
                cmdMap[it.name] = it
                it.alias.forEach { alias ->
                    cmdMap[alias] = it
                }
            }
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.isWebhookMessage || event.member!!.user.isBot) {
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

            it.invoke(event, args.drop(1))
        }
    }
}
