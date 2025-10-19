package com.qzero.mcga.minecraft

// 注意，同一条输出可能会对应多个调用，且调用顺序不固定
interface MinecraftServerEventListener {

    fun onOutputLine(serverName: String, line: String) {

    }

    fun onErrorLine(serverName: String, line: String) {

    }

    fun onServerStopped(serverName: String) {

    }

    fun onServerStarted(serverName: String) {

    }
}