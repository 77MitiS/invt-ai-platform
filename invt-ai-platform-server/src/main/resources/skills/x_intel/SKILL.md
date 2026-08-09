---
name: x_intel
description: "通过 xurl CLI 读取 X（Twitter）帖子、搜索、时间线与用户资料。"
nameZh: X 情报采集
nameEn: X Intel
version: 1.0.0
icon: 🐦
author: Invt
optional: true
tags:
  - x
  - twitter
  - social-media
  - research
  - xurl
platforms:
  - linux
  - macos
dependencies:
  commands:
    - xurl
  tools:
    - execute_shell_command
---

# x_intel — X（Twitter）情报采集

`x_intel` 让 Agent 能够通过 `xurl`（X 开发者平台的官方 CLI）拉取帖子、搜索结果、时间线和用户资料。**本技能设计为只读**——有意省略了发帖、回复、删除、发送私信等任何写入操作。如需独立的发布技能，请参见后续工作。

适用场景：

- 通过 ID 或 URL 查找单条帖子
- 使用 X 搜索查询语法（`from:user`、`lang:en`、`#hashtag` 等）搜索帖子
- 读取 Agent 操作者的首页时间线、提及、书签、点赞
- 通过用户名查看用户资料
- 遍历社交图谱（某人关注了谁 / 被谁关注）
- 当快捷命令不够用时，对任意 X API v2 GET 端点的原始读取访问

---

## 凭证安全（强制要求）

在 Agent 会话中调用时的关键规则：

- **绝不**将 `~/.xurl` 读入、打印、解析、总结、上传或引用到聊天上下文中。它是一个 YAML 令牌存储文件。
- **绝不**要求用户在对话中粘贴凭证 / 令牌。
- **绝不**在 Agent 会话中建议或执行包含内联密钥的认证命令。
- **绝不**传递 `--verbose` / `-v`——它会将认证头打印到标准输出。
- 本技能唯一会接触凭证的命令是 `xurl auth status`（仅状态，不含密钥）。

Agent 发出的命令中禁止以下标志（每个都接受内联密钥）：
`--bearer-token`、`--consumer-key`、`--consumer-secret`、`--access-token`、`--token-secret`、`--client-id`、`--client-secret`。

应用注册和 OAuth 2.0 PKCE 流程必须由用户**在** Agent 会话之外**执行（见下方"用户设置"）。令牌持久保存在 `~/.xurl`（YAML）中；OAuth 2.0 自动刷新。

---

## 安装

Agent 应进行验证而非安装。如缺失，指引用户安装。

```bash
# Shell 脚本（Linux + macOS，安装到 ~/.local/bin，无需 sudo）
curl -fsSL https://raw.githubusercontent.com/xdevplatform/xurl/main/install.sh | bash

# Homebrew（macOS）
brew install --cask xdevplatform/tap/xurl

# Go（跨平台）
go install github.com/xdevplatform/xurl@latest
```

验证：

```bash
xurl --help
xurl auth status
```

---

## 用户设置（由用户执行，Agent 不得执行）

Agent 不得执行这些步骤——它们涉及粘贴密钥。将用户指引至本节原文。

1. 打开 X 开发者控制台：<https://developer.x.com/en/portal/dashboard>
2. 在应用的用户认证设置中，将重定向 URI 设为 `http://localhost:8080/callback`，应用类型设为 **Web 应用、自动化应用或机器人**。
3. 复制应用的 Client ID 和 Client Secret。
4. 在本地注册应用：
   ```bash
   xurl auth apps add my-app --client-id YOUR_CLIENT_ID --client-secret YOUR_CLIENT_SECRET
   ```
5. 进行认证（这将打开浏览器进行 OAuth 2.0 PKCE）：
   ```bash
   xurl auth oauth2 --app my-app
   ```
   如果在 OAuth 后 X 返回 `UsernameNotFound` 或对 `/2/users/me` 查找返回 403，请显式传递用户名（需 xurl v1.1.0+）：
   ```bash
   xurl auth oauth2 --app my-app YOUR_HANDLE
   ```
