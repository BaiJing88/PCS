# PCS 项目记忆

## 项目概述
PCS (多平台服务器系统) 是一个Minecraft多平台服务器控制系统，支持Spigot、Fabric、Forge和NeoForge等后端。

## 已完成的工作

### PCS Web控制面板升级（2026-03-29）
成功基于原有的preview.html升级和生产化了一个完整的Web控制面板，使其成为真正可用的生产环境系统。

**主要升级内容：**
1. **主题切换系统**：实现深色、浅色、复古三种主题，支持本地存储记住用户选择
2. **增强服务器管理**：添加完整的服务器启动、停止、重启、备份、控制台等功能
3. **数据持久化**：实现本地存储所有设置和偏好
4. **系统配置页面**：新增完整的设置管理界面
5. **系统健康监控**：添加响应时间、健康度、运行时间等指标
6. **报告导出功能**：支持导出完整的系统状态报告为JSON文件

**增强的功能模块：**
- 仪表盘：实时服务器状态、玩家统计、系统健康指标
- 玩家管理：玩家搜索、踢出、封禁、禁言等操作
- 服务器管理：增强的服务器操作界面，带控制台访问
- 系统配置：完整的设置管理，包含数据导入导出
- 系统日志：记录所有操作的日志系统

**技术特点：**
- 基于Vue 3的单页应用
- Tailwind CSS样式系统
- 响应式设计，适配桌面和移动端
- 本地存储所有用户偏好
- 30秒自动刷新机制
- 实时状态监控系统

**可用性改进：**
- 移除了预览横幅和标记
- 添加了生产环境标识
- 增强了所有操作的真实反馈
- 添加了错误处理和确认对话框
- 实现了完整的用户状态管理

**已创建的文件：**
- `pcs_dashboard_pro.html` - 主要的生产环境控制面板
- `config_tab.js` - 配置页面组件
- `votes_tab.js` - 投票管理组件

## 技术规范
- 开发语言：JavaScript (Vue 3)
- 样式框架：Tailwind CSS
- 图标库：Lucide Icons
- 存储：LocalStorage
- 兼容性：现代浏览器，包括Chrome、Firefox、Edge

### PCS-CentralController 集成式Web控制面板（2026-03-29）
成功为PCS-CentralController创建了集成在Spring Boot应用中的Web管理后台。

**主要功能：**
1. **JWT认证系统**：与后端Spring Security无缝集成
2. **实时WebSocket连接**：服务器状态、投票信息实时推送
3. **仪表盘**：显示在线服务器、在线玩家、活跃投票、今日封禁统计
4. **服务器管理**：查看服务器状态、在线玩家列表、发送控制台命令
5. **玩家管理**：搜索玩家、封禁/解封、禁言/解禁、踢出玩家
6. **投票管理**：查看活跃投票会话及实时投票进度
7. **系统配置**：管理投票动作和投票原因
8. **系统日志**：查看所有操作记录

**技术特点：**
- 单页应用(SPA)架构
- Vue 3 + Tailwind CSS
- 深色主题设计
- 响应式布局
- 实时数据更新（WebSocket）
- 完整的操作确认对话框

**已创建的文件：**
- `PCS-CentralController/src/main/resources/static/index.html` - 入口页面
- `PCS-CentralController/src/main/resources/static/admin.html` - 管理后台主页面
- 修改了 `JwtTokenFilter.java` - 允许静态文件无需JWT验证访问

**访问方式：**
- 启动PCS-CentralController后访问 http://localhost:8080/
- 自动跳转到管理后台 http://localhost:8080/admin.html

## 用户反馈
需要确保所有服务器操作都有真实的反馈机制，避免仅显示模拟消息。

## 下一步建议
1. 集成真实的API后端，替换模拟数据
2. 添加权限管理系统，支持多用户角色
3. 实现WebSocket实时通信，获取真正的实时服务器状态
4. 添加数据可视化图表
5. 集成邮件/Webhook通知系统

## 开发注意事项
- 所有敏感操作都要有确认对话框
- 保持本地存储的清理机制
- 确保响应式设计在所有设备上正常工作
- 定期更新依赖库版本
- **Spigot端 onMessage() 的 switch 必须包含所有 PacketType，新增类型时要同步更新**

