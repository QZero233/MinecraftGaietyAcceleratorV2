package com.qzero.mcga.minecraft.container

import com.qzero.mcga.config.RuntimeConfig
import com.qzero.mcga.minecraft.MinecraftServerConfig
import com.qzero.mcga.minecraft.MinecraftServerEventListener

interface IMinecraftServerContainer {

    val runtimeConfig: RuntimeConfig
    val serverConfig: MinecraftServerConfig
    val eventListener: MinecraftServerEventListener

    fun startServer()
    fun stopServer()
    fun sendCommand(command: String)

    fun isServerRunning(): Boolean
}