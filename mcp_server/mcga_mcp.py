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

    return f"服务器 '{server_name}' 启动命令已发送，服务器返回： {json.dumps(result)}"

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

    return f"服务器 '{server_name}' 停止命令已发送，服务器返回： {json.dumps(result)}"

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

    return f"服务器 '{server_name}' 备份指令已发出，服务器返回： {json.dumps(result)}"

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


    return f"服务器返回： {json.dumps(result)}"

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

# 新增 MCP 工具：获取系统负载概览
@mcp.tool()
def get_system_overhead() -> str:
    """
    获取管理端（应用服务器）所在主机的系统负载概览，调用后端接口：GET /stat/overhead。

    返回：格式化的简要文本，包含CPU总体占用、每核占用（前几个）、内存使用、磁盘和网络速率摘要等。
    """
    try:
        result = server_manager._make_request("GET", "/stat/overhead")

        if "error" in result:
            return f"获取系统负载失败: {result['error']}"

        overhead = result.get("data", {}).get("overhead", {})
        if not overhead:
            return "未获得负载信息"

        # 容错的值格式化器
        def pct(v):
            try:
                if v is None:
                    return "N/A"
                return f"{float(v):.2f}%"
            except Exception:
                return str(v)

        def num(v):
            try:
                if v is None:
                    return "N/A"
                return str(v)
            except Exception:
                return str(v)

        def bytes_human(n):
            try:
                if n is None:
                    return "N/A"
                n = float(n)
                units = ["B", "KB", "MB", "GB", "TB"]
                i = 0
                while n >= 1024 and i < len(units)-1:
                    n /= 1024.0
                    i += 1
                return f"{n:.2f} {units[i]}"
            except Exception:
                return str(n)

        cpu = overhead.get("cpu") or {}
        memory = overhead.get("memory") or {}
        load_avg = overhead.get("loadAverages") or []
        disks = overhead.get("disks") or []
        nets = overhead.get("networkInterfaces") or []

        lines = []
        ts = overhead.get('timestamp')
        try:
            if ts:
                lines.append(f"时间: {datetime.fromtimestamp(int(ts)/1000.0).strftime('%Y-%m-%d %H:%M:%S')}")
            else:
                lines.append("时间: N/A")
        except Exception:
            lines.append(f"时间: {num(ts)}")

        lines.append("--- CPU ---")
        lines.append(f"总体 CPU: {pct(cpu.get('systemLoad'))}")
        per_core = cpu.get('perCore') or []
        if per_core:
            preview = per_core[:8]  # 展示更多核但不过长
            lines.append("每核 (前8): " + ", ".join([pct(p) for p in preview]))
        # 额外字段兼容显示
        for k in ('user', 'system', 'idle'):
            if k in cpu:
                lines.append(f"CPU {k}: {pct(cpu.get(k))}")

        lines.append("--- Memory ---")
        lines.append(f"总内存: {bytes_human(memory.get('totalBytes'))}")
        lines.append(f"已用: {bytes_human(memory.get('usedBytes'))} ({pct(memory.get('usedPercent'))})")
        # swap
        if 'swapUsedBytes' in memory or 'swapUsedPercent' in memory or 'swapTotalBytes' in memory:
            lines.append(f"Swap 总计: {bytes_human(memory.get('swapTotalBytes'))} 已用: {bytes_human(memory.get('swapUsedBytes'))} ({pct(memory.get('swapUsedPercent'))})")

        lines.append("--- LoadAvg ---")
        if load_avg:
            try:
                la_str = ", ".join([("NaN" if (x is None or (isinstance(x, float) and (x != x))) else f"{float(x):.2f}") for x in load_avg])
            except Exception:
                la_str = ", ".join([str(x) for x in load_avg])
            lines.append(f"LoadAvg (1,5,15): {la_str}")
        else:
            lines.append("LoadAvg: N/A")

        # Disk 和 Network 简要（只列出前两项各自最活跃的）
        def top_sorted(items, key):
            try:
                return sorted(items, key=lambda it: float(it.get(key) or 0), reverse=True)
            except Exception:
                return items

        if disks:
            lines.append("--- Disks (top 2 by writeBytesPerSec) ---")
            top_disks = top_sorted(disks, 'writeBytesPerSec')[:2]
            for d in top_disks:
                lines.append(f"{d.get('name') or d.get('device') or 'unknown'} model={d.get('model') or 'N/A'} size={bytes_human(d.get('sizeBytes'))} write={bytes_human(d.get('writeBytesPerSec'))}/s read={bytes_human(d.get('readBytesPerSec'))}/s")

        if nets:
            lines.append("--- Network (top 2 by recvBytesPerSec) ---")
            top_nets = top_sorted(nets, 'recvBytesPerSec')[:2]
            for n in top_nets:
                lines.append(f"{n.get('name') or 'unknown'} {n.get('displayName') or ''} rx={bytes_human(n.get('recvBytesPerSec'))}/s tx={bytes_human(n.get('sentBytesPerSec'))}/s")

        return "\n".join(lines)

    except Exception as e:
        # 捕获任何意外错误，返回错误信息而不是让 MCP 失效
        return f"处理系统负载时发生错误: {str(e)}"

