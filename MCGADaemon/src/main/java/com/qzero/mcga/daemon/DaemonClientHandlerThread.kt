package com.qzero.mcga.daemon

import java.net.Socket

class DaemonClientHandlerThread(
    private val clientSocket: Socket
): Thread() {

    private val reader = clientSocket.getInputStream().bufferedReader()
    private val writer = clientSocket.getOutputStream().bufferedWriter()

    override fun run() {
        super.run()

        // 首先汇报一下进程状态
        writeLine("${DaemonProtocolConstant.TYPE_REPORT_PROCESS_STATE}," +
                if (ProcessWorker.isProcessRunning()) DaemonProtocolConstant.PROCESS_STATE_RUNNING else DaemonProtocolConstant.PROCESS_STATE_IDLE
        )

        try {
            while(true) {
                val line = reader.readLine() ?: break
                println("Received from ${clientSocket.port}: $line")
                val parts = line.split(",")
                val type = parts[0]
                when (type) {
                    DaemonProtocolConstant.TYPE_NEW_PROCESS -> {
                        val workDir = parts[1]
                        val cmd = parts.subList(2, parts.size)
                        ProcessWorker.startProcess(cmd, workDir)
                    }

                    DaemonProtocolConstant.TYPE_WRITE_STD_IO -> {
                        ProcessWorker.writeLine(parts.subList(1, parts.size).joinToString(","))
                    }
                }
            }
        } catch (e: Exception) {
            println("Error in client handler thread for ${clientSocket.port}: ${e.message}")
            e.printStackTrace()
            ProcessWorker.unregisterClientThread(this)
        }

    }

    fun writeLine(line: String) {
        synchronized(writer) {
            try {
                writer.write(line)
                writer.newLine()
                writer.flush()
            } catch (e: Exception) {
                println("Error writing to client ${clientSocket.port} $line: ${e.message}")
                e.printStackTrace()
                ProcessWorker.unregisterClientThread(this)
            }
        }
    }

    fun getPort(): Int {
        return clientSocket.port
    }


}