6. 将此应用标记为默认，使所有命令都使用它：
   ```bash
   xurl auth default my-app
   ```
7. 验证：
   ```bash
   xurl auth status
   xurl whoami
   ```

> **最常见的错误：**在 `xurl auth oauth2` 中遗漏了 `--app my-app`。OAuth 令牌将落入内置的 `default` 配置中，而该配置没有 client-id/client-secret，导致后续所有读取均失败。重新运行 `xurl auth oauth2 --app my-app` 和 `xurl auth default my-app` 即可修复。

---

## 只读命令参考

所有命令均向标准输出返回 JSON。Agent 直接解析 JSON，无需额外工具。

| 操作 | 命令 |
| --- | --- |
| 查看绑定账号 | `xurl whoami` |
| 查看用户 | `xurl user @handle` |
| 读取单条帖子（ID 或 URL） | `xurl read POST_ID` |
| 搜索帖子 | `xurl search "QUERY" -n 10` |
| 首页时间线 | `xurl timeline -n 20` |
| 绑定账号的提及 | `xurl mentions -n 20` |
| 书签列表 | `xurl bookmarks -n 20` |
| 点赞列表 | `xurl likes -n 20` |
| 关注列表 | `xurl following -n 50` |
| 粉丝列表 | `xurl followers -n 50` |
| 另一个用户的图谱 | `xurl following --of HANDLE -n 20` |
| 认证状态 | `xurl auth status` |

备注：

- `POST_ID` 接受完整的 `https://x.com/user/status/...` URL——xurl 会自动提取 ID。
- 用户名可带或不带前导 `@`。

### 搜索查询语法

X 的搜索支持在引号包裹的查询字符串中使用操作符：

```bash
xurl search "from:elonmusk -is:retweet" -n 20
xurl search "#buildinpublic lang:en since:2026-01-01" -n 25
xurl search "OR" -n 10                        # 字面量 OR——必须加引号
xurl search "(rust OR go) lang:en" -n 10
xurl search "to:NASA -is:reply" -n 10
```

常用操作符：`from:`、`to:`、`@`、`#`、`is:retweet`、`is:reply`、`is:quote`、`lang:`、`since:`、`until:`、`has:media`、`has:links`。完整列表请参见 X 搜索语法文档。

---

## 原始 v2 读取访问

对于快捷命令无法覆盖的需求，可直接访问任意 v2 GET 端点：

```bash
# 公开用户字段
xurl /2/users/by/username/elonmusk?user.fields=public_metrics,description,verified

# 单条推文，含指标 + 作者展开
xurl /2/tweets/1234567890?tweet.fields=public_metrics,created_at&expansions=author_id

# 带额外字段的近期搜索（付费层级）
xurl /2/tweets/search/recent?query=langchain&tweet.fields=created_at,public_metrics&max_results=25

# 完整 URL 同样可用
xurl https://api.x.com/2/users/me
```

流式端点会自动检测；如需强制使用，可加 `-s`。**流式端点可能产生较高费用——在未与用户确认意图之前不要启动。**

---

## 常见工作流

### 用户画像分析

```bash
xurl user @handle
xurl /2/users/by/username/handle?user.fields=public_metrics,description,verified,created_at
xurl following --of handle -n 20    # 他们关注了谁
```

### 热门话题筛选

```bash
xurl search "topic lang:en -is:retweet" -n 25
# 从 JSON 中挑选感兴趣的 ID，然后深入查看：
xurl read 1234567890
xurl user @ORIGINAL_POSTER
```

### 查看最新动态

```bash
xurl whoami
xurl mentions -n 20
xurl timeline -n 20
xurl bookmarks -n 10
```

### 对话上下文

```bash
xurl read https://x.com/user/status/1234567890
# 通过原始 v2 进行对话展开
xurl /2/tweets/search/recent?query=conversation_id:1234567890&max_results=25
```

