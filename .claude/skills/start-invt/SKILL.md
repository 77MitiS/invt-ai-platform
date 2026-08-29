---
name: start-invt
description: 启动（start/run/launch/运行）INVT AI Platform 本地开发环境 —— 后端 Spring Boot 端口 18088，前端 Vite 端口 5173。当用户要求启动/运行/start/run/launch invt 项目、后端(server)或前端(ui)时使用。
---

# 启动 INVT AI Platform

Monorepo，根目录为 `invt-ai-platform`（仓库根 = 工作目录）：

| 模块 | 技术栈 | 端口 | 说明 |
|---|---|---|---|
| `invt-ai-platform-server` | Spring Boot 3.5 / Java 21 / H2 file DB | 18088 | `context-path=/`，profile=`dev` |
| `invt-ai-platform-ui` | Vue 3 + Vite 7 + TS | 5173 | `/api` 与 `/skill-assets` 已代理到 18088 |

## 前置条件

| 依赖 | 要求 | 验证命令 |
|---|---|---|
| Java | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node | 18+ | `node -v` |

- **包管理器用 npm**：本机 `pnpm` 未安装（`pnpm -v` 会 `command not found`）。
- `invt-ai-platform-ui/node_modules` 已装好（vite 二进制在 `node_modules/.bin/vite`，`vite --version` 可验证），**无需重新 install**。
- 后端 `settings.xml` 为空（仓库 pom.xml 自带多仓库 fail-over），直接用 `mvn` 即可，无需 `-s`。

## 步骤

### 1. 先检查端口（避免重复启动 / 确认是否需要重启）

```bash
netstat -ano | grep -E ":18088\s"   # 后端在跑会显示 LISTENING
netstat -ano | grep -E ":5173\s"    # 前端在跑会显示 LISTENING
```

已在 LISTENING 就跳过对应启动步骤。**注意**：后台进程可能在会话之间被环境回收，每次进来都先跑这步。

### 2. 启动后端（约 10–12s 启动完成）

```bash
cd invt-ai-platform-server
nohup mvn spring-boot:run > /tmp/invt-server.log 2>&1 &
```

`spring-boot:run` 会编译改动过的源码（含迁移与资源文件），无需单独 `mvn compile`。

### 3. 启动前端（约 5s 就绪）

```bash
cd invt-ai-platform-ui
nohup npm run dev > /tmp/invt-ui.log 2>&1 &
```

### 4. 验证

```bash
# 后端启动完成标志（出现 Started InvtApplication in ... seconds 即成功）
grep -E "Started InvtApplication|Tomcat started" /tmp/invt-server.log

# 登录冒烟测试（期望 code:200）
curl -s -m 15 -X POST http://localhost:18088/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 前端就绪标志（期望 "VITE ... ready" 与 Local: http://localhost:5173）
tail -8 /tmp/invt-ui.log
```

## 访问

- 前端（浏览器直接访问）：http://localhost:5173
- 后端 API：http://localhost:18088
- 默认登录：`admin` / `admin123`

## 注意事项

- **用 `nohup ... &` 后台启动**，不要用 Bash 工具的 `run_in_background` 托管任务——托管任务在会话中断时会把 fork 出的 JVM 一起带走（历史上有过 9 分钟后 exit code 1）。
- 后台进程可能仍会在会话之间被回收：每次先跑「步骤 1」检查端口，down 了再重启即可（幂等，可重复执行）。
- 日志位置：后端 `/tmp/invt-server.log`，前端 `/tmp/invt-ui.log`。
- 若后端日志里出现 `BUILD FAILURE` 但端口已 LISTENING、登录冒烟通过，多为旧的构建噪音，以「Started + 登录 code:200」为准。
