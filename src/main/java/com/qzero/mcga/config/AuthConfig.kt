package com.qzero.mcga.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class AuthConfig(
    @param:Value("\${auth.token}")
    private val token: String
)