package me.din0s.activity

import me.din0s.sql.tables.Levels
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.*
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import pw.forst.exposed.insertOrUpdate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class ActivityListener : ListenerAdapter() {
    private val logger = LogManager.getLogger()

    private fun onMemberJoin(member: Member, channel: VoiceChannel) {
        if (!member.user.isBot) {
            val members = channel.members.filter { !it.user.isBot }
            if (members.size == 2) {
                val active = members.filter { !it.voiceState!!.isDeafened }
                active.forEach { ActivityManager.startVoice(it) }
                active.filter { it.voiceState!!.isStream }.forEach { ActivityManager.startStream(it) }
            } else if (!member.voiceState!!.isDeafened) {
                ActivityManager.startVoice(member)
            }
        }
    }

    private fun onMemberLeave(channel: VoiceChannel) {
        val remaining = channel.members.filter { !it.user.isBot }
        if (remaining.size == 1) {
            ActivityManager.endVoiceStream(remaining[0])
        }
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        logger.info("${event.member.user.name} joined ${event.channelJoined.name}")
        if (event.channelJoined != event.guild.afkChannel) {
            onMemberJoin(event.member, event.channelJoined)
        }
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        logger.info("${event.member.user.name} left ${event.channelLeft.name}")
        onMemberLeave(event.channelLeft)
        ActivityManager.endVoiceStream(event.member)
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        logger.info("${event.member.user.name} moved from ${event.channelLeft.name} to ${event.channelJoined.name}")
        onMemberLeave(event.channelLeft)
        if (event.channelJoined == event.guild.afkChannel) {
            ActivityManager.endVoiceStream(event.member)
        }
    }

    override fun onGuildVoiceStream(event: GuildVoiceStreamEvent) {
        if (event.isStream) {
            if (event.voiceState.channel!!.members.filter { !it.user.isBot }.size > 1) {
                ActivityManager.startStream(event.member)
            }
        } else {
            ActivityManager.endStream(event.member)
        }
    }

    override fun onGuildVoiceSelfDeafen(event: GuildVoiceSelfDeafenEvent) {
        val state = event.voiceState
        if (state.channel == event.guild.afkChannel) {
            return
        }

        if (event.isSelfDeafened) {
            logger.info("${event.member.user.name} deafened")
            ActivityManager.endVoiceStream(event.member)
        } else {
            logger.info("${event.member.user.name} undeafened")
            ActivityManager.startVoice(event.member)
            if (state.isStream) {
                ActivityManager.startStream(event.member)
            }
        }
    }
}
