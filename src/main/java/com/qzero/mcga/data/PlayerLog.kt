package com.qzero.mcga.data

data class PlayerLog(
    val logId: Long,
    val serverName: String,
    val playerName: String,
    val eventType: String,
    val eventTime: String,
)

data class PlayerSession(
    val serverName: String,
    val playerName: String,
    val joinTime: String,
    val leaveTime: String?,
    val durationSeconds: Long?,
) {
    fun durationFormatted(): String {
        if (durationSeconds == null) return "仍在游戏中"
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) "%d小时%d分%d秒".format(hours, minutes, seconds)
        else if (minutes > 0) "%d分%d秒".format(minutes, seconds)
        else "%d秒".format(seconds)
    }
}
