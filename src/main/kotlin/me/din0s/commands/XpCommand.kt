package me.din0s.commands

import me.din0s.sql.tables.Levels
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class XpCommand: Command(
    name = "xp",
    usage = "(user)",
    alias = arrayOf("lvl", "level")
) {
    override fun invoke(event: GuildMessageReceivedEvent, args: List<String>) {
        val member = when {
            event.message.mentionedMembers.isNotEmpty() -> {
                if (event.message.mentionedMembers.size > 1) {
                    event.channel.sendMessage("**Please only mention 1 user!**").queue()
                    return
                }
                event.message.mentionedMembers[0]
            }
            args.isNotEmpty() -> {
                val name = args.joinToString(" ")
                val members = event.guild.getMembersByName(name, true)
                when {
                    members.isEmpty() -> {
                        event.channel.sendMessage("**No members matched your query!**").queue()
                        return
                    }
                    members.size > 1 -> {
                        event.channel.sendMessage("**Too many members matched your query!**").queue()
                        return
                    }
                    else -> members[0]
                }
            }
            else -> {
                event.member!!
            }
        }
        transaction {
            val query = Levels.select { Levels.userId eq member.idLong }

            val voice = when {
                query.empty() -> 0
                else -> query.first()[Levels.voiceMins]
            }

            val stream = when {
                query.empty() -> 0
                else -> query.first()[Levels.streamMins]
            }

            val eb = EmbedBuilder()
            eb.setAuthor(member.user.asTag, null, member.user.effectiveAvatarUrl)
            eb.setColor(member.color)
            eb.addField("Voice Minutes", voice.toString(), true)
            eb.addField("Stream Minutes", stream.toString(), true)
            eb.setTimestamp(Instant.now())

            event.channel.sendMessage(eb.build()).queue()
        }
    }
}
