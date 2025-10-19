package com.qzero.mcga.service

import com.qzero.mcga.config.RuntimeConfig
import com.qzero.mcga.event.PLayerLeaveEvent
import com.qzero.mcga.event.PlayerJoinEvent
import com.qzero.mcga.event.PlayerMessageEvent
import com.qzero.mcga.event.ServerEventCenter
import com.qzero.mcga.event.ServerStartedEvent
import com.qzero.mcga.event.ServerStoppedEvent
import com.qzero.mcga.exception.ResponsiveException
import com.qzero.mcga.minecraft.MinecraftServerConfig
import com.qzero.mcga.minecraft.MinecraftServerContainer
import com.qzero.mcga.minecraft.MinecraftServerEventListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.Properties

@Service
class MinecraftServerService(
    private val runtimeConfig: RuntimeConfig,
    private val serverEventCenter: ServerEventCenter
): MinecraftServerEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val serverContainers: MutableMap<String, MinecraftServerContainer> = mutableMapOf()

    /**
     * 扫描当前工作目录下的子文件夹，识别包含 server.properties 的文件夹为一个服务器实例。
     * 配置项说明（来自 server.properties）：
     * - mcga-enabled: 是否被 mcga 管理，缺省为 true，若为 false 则跳过该文件夹。
     * - server-name: 可选，若未配置则使用文件夹名作为服务器名。
     * - server-jar-file: 必须，指定用于启动的 JAR 文件名（相对 serverDir）。
     * - jvm-params: 可选，启动时传递给 JVM 的参数字符串。
     *
     * 若发现重复的 serverName 将抛出 ResponsiveException。
     */
    fun listAllServers(): List<MinecraftServerConfig> {
        // 扫描当前文件夹下所有的子文件夹，如果包含server.properties，就认为是一个服务器
        // 其中的server-name是服务器名，如果没有这一项，就默认服务器名为子文件夹名字
        // 其中的server-jar-file为jar文件名
        // 其中的jvm-params为启动时Java虚拟机参数
        // 其中的mcga-enabled表示是否纳入mcga管理，如果为false，则跳过，如果没有，就默认true
        // 如果命名有冲突，就直接报错

        val servers = mutableListOf<MinecraftServerConfig>()
        val baseDir = File(".") // 当前目录
        val serverNames = mutableSetOf<String>() // 用于检查重复的服务器名称

        if (!baseDir.exists() || !baseDir.isDirectory) {
            return servers
        }

        baseDir.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
            val propertiesFile = File(folder, "server.properties")
            if (propertiesFile.exists()) {
                val props = Properties().apply {
                    load(propertiesFile.inputStream())
                }

                val enabled = props.getProperty("mcga-enabled", "true").toBoolean()
                if (!enabled) {
                    logger.debug("Skipping disabled server in folder: ${folder.name}")
                    return@forEach
                }

                val serverName = props.getProperty("server-name", folder.name)
                val serverJarFileName = props.getProperty("server-jar-file")
                    ?: throw ResponsiveException("server-jar-file is missing in ${folder.path}/server.properties")
                val serverJvmParameters = props.getProperty("jvm-params", "")

                // 检查服务器名称是否重复
                if (!serverNames.add(serverName)) {
                    throw ResponsiveException("Duplicate server name detected: $serverName")
                }

                servers.add(
                    MinecraftServerConfig(
                        serverName = serverName,
                        serverDir = folder,
                        serverJarFileName = serverJarFileName,
                        serverJvmParameters = serverJvmParameters
                    )
                )
            }
        }

        return servers
    }

    /**
     * 判断指定名称的服务器当前是否处于运行状态。
     * 线程安全地读取内部的 serverContainers 映射（使用 synchronized）。
     *
     * 返回 true 表示正在运行（已在 serverContainers 中存在对应容器）。
     */
    fun isServerRunning(serverName: String): Boolean {
        synchronized(serverContainers) {
            return serverContainers.containsKey(serverName)
        }
    }

    /**
     * 发起启动指定服务器的操作（同步调用 Service 层以启动容器）。
     * 行为与异常：
     * - 如果服务器配置不存在，抛出 ResponsiveException("Server X not found")。
     * - 如果服务器已在运行，抛出 ResponsiveException("Server X is already running")。
     * - 成功时将在 serverContainers 中登记新的 MinecraftServerContainer。
     *
     * 本方法对 serverContainers 的读写采用 synchronized(serverContainers) 保证线程安全。
     */
    fun startServer(serverName: String) {
        val serverConfig = listAllServers().find { it.serverName == serverName }
            ?: throw ResponsiveException("Server $serverName not found")

        synchronized(serverContainers) {
            if (serverContainers.containsKey(serverName)) {
                throw ResponsiveException("Server $serverName is already running")
            }

            val container = MinecraftServerContainer(runtimeConfig, serverConfig, this)
            container.startServer()
            serverContainers[serverName] = container
        }
    }

    /**
     * 发起停止指定服务器的操作。
     * 行为与异常：
     * - 如果服务器未在运行，抛出 ResponsiveException("Server X is not running")。
     * - 成功时会调用容器的 stopServer 并从 serverContainers 中移除该容器。
     *
     * 本方法对 serverContainers 的读写采用 synchronized(serverContainers) 保证线程安全。
     */
    fun stopServer(serverName: String) {
        synchronized(serverContainers) {
            val container = serverContainers[serverName]
                ?: throw ResponsiveException("Server $serverName is not running")

            container.stopServer()
            serverContainers.remove(serverName)
        }
    }

    fun sendCommand(serverName: String, command: String) {
        val container = serverContainers[serverName]
            ?: throw ResponsiveException("Server $serverName is not running")

        container.sendCommand(command)
    }

    fun listAllProperties(serverName: String): Map<String, String> {
        val serverConfig = listAllServers().find { it.serverName == serverName }
            ?: throw ResponsiveException("Server $serverName not found")

        return serverConfig.getServerProperties()
    }

    fun changeServerProperty(serverName: String, key: String, value: String) {
        val serverConfig = listAllServers().find { it.serverName == serverName }
            ?: throw ResponsiveException("Server $serverName not found")

        val properties = serverConfig.getServerProperties().toMutableMap()
        properties[key] = value
        serverConfig.saveServerProperties(properties)
    }

    override fun onServerStarted(serverName: String) {
        serverEventCenter.publishServerEvent(ServerStartedEvent(serverName))
    }

    override fun onServerStopped(serverName: String) {
        serverEventCenter.publishServerEvent(ServerStoppedEvent(serverName))
        synchronized(serverContainers) {
            serverContainers.remove(serverName)
        }
    }

    override fun onOutputLine(serverName: String, line: String) {
        // 匹配玩家发言的格式，包括尖括号
        run {
            val messageRegex = Regex(""".*<(.+)> (.+)""")
            val matchResult = messageRegex.find(line)
            if (matchResult != null) {
                val playerName = matchResult.groupValues[1]
                val messageContent = matchResult.groupValues[2]
                logger.debug("On Server $serverName Player $playerName sent message: $messageContent")
                serverEventCenter.publishServerEvent(PlayerMessageEvent(serverName, playerName, messageContent))
            }
        }

        // 匹配玩家加入
        run {
            val joinRegex = Regex(""".*: (.*) joined the game""")
            val matchResult = joinRegex.find(line)
            if (matchResult != null) {
                val playerName = matchResult.groupValues[1]
                logger.debug("Player $playerName joined the game on server $serverName")
                serverEventCenter.publishServerEvent(PlayerJoinEvent(serverName, playerName))
            }
        }

        // 匹配玩家退出
        run {
            val joinRegex = Regex(""".*: (.*) left the game""")
            val matchResult = joinRegex.find(line)
            if (matchResult != null) {
                val playerName = matchResult.groupValues[1]
                logger.debug("Player $playerName left the game on server $serverName")
                serverEventCenter.publishServerEvent(PLayerLeaveEvent(serverName, playerName))
            }
        }

        logger.debug("[Info][Server-$serverName] $line")
    }

    override fun onErrorLine(serverName: String, line: String) {
        logger.debug("[Error][Server-$serverName] $line")
    }
}