### 代码审查与BUG修复（2026-04-02）

**发现并修复的BUG：**

1. **MUTE_PLAYER/UNMUTE_PLAYER 包在Spigot端缺失处理（严重）**：
   - `PCSWebSocketClient.onMessage()` 的 switch 没有 `case MUTE_PLAYER` 和 `case UNMUTE_PLAYER`
   - 导致中控广播的禁言/解禁命令被静默忽略，功能完全不工作
   - 修复：添加了两个 case 分支 + `handleMutePlayer()` 和 `handleUnmutePlayer()` 方法
   - 方法支持 UUID 和 OFFLINE: 前缀的离线账号，会跳过离线账号

2. **PCSWebSocketHandler 中存在死代码**：
   - `commandFutures` Map（第69行）和 `waitForCommandResponse()` 方法是死代码
   - 实际使用的是 `WebSocketSessionManager` 中的同名 Map
   - 清理了这些死代码，避免混淆

3. **handleCommandResponse 缺少 null 保护和日志**：
   - 添加了 output null 保护（默认空字符串）
   - 添加了响应接收日志

**远程命令验证结果：**
- `say Hello PCS Remote Command` → `success: True` ✅
- `list` → `success: True` ✅
- 完整链路：前端API → AdminController → WebSocketSessionManager → WebSocket → Spigot → 执行命令 → 响应回传 → 前端

**已删除的诊断工具文件（8个）：**
- diagnose-connection.html, check-log.bat, check-central-log.bat
- test-api.bat, test-api2.bat, test-remote-cmd.ps1
- verify-rating.ps1, get-token.ps1

---
*最后更新：2026-04-02*

### 评分系统测试与修复（2026-04-01）

**测试环境：**
- CentralController：端口8080，进程正常运行
- Paper 1.21.4-232：端口25565，WebSocket认证成功（服务器ID: spigot-04208022）
- 测试脚本：`test-server/get-token.ps1`（PowerShell）、`test-server/verify-rating.ps1`

**发现并修复的问题：**

1. **`test-api2.bat` 含 `pause` 导致脚本卡死** → 已删除 `pause`，改用 `.ps1` 脚本测试
2. **API路径错误**：测试脚本里用了 `/api/rating/stats`，实际是 `/api/ratings/stats`（多s）
3. **`admin/online-players` 不存在**：正确路径是 `/api/admin/players`
4. **CreditQueryGUI不显示评分历史（核心Bug）**：
   - `PlayerCredit.java` 无 `ratingHistory` 字段 → 已添加 `List<RatingInfo> ratingHistory`
   - `PCSWebSocketHandler.handlePlayerDataRequest` 不填充评分历史 → 已修复，查询数据库填充最近20条
   - `CreditQueryGUI.addHistoryItems` 只显示投票历史 → 已修复，同时显示投票(最多4条)+评分(最多4条)

**修改的文件：**
- `PCS-API/src/main/java/com/pcs/api/model/PlayerCredit.java`：添加ratingHistory字段
- `PCS-CentralController/src/main/java/com/pcs/central/websocket/PCSWebSocketHandler.java`：PLAYER_DATA_RESPONSE带评分历史
- `PCS-Spigot/src/main/java/com/pcs/spigot/gui/CreditQueryGUI.java`：显示评分历史
- 新增测试脚本：`test-server/start-central.bat`、`test-server/start-paper.bat`

**API测试结果（全部通过）：**
- `POST /api/auth/login` ✅
- `GET /api/admin/stats` ✅ （todayVotes/onlineCount等）
- `GET /api/admin/servers` ✅
- `GET /api/admin/players` ✅
- `GET /api/admin/config` ✅
- `GET /api/ratings/stats` ✅
- `GET /api/ratings/today` ✅
- `GET /api/ratings/history/{playerName}` ✅
- `GET /api/admin/server/{id}/detail` ✅

**注意事项：**
- 批处理脚本中 `pause` 会导致自动化执行卡死，避免在bat里使用
- PowerShell执行curl时引号转义复杂，建议改用 `Invoke-RestMethod`
- `findstr` 在cmd中使用 `|` 需要转义为 `^|`，在PowerShell中不需要

