package com.qzero.mcga.minecraft.container.daemon

import org.slf4j.LoggerFactory
import java.net.Socket

class DaemonClientThread(
    private val daemonPort: Int,
    private val listener: DaemonIOListener
): Thread() {

    private val logger = LoggerFactory.getLogger("MinecraftServerRunner")

    private var processRunning = false
    private var daemonConnectionHealthy = false
    private var daemonSocket: Socket? = null

    private var reader: java.io.BufferedReader? = null
    private var writer: java.io.BufferedWriter? = null

    override fun run() {
        super.run()
        try {
            daemonSocket = Socket("localhost", daemonPort)
            reader = daemonSocket?.getInputStream()?.bufferedReader()
            writer = daemonSocket?.getOutputStream()?.bufferedWriter()

            daemonConnectionHealthy = true
            listener.onConnectionEstablished()
            logger.info("DaemonClientThread for $daemonPort connected.")

            while (true) {
                val line = reader?.readLine() ?: break
                // 处理来自守护进程的消息
                logger.debug("[Daemon-${daemonPort}]Received line: $line")

                val parts = line.split(",")
                val type = parts[0]
                when (type) {
                    DaemonProtocolConstant.TYPE_RECEIVED_STD_IO -> {
                        val stdioLine = parts.subList(1, parts.size).joinToString(",")
                        listener.onDaemonReceiveStdIO(stdioLine)
                    }

                    DaemonProtocolConstant.TYPE_REPORT_PROCESS_STATE -> {
                        val state = parts.getOrNull(1)
                        when (state) {
                            DaemonProtocolConstant.PROCESS_STATE_RUNNING -> {
                                processRunning = true
                            }
                            DaemonProtocolConstant.PROCESS_STATE_IDLE -> {
                                processRunning = false
                            }
                            else -> {
                                logger.warn("DaemonClientThread-$daemonPort received unknown process state: $state")
                            }
                        }

                        listener.onDaemonProcessStateReported(processRunning)
                    }
                    else -> {
                        logger.warn("DaemonClientThread-$daemonPort received unknown message type: $type, full line: $line")
                    }
                }

            }
        }catch (e: Exception) {
            logger.error("DaemonClientThread-$daemonPort encountered an error: ${e.message}", e)
        }finally {
            daemonConnectionHealthy = false
            daemonSocket = null
            listener.onConnectionLost()
            logger.info("DaemonClientThread for $daemonPort has stopped.")
        }

    }

    fun writeLine(line: String) {
        logger.debug("Write line to Daemon-$daemonPort: $line")
        writer?.write(line)
        writer?.newLine()
        writer?.flush()
    }

    fun isProcessRunning(): Boolean {
        return processRunning
    }

    fun isDaemonConnectionHealthy(): Boolean {
        return daemonConnectionHealthy
    }

}

interface DaemonIOListener {
    fun onDaemonReceiveStdIO(line: String)
    fun onConnectionEstablished()
    fun onConnectionLost()
    fun onDaemonProcessStateReported(running: Boolean)
}