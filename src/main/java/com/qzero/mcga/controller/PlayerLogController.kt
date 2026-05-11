package com.qzero.mcga.controller

import com.qzero.mcga.ActionResult
import com.qzero.mcga.service.PlayerLogService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class PlayerLogController(
    private val playerLogService: PlayerLogService,
) {

    /**
     * 接口名称：获取玩家登录/退出日志
     * 接口路径：/player-log/recent
     * 请求方法：GET
     *
     * 请求参数：
     * - serverName（可选）：筛选指定服务器的日志
     * - limit（可选）：返回条数上限，默认50
     *
     * 响应：ActionResult，其中data如下：
     * - logs: List<PlayerLog>，每条包含 logId, serverName, playerName, eventType, eventTime
     */
    @GetMapping("/player-log/recent")
    fun getRecentLogs(
        @RequestParam(required = false) serverName: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ActionResult {
        val logs = playerLogService.getRecentLogs(serverName, limit)
        return ActionResult("logs", logs.map { mapOf(
            "logId" to it.logId,
            "serverName" to it.serverName,
            "playerName" to it.playerName,
            "eventType" to it.eventType,
            "eventTime" to it.eventTime,
        )})
    }

    /**
     * 接口名称：获取当前在线玩家（含加入时间）
     * 接口路径：/player-log/current-players
     * 请求方法：GET
     *
     * 请求参数：
     * - serverName：服务器名称（必填）
     *
     * 响应：ActionResult，其中data如下：
     * - players: List<{playerName, joinTime}>，当前在线玩家名称及加入时间
     */
    @GetMapping("/player-log/current-players")
    fun getCurrentPlayers(@RequestParam serverName: String): ActionResult {
        val players = playerLogService.getCurrentPlayersWithTime(serverName)
        return ActionResult("players", players)
    }

    /**
     * 接口名称：查询指定玩家的历史记录
     * 接口路径：/player-log/player/{playerName}
     * 请求方法：GET
     *
     * 请求参数：
     * - path variable: playerName - 玩家名称
     * - serverName（可选）：筛选指定服务器
     *
     * 响应：ActionResult，其中data如下：
     * - logs: List<PlayerLog>，该玩家的登录/退出记录
     */
    @GetMapping("/player-log/player/{playerName}")
    fun getPlayerHistory(
        @PathVariable playerName: String,
        @RequestParam(required = false) serverName: String?,
    ): ActionResult {
        val logs = playerLogService.getPlayerHistory(playerName, serverName)
        return ActionResult("logs", logs.map { mapOf(
            "logId" to it.logId,
            "serverName" to it.serverName,
            "playerName" to it.playerName,
            "eventType" to it.eventType,
            "eventTime" to it.eventTime,
        )})
    }

    /**
     * 接口名称：获取每日登录/退出统计
     * 接口路径：/player-log/statistics
     * 请求方法：GET
     *
     * 请求参数：
     * - serverName：服务器名称（必填）
     * - date：日期，格式 yyyy-MM-dd，默认今天
     *
     * 响应：ActionResult，其中data如下：
     * - statistics: Map，包含 serverName, date, joinCount, leaveCount
     */
    @GetMapping("/player-log/statistics")
    fun getDailyStatistics(
        @RequestParam serverName: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): ActionResult {
        val targetDate = date ?: LocalDate.now()
        val statistics = playerLogService.getDailyStatistics(serverName, targetDate)
        return ActionResult("statistics", statistics)
    }

    /**
     * 接口名称：获取玩家的在线会话记录（含在线时长）
     * 接口路径：/player-log/sessions
     * 请求方法：GET
     *
     * 请求参数：
     * - playerName：玩家名称（必填）
     * - serverName（可选）：筛选指定服务器
     *
     * 响应：ActionResult，其中data如下：
     * - sessions: List<PlayerSession>，包含 joinTime, leaveTime, durationSeconds, durationFormatted
     */
    @GetMapping("/player-log/sessions")
    fun getPlayerSessions(
        @RequestParam playerName: String,
        @RequestParam(required = false) serverName: String?,
    ): ActionResult {
        val sessions = playerLogService.getPlayerSessions(playerName, serverName)
        return ActionResult("sessions", sessions.map { mapOf(
            "serverName" to it.serverName,
            "playerName" to it.playerName,
            "joinTime" to it.joinTime,
            "leaveTime" to it.leaveTime,
            "durationSeconds" to it.durationSeconds,
            "durationFormatted" to it.durationFormatted(),
        )})
    }
}
