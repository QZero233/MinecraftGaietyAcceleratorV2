package com.qzero.mcga.controller

import com.qzero.mcga.ActionResult
import com.qzero.mcga.service.ServerOverheadService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ServerOverheadController(
    private val serverOverheadService: ServerOverheadService
) {

    /**
     * 获取当前系统负载快照，返回包含 CPU、内存、磁盘、网络、IO 等信息的详细结构。
     * 路径：/stat/overhead
     * 方法：GET
     *
     * 返回说明（JSON结构位于 ActionResult.data.overhead）：
     * - timestamp: Long，时间戳（毫秒）
     * - cpu: Object，CPU 相关
     *     - systemLoad: Double，总体 CPU 使用率（百分比，0-100）
     *     - perCore: Array[Double]，每个逻辑核心的使用率（百分比）
     *     - logicalCount: Int，逻辑核心数
     *     - physicalCount: Int，物理核心数
     * - memory: Object，内存相关
     *     - totalBytes: Long，总内存字节数
     *     - availableBytes: Long，可用内存字节数
     *     - usedBytes: Long，已用内存字节数（total - available）
     *     - usedPercent: Double，内存使用百分比（0-100）
     *     - swapTotalBytes: Long，交换分区/文件总字节数
     *     - swapUsedBytes: Long，已用交换字节数
     *     - swapUsedPercent: Double，交换使用百分比（0-100）
     * - loadAverages: Array[Double]，系统负载均值（1,5,15 分钟）；若平台不支持，某些值可能为 NaN（注意：部分 JVM/平台仅返回 1 分钟值）
     * - uptimeSeconds: Long，系统/进程运行时长（秒，基于 JVM 启动时间）
     * - processCount: Int，进程数量（操作系统报告）
     * - threadCount: Int，线程数量（操作系统报告）
     * - fileStores: Array[Object]，文件系统挂载点列表，每项包含：
     *     - name: String，挂载名或设备名
     *     - mount: String，挂载点路径
     *     - type: String，文件系统类型（如 ext4、ntfs）
     *     - totalBytes: Long，分区总大小
     *     - usableBytes: Long，可用字节数
     *     - usedBytes: Long，已用字节数
     *     - usedPercent: Double，已用比例（0-1）
     * - disks: Array[Object]，物理磁盘统计（基于 OSHI diskStores），每项包含：
     *     - name: String，磁盘名称
     *     - model: String，磁盘型号（若有）
     *     - serial: String，磁盘序列号（若有）
     *     - sizeBytes: Long，磁盘容量字节数
     *     - readsPerSec: Double，读操作次数每秒
     *     - writesPerSec: Double，写操作次数每秒
     *     - readBytesPerSec: Double，读字节速率（B/s）
     *     - writeBytesPerSec: Double，写字节速率（B/s）
     *     注意：readsPerSec 等为基于前一次接口调用的差值除以时间间隔计算，第一次调用可能为 0。
     * - networkInterfaces: Array[Object]，网卡统计，每项包含：
     *     - name: String，接口名
     *     - displayName: String，可读名称
     *     - ifType: Int，接口类型编号（操作系统/OSHI 提供）
     *     - mac: String，MAC 地址（若可用）
     *     - recvBytesPerSec: Double，接收字节速率（B/s）
     *     - sentBytesPerSec: Double，发送字节速率（B/s）
     *     - recvPktsPerSec: Double，接收包速率（pkt/s）
     *     - sentPktsPerSec: Double，发送包速率（pkt/s）
     *
     * 注意事项：
     * - 磁盘与网络的速率项依赖于服务上次采样快照，首次调用可能不会立即反映真实速率；建议客户端以间隔方式轮询（例如每秒或每几秒）以获得稳定速率。
     * - 在某些受限环境（容器、受限权限）或平台上，某些字段可能不可用或返回 NaN/0。
     */
    @GetMapping("/stat/overhead")
    fun getOverhead(): ActionResult {
        val data = serverOverheadService.getSystemOverhead()
        return ActionResult("overhead", data)
    }

}