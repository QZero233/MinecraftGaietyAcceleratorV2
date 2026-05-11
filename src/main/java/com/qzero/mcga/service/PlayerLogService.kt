package com.qzero.mcga.service

import com.qzero.mcga.data.PlayerLog
import com.qzero.mcga.data.PlayerLogStorage
import com.qzero.mcga.data.PlayerSession
import com.qzero.mcga.event.PLayerLeaveEvent
import com.qzero.mcga.event.PlayerJoinEvent
import com.qzero.mcga.event.ServerEvent
import com.qzero.mcga.event.ServerEventCenter
import com.qzero.mcga.event.ServerEventListener
import com.qzero.mcga.utils.UUIDUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PlayerLogService(
    private val storage: PlayerLogStorage,
    serverEventCenter: ServerEventCenter,
) : ServerEventListener {

    override val listenerId: String = UUIDUtils.getRandomUUID()

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        serverEventCenter.registerListener(this)
    }

    override fun onEvent(event: ServerEvent) {
        when (event) {
            is PlayerJoinEvent -> {
                val log = storage.append(event.serverName, event.playerName, "join")
                logger.debug("Recorded player join: ${event.playerName} on ${event.serverName}")
            }
            is PLayerLeaveEvent -> {
                val log = storage.append(event.serverName, event.playerName, "leave")
                logger.debug("Recorded player leave: ${event.playerName} on ${event.serverName}")
            }
        }
    }

    fun getRecentLogs(serverName: String?, limit: Int = 50): List<PlayerLog> {
        return if (serverName != null) {
            storage.findRecentByServer(serverName, limit)
        } else {
            storage.findAll().sortedByDescending { it.eventTime }.take(limit)
        }
    }

    fun getCurrentPlayers(serverName: String): List<String> {
        return storage.findCurrentPlayers(serverName)
    }

    fun getCurrentPlayersWithTime(serverName: String): List<Map<String, String>> {
        return storage.findCurrentPlayersWithTime(serverName).map { (name, time) ->
            mapOf("playerName" to name, "joinTime" to time)
        }
    }

    fun getPlayerSessions(playerName: String, serverName: String?): List<PlayerSession> {
        return storage.findSessionsByPlayer(playerName, serverName)
    }

    fun getPlayerHistory(playerName: String, serverName: String?): List<PlayerLog> {
        return storage.findByPlayer(playerName, serverName)
    }

    fun getDailyStatistics(serverName: String, date: LocalDate): Map<String, Any> {
        val dateStr = date.toString()
        return mapOf(
            "serverName" to serverName,
            "date" to dateStr,
            "joinCount" to storage.countByServerAndTypeAndDate(serverName, "join", dateStr),
            "leaveCount" to storage.countByServerAndTypeAndDate(serverName, "leave", dateStr),
        )
    }
}