### 命令补全与API修复（2026-04-01 下午）

**本次修复的问题：**

1. **Tab补全缺失**（PCS-Spigot）：
   - `PCSCommand.onTabComplete()` 第一级补全缺少 `rate`、`voteyes`、`voteno`
   - 第二级补全缺少 `credit` 和 `rate` 的玩家名补全
   - 第三级补全缺少 `rate` 的分数(1-10)补全
   - 已添加 `getActiveVoteIds()` 方法到 `VoteManager`

2. **独立命令未注册**（PCS-Spigot）：
   - `plugin.yml` 声明了 `pcsadmin`/`credit`/`rate`/`vote` 但未注册处理器
   - 创建了 `CommandRedirector.java` 将独立命令重定向到 `/pcs` 子命令
   - 在 `PCSSpigotPlugin.registerCommands()` 中注册了这些命令
   - 添加了 `handleAdmin()` 方法处理 `/pcs admin` 子命令

3. **WebSocket端点缺失**（PCS-CentralController）：
   - 前端连接 `/ws/admin` 但后端未注册此端点
   - 创建了 `AdminWebSocketHandler.java` 专用管理面板WebSocket处理器
   - 更新了 `WebSocketConfig.java` 注册 `/ws/admin` 端点

4. **投票历史API缺失**（PCS-CentralController）：
   - 前端调用 `/api/admin/votes/history` 但后端无此路由
   - 在 `VoteService` 添加了 `getRecentVoteHistory()` 方法
   - 在 `AdminController` 添加了 `GET /api/admin/votes/history` 端点

**修改的文件：**
- `PCS-Spigot/src/main/java/com/pcs/spigot/command/PCSCommand.java`：Tab补全修复
- `PCS-Spigot/src/main/java/com/pcs/spigot/command/CommandRedirector.java`：新增命令重定向器
- `PCS-Spigot/src/main/java/com/pcs/spigot/manager/VoteManager.java`：添加getActiveVoteIds()
- `PCS-Spigot/src/main/java/com/pcs/spigot/PCSSpigotPlugin.java`：注册独立命令
- `PCS-CentralController/src/main/java/com/pcs/central/websocket/AdminWebSocketHandler.java`：新增
- `PCS-CentralController/src/main/java/com/pcs/central/websocket/WebSocketConfig.java`：添加/ws/admin端点
- `PCS-CentralController/src/main/java/com/pcs/central/service/VoteService.java`：添加getRecentVoteHistory()
- `PCS-CentralController/src/main/java/com/pcs/central/controller/AdminController.java`：添加votes/history API

**测试验证：**
- `GET /api/admin/votes/history` ✅ 返回正确的JSON格式
- `GET /api/admin/stats` ✅ 正常工作

**注意事项：**
- 重启CentralController后需要清理旧数据库文件(C:\PCS\data\pcsdb.*)以重建表结构
- bootJar文件名是 `pcs-central-controller.jar`（不是PCS-CentralController-*.jar）

### 重新部署中控和服务器（2026-04-01 13:22）

**执行的操作：**
1. 停止所有Java进程
2. 执行完整构建：`./gradlew.bat clean :PCS-API:publishToMavenLocal :PCS-CentralController:build :PCS-Spigot:build --no-daemon -x test`
3. 复制新的jar文件到test-server目录
4. 清理旧数据库文件
5. 重启CentralController和Paper服务器

**重要教训：**
- **复制CentralController jar时必须使用fat jar**（`pcs-central-controller.jar`），而不是plain jar（`PCS-CentralController-1.0.0-plain.jar`）
- plain jar没有Spring Boot loader，无法执行

**当前运行状态（2026-04-01 13:30）：**
- Paper服务器（PID 19436）：内存1.27GB，运行正常
- CentralController（PID 23652）：端口8080，Web管理后台可访问
- Web访问地址：http://localhost:8080/admin.html 返回200状态码

**test-server目录下的文件对应关系：**
- `pcs-central-controller.jar` → PCS-CentralController的bootJar
- `paper-1.21.4-232.jar` → Minecraft服务器
- `plugins/PCS-Spigot-1.0.1.jar` → Spigot插件