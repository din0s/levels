package me.din0s.activity

import me.din0s.sql.tables.Levels
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import pw.forst.exposed.insertOrUpdate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

object ActivityManager {
    private val logger = LogManager.getLogger()

    private val activeMembers = mutableMapOf<Long, OffsetDateTime>()
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

    private fun restart(member: Member) {
        val id = member.idLong
        val voice = activeMembers[id]?.let {
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        val stream = activeStreamers[id]?.let {
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        if (voice != 0L || stream != 0L) {
            logger.debug("Restarting state for ${member.user.asTag}")
            updateActivity(id, voice, stream)
            activeMembers[id] = OffsetDateTime.now()
            activeStreamers[id] = OffsetDateTime.now()
        }
    }

    fun init(jda: JDA) {
        logger.info("Initializing")
        jda.voiceChannels
            .filter { vc -> vc.members.filter { !it.user.isBot }.size > 1 }
            .flatMap { it.members }
            .filter { !it.user.isBot }
            .forEach {
                logger.debug("${it.user.asTag} is connected")

                val state = it.voiceState!!
                if (!state.isDeafened) {
                    startVoice(it)

                    if (state.isStream) {
                        startStream(it)
                    }
                }
            }
    }

    fun endAll() {
        logger.info("Terminating all voice states")
        activeMembers.keys.forEach { endVoiceStream(it) }
    }

    fun getActivity(member: Member): Activity {
        restart(member)
        return transaction {
            val query = Levels.select { Levels.userId eq member.idLong }

            val voice = when {
                query.empty() -> 0
                else -> query.first()[Levels.voiceMins]
            }

            val stream = when {
                query.empty() -> 0
                else -> query.first()[Levels.streamMins]
            }

            return@transaction Activity(voice, stream)
        }
    }

    fun startVoice(member: Member) {
        logger.debug("Starting voice for ${member.user.asTag}")
        activeMembers[member.idLong] = OffsetDateTime.now()
    }

    fun startStream(member: Member) {
        logger.debug("Starting stream for ${member.user.asTag}")
        activeStreamers[member.idLong] = OffsetDateTime.now()
    }

    fun endStream(member: Member) {
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

    private fun endVoiceStream(id: Long, tag: String? = null) {
        val voice = activeMembers[id]?.let {
            logger.debug("Ending voice for ${tag ?: id}")
            activeMembers.remove(id)
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        val stream = activeStreamers[id]?.let {
            logger.debug("Ending stream for ${tag ?: id}")
            activeStreamers.remove(id)
            ChronoUnit.MINUTES.between(it, OffsetDateTime.now())
        } ?: 0

        if (voice != 0L || stream != 0L) {
            updateActivity(id, voice, stream)
        }
    }

    fun endVoiceStream(member: Member) {
        endVoiceStream(member.idLong, member.user.asTag)
    }
}
