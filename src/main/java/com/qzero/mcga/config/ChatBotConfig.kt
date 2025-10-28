package com.qzero.mcga.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class ChatBotConfig(
    @param:Value("\${chatbot.enabled:false}")
    val enabled: Boolean,
    @param:Value("\${chatbot.api-url:}")
    val apiUrl: String,
    @param:Value("\${chatbot.api-key:}")
    val apiKey: String
)