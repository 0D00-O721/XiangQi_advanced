# snkdfheiretrjoiei
## Main Design
### Base
+ 游戏初始化
+ 多用户登录
+ 游戏存读档
+ 对局逻辑
+ 图形用户界面

### Advance
+ 计时器
+ 逐步记录 -> 回放、悔棋或棋谱导出与导入
+ 在线或局域网对战
+ 音效
+ 机器
  - Human vs **LLM** ( He )
  - Human vs **Searcher** ( Wang )

## UI
部分设计采取AI方案，后续考虑改为奇怪、稀有、简洁的界面

## Codex Agent 对局

Java Swing 中国象棋支持双人对弈、本地搜索 AI，以及通过 Codex Agent 对弈。

## Codex Agent 配置

需要 JDK 17+、已登录的 Codex 桌面应用，并确保 `codex` 命令可用。Codex 配置文件位于 `%USERPROFILE%\.codex\config.toml`，加入：

```toml
[mcp_servers.xiangqi]
url = "http://127.0.0.1:8765/mcp"
enabled = true

[mcp_servers.xiangqi.tools.get_board]
approval_mode = "approve"

[mcp_servers.xiangqi.tools.play_move]
approval_mode = "approve"
```

启动程序后选择 **Agent 对弈**。程序会在本机 `127.0.0.1:8765` 提供棋盘 MCP 工具，并通过 `codex app-server --stdio` 启动 Agent；每局使用一个 Codex 对话，黑方每回合读取棋盘并提交走法。若启动时报找不到 `codex`，请将 Codex CLI 加入 `PATH`，或在 Java 启动参数中设置 `-Dxiangqi.codex.command=完整的codex.exe路径`。若 8765 端口已被占用，MCP 服务无法启动。
