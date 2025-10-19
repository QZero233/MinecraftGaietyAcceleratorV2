package com.qzero.mcga.controller

import com.qzero.mcga.ActionResult
import com.qzero.mcga.exception.ResponsiveException
import com.qzero.mcga.service.MapBackupService
import com.qzero.mcga.service.MinecraftServerService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
class MapBackupController(
    private val serverService: MinecraftServerService,
    private val backupService: MapBackupService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 接口名称：备份指定服务器的地图存档
     * 接口路径：/server/{serverName}/backup
     * 请求方法：POST
     *
     * 请求参数：
     * - path variable: serverName - 目标服务器名称（String）
     * - request param (optional): fileName - 指定备份文件名（例如 mybackup.zip）。若不提供，
     *   系统会在服务器目录下的 `backups` 目录创建文件，命名为 {serverName}-{timestamp}.zip
     *
     * 响应：ActionResult
     * - 成功时仅返回表示成功的 ActionResult（不包含 data）
     * - 失败时通过抛出 ResponsiveException 由全局异常处理器转换为错误响应
     */
    @PostMapping("/server/{serverName}/backup")
    fun backupMap(
        @PathVariable serverName: String,
        @RequestParam(required = false) fileName: String?
    ): ActionResult {
        // 查找服务器配置（与 Service 内部查找方式保持一致）
        val serverConfig = serverService.listAllServers().find { it.serverName == serverName }
            ?: throw ResponsiveException("Server $serverName not found")

        // 决定备份文件的路径：如果客户端指定 fileName 则使用该名字（相对路径相对于 backupDir），
        // 否则使用 serverConfig.backupDir/{serverName}-{timestamp}.zip
        val backupFile = if (!fileName.isNullOrBlank()) {
            val f = File(fileName)
            if (f.isAbsolute) f else File(serverConfig.backupDir, fileName)
        } else {
            val backupsDir = serverConfig.backupDir
            if (!backupsDir.exists()) backupsDir.mkdirs()

            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            File(backupsDir, "${serverName}-$ts.zip")
        }

        logger.info("Request backup for server $serverName -> ${backupFile.path}")

        // 确保目标目录存在
        backupFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

        // 调用 Service 执行备份（会抛出 ResponsiveException 以供上层处理）
        backupService.backupMap(serverConfig, backupFile)

        // 不返回任何 data，仅返回成功状态
        return ActionResult(true)
    }

}