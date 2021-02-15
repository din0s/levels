package me.din0s.listeners

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

    private val activeUsers = mutableMapOf<Long, OffsetDateTime>()
    private val activeStreamers = mutableMapOf<Long, OffsetDateTime>()

    private fun updateActivity(id: Long, voice: Long, stream: Long) {
        transaction {
            val userQuery = Levels.select { Levels.userId eq id }

            val activeVoice = when {
                userQuery.empty() -> voice
                else -> userQuery.first()[Levels.voiceMins] + voice
            }

            val activeStream = when {
                userQuery.empty() -> stream
                else -> userQuery.first()[Levels.streamMins] + stream
            }

            Levels.insertOrUpdate(Levels.userId) {
                it[userId] = id
                it[voiceMins] = activeVoice
                it[streamMins] = activeStream
            }
        }
    }

    private fun onStartVoice(member: Member) {
        logger.debug("Starting voice for ${member.user.asTag}")
        activeUsers[member.idLong] = OffsetDateTime.now()
    }

    private fun onStartStream(member: Member) {
        logger.debug("Starting stream for ${member.user.asTag}")
        activeStreamers[member.idLong] = OffsetDateTime.now()
    }

    private fun onEndStream(member: Member) {
        val id = member.idLong
        val stream = activeStreamers[id]?.let {
            logger.debug("Ending stream for ${member.user.asTag}")
            activeStreamers.remove(id)
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        if (stream != 0L) {
            updateActivity(id, 0, stream)
        }
    }

    private fun onEndVoiceStream(member: Member) {
        val id = member.idLong
        val voice = activeUsers[id]?.let {
            logger.debug("Ending voice for ${member.user.asTag}")
            activeUsers.remove(id)
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        val stream = activeStreamers[id]?.let {
            logger.debug("Ending stream for ${member.user.asTag}")
            activeStreamers.remove(id)
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        if (voice != 0L || stream != 0L) {
            updateActivity(id, voice, stream)
        }
    }

    private fun onMemberJoin(member: Member, channel: VoiceChannel) {
        if (!member.user.isBot) {
            val members = channel.members.filter { !it.user.isBot }
            if (members.size == 2) {
                val active = members.filter { !it.voiceState!!.isDeafened }
                active.forEach { onStartVoice(it) }
                active.filter { it.voiceState!!.isStream }.forEach { onStartStream(it) }
            } else if (!member.voiceState!!.isDeafened) {
                onStartVoice(member)
            }
        }
    }

    private fun onMemberLeave(channel: VoiceChannel) {
        val remaining = channel.members.filter { !it.user.isBot }
        if (remaining.size == 1) {
            onEndVoiceStream(remaining[0])
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
        onEndVoiceStream(event.member)
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        logger.info("${event.member.user.name} moved from ${event.channelLeft.name} to ${event.channelJoined.name}")
        onMemberLeave(event.channelLeft)
        if (event.channelJoined == event.guild.afkChannel) {
            onEndVoiceStream(event.member)
        }
    }

    override fun onGuildVoiceStream(event: GuildVoiceStreamEvent) {
        if (event.isStream) {
            logger.info("${event.member.user.name} started streaming")
            if (event.voiceState.channel!!.members.filter { !it.user.isBot }.size > 1) {
                onStartStream(event.member)
            }
        } else {
            logger.info("${event.member.user.name} stopped streaming")
            onEndStream(event.member)
        }
    }

    override fun onGuildVoiceSelfDeafen(event: GuildVoiceSelfDeafenEvent) {
        val state = event.voiceState
        if (state.channel == event.guild.afkChannel) {
            return
        }

        if (event.isSelfDeafened) {
            logger.info("${event.member.user.name} deafened")
            onEndVoiceStream(event.member)
        } else {
            logger.info("${event.member.user.name} undeafened")
            onStartVoice(event.member)
            if (state.isStream) {
                onStartStream(event.member)
            }
        }
    }
}
