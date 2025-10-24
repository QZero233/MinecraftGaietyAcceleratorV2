package com.qzero.mcga.controller

import com.qzero.mcga.ActionResult
import com.qzero.mcga.ResponseCodeList
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

@RestController
class ChestInfoController {

    /**
     * 接口名称：查看指定区域内的容器物品信息
     * 接口路径：/chest_info
     * 请求方法：GET
     *
     * 请求参数：指定区域的角点坐标（注意：角点坐标是包含在区域内的）
     * - x1
     * - y1
     * - z1
     * - x2
     * - y2
     * - z2
     *
     * 响应：ActionResult，其中data部分的定义为：
     * 字段定义（逐项说明）
     * 根对象（root）
     * 类型：Object
     * 说明：顶层 JSON 对象，包含区域信息、合并统计和每个容器明细（或 error 字段，见错误响应部分）。
     * region
     * 类型：Object
     * 必需：是（在成功响应时）
     * 结构：
     * from: Array[3]（整数） — 最小坐标 [x_min, y_min, z_min]（包含端点）
     * to: Array[3]（整数） — 最大坐标 [x_max, y_max, z_max]（包含端点）
     * 说明：表示请求中两个坐标规范化后的包围盒（从小角到大角）。用于让调用方确认服务端所扫描的范围。
     * totalItems
     * 类型：Object（map）
     * 必需：是（若范围内无容器则为空对象 {}）
     * 键（key）：字符串 — item 的注册 ID（Registry ID），例如 "minecraft:iron_ingot"。
     * 值（value）：Object，包含：
     * count: Integer — 在整个指定区域内该物品的总数量（已合并多个箱子）。例如 15。
     * displayName: String — 该物品的显示名称（本地化的 stack.name.string），用于展示中文名称；如果无法获取则回退为 item id。
     * 说明：按 item id 汇总的总计结果，便于机器解析并保留人类可读名称。
     * containers
     * 类型：Array[Object]
     * 必需：是（若范围内无容器则返回空数组 []）
     * 每个元素（单个容器）结构：
     * pos: Array[3]（整数） — 容器方块的坐标 [x, y, z]。
     * items: Object（map） — 类似于 totalItems 的映射，键为 item id，值为：
     * count: Integer — 该容器内该物品的数量（合并该格位或多个格位数量）。
     * displayName: String — 本地化显示名（与 totalItems 的 displayName 对应）。
     * 说明：按容器（箱子）逐个列出位置与内部物品明细，便于定位和进一步处理。
     * 错误响应格式（统一 JSON）
     * 当发生错误或请求无效时返回的 JSON（示例）：
     * 格式：{ "error": "<错误信息文本>" }
     * 说明：错误信息应为人类可读字符串，调用方应根据 presence of "error" 判断请求是否成功。示例错误：
     * {"error":"empty input"}
     * {"error":"coordinates must be integers"}
     * {"error":"region too large: 250000 blocks (limit 200000)"}
     * {"error":"timed out or failed to query server: <message>"}</message>
     * {"error":"internal server error"}
     */
    @GetMapping("/chest_info")
    fun getChestInfo(
        @RequestParam("x1") x1Str: String?,
        @RequestParam("y1") y1Str: String?,
        @RequestParam("z1") z1Str: String?,
        @RequestParam("x2") x2Str: String?,
        @RequestParam("y2") y2Str: String?,
        @RequestParam("z2") z2Str: String?
    ): ActionResult {
        // Validate presence
        if (x1Str.isNullOrBlank() || y1Str.isNullOrBlank() || z1Str.isNullOrBlank()
            || x2Str.isNullOrBlank() || y2Str.isNullOrBlank() || z2Str.isNullOrBlank()
        ) {
            val errMap = mapOf("error" to "empty input")
            return ActionResult(ResponseCodeList.KNOWN_ERROR, "empty input", errMap)
        }

        // Parse ints
        val coords = try {
            intArrayOf(
                x1Str.toInt(), y1Str.toInt(), z1Str.toInt(),
                x2Str.toInt(), y2Str.toInt(), z2Str.toInt()
            )
        } catch (e: NumberFormatException) {
            val errMap = mapOf("error" to "coordinates must be integers")
            return ActionResult(ResponseCodeList.KNOWN_ERROR, "coordinates must be integers", errMap)
        }

        // Compose line to send
        val toSend = "${coords[0]} ${coords[1]} ${coords[2]} ${coords[3]} ${coords[4]} ${coords[5]}\n"

        // Connect to localhost:7880 and exchange data
        try {
            Socket("localhost", 7880).use { socket ->
                socket.soTimeout = 30_000 // 30s timeout for read

                // write WITHOUT closing the output stream yet. We'll shutdownOutput() to signal EOF
                val osw = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                osw.write(toSend)
                osw.flush()

                // read entire response until EOF (this will consume any buffered data even if remote closes)
                val sb = StringBuilder()
                BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.append(line)
                        sb.append('\n')
                        line = reader.readLine()
                    }
                }

                // close writer after we've finished reading to avoid closing the socket prematurely
                try {
                    osw.close()
                } catch (ignored: Exception) {}

                val raw = sb.toString().trim()
                if (raw.isEmpty()) {
                    val errMap = mapOf("error" to "timed out or failed to query server: empty response")
                    return ActionResult(ResponseCodeList.KNOWN_ERROR, "timed out or failed to query server: empty response", errMap)
                }

                // Parse JSON using fastjson2
                val jsonObj: JSONObject = try {
                    // JSON.parseObject returns a JSONObject
                    JSON.parseObject(raw)
                } catch (e: Exception) {
                    val errMap = mapOf("error" to "timed out or failed to query server: invalid json: ${e.message}")
                    return ActionResult(ResponseCodeList.KNOWN_ERROR, "timed out or failed to query server: invalid json", errMap)
                }

                // Return success with data set to parsed JSONObject (as a Map)
                val resultMap = java.util.HashMap<String, Any?>()
                // copy entries to a java HashMap to match ActionResult(Map) constructor expectations
                for ((k, v) in jsonObj) {
                    resultMap[k] = v
                }

                println(resultMap)
                return ActionResult(resultMap)
            }
        } catch (e: Exception) {
            val errMap = mapOf("error" to "timed out or failed to query server: ${e.message}")
            return ActionResult(ResponseCodeList.KNOWN_ERROR, "timed out or failed to query server", errMap)
        }
    }

}