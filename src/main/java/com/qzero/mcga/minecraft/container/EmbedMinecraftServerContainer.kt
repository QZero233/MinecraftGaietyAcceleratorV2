package com.qzero.mcga.minecraft.container

import com.qzero.mcga.config.RuntimeConfig
import com.qzero.mcga.exception.ResponsiveException
import com.qzero.mcga.minecraft.MinecraftServerConfig
import com.qzero.mcga.minecraft.MinecraftServerEventListener
import org.slf4j.LoggerFactory
import java.io.File

class EmbedMinecraftServerContainer(
    override val runtimeConfig: RuntimeConfig,
    override val serverConfig: MinecraftServerConfig,
    override val eventListener: MinecraftServerEventListener
): IMinecraftServerContainer {

    private val logger = LoggerFactory.getLogger(javaClass)

    private var runnerThread: ServerRunnerThread? = null

    interface ServerIOListener {
        fun onOutputLine(line: String)
        fun onErrorLine(line: String)
        fun onServerStopped()
    }

    class ServerRunnerThread(
        private val serverConfig: MinecraftServerConfig,
        private val startCommand: String,
        private val workDir: File,
        private val ioListener: ServerIOListener,
    ) : Thread() {

        private val logger = LoggerFactory.getLogger("MinecraftServerRunner")

        private var process: Process? = null

        override fun run() {
            try {
                process = ProcessBuilder(startCommand.split(" ").filter { it.isNotBlank() })
                    .directory(workDir)
                    .redirectErrorStream(true)
                    .start()

                val inputReader = process!!.inputStream.bufferedReader()
                val errorReader = process!!.errorStream.bufferedReader()

                // 读取标准输出
                val outputThread = Thread {
                    inputReader.useLines { lines ->
                        lines.forEach { line ->
                            logger.debug("[Server-${serverConfig.serverName}] $line")
                            ioListener.onOutputLine(line)
                        }
                    }
                }

                // 读取错误输出
                val errorThread = Thread {
                    errorReader.useLines { lines ->
                        lines.forEach { line ->
                            logger.debug("[Server-${serverConfig.serverName}] $line")
                            ioListener.onErrorLine(line)
                        }
                    }
                }

                outputThread.start()
                errorThread.start()

                outputThread.join()
                errorThread.join()

                process?.waitFor()
                process = null
                ioListener.onServerStopped()
            } catch (e: Exception) {
                logger.error("Error running server ${serverConfig.serverName}", e)
                process = null
                ioListener.onServerStopped()
            }
        }

        fun writeLine(line: String) {
            // 向服务器输入命令
            try {
                logger.info("Writing to server ${serverConfig.serverName}: $line")
                val writer = process!!.outputStream.bufferedWriter()
                writer.write(line)
                writer.newLine()
                writer.flush()
            } catch (e: Exception) {
                logger.error("Error writing to server ${serverConfig.serverName}", e)
                throw ResponsiveException("Error writing to server: ${e.message}")
            }
        }

        fun isProcessRunning(): Boolean {
            return process != null
        }
    }


    override fun startServer() {
        synchronized(this) {
            if (runnerThread != null && runnerThread!!.isProcessRunning()) {
                throw ResponsiveException("Server ${serverConfig.serverName} is already running")
            }

            runnerThread = ServerRunnerThread(
                serverConfig = serverConfig,
                workDir = serverConfig.serverDir,
                startCommand = serverConfig.genStartCommand(runtimeConfig),
                ioListener = object : ServerIOListener {
                    override fun onOutputLine(line: String) {
                        eventListener.onOutputLine(serverConfig.serverName, line)
                    }

                    override fun onErrorLine(line: String) {
                        eventListener.onErrorLine(serverConfig.serverName, line)
                    }

                    override fun onServerStopped() {
                        logger.info("Server ${serverConfig.serverName} has stopped")
                        synchronized(this@EmbedMinecraftServerContainer) {
                            runnerThread = null
                        }

                        eventListener.onServerStopped(serverConfig.serverName)
                    }
                }
            )

            runnerThread?.start()
        }
    }

    override fun stopServer() {
        synchronized(this) {
            if (runnerThread == null || !runnerThread!!.isProcessRunning()) {
                throw ResponsiveException("Server ${serverConfig.serverName} is not running")
            }

            runnerThread?.writeLine("stop")
        }
    }

    override fun sendCommand(command: String) {
        synchronized(this) {
            if (runnerThread == null || !runnerThread!!.isProcessRunning()) {
                throw ResponsiveException("Server ${serverConfig.serverName} is not running")
            }

            runnerThread?.writeLine(command)
        }
    }

    override fun isServerRunning(): Boolean {
        return runnerThread != null && runnerThread!!.isProcessRunning()
    }

}