@mcp.tool()
def get_chest_info(x1: int, y1: int, z1: int, x2: int, y2: int, z2: int) -> str:
    """
    查询指定区域内的容器（箱子）并返回可读的文本报告。

    返回：多行的中文可读文本，包含区域信息、总计物品摘要（按数量排序，限制展示前若干项）、容器数量，并列出若干容器的位置与其物品明细（每个容器限制展示若干条目以避免输出过长）。
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

    result = server_manager._make_request("GET", "/chest_info", params=params)

    if "error" in result:
        return f"获取容器信息失败: {result['error']}"

    # 兼容 data 包装或直接返回 root 的情况
    payload = result.get("data") if isinstance(result.get("data"), dict) else result
    if not payload:
        return "未获得容器信息"

    if isinstance(payload, dict) and payload.get("error"):
        return f"获取容器信息失败: {payload.get('error')}"

    # 安全获取字段
    region = payload.get("region") or {}
    region_from = region.get("from") or []
    region_to = region.get("to") or []
    total_items = payload.get("totalItems") or {}
    containers = payload.get("containers") or []

    lines = []
    # Region summary
    try:
        lines.append("=== Chest Info ===")
        if region_from and region_to and len(region_from) >= 3 and len(region_to) >= 3:
            lines.append(f"区域: from {region_from[0]},{region_from[1]},{region_from[2]} to {region_to[0]},{region_to[1]},{region_to[2]}")
            # 计算体积（容错）
            try:
                dx = abs(int(region_to[0]) - int(region_from[0])) + 1
                dy = abs(int(region_to[1]) - int(region_from[1])) + 1
                dz = abs(int(region_to[2]) - int(region_from[2])) + 1
                vol = dx * dy * dz
                lines.append(f"体积 (blocks): {vol}")
            except Exception:
                pass
        else:
            lines.append("区域: N/A")

        lines.append(f"容器数量: {len(containers)}")
        lines.append(f"物品种类数: {len(total_items)}")

        # Top total items
        if total_items:
            lines.append("-- 总计物品（按数量排序） --")
            try:
                items_list = sorted(total_items.items(), key=lambda kv: int(kv[1].get('count', 0)), reverse=True)
            except Exception:
                # 兼容不同数据结构
                items_list = list(total_items.items())
            for idx, (item_id, item_obj) in enumerate(items_list):
                try:
                    cnt = item_obj.get('count', item_obj if isinstance(item_obj, int) else 0)
                    name = item_obj.get('displayName', item_id) if isinstance(item_obj, dict) else item_id
                except Exception:
                    cnt = item_obj if isinstance(item_obj, int) else 0
                    name = item_id
                lines.append(f"{idx+1}. {name} ({item_id}) x {cnt}")
        else:
            lines.append("无总计物品数据")

        # Containers detail — 不限制数量，返回后端所有容器数据
        if containers:
            lines.append(f"-- 容器明细（返回后端提供的全部容器） --")
            for ci, cont in enumerate(containers):
                try:
                    # cont is expected to be object with pos array and items map
                    pos = cont.get('pos') if isinstance(cont, dict) else None
                    items = cont.get('items') if isinstance(cont, dict) else None
                    if pos and len(pos) >= 3:
                        lines.append(f"[{ci+1}] 位置: {pos[0]},{pos[1]},{pos[2]}")
                    else:
                        # Some backends may send pair-like arrays [pos, items]
                        if isinstance(cont, list) and len(cont) >= 2:
                            pos = cont[0]
                            items = cont[1]
                            if isinstance(pos, list) and len(pos) >= 3:
                                lines.append(f"[{ci+1}] 位置: {pos[0]},{pos[1]},{pos[2]}")
                            else:
                                lines.append(f"[{ci+1}] 位置: N/A")
                        else:
                            lines.append(f"[{ci+1}] 位置: N/A")

                    if not items:
                        lines.append("  无物品")
                        continue

                    # items is expected to be map id -> {count, displayName}
                    try:
                        items_list = sorted(items.items(), key=lambda kv: int(kv[1].get('count', 0)), reverse=True)
                    except Exception:
                        # fallback if items structure different
                        items_list = list(items.items())

                    for ii, (iid, iobj) in enumerate(items_list):
                        try:
                            icnt = iobj.get('count', iobj if isinstance(iobj, int) else 0)
                            iname = iobj.get('displayName', iid) if isinstance(iobj, dict) else iid
                        except Exception:
                            icnt = iobj if isinstance(iobj, int) else 0
                            iname = iid
                        lines.append(f"  - {iname} ({iid}) x {icnt}")

                except Exception as e:
                    lines.append(f"  [解析容器时出错] {str(e)}")
        else:
            lines.append("无容器数据")

        return "\n".join(lines)
    except Exception as e:
        return f"处理容器信息时发生错误: {str(e)}"

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

    return "服务器返回结果：" + json.dumps(result)

# 新增：通过备份目录加载地图（mapName 为 zip 文件名）
@mcp.tool()
def load_map(server_name: str, map_name: str) -> str:
    """
    从服务器的备份目录加载指定的地图压缩包（map_name），并将其设置为当前 level-name。
    注意：服务器必须处于停止状态，Service 会校验并抛出错误。
    """
    params = {"mapName": map_name}
    result = server_manager._make_request("POST", f"/server/{server_name}/loadMap", params=params)
    print(result)

    return "服务器返回结果：" + json.dumps(result)

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

@mcp.tool()
def get_player_logs(server_name: str = None, limit: int = 50) -> str:
    """
    获取玩家登录和退出服务器的日志记录。

    Args:
        server_name (str, optional): 指定服务器名称，不传则返回所有服务器
        limit (int, optional): 返回的日志条数上限，默认50

    Returns:
        str: 玩家日志的格式化信息
    """
    params = {"limit": limit}
    if server_name:
        params["serverName"] = server_name

    result = server_manager._make_request("GET", "/player-log/recent", params=params)

    if "error" in result:
        return f"获取玩家日志失败: {result['error']}"

    logs = result.get("data", {}).get("logs", [])

    if not logs:
        return "暂无玩家日志记录"

    lines = [f"玩家日志 (最近{limit}条):", "=" * 50]
    event_map = {"join": "加入游戏", "leave": "离开游戏"}
    for log in logs:
        event_type = event_map.get(log.get("eventType"), log.get("eventType"))
        lines.append(f"[{log.get('eventTime')}] {log.get('playerName')} {event_type} @ {log.get('serverName')}")

    return "\n".join(lines)


@mcp.tool()
def get_player_history(player_name: str, server_name: str = None) -> str:
    """
    查询指定玩家的登录/退出历史记录，包含每次事件的时间。

    Args:
        player_name (str): 玩家名称
        server_name (str, optional): 筛选指定服务器

    Returns:
        str: 玩家历史的格式化信息（含时间）
    """
    params = {}
    if server_name:
        params["serverName"] = server_name

    result = server_manager._make_request("GET", f"/player-log/player/{player_name}", params=params)

    if "error" in result:
        return f"获取玩家历史失败: {result['error']}"

    logs = result.get("data", {}).get("logs", [])

    if not logs:
        server_info = f" on {server_name}" if server_name else ""
        return f"未找到玩家 '{player_name}'{server_info} 的登录记录"

    lines = [f"玩家 '{player_name}' 的登录历史:", "=" * 50]
    event_map = {"join": "加入", "leave": "离开"}
    for log in logs:
        event_type = event_map.get(log.get("eventType"), log.get("eventType"))
        lines.append(f"  [{log.get('eventTime')}] {event_type} {log.get('serverName')}")

    return "\n".join(lines)


@mcp.tool()
def get_player_statistics(server_name: str, date: str = None) -> str:
    """
    获取指定服务器某天的玩家登录/退出统计。

    Args:
        server_name (str): 目标服务器名称
        date (str, optional): 日期，格式 yyyy-MM-dd，默认为今天

    Returns:
        str: 统计信息的格式化文本
    """
    params = {"serverName": server_name}
    if date:
        params["date"] = date

    result = server_manager._make_request("GET", "/player-log/statistics", params=params)

    if "error" in result:
        return f"获取玩家统计失败: {result['error']}"

    stats = result.get("data", {}).get("statistics", {})

    if not stats:
        return "未获取到统计数据"

    return f"""服务器 '{stats.get('serverName')}' 玩家统计 ({stats.get('date')}):
