package me.din0s.listeners

import me.din0s.sql.tables.Levels
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

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        logger.info("${event.member.user.name} joined ${event.channelJoined.name}")
        activeUsers[event.member.idLong] = OffsetDateTime.now()
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        logger.info("${event.member.user.name} left ${event.channelLeft.name}")
        val id = event.member.idLong

        val voice = activeUsers[id]?.let {
            activeUsers.remove(id)
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        val stream = activeStreamers[id]?.let {
            activeStreamers.remove(id)
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        if (voice != 0L || stream != 0L) {
            updateActivity(id, voice, stream)
        }
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        logger.info("${event.member.user.name} moved from ${event.channelJoined.name} to ${event.channelLeft.name}")
        val id = event.member.idLong

        val stream = activeStreamers[id]?.let {
            activeStreamers.remove(id)
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        if (stream != 0L) {
            updateActivity(id, 0, stream)
        }
    }

    override fun onGuildVoiceStream(event: GuildVoiceStreamEvent) {
        val id = event.member.idLong
        if (event.isStream) {
            logger.info("${event.member.user.name} started streaming")
            activeStreamers[id] = OffsetDateTime.now()
        } else {
            logger.info("${event.member.user.name} stopped streaming")

            val stream = activeStreamers[id]?.let {
                activeStreamers.remove(id)
                ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
            } ?: 0

            if (stream != 0L) {
                updateActivity(id, 0, stream)
            }
        }
    }

    override fun onGuildVoiceSelfDeafen(event: GuildVoiceSelfDeafenEvent) {
        val id = event.member.idLong
        if (event.isSelfDeafened) {
            logger.info("${event.member.user.name} deafened")

            val voice = activeUsers[id]?.let {
                activeUsers.remove(id)
                ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
            } ?: 0

            if (voice != 0L) {
                updateActivity(id, voice, 0)
            }
        } else {
            logger.info("${event.member.user.name} undeafened")
            activeUsers[id] = OffsetDateTime.now()
        }
    }
}
