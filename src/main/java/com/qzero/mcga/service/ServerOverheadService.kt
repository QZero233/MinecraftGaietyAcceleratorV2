package com.qzero.mcga.service

import org.springframework.stereotype.Service
import oshi.SystemInfo
import java.lang.management.ManagementFactory

@Service
class ServerOverheadService {

    private val systemInfo = SystemInfo()
    private val processor = systemInfo.hardware.processor
    private val memory = systemInfo.hardware.memory

    // previous ticks/snapshots used to compute rates between calls
    @Volatile
    private var prevCpuTicks: LongArray = processor.systemCpuLoadTicks
    @Volatile
    private var prevProcTicks: Array<LongArray> = processor.processorCpuLoadTicks
    @Volatile
    private var prevDiskSnapshot: Map<String, DiskSnapshot> = emptyMap()
    @Volatile
    private var prevNetSnapshot: Map<String, NetSnapshot> = emptyMap()
    @Volatile
    private var prevSampleTimeMs: Long = System.currentTimeMillis()

    private data class DiskSnapshot(
        val reads: Long,
        val writes: Long,
        val readBytes: Long,
        val writeBytes: Long
    )

    private data class NetSnapshot(
        val bytesRecv: Long,
        val bytesSent: Long,
        val packetsRecv: Long,
        val packetsSent: Long
    )

    /**
     * Collect a comprehensive snapshot of system overhead metrics.
     * Returns a Map suitable for serializing to JSON and embedding inside ActionResult.
     */
    fun getSystemOverhead(): Map<String, Any> {
        val nowMs = System.currentTimeMillis()
        val elapsedSec = (nowMs - prevSampleTimeMs).coerceAtLeast(1L) / 1000.0 // avoid div by zero

        val hw = systemInfo.hardware
        val os = systemInfo.operatingSystem

        // CPU
        val currentCpuTicks = processor.systemCpuLoadTicks
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevCpuTicks).coerceIn(0.0, 1.0)

        // Per-core
        val currentProcTicks = processor.processorCpuLoadTicks
        val perCoreLoads = processor.getProcessorCpuLoadBetweenTicks(prevProcTicks).map { it.coerceIn(0.0, 1.0) }

        // Update prev ticks for next call
        prevCpuTicks = currentCpuTicks
        prevProcTicks = currentProcTicks

        // Memory
        val totalMem = memory.total
        val availableMem = memory.available
        val usedMem = totalMem - availableMem
        val memUsedPercent = if (totalMem > 0) usedMem.toDouble() / totalMem.toDouble() else 0.0

        val virtualMem = memory.virtualMemory
        val swapTotal = virtualMem.swapTotal
        val swapUsed = virtualMem.swapUsed
        val swapUsedPercent = if (swapTotal > 0) swapUsed.toDouble() / swapTotal.toDouble() else 0.0

        // Load averages (1,5,15) - use JVM OS bean for 1-min and fallback to NaN for others
        val loadAverages = try {
            val one = ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
            listOf(one, Double.NaN, Double.NaN)
        } catch (_: Exception) {
            listOf(Double.NaN, Double.NaN, Double.NaN)
        }

        // Uptime (seconds) using JVM RuntimeMXBean start time
        val runtime = ManagementFactory.getRuntimeMXBean()
        val uptimeSec = (System.currentTimeMillis() - runtime.startTime) / 1000

        // Processes / threads
        val processCount = os.processCount
        val threadCount = os.threadCount

        // Disks: usage per file store and IO rates per physical disk
        val fs = os.fileSystem
        val fileStores = fs.fileStores.map { store ->
            try {
                mapOf(
                    "name" to store.name,
                    "mount" to store.mount,
                    "type" to store.type,
                    "totalBytes" to store.totalSpace,
                    "usableBytes" to store.usableSpace,
                    "usedBytes" to (store.totalSpace - store.usableSpace),
                    "usedPercent" to (if (store.totalSpace > 0) (store.totalSpace - store.usableSpace).toDouble() / store.totalSpace.toDouble() else 0.0)
                )
            } catch (e: Exception) {
                mapOf("name" to store.name, "error" to e.message)
            }
        }

