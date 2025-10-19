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