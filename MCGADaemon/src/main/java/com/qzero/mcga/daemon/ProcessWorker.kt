package com.qzero.mcga.daemon

import java.io.File

object ProcessWorker {

    private val clientThreadList = mutableListOf<DaemonClientHandlerThread>()
    private var processThread: ProcessThread? = null
    private var heartBeatThread = object : Thread() {
        override fun run() {
            while (true) {
                val isProcessRunning = processThread != null && processThread!!.isProcessRunning()
                broadcastClients("${DaemonProtocolConstant.TYPE_REPORT_PROCESS_STATE}," +
                        if (isProcessRunning) DaemonProtocolConstant.PROCESS_STATE_RUNNING else DaemonProtocolConstant.PROCESS_STATE_IDLE
                )
                Thread.sleep(10 * 1000)
            }
        }
    }

    private var localStdIOThread = object : Thread() {
        override fun run() {
            val reader = System.`in`.bufferedReader()
            while (true) {
                val line = reader.readLine() ?: break
                processThread?.writeLine(line)
            }
        }
    }

    init {
        heartBeatThread.start()
        localStdIOThread.start()
    }

    fun registerClientThread(clientThread: DaemonClientHandlerThread) {
        println("Registering client thread from ${clientThread.getPort()}")
        clientThreadList.add(clientThread)
    }

    fun unregisterClientThread(clientThread: DaemonClientHandlerThread) {
        println("Unregistering client thread from ${clientThread.getPort()}")
        clientThreadList.remove(clientThread)
    }

    fun startProcess(cmd: List<String>, workDir: String) {
        synchronized(this) {
            if (processThread != null && processThread!!.isProcessRunning()) {
                println("Process is already running, can not start $cmd")
                return
            }

            processThread = ProcessThread(
                cmd,
                workDir,
                object : ProcessThread.ServerIOListener {
                    override fun onOutputLine(line: String) {
                        broadcastClients("${DaemonProtocolConstant.TYPE_RECEIVED_STD_IO},$line")
                    }

                    override fun onErrorLine(line: String) {
                        broadcastClients("${DaemonProtocolConstant.TYPE_RECEIVED_STD_IO},$line")
                    }

                    override fun onProcessStopped() {
                        broadcastClients("${DaemonProtocolConstant.TYPE_REPORT_PROCESS_STATE},${DaemonProtocolConstant.PROCESS_STATE_IDLE}")
                    }
                }
            )

            processThread!!.start()
            println("Started process: $cmd")
        }
    }

    fun writeLine(line: String) {
        println("ProcessWorker writing line to process: $line")
        processThread?.writeLine(line)
    }

    fun broadcastClients(line: String) {
        // 为了防止并发修改，这里复制一份列表
        val clientThreadList = synchronized(this.clientThreadList) {
            this.clientThreadList.toList()
        }
        clientThreadList.forEach {
            try {
                it.writeLine(line)
            } catch (_: Exception) { }
        }
    }

    fun isProcessRunning(): Boolean {
        return processThread != null && processThread!!.isProcessRunning()
    }

}

class ProcessThread(
    private val cmd: List<String>,
    private val workDir: String,
    private val ioListener: ServerIOListener
): Thread() {

    interface ServerIOListener {
        fun onOutputLine(line: String)
        fun onErrorLine(line: String)
        fun onProcessStopped()
    }

    private var process: Process? = null

    override fun run() {
        try {
            process = ProcessBuilder(cmd)
                .directory(File(workDir))
                .redirectErrorStream(true)
                .start()

            val inputReader = process!!.inputStream.bufferedReader()
            val errorReader = process!!.errorStream.bufferedReader()

            // 读取标准输出
            val outputThread = Thread {
                inputReader.useLines { lines ->
                    lines.forEach { line ->
                        println("[Process-${process?.pid()}-Info] $line")
                        ioListener.onOutputLine(line)
                    }
                }
            }

            // 读取错误输出
            val errorThread = Thread {
                errorReader.useLines { lines ->
                    lines.forEach { line ->
                        println("[Process-${process?.pid()}-Error] $line")
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
            ioListener.onProcessStopped()
        } catch (e: Exception) {
            println("Error running process: ${e.message}")
            e.printStackTrace()
            process = null
            ioListener.onProcessStopped()
        }
    }

    fun writeLine(line: String) {
        // 向服务器输入命令
        try {
            println("Writing to process ${process?.pid()}: $line")
            val writer = process!!.outputStream.bufferedWriter()
            writer.write(line)
            writer.newLine()
            writer.flush()
        } catch (e: Exception) {
            println("Error writing to process ${process?.pid()}: ${e.message}")
            e.printStackTrace()
        }
    }

    fun isProcessRunning(): Boolean {
        return process != null
    }
}