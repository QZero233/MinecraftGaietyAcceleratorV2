package com.qzero.mcga.minecraft.container.daemon

import com.qzero.mcga.config.RuntimeConfig
import com.qzero.mcga.exception.ResponsiveException
import com.qzero.mcga.minecraft.MinecraftServerConfig
import com.qzero.mcga.minecraft.MinecraftServerEventListener
import com.qzero.mcga.minecraft.container.IMinecraftServerContainer
import org.slf4j.LoggerFactory

class DaemonMinecraftServerContainer(
    override val runtimeConfig: RuntimeConfig,
    override val serverConfig: MinecraftServerConfig,
    override val eventListener: MinecraftServerEventListener
): IMinecraftServerContainer, DaemonIOListener {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var daemonClientThread: DaemonClientThread? = null
    private var lastServerStateRunning = false

    init {
        // 启动daemon client
        daemonClientThread = DaemonClientThread(
            daemonPort = serverConfig.daemonPort,
            listener = this
        )
        daemonClientThread!!.start()
    }

    private fun tryReConnect() {
        synchronized(this) {
            if (daemonClientThread != null && daemonClientThread!!.isDaemonConnectionHealthy()) {
                logger.debug("No need to reconnect, connection at ${serverConfig.daemonPort} is healthy.")
                return
            }

            logger.debug("Try to reconnect to daemon at port ${serverConfig.daemonPort} after 30s...")
            Thread.sleep(30000)

            daemonClientThread = DaemonClientThread(
                daemonPort = serverConfig.daemonPort,
                listener = this
            )
            daemonClientThread!!.start()
        }
    }

    override fun startServer() {
        synchronized(this) {
            if (!daemonClientThread!!.isDaemonConnectionHealthy()) {
                logger.info("Can not start server, connection to daemon at port ${serverConfig.daemonPort} is not healthy.")
                throw ResponsiveException("Cannot start server, connection to daemon is not healthy.")
            }

            if (daemonClientThread!!.isProcessRunning()) {
                logger.info("Can not start server, server process is already running on daemon at port ${serverConfig.daemonPort}.")
                throw ResponsiveException("Cannot start server, server process is already running.")
            }

            val startCommand = serverConfig.genStartCommand(runtimeConfig)
            daemonClientThread!!.writeLine("${DaemonProtocolConstant.TYPE_NEW_PROCESS},${serverConfig.serverDir.absolutePath},${startCommand.split(" ").filter { it.isNotBlank() }.joinToString(",")}")
        }
    }

    override fun stopServer() {
        synchronized(this) {
            if (!daemonClientThread!!.isDaemonConnectionHealthy()) {
                logger.info("Can not stop server, connection to daemon at port ${serverConfig.daemonPort} is not healthy.")
                throw ResponsiveException("Cannot stop server, connection to daemon is not healthy.")
            }

            if (!daemonClientThread!!.isProcessRunning()) {
                logger.info("Can not stop server, server process is not running on daemon at port ${serverConfig.daemonPort}.")
                throw ResponsiveException("Cannot stop server, server process is not running.")
            }

            daemonClientThread!!.writeLine("${DaemonProtocolConstant.TYPE_WRITE_STD_IO},stop")
        }
    }

    override fun sendCommand(command: String) {
        synchronized(this) {
            if (!daemonClientThread!!.isDaemonConnectionHealthy()) {
                logger.info("Can not send command $command, connection to daemon at port ${serverConfig.daemonPort} is not healthy.")
                throw ResponsiveException("Cannot send command, connection to daemon is not healthy.")
            }

            if (!daemonClientThread!!.isProcessRunning()) {
                logger.info("Can not send command $command, server process is not running on daemon at port ${serverConfig.daemonPort}.")
                throw ResponsiveException("Cannot send command, server process is not running.")
            }

            daemonClientThread!!.writeLine("${DaemonProtocolConstant.TYPE_WRITE_STD_IO},$command")
        }
    }

    override fun isServerRunning(): Boolean {
        return lastServerStateRunning
    }

    override fun onDaemonReceiveStdIO(line: String) {
        eventListener.onOutputLine(serverConfig.serverName, line)
    }

    override fun onConnectionEstablished() {
        logger.info("DaemonMinecraftServerContainer connected to daemon at port ${serverConfig.daemonPort}")
    }

    override fun onConnectionLost() {
        logger.info("DaemonMinecraftServerContainer lost connection to daemon at port ${serverConfig.daemonPort}, trying to reconnect...")
        tryReConnect()
    }

    override fun onDaemonProcessStateReported(running: Boolean) {
        if (lastServerStateRunning && !running) {
            eventListener.onServerStopped(serverConfig.serverName)
            logger.info("Server ${serverConfig.serverName} stopped.")
        }

        if (!lastServerStateRunning && running) {
            logger.info("Server ${serverConfig.serverName} started.")
        }

        lastServerStateRunning = running
    }

}