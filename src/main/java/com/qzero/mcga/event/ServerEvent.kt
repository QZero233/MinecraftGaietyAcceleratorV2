package com.qzero.mcga.event

abstract class ServerEvent(
    val serverName: String,
) {
    abstract val eventName: String
}

class ServerStartedEvent(
    serverName: String,
): ServerEvent(serverName) {
    override val eventName: String = "ServerStarted"
}

class ServerStoppedEvent(
    serverName: String,
): ServerEvent(serverName) {
    override val eventName: String = "ServerStopped"
}

class PlayerMessageEvent(
    serverName: String,
    val playerName: String,
    val message: String,
): ServerEvent(serverName) {
    override val eventName: String = "PlayerMessage"
    override fun toString(): String {
        return "PlayerMessageEvent(playerName='$playerName', message='$message', eventName='$eventName')"
    }

}

class PlayerJoinEvent(
    serverName: String,
    val playerName: String,
): ServerEvent(serverName) {
    override val eventName: String = "PlayerJoin"
    override fun toString(): String {
        return "PlayerJoinEvent(playerName='$playerName', eventName='$eventName')"
    }

}

class PLayerLeaveEvent(
    serverName: String,
    val playerName: String,
): ServerEvent(serverName) {
    override val eventName: String = "PlayerLeave"
    override fun toString(): String {
        return "PLayerLeaveEvent(playerName='$playerName', eventName='$eventName')"
    }

}