========================================
  登录次数: {stats.get('joinCount', 0)}
  退出次数: {stats.get('leaveCount', 0)}
========================================"""


@mcp.tool()
def get_player_sessions(player_name: str, server_name: str = None) -> str:
    """
    获取指定玩家的在线会话记录，包含每次登录和退出的具体时间以及在线时长。

    Args:
        player_name (str): 玩家名称
        server_name (str, optional): 筛选指定服务器

    Returns:
        str: 玩家在线时长的格式化信息
    """
    params = {"playerName": player_name}
    if server_name:
        params["serverName"] = server_name

    result = server_manager._make_request("GET", "/player-log/sessions", params=params)

    if "error" in result:
        return f"获取玩家在线时长失败: {result['error']}"

    sessions = result.get("data", {}).get("sessions", [])

    if not sessions:
        return f"未找到玩家 '{player_name}' 的在线记录"

    total_online = 0
    lines = [f"玩家 '{player_name}' 的在线会话记录:", "=" * 60]
    for idx, s in enumerate(sessions, 1):
        join_time = s.get("joinTime", "?")
        leave_time = s.get("leaveTime", "---")
        duration = s.get("durationFormatted", "仍在游戏中")
        lines.append(f"  #{idx}")
        lines.append(f"    上线: {join_time}")
        lines.append(f"    下线: {leave_time}")
        lines.append(f"    时长: {duration}")
        lines.append("")

        if s.get("durationSeconds") is not None:
            total_online += s["durationSeconds"]

    # 总在线时长
    hours = total_online // 3600
    minutes = (total_online % 3600) // 60
    seconds = total_online % 60
    total_str = f"{hours}小时{minutes}分{seconds}秒" if hours > 0 else f"{minutes}分{seconds}秒" if minutes > 0 else f"{seconds}秒"
    lines.append(f"  总在线时长: {total_str}")

    return "\n".join(lines)


@mcp.tool()
def get_current_players(server_name: str) -> str:
    """
    获取指定服务器当前在线的玩家列表及每个玩家上线时间。

    Args:
        server_name (str): 目标服务器名称

    Returns:
        str: 当前在线玩家的格式化信息（含加入时间）
    """
    params = {"serverName": server_name}
    result = server_manager._make_request("GET", "/player-log/current-players", params=params)

    if "error" in result:
        return f"获取当前玩家失败: {result['error']}"

    players = result.get("data", {}).get("players", [])

    if not players:
        return f"服务器 '{server_name}' 当前没有玩家在线"

    lines = [f"服务器 '{server_name}' 当前在线玩家 ({len(players)}人):", "=" * 40]
    for p in players:
        lines.append(f"  - {p.get('playerName')} (上线时间: {p.get('joinTime')})")

    return "\n".join(lines)


if __name__ == "__main__":
    # 运行MCP服务器（使用stdio传输，适合与AI客户端集成）
    mcp.run(transport="stdio")