---

## 输出格式

每条命令输出 X API v2 格式的 JSON 到标准输出：

```json
{
  "data": { "id": "1234567890", "text": "Hello world!" },
  "includes": { "users": [{ "id": "...", "username": "..." }] }
}
```

错误同样为 JSON：

```json
{ "errors": [ { "message": "Not authorized", "code": 403 } ] }
```

非零退出码用于区分错误和空结果。

---

## Agent 工作流

1. 验证前置条件：`xurl --help`（命令存在）和 `xurl auth status`（用户至少有一个应用具有 `oauth2` 令牌，且标记为 `▸` 默认）。
2. **在执行任何其他命令之前先解析 `auth status` 的输出。**如果默认应用显示 `oauth2: (none)`，但某个非默认应用有有效令牌，则指示用户运行 `xurl auth default <该应用>`——这是最常见的配置故障，无需重新登录。
3. 如果 `auth status` 显示没有应用或没有令牌，**停止**。告知用户按照"用户设置"部分操作。不要尝试注册应用或执行任何认证流程。
4. 从最轻量的读取开始（`xurl whoami` / `xurl user @handle` / `xurl search ... -n 3`），以确认可达性和请求格式。
5. 区分处理 401 / 403 / 429：401 → 需要重新认证，403 → 权限范围或套餐问题，429 → 等待后重试（X 按端点进行速率限制）。
6. 即使在排查问题时，也绝不将 `~/.xurl` 的内容粘贴回对话中。
7. 对费用有疑问时：X 的 API 有付费层级和按端点的速率限制。未经用户明确确认，不要运行无限制的循环或流。

---

## 故障排除

| 症状 | 原因 | 修复方式 |
| --- | --- | --- |
| `auth status` 在默认应用上显示 `oauth2: (none)` | 令牌保存到了内置 `default` 配置中（没有 client-id/secret） | 重新运行 `xurl auth oauth2 --app my-app`，然后 `xurl auth default my-app` |
| OAuth 时出现 `unauthorized_client` | 在 X 控制台中应用类型设置为了"原生应用" | 改为"Web 应用、自动化应用或机器人" |
| OAuth 后立即出现 `UsernameNotFound` / 403 | X 未从 `/2/users/me` 返回用户名 | `xurl auth oauth2 --app my-app YOUR_HANDLE`（xurl v1.1.0+） |
| 每次读取都返回 401 | 令牌已过期或默认应用错误 | 检查 `xurl auth status`——确认 `▸` 指向具有 oauth2 令牌的应用 |
| `client-forbidden` / `client-not-enrolled` | X 平台注册问题 | 开发者控制台 → 应用 → 管理 → 生产环境 |
| `CreditsDepleted` | X API 余额为 $0 | 在开发者控制台 → 账单中购买额度 |
| 搜索 / 时间线返回 429 | 触发了按端点的速率限制 | 暂停，以更小的 `-n` 重试，或等待重置窗口 |

---

## 备注

- **费用：**X API 对于有意义的用量是付费的。许多故障是套餐或速率限制问题，而非技能问题。
- **权限范围：**OAuth 2.0 令牌使用宽泛的权限范围；特定读取出现 403 通常意味着令牌缺少某个权限范围——让用户重新运行 `xurl auth oauth2`。
- **令牌刷新：**OAuth 2.0 令牌自动刷新；无需干预。
- **多应用：**`xurl --app NAME ...` 可在不更改默认设置的情况下针对特定应用执行单次读取。
- **令牌存储：**`~/.xurl` 是 YAML 格式。请将其视为私钥。绝不读取或发送到 LLM 上下文中。

---

## 归属

- 底层 CLI：<https://github.com/xdevplatform/xurl>（X 开发者平台）。
- 本技能封装了 CLI 的读取命令，并记录了 Agent 端的安全规则。除本 SKILL.md 外不附带任何代码。