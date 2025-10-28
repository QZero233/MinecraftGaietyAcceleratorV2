package com.qzero.mcga.debug

import com.qzero.mcga.event.ServerEvent
import com.qzero.mcga.event.ServerEventCenter
import com.qzero.mcga.event.ServerEventListener
import com.qzero.mcga.service.ChatBotService
import com.qzero.mcga.service.MapBackupService
import com.qzero.mcga.service.MinecraftServerService
import com.qzero.mcga.utils.UUIDUtils
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
class DebugController(
    private val serverService: MinecraftServerService,
    private val eventCenter: ServerEventCenter,
    private val backupService: MapBackupService,
    private val chatBotService: ChatBotService
): ServerEventListener {

    override val listenerId: String = UUIDUtils.getRandomUUID()
    override fun onEvent(event: ServerEvent) {
        logger.info("Event ${event.eventName} : $event")
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/debug/servers")
    fun listAllServers() {
        logger.info("${serverService.listAllServers()}")
    }

    @GetMapping("/debug/start-server")
    fun startServer() {
        serverService.startServer("test")
        eventCenter.registerListener(this)
    }

    @GetMapping("/debug/stop-server")
    fun stopServer() {
        serverService.stopServer("test")
    }

    @GetMapping("/debug/send-command")
    fun sendCommand(
        @RequestParam
        content: String,
    ) {
        serverService.sendCommand("test", content)
    }

    @GetMapping("/debug/update-property")
    fun updateProperty(
        @RequestParam
        key: String,
        @RequestParam
        value: String
    ) {
        serverService.changeServerProperty("test", key, value)
    }

    @GetMapping("/debug/backup-map")
    fun backupMap() {
        backupService.backupMap(serverService.listAllServers()[0], File("./server/backup.zip"))
    }

    @GetMapping("/debug/chatbot/trigger")
    fun triggerChat(
        @RequestParam message: String
    ): String {
        return chatBotService.getChatResponse(
            message = message,
            serverName = "debug_server",
            playerId = "debug_player"
        )
    }

}