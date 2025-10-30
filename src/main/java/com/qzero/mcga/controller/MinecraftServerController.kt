package com.qzero.mcga.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

import com.qzero.mcga.ActionResult
import com.qzero.mcga.service.MinecraftServerService

@RestController
class MinecraftServerController(
    private val minecraftServerService: MinecraftServerService
) {

    /**
     * 接口名称：列出所有服务器
     * 接口路径：/server/
     * 请求方法：GET
     *
     * 请求参数：无
     *
     * 响应：ActionResult，其中data如下：
     * - servers: List<Server>，服务器列表
     *
     * 其中，Server对象包含以下字段（字段名与返回JSON严格对应）：
     * serverName: String，服务器名称
     * serverStatus: String，服务器状态，取值："running" 或 "stopped"（注意字段名为 serverStatus）
     * serverJarName: String，服务器的JAR文件名
     * serverJvmParams: String，服务器的JVM参数
     */
    @GetMapping("/server/")
    fun listServers(): ActionResult {
        val servers = minecraftServerService.listAllServers()

        val serverList = servers.map { cfg ->
            mapOf(
                "serverName" to cfg.serverName,
                "serverStatus" to if (minecraftServerService.isServerRunning(cfg.serverName)) "running" else "stopped",
                "serverJarName" to cfg.serverJarFileName,
                "serverJvmParams" to cfg.serverJvmParameters
            )
        }

        return ActionResult("servers", serverList)
    }

    /**
     * 接口名称：启动指定服务器
     * 接口路径：/server/{serverName}/start
     * 请求方法：POST
     *
     * 请求参数：
     * - path variable: serverName - 要启动的服务器名称（String）
     *
     * 响应：ActionResult
     * - 本接口不在返回体中包含 data 部分（即 data 字段不传），仅通过 ActionResult 的 responseCode 表明请求是否成功。
     *
     * 行为说明：
     * - 本接口会调用 Service 层的 startServer 方法。Service 层负责校验服务器是否存在、是否已在运行并真正发起启动流程。
     * - 如果服务器不存在或已在运行，Service 会抛出异常（会由全局异常处理器转换为合适的 HTTP 响应）。
     * - 启动为异步/同步取决于 Service 的实现；本接口仅负责发起启动请求。
     */
    @PostMapping("/server/{serverName}/start")
    fun startServer(@PathVariable serverName: String): ActionResult {
        // 调用 Service 发起启动
        minecraftServerService.startServer(serverName)

        // 启动请求已发出，不返回 data，仅返回成功状态
        return ActionResult(true)
    }

    /**
     * 接口名称：停止指定服务器
     * 接口路径：/server/{serverName}/stop
     * 请求方法：POST
     *
     * 请求参数：
     * - path variable: serverName - 要停止的服务器名称（String）
     *
     * 响应：ActionResult
     * - 本接口不在返回体中包含 data 部分（即 data 字段不传），仅通过 ActionResult 的 responseCode 表明请求是否成功。
     *
     * 行为说明：
     * - 本接口会调用 Service 层的 stopServer 方法。Service 层负责校验服务器是否存在并真正发起停止流程。
     * - 如果服务器不存在或未在运行，Service 会抛出异常（会由全局异常处理器转换为合适的 HTTP 响应）。
     * - 停止为异步/同步取决于 Service 的实现；本接口仅负责发起停止请求。
     */
    @PostMapping("/server/{serverName}/stop")
    fun stopServer(@PathVariable serverName: String): ActionResult {
        // 调用 Service 发起停止
        minecraftServerService.stopServer(serverName)

        // 停止请求已发出，不返回 data，仅返回成功状态
        return ActionResult(true)
    }

    /**
     * 接口名称：向指定服务器发送控制台命令
     * 接口路径：/server/{serverName}/command
     * 请求方法：POST
     *
     * 请求参数：
     * - path variable: serverName - 目标服务器名称（String）
     * - request param: command - 要发送的控制台命令文本（String）
     *
     * 响应：ActionResult
     * - 本接口不在返回体中包含 data 部分（即 data 字段不传），仅通过 ActionResult 的 responseCode 表明请求是否成功。
     *
     * 行为说明：
     * - 本接口会调用 Service 层的 sendCommand 方法，Service 层负责校验服务器是否存在且正在运行并真正将命令发送到服务器。
     * - 如果服务器未在运行或不存在，Service 会抛出异常（由全局异常处理器转换为合适的 HTTP 响应）。
     */
    @PostMapping("/server/{serverName}/command")
    fun sendCommand(
        @PathVariable serverName: String,
        @RequestParam command: String
    ): ActionResult {
        // 调用 Service 发出命令
        minecraftServerService.sendCommand(serverName, command)

        // 命令已发出，不返回 data，仅返回成功状态
        return ActionResult(true)
    }

    /**
     * 接口名称：列出指定服务器的属性（server.properties）
     * 接口路径：/server/{serverName}/properties
     * 请求方法：GET
     *
     * 请求参数：
     * - path variable: serverName - 目标服务器名称（String）
     *
     * 响应：ActionResult，其中data如下：
     * - properties: Map<String, String>，服务器的 properties 键值对集合
     *
     * 行为说明：
     * - 本接口会调用 Service 层的 listAllProperties 方法，Service 层负责校验服务器配置是否存在并读取 server.properties 内容。
     * - 读取异常或服务器未找到将抛出 ResponsiveException（由全局异常处理器处理）。
     */
    @GetMapping("/server/{serverName}/properties")
    fun listProperties(@PathVariable serverName: String): ActionResult {
        val properties = minecraftServerService.listAllProperties(serverName)
        return ActionResult("properties", properties)
    }

    /**
     * 接口名称：更新指定服务器的单个属性（写入 server.properties）
     * 接口路径：/server/{serverName}/property
     * 请求方法：POST
     *
     * 请求参数：
     * - path variable: serverName - 目标服务器名称（String）
     * - request param: key - 要更新的属性键（String）
     * - request param: value - 要设置的属性值（String）
     *
     * 响应：ActionResult
     * - 本接口不在返回体中包含 data 部分（即 data 字段不传），仅通过 ActionResult 的 responseCode 表明请求是否成功。
     *
     * 行为说明：
     * - 本接口会调用 Service 层的 changeServerProperty 方法，Service 层负责校验服务器配置是否存在并写入到 server.properties。
     * - 写入失败或服务器未找到将抛出 ResponsiveException（由全局异常处理器处理）。
     */
    @PostMapping("/server/{serverName}/property")
    fun updateProperty(
        @PathVariable serverName: String,
        @RequestParam key: String,
        @RequestParam value: String
    ): ActionResult {
        // 调用 Service 更新属性
        minecraftServerService.changeServerProperty(serverName, key, value)

        // 更新已发出且完成，不返回 data，仅返回成功状态
        return ActionResult(true)
    }

    /**
     * 接口名称：重新加载服务器容器
     * 接口路径：/server/{serverName}/reload
     * 请求方法：POST
     *
     * 请求参数：无
     *
     * 响应：ActionResult
     * - 本接口不在返回体中包含 data 部分（即 data 字段不传），仅通过 ActionResult 的 responseCode 表明请求是否成功。
     */
    @PostMapping("/server/{serverName}/reload")
    fun reloadServerContainer(@PathVariable serverName: String): ActionResult {
        // 调用 Service 重新加载服务器容器
        minecraftServerService.reloadServerContainer(serverName)

        // 重新加载请求已发出且完成，不返回 data，仅返回成功状态
        return ActionResult(true)
    }

}