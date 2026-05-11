package com.qzero.mcga.data

import com.alibaba.fastjson2.JSON
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime

@Component
class PlayerLogStorage {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val file = File("player_logs.jsonl")
    private val logs = mutableListOf<PlayerLog>()
    private var nextId = 1L
    private val lock = Any()

    init {
        loadFromFile()
    }

    fun append(serverName: String, playerName: String, eventType: String): PlayerLog {
        synchronized(lock) {
            val log = PlayerLog(
                logId = nextId++,
                serverName = serverName,
                playerName = playerName,
                eventType = eventType,
                eventTime = LocalDateTime.now().toString()
            )
            logs.add(log)
            file.appendText(JSON.toJSONString(log) + "\n")
            return log
        }
    }

    fun findAll(): List<PlayerLog> {
        synchronized(lock) {
            return logs.toList()
        }
    }

    fun findRecentByServer(serverName: String, limit: Int = 50): List<PlayerLog> {
        synchronized(lock) {
            return logs.filter { it.serverName == serverName }
                .sortedByDescending { it.eventTime }
                .take(limit)
        }
    }

    /**
     * 查询指定服务器当前在线的玩家（最新事件为 join 的玩家）
     */
    fun findCurrentPlayers(serverName: String): List<String> {
        synchronized(lock) {
            val serverLogs = logs.filter { it.serverName == serverName }
            val latestByPlayer = serverLogs.groupBy { it.playerName }
                .mapValues { (_, playerLogs) -> playerLogs.maxBy { it.eventTime } }
            return latestByPlayer.filter { (_, log) -> log.eventType == "join" }
                .keys
                .toList()
        }
    }

    fun findByPlayer(playerName: String, serverName: String? = null): List<PlayerLog> {
        synchronized(lock) {
            return logs.filter {
                it.playerName == playerName && (serverName == null || it.serverName == serverName)
            }.sortedByDescending { it.eventTime }
        }
    }

    /**
     * 获取当前在线玩家及其加入时间的映射
     */
    fun findCurrentPlayersWithTime(serverName: String): List<Pair<String, String>> {
        synchronized(lock) {
            val serverLogs = logs.filter { it.serverName == serverName }
            val latestByPlayer = serverLogs.groupBy { it.playerName }
                .mapValues { (_, playerLogs) -> playerLogs.maxBy { it.eventTime } }
            return latestByPlayer.filter { (_, log) -> log.eventType == "join" }
                .map { (name, log) -> name to log.eventTime }
        }
    }

    /**
     * 计算指定玩家的在线会话列表（配对 join/leave）
     */
    fun findSessionsByPlayer(playerName: String, serverName: String? = null): List<PlayerSession> {
        synchronized(lock) {
            val playerLogs = logs.filter {
                it.playerName == playerName && (serverName == null || it.serverName == serverName)
            }.sortedBy { it.eventTime }

            val sessions = mutableListOf<PlayerSession>()
            var currentJoin: PlayerLog? = null

            for (log in playerLogs) {
                when (log.eventType) {
                    "join" -> {
                        // 如果已有未关闭的 session，先关闭（异常情况：连续两次 join）
                        if (currentJoin != null) {
                            sessions.add(PlayerSession(
                                serverName = currentJoin!!.serverName,
                                playerName = playerName,
                                joinTime = currentJoin!!.eventTime,
                                leaveTime = null,
                                durationSeconds = null,
                            ))
                        }
                        currentJoin = log
                    }
                    "leave" -> {
                        if (currentJoin != null) {
                            val joinTime = LocalDateTime.parse(currentJoin!!.eventTime)
                            val leaveTime = LocalDateTime.parse(log.eventTime)
                            val duration = java.time.Duration.between(joinTime, leaveTime).seconds
                            sessions.add(PlayerSession(
                                serverName = log.serverName,
                                playerName = playerName,
                                joinTime = currentJoin!!.eventTime,
                                leaveTime = log.eventTime,
                                durationSeconds = duration,
                            ))
                            currentJoin = null
                        }
                        // 没有匹配的join就忽略这条leave
                    }
                }
            }
            // 最后还有未关闭的 session（玩家仍在线）
            if (currentJoin != null) {
                sessions.add(PlayerSession(
                    serverName = currentJoin!!.serverName,
                    playerName = playerName,
                    joinTime = currentJoin!!.eventTime,
                    leaveTime = null,
                    durationSeconds = null,
                ))
            }

            return sessions
        }
    }

    fun countByServerAndTypeAndDate(serverName: String, eventType: String, date: String): Long {
        synchronized(lock) {
            return logs.count {
                it.serverName == serverName &&
                it.eventType == eventType &&
                it.eventTime.startsWith(date)
            }.toLong()
        }
    }

    private fun loadFromFile() {
        if (!file.exists()) {
            logger.info("Player log file not found, will create new one: ${file.absolutePath}")
            return
        }

        var lineNumber = 0
        file.forEachLine { line ->
            lineNumber++
            try {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEachLine
                val log = JSON.parseObject(trimmed, PlayerLog::class.java)
                logs.add(log)
                if (log.logId >= nextId) {
                    nextId = log.logId + 1
                }
            } catch (e: Exception) {
                logger.warn("Skipping malformed line $lineNumber in ${file.name}: ${e.message}")
            }
        }
        logger.info("Loaded ${logs.size} player log entries from ${file.name}")
    }
}
