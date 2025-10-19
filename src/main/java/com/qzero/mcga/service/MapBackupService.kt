package com.qzero.mcga.service

import com.qzero.mcga.exception.ResponsiveException
import com.qzero.mcga.minecraft.MinecraftServerConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class MapBackupService {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun backupMap(serverConfig: MinecraftServerConfig, backupFile: File) {
        // 从serverConfig读取level-name，然后存档位置就是serverDir/level-name
        // 这个方法需要把这个存档文件夹打包到backupFile指定的zip压缩包
        logger.info("Start backup map for server: ${serverConfig.serverName} to file: ${backupFile.path}")

        val props = try {
            serverConfig.getServerProperties()
        } catch (e: Exception) {
            throw ResponsiveException("读取 server.properties 失败: ${e.message}")
        }

        val levelName = props["level-name"]?.takeIf { it.isNotBlank() } ?: "world"
        val worldDir = File(serverConfig.serverDir, levelName)
        logger.info("Backing up world directory: ${worldDir.path}")

        if (!worldDir.exists() || !worldDir.isDirectory) {
            throw ResponsiveException("存档目录不存在: ${worldDir.path}")
        }

        backupFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

        try {
            FileOutputStream(backupFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    ZipOutputStream(bos).use { zos ->
                        val basePath = serverConfig.serverDir.toPath()

                        fun addFileToZip(file: File) {
                            // 跳过正在写入的备份文件本身（以防 backupFile 在被打包目录内）
                            try {
                                if (backupFile.canonicalPath == file.canonicalPath) return
                            } catch (_: Exception) { /* ignore */ }

                            val relPath = basePath.relativize(file.toPath()).toString().replace(File.separatorChar, '/')
                            if (file.isDirectory) {
                                val dirEntryName = if (relPath.endsWith("/")) relPath else "$relPath/"
                                val entry = ZipEntry(dirEntryName)
                                entry.time = file.lastModified()
                                zos.putNextEntry(entry)
                                zos.closeEntry()
                                file.listFiles()?.forEach { child -> addFileToZip(child) }
                            } else {
                                val entry = ZipEntry(relPath)
                                entry.time = file.lastModified()
                                zos.putNextEntry(entry)
                                BufferedInputStream(FileInputStream(file)).use { bis ->
                                    val buffer = ByteArray(4096)
                                    var read = bis.read(buffer)
                                    while (read != -1) {
                                        zos.write(buffer, 0, read)
                                        read = bis.read(buffer)
                                    }
                                }
                                zos.closeEntry()
                            }
                        }

                        // 只打包 levelName 目录（相对于 serverDir）
                        addFileToZip(worldDir)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error during backup server name ${serverConfig.serverName} to ${backupFile.path}", e)
            throw ResponsiveException("打包存档失败: ${e.message}")
        }

        logger.info("Backup completed to file: ${backupFile.path}")
    }

}