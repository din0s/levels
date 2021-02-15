package me.din0s.sql.tables

import org.jetbrains.exposed.sql.Table

object Levels: Table() {
    val userId = long("user_id").primaryKey()
    val voiceMins = long("voice_mins")
    val streamMins = long("stream_mins")
}
