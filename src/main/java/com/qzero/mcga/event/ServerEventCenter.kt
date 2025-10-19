package com.qzero.mcga.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ServerEventCenter {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val listenerMap: MutableMap<String, ServerEventListener> = mutableMapOf()

    fun registerListener(listener: ServerEventListener) {
        listenerMap[listener.listenerId] = listener
    }

    fun unregisterListener(listenerId: String) {
        listenerMap.remove(listenerId)
    }

    fun publishServerEvent(event: ServerEvent) {
        listenerMap.values.forEach { listener ->
            try {
                listener.onEvent(event)
            } catch (e: Exception) {
                logger.error("Error while notifying listener ${listener.listenerId} of event $event", e)
            }
        }
    }

}