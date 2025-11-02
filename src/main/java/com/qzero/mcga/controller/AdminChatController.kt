package com.qzero.mcga.controller

import com.qzero.mcga.service.ChatBotService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminChatController(
    private val chatBotService: ChatBotService
) {

    @PostMapping("/admin/chat")
    fun triggerChatFromAdmin(
        @RequestParam message: String
    ): String {
        return chatBotService.getChatResponse(message, "", "", true)
    }

}