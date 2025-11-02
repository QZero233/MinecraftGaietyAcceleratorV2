package com.qzero.mcga.service

import com.qzero.mcga.config.ChatBotConfig
import com.qzero.mcga.event.PlayerMessageEvent
import com.qzero.mcga.event.ServerEvent
import com.qzero.mcga.event.ServerEventCenter
import com.qzero.mcga.event.ServerEventListener
import com.qzero.mcga.utils.UUIDUtils
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.springframework.stereotype.Service

@Service
class ChatBotService(
    private val chatBotConfig: ChatBotConfig,
    serverEventCenter: ServerEventCenter,
    private val minecraftServerService: MinecraftServerService
): ServerEventListener {

    override val listenerId: String = UUIDUtils.getRandomUUID()
    override fun onEvent(event: ServerEvent) {
        if (event is PlayerMessageEvent) {
            val message = event.message
            if (message.startsWith("#")) {
                val query = message.removePrefix("#").trim()
                val response = getChatResponse(query, event.serverName, event.playerName)
                minecraftServerService.sendChatMessage(
                    event.serverName,
                    response.split("\n").map {
                        "§6[A酱]§  $it"
                    }
                )
            }
        }
    }

    init {
        serverEventCenter.registerListener(this)
    }

    private var savedApiKey = chatBotConfig.apiKey

    fun getChatResponse(message: String, serverName: String, playerId: String, fromMCGAAdmin: Boolean = false): String {
        if (!chatBotConfig.enabled) {
            return "ChatBot service is disabled."
        }

        HttpClients.createDefault().use { client ->
            val post = HttpPost(chatBotConfig.apiUrl)
            post.addHeader("Authorization", savedApiKey)
            val formData = listOf(
                "message" to message,
                "serverName" to serverName,
                "minecraftId" to playerId,
                "fromMCGAAdmin" to fromMCGAAdmin.toString()
            )
            val entity = org.apache.http.client.entity.UrlEncodedFormEntity(
                formData.map { org.apache.http.message.BasicNameValuePair(it.first, it.second) },
                "UTF-8"
            )
            post.entity = entity
            client.execute(post).use { response ->
                val statusCode = response.statusLine.statusCode
                if (statusCode != 200) {
                    return "Error: Received status code $statusCode from ChatBot API."
                }
                val responseBody = response.entity.content.bufferedReader().use { it.readText() }

                if (response.getFirstHeader("New-Authorization") != null) {
                    val newApiKey = response.getFirstHeader("New-Authorization").value
                    savedApiKey = newApiKey
                }

                return responseBody
            }
        }
    }

}