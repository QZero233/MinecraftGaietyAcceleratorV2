package com.qzero.mcga.daemon

import java.net.InetAddress
import java.net.ServerSocket

class DaemonServer(
    private val port: Int
) {

    private var serverSocket: ServerSocket = ServerSocket(port, 50, InetAddress.getLoopbackAddress())

    fun startListen() {
        println("Server started on port $port")
        while (true) {
            val clientSocket = serverSocket.accept()
            println("Accepted connection from ${clientSocket.port}")
            val clientThread = DaemonClientHandlerThread(clientSocket)
            clientThread.start()
            ProcessWorker.registerClientThread(clientThread)
        }
    }

}