---
name: himalaya
description: "通过 Himalaya CLI 在终端管理邮件（IMAP/SMTP）。支持列表查看、读写、回复、转发、搜索与整理，多账户管理，MML 格式撰写邮件。"
optional: true
dependencies:
  commands:
    - himalaya
platforms:
  - macos
  - linux
---

# Himalaya 邮件 CLI

Himalaya 是一个 CLI 邮件客户端，让你能通过终端，使用 IMAP、SMTP、Notmuch 或 Sendmail 后端管理邮件。

## 参考文档

- `references/configuration.md`（配置文件设置 + IMAP/SMTP 认证）

## 前置条件

1. **Himalaya CLI** — `himalaya` 二进制文件必须已在 `PATH` 中。用 `himalaya --version` 检查。
   - **推荐：v1.2.0 或更新版本。** 旧版本在某些 IMAP 服务器上可能失败。
2. 位于 `~/.config/himalaya/config.toml` 的配置文件
3. IMAP/SMTP 凭证已配置（密码安全存储）

## 配置设置

运行交互式向导设置账户：

```bash
himalaya account configure default
```

或手动创建 `~/.config/himalaya/config.toml`：

```toml
[accounts.personal]
email = "you@example.com"
display-name = "Your Name"
default = true

backend.type = "imap"
backend.host = "imap.example.com"
backend.port = 993
backend.encryption.type = "tls"
backend.login = "you@example.com"
backend.auth.type = "password"
backend.auth.cmd = "pass show email/imap"  # 或使用 keyring

message.send.backend.type = "smtp"
message.send.backend.host = "smtp.example.com"
message.send.backend.port = 587
message.send.backend.encryption.type = "start-tls"
message.send.backend.login = "you@example.com"
message.send.backend.auth.type = "password"
message.send.backend.auth.cmd = "pass show email/smtp"
```

如果使用 163 邮箱，需添加 `backend.extensions.id.send-after-auth = true`。

## 常用操作

### 列出文件夹

```bash
himalaya folder list
```

### 列出邮件

```bash
himalaya envelope list                           # 收件箱（默认）
himalaya envelope list --folder "Sent"           # 指定文件夹
himalaya envelope list --page 1 --page-size 20   # 分页显示
```

### 搜索邮件

```bash
himalaya envelope list from john@example.com subject meeting
```

### 阅读邮件

```bash
himalaya message read 42          # 纯文本
himalaya message export 42 --full # 原始 MIME
```

### 发送 / 撰写邮件

**推荐：** 使用 `template write | template send` 管道：

```bash
export EDITOR=cat
himalaya template write \
  -H "To: recipient@example.com" \
  -H "Subject: Email Subject" \
  "Email body content" | himalaya template send
```

**含抄送：**

```bash
export EDITOR=cat
himalaya template write \
  -H "To: recipient@example.com" \
  -H "Cc: cc@example.com" \
  -H "Subject: Email Subject" \
  "Email body content" | himalaya template send
```

**含附件（Python 后备方案）：**

```python
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.base import MIMEBase
from email import encoders

msg = MIMEMultipart()
msg['From'] = 'sender@example.com'
msg['To'] = 'recipient@example.com'
msg['Subject'] = 'Email with attachment'
msg.attach(MIMEText('Email body', 'plain'))

with open('/path/to/file.pdf', 'rb') as f:
    part = MIMEBase('application', 'octet-stream')
    part.set_payload(f.read())
    encoders.encode_base64(part)
    part.add_header('Content-Disposition', 'attachment; filename="file.pdf"')
    msg.attach(part)

server = smtplib.SMTP_SSL('smtp.example.com', 465)
server.login('sender@example.com', 'password')
server.send_message(msg)
server.quit()
```

**已知限制：**
- himalaya v1.1.0 中 MML 附件解析可能失败 — 请使用 Python 处理附件
- `message write` 在非交互模式下会挂起 — 请使用 `template write | template send`
- `message send` 可能因头部解析失败 — 请使用 `template send`

**配置要求：** 在 config.toml 中设置 `message.send.save-to-folder`：

```toml
[accounts.default]
message.send.save-to-folder = "Sent"
```

### 移动 / 复制邮件

```bash
himalaya message move 42 "Archive"
himalaya message copy 42 "Important"
```

### 删除邮件

```bash
himalaya message delete 42
```

### 管理标记

```bash
himalaya flag add 42 --flag seen
himalaya flag remove 42 --flag seen
```

## 多账户

```bash
himalaya account list                      # 列出账户
himalaya --account work envelope list      # 使用指定账户
```

## 附件

```bash
himalaya attachment download 42              # 保存附件
himalaya attachment download 42 --dir ~/dl   # 保存到指定目录
```

## 输出格式

```bash
himalaya envelope list --output json
himalaya envelope list --output plain
```

## 调试

```bash
RUST_LOG=debug himalaya envelope list
RUST_LOG=trace RUST_BACKTRACE=1 himalaya envelope list
```

## 提示

- 邮件 ID 相对于当前文件夹；切换文件夹后需重新列出。
- 使用 `pass`、系统钥匙串或命令安全存储密码。
- **自动化场景：** 始终使用 `template write | template send` 并配合 `export EDITOR=cat`。
- **163 邮箱：** 设置 `backend.extensions.id.send-after-auth = true` 和 `message.send.save-to-folder = "Sent"`。
- **文件夹名称：** 使用英文文件夹名以获得更好的兼容性。