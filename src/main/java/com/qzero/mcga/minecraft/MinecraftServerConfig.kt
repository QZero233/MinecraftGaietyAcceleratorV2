package com.qzero.mcga.minecraft

import com.qzero.mcga.config.RuntimeConfig
import com.qzero.mcga.exception.ResponsiveException
import java.io.File
import java.util.Properties

class MinecraftServerConfig(
    // Name可以在Properties里配置，如果没有就默认是文件夹名字
    val serverName: String,
    val serverDir: File,
    val serverJarFileName: String,
    val serverJvmParameters: String,
    // backupDir: 指定备份文件夹的路径（如果在 server.properties 中配置了 backup-dir，则使用该路径；
    // 否则由上层构造时提供默认值，例如 serverDir 下的 backups 目录）
    val backupDir: File
) {

    fun genStartCommand(runtimeConfig: RuntimeConfig): String {
        return "${runtimeConfig.javaPath} $serverJvmParameters -jar $serverJarFileName nogui"
    }

    fun getServerProperties(): Map<String, String> {
        val propertiesFile = File(serverDir, "server.properties")
        if (!propertiesFile.exists()) {
            throw ResponsiveException("server.properties file does not exist in server directory: ${serverDir.path}")
        }

        val props = Properties()
        props.load(propertiesFile.inputStream())
        return props.entries.associate { it.key.toString() to it.value.toString() }
    }

    fun saveServerProperties(properties: Map<String, String>) {
        val propertiesFile = File(serverDir, "server.properties")
        val props = Properties()
        props.putAll(properties)
        propertiesFile.outputStream().use { props.store(it, null) }
    }

    override fun toString(): String {
        return "MinecraftServerConfig(serverName='$serverName', serverDir=$serverDir, serverJarFileName='$serverJarFileName', serverJvmParameters='$serverJvmParameters', backupDir=$backupDir)"
    }


}