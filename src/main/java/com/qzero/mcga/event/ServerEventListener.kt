package com.qzero.mcga.event

interface ServerEventListener {

    val listenerId: String

    fun onEvent(event: ServerEvent)

}