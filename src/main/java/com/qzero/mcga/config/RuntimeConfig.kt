package com.qzero.mcga.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class RuntimeConfig(
    @param:Value("\${runtime.java-path}")
    val javaPath: String,
    @param:Value("\${runtime.terminal-path}")
    val terminalPath: String
)
