package com.qzero.mcga.daemon

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Must specify a port")
        return
    }

    val port = args[0].toInt()
    val server = DaemonServer(port)
    server.startListen()
}