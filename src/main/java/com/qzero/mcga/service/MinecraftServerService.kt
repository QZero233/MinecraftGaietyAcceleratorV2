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
import com.qzero.mcga.minecraft.container.EmbedMinecraftServerContainer
import com.qzero.mcga.minecraft.MinecraftServerEventListener
import com.qzero.mcga.minecraft.container.IMinecraftServerContainer
import com.qzero.mcga.minecraft.container.daemon.DaemonMinecraftServerContainer
import org.glavo.rcon.Rcon
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
    private val serverContainers: MutableMap<String, IMinecraftServerContainer> = mutableMapOf()

    /**
     * 获取指定名称的服务器容器实例，若不存在则初始化一个新的实例并返回。
     */
    private fun getContainerAndInitIfMissing(serverName: String): IMinecraftServerContainer {
        val serverConfig = listAllServers().find { it.serverName == serverName }
            ?: throw ResponsiveException("Server $serverName not found")

        synchronized(serverContainers) {
            val container: IMinecraftServerContainer
            if (!serverContainers.containsKey(serverName)) {
                container = if (serverConfig.daemonPort != 0) {
                    val c = DaemonMinecraftServerContainer(runtimeConfig, serverConfig, this)
                    Thread.sleep(1000) // 等待1秒确保TCP连接完成
                    c
                } else {
                    EmbedMinecraftServerContainer(runtimeConfig, serverConfig, this)
                }
                serverContainers[serverName] = container
            } else {
                container = serverContainers[serverName]!!
            }

            return container
        }
    }

    /**
     * 扫描当前工作目录下的子文件夹，识别包含 server.properties 的文件夹为一个服务器实例。
     * 配置项说明（来自 server.properties）：
     * - mcga-enabled: 是否被 mcga 管理，缺省为 true，若为 false 则跳过该文件夹。
     * - server-name: 可选，若未配置则使用文件夹名作为服务器名。
     * - server-jar-file: 必须，指定用于启动的 JAR 文件名（相对 serverDir）。
     * - jvm-params: 可选，启动时传递给 JVM 的参数字符串。
     * - backup-dir: 可选，指定备份目录（可以是绝对路径或相对于 serverDir 的相对路径），
     *   若未配置则默认使用 serverDir 下的 backups 目录。
     *
     * 若发现重复的 serverName 将抛出 ResponsiveException。
     */
    fun listAllServers(): List<MinecraftServerConfig> {
        // 扫描当前文件夹下所有的子文件夹，如果包含server.properties，就认为是一个服务器
        // 其中的server-name是服务器名，如果没有这一项，就默认服务器名为子文件夹名字
        // 其中的server-jar-file为jar文件名
        // 其中的jvm-params为启动时Java虚拟机参数
        // 其中的backup-dir为地图备份路径（如果为空默认是服务器目录的backups文件夹）
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

                // 解析 backup-dir（可以是相对路径或绝对路径），缺省为 folder/backups
                val backupDirProp = props.getProperty("backup-dir", "").trim()
                val backupDir = if (backupDirProp.isNotEmpty()) {
                    val candidate = File(backupDirProp)
                    if (candidate.isAbsolute) candidate else File(folder, backupDirProp)
                } else {
                    File(folder, "backups")
                }

                // 解析daemonPort（可选，没有或者为0就代表不使用daemon）
                val daemonPort = props.getProperty("daemon-port", "0").toIntOrNull() ?: 0

                // 检查服务器名称是否重复
                if (!serverNames.add(serverName)) {
                    throw ResponsiveException("Duplicate server name detected: $serverName")
                }

                servers.add(
                    MinecraftServerConfig(
                        serverName = serverName,
                        serverDir = folder,
                        serverJarFileName = serverJarFileName,
                        serverJvmParameters = serverJvmParameters,
                        backupDir = backupDir,
                        daemonPort = daemonPort
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
        return getContainerAndInitIfMissing(serverName).isServerRunning()
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
        val container = getContainerAndInitIfMissing(serverName)
        if (container.isServerRunning()) {
            throw ResponsiveException("Server $serverName is already running")
        }

        container.startServer()
        waitForServerState(container, true)
    }

    /**
     * 发起停止指定服务器的操作。
     * 行为与异常：
     * - 如果服务器未在运行，抛出 ResponsiveException("Server X is not running")。
     */
    fun stopServer(serverName: String) {
        val container = getContainerAndInitIfMissing(serverName)

        container.stopServer()
        waitForServerState(container, false)
    }

    // 10秒内每0.2秒检查一次，状态正确了再返回，否则就抛出异常
    private fun waitForServerState(container: IMinecraftServerContainer, desiredRunningState: Boolean) {
        val maxWaitTimeMs = 10_000L
        val checkIntervalMs = 200L
        var waitedTime = 0L

        while (container.isServerRunning() != desiredRunningState && waitedTime < maxWaitTimeMs) {
            Thread.sleep(checkIntervalMs)
            waitedTime += checkIntervalMs
        }

        if (container.isServerRunning() != desiredRunningState) {
            throw ResponsiveException("Failed to change server state within timeout, desired running state: $desiredRunningState, server name : ${container.serverConfig.serverName}")
        }
    }

    fun sendCommand(serverName: String, command: String) {
        val container = getContainerAndInitIfMissing(serverName)

        container.sendCommand(command)
    }

    /**
     * 读取服务器配置，如果rcon开启了，那么就使用配置里的RCON端口和密码访问，并发送指令，然后返回执行结果
     */
    fun sendCommandRCON(serverName: String, command: String): String {
        val serverConfig = listAllServers().find { it.serverName == serverName }
            ?: throw ResponsiveException("Server $serverName not found")

        val container = getContainerAndInitIfMissing(serverName)
        if (!container.isServerRunning()) {
            throw ResponsiveException("Server $serverName is not running")
        }

        val properties = serverConfig.getServerProperties()
        val rconEnabled = properties["enable-rcon"]?.toBoolean() ?: false
        if (!rconEnabled) {
            throw ResponsiveException("RCON is not enabled on server $serverName")
        }

        val rconPort = properties["rcon.port"]?.toIntOrNull()
            ?: throw ResponsiveException("RCON port is not configured properly on server $serverName")
        val rconPassword = properties["rcon.password"]
            ?: throw ResponsiveException("RCON password is not configured on server $serverName")

        Rcon("localhost", rconPort, rconPassword).use {
            return it.command(command)
        }
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


    override fun onServerStopped(serverName: String) {
        serverEventCenter.publishServerEvent(ServerStoppedEvent(serverName))
    }

    override fun onOutputLine(serverName: String, line: String) {
        // 匹配服务器启动
        run {
            if (line.contains(Regex("""Done \(\d+\.\d+s\)\! For help, type "help""""))) {
                logger.info("Server $serverName has started")
                serverEventCenter.publishServerEvent(ServerStartedEvent(serverName))
            }
        }

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

    fun sendChatMessage(serverName: String, messageLines: List<String>) {
        val container = getContainerAndInitIfMissing(serverName)
        messageLines.forEach {
            container.sendCommand("say $it")
        }
    }

    fun reloadServerContainer(serverName: String) {
        synchronized(serverContainers) {
            if (serverContainers.containsKey(serverName)) {
                if (serverContainers[serverName]!!.isServerRunning() && serverContainers[serverName] is EmbedMinecraftServerContainer) {
                    throw ResponsiveException("Cannot reload a running embed server: $serverName")
                } else {
                    serverContainers.remove(serverName)
                }
            }
        }

        getContainerAndInitIfMissing(serverName)
    }

    /**
     * 从地图备份目录加载地图
     * 地图会加载到一个与当前level-name不重名的文件夹，并不会对现有的存档造成影响
     * 加载完成后会修改server.properties中的level-name为新加载的地图名称
     *
     * mapName是一个zip压缩包
     */
    fun loadMap(serverName: String, mapName: String) {
        val container = getContainerAndInitIfMissing(serverName)
        if (container.isServerRunning()) {
            throw ResponsiveException("Cannot load map while server is running: $serverName")
        }

        val serverConfig = listAllServers().find { it.serverName == serverName }
            ?: throw ResponsiveException("Server $serverName not found")

        val backupDir = serverConfig.backupDir
        val mapFile = File(backupDir, mapName)
        if (!mapFile.exists() || !mapFile.isFile) {
            throw ResponsiveException("Map file not found: $mapName")
        }

        // 解压缩地图文件到一个新的文件夹，文件夹名称为mapName去掉.zip
        var newMapDirName = mapName.removeSuffix(".zip")
        val newMapDir = File(serverConfig.serverDir, newMapDirName)
        if (newMapDir.exists()) {
            // 如果有冲突就换个名字，直到没有冲突为止
            var index = 1
            while (true) {
                val candidateName = "${newMapDirName}_$index"
                val candidateDir = File(serverConfig.serverDir, candidateName)
                if (!candidateDir.exists()) {
                    newMapDirName = candidateName
                    break
                }
                index++
            }
        }
        logger.info("Extracting map $mapName to ${newMapDir.path} ...")

        try {
            unzip(mapFile, newMapDir)
        } catch (e: Exception) {
            logger.error("Failed to extract map file: ${e.message}", e)
            throw ResponsiveException("Failed to extract map file: ${e.message}")
        }

        // 检查一下，如果文件夹里面没有level.dat，并且还有子文件夹，那么就把字文件夹复制出来，直到指定文件夹里有level.dat为止
        fun containsLevelDat(dir: File): Boolean {
            return File(dir, "level.dat").exists()
        }
        var currentDir = newMapDir
        while (!containsLevelDat(currentDir)) {
            val subDirs = currentDir.listFiles()?.filter { it.isDirectory } ?: break
            if (subDirs.size == 1) {
                currentDir = subDirs[0]
            } else {
                break
            }
        }
        logger.info("Final map directory determined to be: ${currentDir.path}")
        if (currentDir != newMapDir) {
            // 把currentDir的内容复制到newMapDir，然后删除currentDir
            currentDir.listFiles()?.forEach { file ->
                val destFile = File(newMapDir, file.name)
                if (file.isDirectory) {
                    file.copyRecursively(destFile, true)
                } else {
                    file.copyTo(destFile, true)
                }
            }
            currentDir.deleteRecursively()
        }
        logger.info("Map $mapName moved to ${newMapDir.path}")

        // 修改server.properties中的level-name
        val properties = serverConfig.getServerProperties().toMutableMap()
        properties["level-name"] = newMapDirName
        serverConfig.saveServerProperties(properties)

        reloadServerContainer(serverName)
    }

    private fun unzip(zipFile: File, targetDir: File) {
        java.util.zip.ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val entryDest = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    entryDest.mkdirs()
                } else {
                    entryDest.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        entryDest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}