        val diskStores = hw.diskStores
        val diskStats = diskStores.map { ds ->
            // snapshot current
            val curReads = ds.reads
            val curWrites = ds.writes
            val curReadBytes = ds.readBytes
            val curWriteBytes = ds.writeBytes

            val prev = prevDiskSnapshot[ds.name]
            val readsPerSec = if (prev != null) (curReads - prev.reads) / elapsedSec else 0.0
            val writesPerSec = if (prev != null) (curWrites - prev.writes) / elapsedSec else 0.0
            val readBytesPerSec = if (prev != null) (curReadBytes - prev.readBytes) / elapsedSec else 0.0
            val writeBytesPerSec = if (prev != null) (curWriteBytes - prev.writeBytes) / elapsedSec else 0.0

            // update snapshot
            DiskStatResult(
                name = ds.name,
                model = ds.model ?: ds.name,
                serial = ds.serial ?: "",
                size = ds.size,
                readsPerSec = readsPerSec,
                writesPerSec = writesPerSec,
                readBytesPerSec = readBytesPerSec,
                writeBytesPerSec = writeBytesPerSec
            ) to DiskSnapshot(curReads, curWrites, curReadBytes, curWriteBytes)
        }

        // Persist disk snapshots
        prevDiskSnapshot = diskStats.associate { it.first.name to it.second }

        // Network: update attributes and compute per-second rates
        val netIfs = hw.networkIFs
        val netResults = mutableListOf<NetResult>()
        netIfs.forEach { net ->
            net.updateAttributes()
            val curRecv = net.bytesRecv
            val curSent = net.bytesSent
            val curPktsRecv = net.packetsRecv
            val curPktsSent = net.packetsSent

            val prev = prevNetSnapshot[net.name]
            val recvPerSec = if (prev != null) (curRecv - prev.bytesRecv) / elapsedSec else 0.0
            val sentPerSec = if (prev != null) (curSent - prev.bytesSent) / elapsedSec else 0.0
            val pktsRecvPerSec = if (prev != null) (curPktsRecv - prev.packetsRecv) / elapsedSec else 0.0
            val pktsSentPerSec = if (prev != null) (curPktsSent - prev.packetsSent) / elapsedSec else 0.0

            netResults.add(NetResult(net.name, net.displayName, net.ifType, net.macaddr ?: "", recvPerSec, sentPerSec, pktsRecvPerSec, pktsSentPerSec))
            // store snapshot
            prevNetSnapshot = prevNetSnapshot + (net.name to NetSnapshot(curRecv, curSent, curPktsRecv, curPktsSent))
        }

        // Prepare CPU per-core as percentages
        val perCorePercent = perCoreLoads.map { it * 100.0 }

        // Prepare diskStats map list
        val diskStatsMap = diskStats.map { it.first.toMap() }

        // Prepare network map list
        val netMapList = netResults.map { it.toMap() }

        // Update sample time
        prevSampleTimeMs = nowMs

        // Assemble result map
        val result = mutableMapOf<String, Any>()
        result["timestamp"] = nowMs
        result["cpu"] = mapOf(
            "systemLoad" to (cpuLoad * 100.0),
            "perCore" to perCorePercent,
            "logicalCount" to processor.logicalProcessorCount,
            "physicalCount" to processor.physicalProcessorCount
        )
        result["memory"] = mapOf(
            "totalBytes" to totalMem,
            "availableBytes" to availableMem,
            "usedBytes" to usedMem,
            "usedPercent" to memUsedPercent * 100.0,
            "swapTotalBytes" to swapTotal,
            "swapUsedBytes" to swapUsed,
            "swapUsedPercent" to swapUsedPercent * 100.0
        )
        result["loadAverages"] = loadAverages
        result["uptimeSeconds"] = uptimeSec
        result["processCount"] = processCount
        result["threadCount"] = threadCount
        result["fileStores"] = fileStores
        result["disks"] = diskStatsMap
        result["networkInterfaces"] = netMapList

        return result
    }

    // small helper data classes for conversion
    private data class DiskStatResult(
        val name: String,
        val model: String,
        val serial: String,
        val size: Long,
        val readsPerSec: Double,
        val writesPerSec: Double,
        val readBytesPerSec: Double,
        val writeBytesPerSec: Double
    ) {
        fun toMap(): Map<String, Any> = mapOf(
            "name" to name,
            "model" to model,
            "serial" to serial,
            "sizeBytes" to size,
            "readsPerSec" to readsPerSec,
            "writesPerSec" to writesPerSec,
            "readBytesPerSec" to readBytesPerSec,
            "writeBytesPerSec" to writeBytesPerSec
        )
    }

    private data class NetResult(
        val name: String,
        val displayName: String,
        val ifType: Int,
        val mac: String,
        val recvBytesPerSec: Double,
        val sentBytesPerSec: Double,
        val recvPktsPerSec: Double,
        val sentPktsPerSec: Double
    ) {
        fun toMap(): Map<String, Any> = mapOf(
            "name" to name,
            "displayName" to displayName,
            "ifType" to ifType,
            "mac" to mac,
            "recvBytesPerSec" to recvBytesPerSec,
            "sentBytesPerSec" to sentBytesPerSec,
            "recvPktsPerSec" to recvPktsPerSec,
            "sentPktsPerSec" to sentPktsPerSec
        )
    }

}