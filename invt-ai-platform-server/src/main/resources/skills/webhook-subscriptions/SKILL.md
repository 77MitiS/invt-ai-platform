```yaml
---
name: webhook-subscriptions
description: 'Webhook 订阅：事件驱动的 Agent 自动运行。'
version: 1.1.0
tags:
- webhook
- 事件
- 自动化
- 集成
- 通知
- 推送
author: ported
---
# Webhook 订阅

创建动态 webhook 订阅，让外部服务（GitHub、GitLab、Stripe、CI/CD、IoT 传感器、监控工具）可以通过向某个 URL 发送 POST 事件来触发 Agent 运行。

## 设置（必须先完成）

必须先启用 webhook 平台才能创建订阅。检查方式：
```bash
the agent webhook list
```

如果提示 "Webhook platform is not enabled"，请进行设置：

### 方式一：安装向导
```bash
the agent gateway setup
```
按提示启用 webhook、设置端口并配置全局 HMAC 密钥。

### 方式二：手动配置
在 `~/.invt/config.yaml` 中添加：
```yaml
platforms:
  webhook:
    enabled: true
    extra:
      host: "0.0.0.0"
      port: 8644
      secret: "generate-a-strong-secret-here"
```

### 方式三：环境变量
在 `~/.invt/.env` 中添加：
```bash
WEBHOOK_ENABLED=true
WEBHOOK_PORT=8644
WEBHOOK_SECRET=generate-a-strong-secret-here
```

配置完成后，启动（或重启）网关：
```bash
the agent gateway run
# 如果使用 systemd：
systemctl --user restart the agent-gateway
```

验证是否运行中：
```bash
curl http://localhost:8644/health
```

## 命令

所有管理操作均通过 `the agent webhook` CLI 命令完成：

### 创建订阅
```bash
the agent webhook subscribe <名称> \
  --prompt "提示模板，可使用 {payload.字段}" \
  --events "事件1,事件2" \
  --description "此订阅的用途" \
  --skills "技能1,技能2" \
  --deliver telegram \
  --deliver-chat-id "12345" \
  --secret "可选的自定义密钥"
```

返回 webhook URL 和 HMAC 密钥。用户在自己的服务中配置向该 URL 发送 POST 请求。

### 列出订阅
```bash
the agent webhook list
```

### 删除订阅
```bash
the agent webhook remove <名称>
```

### 测试订阅
```bash
the agent webhook test <名称>
the agent webhook test <名称> --payload '{"key": "value"}'
```

## 提示模板

提示支持使用 `{点号标记}` 访问嵌套的 payload 字段：

- `{issue.title}` — GitHub Issue 标题
- `{pull_request.user.login}` — PR 作者
- `{data.object.amount}` — Stripe 支付金额
- `{sensor.temperature}` — IoT 传感器读数

如果未指定提示，完整的 JSON payload 将被直接转储到 Agent 提示中。

## 常见模式

### GitHub：新 Issue
```bash
the agent webhook subscribe github-issues \
  --events "issues" \
  --prompt "新的 GitHub Issue #{issue.number}: {issue.title}\n\n操作: {action}\n作者: {issue.user.login}\n内容:\n{issue.body}\n\n请对此 Issue 进行分类处理。" \
  --deliver telegram \
  --deliver-chat-id "-100123456789"
```

然后在 GitHub 仓库 Settings → Webhooks → Add webhook 中：
- Payload URL：返回的 webhook_url
- Content type：application/json
- Secret：返回的 secret
- Events：选择 "Issues"

### GitHub：PR 审查
```bash
the agent webhook subscribe github-prs \
  --events "pull_request" \
  --prompt "PR #{pull_request.number} {action}: {pull_request.title}\n作者: {pull_request.user.login}\n分支: {pull_request.head.ref}\n\n{pull_request.body}" \
  --skills "github-code-review" \
  --deliver github_comment
```

### Stripe：支付事件
```bash
the agent webhook subscribe stripe-payments \
  --events "payment_intent.succeeded,payment_intent.payment_failed" \
  --prompt "支付 {data.object.status}: {data.object.amount} 分，来自 {data.object.receipt_email}" \
  --deliver telegram \
  --deliver-chat-id "-100123456789"
```

### CI/CD：构建通知
```bash
the agent webhook subscribe ci-builds \
  --events "pipeline" \
  --prompt "构建 {object_attributes.status}，项目 {project.name}，分支 {object_attributes.ref}\n提交: {commit.message}" \
  --deliver discord \
  --deliver-chat-id "1234567890"
```

### 通用监控告警
```bash
the agent webhook subscribe alerts \
  --prompt "告警: {alert.name}\n严重程度: {alert.severity}\n消息: {alert.message}\n\n请排查并建议修复方案。" \
  --deliver origin
```

### 直接投递（无 Agent 调用，零 LLM 成本）

如果只需要将通知推送到用户聊天中——无需推理、不走 Agent 循环——添加 `--deliver-only`。渲染后的 `--prompt` 模板将直接作为消息正文投递到目标适配器。

适用于：
- 外部服务推送通知（Supabase/Firebase webhook → Telegram）
- 需要原样转发的监控告警
- Agent 间通知：一个 Agent 向另一个 Agent 的用户传达消息
- 任何不需要 LLM 处理的 webhook

```bash
the agent webhook subscribe antenna-matches \
  --deliver telegram \
  --deliver-chat-id "123456789" \
  --deliver-only \
  --prompt "🎉 新匹配: {match.user_name} 与你匹配成功！" \
  --description "Antenna 匹配通知"
```

投递成功时 POST 返回 `200 OK`，目标投递失败时返回 `502`——上游服务可据此进行智能重试。HMAC 认证、速率限制和幂等性仍然生效。

要求 `--deliver` 为真实目标（telegram、discord、slack、github_comment 等）——`--deliver log` 会被拒绝，因为仅记录日志的直接投递没有意义。

## 安全

- 每个订阅自动生成 HMAC-SHA256 密钥（也可通过 `--secret` 自行指定）
- Webhook 适配器会对每次收到的 POST 请求验证签名
- config.yaml 中的静态路由不会被动态订阅覆盖
- 订阅持久化到 `~/.invt/webhook_subscriptions.json`

## 工作原理

1. `the agent webhook subscribe` 写入 `~/.invt/webhook_subscriptions.json`
2. Webhook 适配器在每次收到请求时热加载该文件（基于 mtime 判断，开销可忽略）
3. 当有 POST 请求匹配到某条路由时，适配器格式化提示并触发 Agent 运行
4. Agent 的回复将被投递到配置的目标渠道（Telegram、Discord、GitHub 评论等）

## 故障排查

如果 webhook 不工作：

1. **网关是否在运行？** 用 `systemctl --user status the agent-gateway` 或 `ps aux | grep gateway` 检查
2. **Webhook 服务器是否在监听？** `curl http://localhost:8644/health` 应返回 `{"status": "ok"}`
3. **检查网关日志：** `grep webhook ~/.invt/logs/gateway.log | tail -20`
4. **签名不匹配？** 确认服务中的密钥与 `the agent webhook list` 返回的一致。GitHub 发送的是 `X-Hub-Signature-256`，GitLab 发送的是 `X-Gitlab-Token`。
5. **防火墙/NAT？** Webhook URL 必须能被外部服务访问到。本地开发可使用隧道工具（ngrok、cloudflared）。
6. **事件类型不对？** 检查 `--events` 过滤器是否与服务发送的事件匹配。使用 `the agent webhook test <名称>` 验证路由是否正常。
```