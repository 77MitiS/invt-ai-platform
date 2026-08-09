```yaml
---
name: apple-notes
description: 通过 memo CLI 管理 Apple 备忘录：创建、搜索、编辑。
version: 1.0.0
optional: true
platforms:
- macos
requires:
- key: memo
  type: binary
  check: memo
  optional: false
tags:
- 备忘录
- Apple
- macOS
- 笔记
author: ported
---
# Apple 备忘录

使用 `memo` 在终端中直接管理 Apple 备忘录。笔记可通过 iCloud 在所有 Apple 设备间同步。

## 前提条件

- **macOS** 并安装 Notes.app
- 安装：`brew tap antoniorodr/memo && brew install antoniorodr/memo/memo`
- 收到提示时为 Notes.app 授予自动化访问权限（系统设置 → 隐私 → 自动化）

## 适用场景

- 用户要求创建、查看或搜索 Apple 备忘录
- 将信息保存到 Notes.app 以便跨设备访问
- 将笔记整理到文件夹
- 将笔记导出为 Markdown/HTML

## 不适用场景

- Obsidian 仓库管理 → 使用 `obsidian` 技能
- Bear Notes → 独立应用（此处不支持）
- 仅供 Agent 使用的快速笔记 → 改用 `memory` 工具

## 速查参考

### 查看笔记

```bash
memo notes                        # 列出所有笔记
memo notes -f "文件夹名称"         # 按文件夹筛选
memo notes -s "查询"               # 搜索笔记（模糊匹配）
```

### 创建笔记

```bash
memo notes -a                     # 交互式编辑器
memo notes -a "笔记标题"           # 带标题快速添加
```

### 编辑笔记

```bash
memo notes -e                     # 交互式选择并编辑
```

### 删除笔记

```bash
memo notes -d                     # 交互式选择并删除
```

### 移动笔记

```bash
memo notes -m                     # 将笔记移动到文件夹（交互式）
```

### 导出笔记

```bash
memo notes -ex                    # 导出为 HTML/Markdown
```

## 局限性

- 无法编辑包含图片或附件的笔记
- 交互式提示需要终端访问权限（必要时使用 pty=true）
- 仅限 macOS — 需要 Apple Notes.app

## 规则

1. 当用户需要跨设备同步（iPhone/iPad/Mac）时，优先使用 Apple 备忘录
2. 对于无需同步的 Agent 内部笔记，使用 `memory` 工具
3. 对于 Markdown 原生知识管理，使用 `obsidian` 技能
```