import requests
import json
from typing import Dict, Any, List, Optional
from mcp.server.fastmcp import FastMCP
import os
from datetime import datetime

# 初始化MCP服务器
mcp = FastMCP("ServerManagement")

class ServerManager:
    def __init__(self):
        self.base_url = os.getenv("SERVER_MANAGEMENT_BASE_URL", "")
        self.token = os.getenv("SERVER_MANAGEMENT_TOKEN", "")
        self.headers = {
            "Authorization": f"{self.token}",
            "Content-Type": "application/json"
        }

    def _make_request(self, method: str, endpoint: str, params: Optional[Dict] = None, data: Optional[Dict] = None) -> Dict[str, Any]:
        """统一的HTTP请求方法"""
        url = f"{self.base_url}{endpoint}"
        try:
            response = requests.request(
                method=method,
                url=url,
                params=params,
                json=data,
                headers=self.headers,
                timeout=30
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            return {"error": f"API请求失败: {str(e)}"}

# 创建服务器管理器实例
server_manager = ServerManager()

@mcp.tool()
def list_servers() -> str:
    """
    获取所有服务器的列表信息，包括服务器名称、状态、JAR文件名和JVM参数。

    Returns:
        str: 服务器列表的格式化信息
    """
    result = server_manager._make_request("GET", "/server/")

    if "error" in result:
        return f"获取服务器列表失败: {result['error']}"

    servers = result.get("data", {}).get("servers", [])

    if not servers:
        return "当前没有可用的服务器"

    server_info = []
    for server in servers:
        status_map = {"running": "运行中", "stopped": "已停止"}
        status = status_map.get(server.get("serverStatus", "stopped"), "未知")

        info = f"""服务器名称: {server.get('serverName', '未知')}
状态: {status}
JAR文件: {server.get('serverJarName', '未知')}
JVM参数: {server.get('serverJvmParams', '无')}
"""
        server_info.append(info)

    return "\n" + "="*50 + "\n".join(server_info) + "="*50

@mcp.tool()
def start_server(server_name: str) -> str:
    """
    启动指定的服务器。

    Args:
        server_name (str): 要启动的服务器名称

    Returns:
        str: 启动操作的结果信息
    """
    result = server_manager._make_request("POST", f"/server/{server_name}/start")

    if "error" in result:
        return f"启动服务器 '{server_name}' 失败: {result['error']}"

    return f"服务器 '{server_name}' 启动命令已发送"

@mcp.tool()
def stop_server(server_name: str) -> str:
    """
    停止指定的服务器。

    Args:
        server_name (str): 要停止的服务器名称

    Returns:
        str: 停止操作的结果信息
    """
    result = server_manager._make_request("POST", f"/server/{server_name}/stop")

    if "error" in result:
        return f"停止服务器 '{server_name}' 失败: {result['error']}"

    return f"服务器 '{server_name}' 停止命令已发送"

@mcp.tool()
def backup_server(server_name: str, file_name: Optional[str] = None) -> str:
    """
    备份指定服务器的地图存档。

    Args:
        server_name (str): 要备份的服务器名称
        file_name (str, optional): 指定的备份文件名，如不提供则使用默认命名

    Returns:
        str: 备份操作的结果信息
    """
    params = {}
    if file_name:
        params["fileName"] = file_name

    result = server_manager._make_request("POST", f"/server/{server_name}/backup", params=params)

    if "error" in result:
        return f"备份服务器 '{server_name}' 失败: {result['error']}"

    if file_name:
        return f"服务器 '{server_name}' 已备份为 {file_name}"
    else:
        return f"服务器 '{server_name}' 备份已完成（使用默认文件名）"

@mcp.tool()
def list_server_properties(server_name: str) -> str:
    """
    列出指定服务器的所有属性（server.properties）。

    Args:
        server_name (str): 目标服务器名称

    Returns:
        str: 服务器属性的格式化信息
    """
    result = server_manager._make_request("GET", f"/server/{server_name}/properties")

    if "error" in result:
        return f"获取服务器 '{server_name}' 属性失败: {result['error']}"

    properties = result.get("data", {}).get("properties", {})

    if not properties:
        return f"服务器 '{server_name}' 没有找到任何属性"

    prop_info = [f"服务器 '{server_name}' 的属性列表:"]
    for key, value in properties.items():
        prop_info.append(f"  {key} = {value}")

    return "\n".join(prop_info)

@mcp.tool()
def update_server_property(server_name: str, key: str, value: str) -> str:
    """
    更新指定服务器的单个属性。

    Args:
        server_name (str): 目标服务器名称
        key (str): 要更新的属性键
        value (str): 要设置的属性值

    Returns:
        str: 属性更新结果信息
    """
    params = {"key": key, "value": value}
    result = server_manager._make_request("POST", f"/server/{server_name}/property", params=params)

    if "error" in result:
        return f"更新服务器 '{server_name}' 属性失败: {result['error']}"

    return f"服务器 '{server_name}' 的属性 '{key}' 已更新为 '{value}'"

@mcp.tool()
def get_server_status(server_name: str) -> str:
    """
    获取指定服务器的详细状态信息。

    Args:
        server_name (str): 目标服务器名称

    Returns:
        str: 服务器状态信息
    """
    # 先获取服务器列表找到目标服务器
    result = server_manager._make_request("GET", "/server/")

    if "error" in result:
        return f"获取服务器状态失败: {result['error']}"

    servers = result.get("data", {}).get("servers", [])
    target_server = None

    for server in servers:
        if server.get("serverName") == server_name:
            target_server = server
            break

    if not target_server:
        return f"未找到名为 '{server_name}' 的服务器"

    status_map = {"running": "🟢 运行中", "stopped": "🔴 已停止"}
    status = status_map.get(target_server.get("serverStatus", "stopped"), "⚪ 未知")

    status_info = f"""
服务器状态报告:
======================
名称: {target_server.get('serverName', '未知')}
状态: {status}
JAR文件: {target_server.get('serverJarName', '未知')}
JVM参数: {target_server.get('serverJvmParams', '无')}
======================
    """

    return status_info

@mcp.tool()
def get_chest_info(x1: int, y1: int, z1: int, x2: int, y2: int, z2: int) -> str:
    """
    查看指定区域内的容器物品信息。

    Args:
        x1 (int): 区域第一个角点的x坐标
        y1 (int): 区域第一个角点的y坐标
        z1 (int): 区域第一个角点的z坐标
        x2 (int): 区域第二个角点的x坐标
        y2 (int): 区域第二个角点的z坐标
        z2 (int): 区域第二个角点的z坐标

    Returns:
        str: 容器物品信息的格式化结果，其中json的定义如下：
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

    """
    # 构建查询参数
    params = {
        "x1": x1,
        "y1": y1,
        "z1": z1,
        "x2": x2,
        "y2": y2,
        "z2": z2
    }

    # 发送GET请求到/chest_info端点
    result = server_manager._make_request("GET", "/chest_info", params=params)

    return json.dumps(result.get("data", {}))

@mcp.tool()
def reload_server_container(server_name: str):
    """
    重新加载指定服务器的容器数据。

    Args:
        server_name (str): 目标服务器名称

    Returns:
        str: 重新加载操作的结果信息
    """
    result = server_manager._make_request("POST", f"/server/{server_name}/reload")

    if "error" in result:
        return f"重新加载服务器 '{server_name}' 容器数据失败: {result['error']}"

    return f"服务器 '{server_name}' 的容器数据已重新加载"

# 新增：通过备份目录加载地图（mapName 为 zip 文件名）
@mcp.tool()
def load_map(server_name: str, map_name: str) -> str:
    """
    从服务器的备份目录加载指定的地图压缩包（map_name），并将其设置为当前 level-name。
    注意：服务器必须处于停止状态，Service 会校验并抛出错误。
    """
    params = {"mapName": map_name}
    result = server_manager._make_request("POST", f"/server/{server_name}/loadMap", params=params)

    if "error" in result:
        return f"加载地图 '{map_name}' 到服务器 '{server_name}' 失败: {result['error']}"

    return f"地图 '{map_name}' 已成功加载到服务器 '{server_name}'，并已更新 level-name"

# 新增：通过 RCON 发送命令并获取返回结果
@mcp.tool()
def send_command_rcon(server_name: str, command: str) -> str:
    """
    通过服务器的 RCON 接口发送命令并返回执行结果字符串。
    """
    params = {"command": command}
    result = server_manager._make_request("POST", f"/server/{server_name}/rcon", params=params)

    if "error" in result:
        return f"通过 RCON 发送命令到服务器 '{server_name}' 失败: {result['error']}"

    # 如果后端返回结构为 { "data": { "result": "..." } } 或直接 { "result": "..." }，兼容处理
    data = result.get("data", result)
    rcon_result = data.get("result") if isinstance(data, dict) else None
    if rcon_result is None:
        # 尝试把整个 data 序列化返回
        return f"RCON 返回: {json.dumps(data)}"
    return f"RCON 返回: {rcon_result}"

# 资源定义：提供服务器统计信息
@mcp.resource("server://stats")
def get_server_stats() -> str:
    """获取所有服务器的统计信息"""
    result = server_manager._make_request("GET", "/server/")

    if "error" in result:
        return f"无法获取服务器统计: {result['error']}"

    servers = result.get("data", {}).get("servers", [])

    running_servers = [s for s in servers if s.get("serverStatus") == "running"]
    stopped_servers = [s for s in servers if s.get("serverStatus") == "stopped"]

    stats = f"""
服务器统计信息 (更新时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')})
========================================
总服务器数: {len(servers)}
运行中: {len(running_servers)}
已停止: {len(stopped_servers)}
========================================
    """

    return stats

if __name__ == "__main__":
    # 运行MCP服务器（使用stdio传输，适合与AI客户端集成）
    mcp.run(transport="stdio")