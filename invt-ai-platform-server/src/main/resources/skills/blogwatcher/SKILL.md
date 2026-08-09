---
name: blogwatcher
description: 通过 blogwatcher-cli 监控博客与 RSS/Atom 订阅源。
version: 2.0.0
optional: true
requires:
- key: blogwatcher-cli
  type: binary
  check: blogwatcher-cli
  optional: false
tags:
- RSS 订阅
- 博客
- 订阅源阅读器
- 监控
author: ported
---
# Blogwatcher

使用 `blogwatcher-cli` 工具追踪博客和 RSS/Atom 订阅源更新。支持自动发现订阅源、HTML 抓取回退、OPML 导入以及已读/未读文章管理。

## 安装

任选一种方式：

- **Go：** `go install github.com/JulienTant/blogwatcher-cli/cmd/blogwatcher-cli@latest`
- **Docker：** `docker run --rm -v blogwatcher-cli:/data ghcr.io/julientant/blogwatcher-cli`
- **二进制文件（Linux amd64）：** `curl -sL https://github.com/JulienTant/blogwatcher-cli/releases/latest/download/blogwatcher-cli_linux_amd64.tar.gz | tar xz -C /usr/local/bin blogwatcher-cli`
- **二进制文件（Linux arm64）：** `curl -sL https://github.com/JulienTant/blogwatcher-cli/releases/latest/download/blogwatcher-cli_linux_arm64.tar.gz | tar xz -C /usr/local/bin blogwatcher-cli`
- **二进制文件（macOS Apple Silicon）：** `curl -sL https://github.com/JulienTant/blogwatcher-cli/releases/latest/download/blogwatcher-cli_darwin_arm64.tar.gz | tar xz -C /usr/local/bin blogwatcher-cli`
- **二进制文件（macOS Intel）：** `curl -sL https://github.com/JulienTant/blogwatcher-cli/releases/latest/download/blogwatcher-cli_darwin_amd64.tar.gz | tar xz -C /usr/local/bin blogwatcher-cli`

所有发布版本：https://github.com/JulienTant/blogwatcher-cli/releases

### Docker 持久化存储

默认情况下数据库位于 `~/.blogwatcher-cli/blogwatcher-cli.db`。在 Docker 中，容器重启后数据会丢失。使用 `BLOGWATCHER_DB` 环境变量或挂载数据卷来持久化存储：

```bash
# 命名数据卷（最简单）
docker run --rm -v blogwatcher-cli:/data -e BLOGWATCHER_DB=/data/blogwatcher-cli.db ghcr.io/julientant/blogwatcher-cli scan

# 主机目录挂载
docker run --rm -v /path/on/host:/data -e BLOGWATCHER_DB=/data/blogwatcher-cli.db ghcr.io/julientant/blogwatcher-cli scan
```

### 从原版 blogwatcher 迁移

如果从 `Hyaxia/blogwatcher` 升级，请移动数据库：

```bash
mv ~/.blogwatcher/blogwatcher.db ~/.blogwatcher-cli/blogwatcher-cli.db
```

二进制文件名已从 `blogwatcher` 改为 `blogwatcher-cli`。

## 常用命令

### 管理博客

- 添加博客：`blogwatcher-cli add "My Blog" https://example.com`
- 添加时明确指定订阅源：`blogwatcher-cli add "My Blog" https://example.com --feed-url https://example.com/feed.xml`
- 添加时使用 HTML 抓取：`blogwatcher-cli add "My Blog" https://example.com --scrape-selector "article h2 a"`
- 列出已追踪的博客：`blogwatcher-cli blogs`
- 移除博客：`blogwatcher-cli remove "My Blog" --yes`
- 从 OPML 导入：`blogwatcher-cli import subscriptions.opml`

### 扫描与阅读

- 扫描所有博客：`blogwatcher-cli scan`
- 扫描单个博客：`blogwatcher-cli scan "My Blog"`
- 列出未读文章：`blogwatcher-cli articles`
- 列出全部文章：`blogwatcher-cli articles --all`
- 按博客筛选：`blogwatcher-cli articles --blog "My Blog"`
- 按分类筛选：`blogwatcher-cli articles --category "Engineering"`
- 标记文章为已读：`blogwatcher-cli read 1`
- 标记文章为未读：`blogwatcher-cli unread 1`
- 全部标记为已读：`blogwatcher-cli read-all`
- 将某博客全部标记为已读：`blogwatcher-cli read-all --blog "My Blog" --yes`

## 环境变量

所有参数均可通过 `BLOGWATCHER_` 前缀的环境变量设置：

| 变量 | 说明 |
|---|---|
| `BLOGWATCHER_DB` | SQLite 数据库文件路径 |
| `BLOGWATCHER_WORKERS` | 并发扫描工作线程数（默认：8） |
| `BLOGWATCHER_SILENT` | 扫描时仅输出「扫描完成」 |
| `BLOGWATCHER_YES` | 跳过确认提示 |
| `BLOGWATCHER_CATEGORY` | 按分类筛选文章的默认值 |

## 输出示例

```
$ blogwatcher-cli blogs
已追踪的博客 (1)：

  xkcd
    URL: https://xkcd.com
    Feed: https://xkcd.com/atom.xml
    上次扫描: 2026-04-03 10:30
```

```
$ blogwatcher-cli scan
正在扫描 1 个博客...

  xkcd
    来源: RSS | 找到: 4 | 新增: 4

总共发现 4 篇新文章！
```

```
$ blogwatcher-cli articles
未读文章 (2)：

  [1] [new] Barrel - Part 13
       博客: xkcd
       URL: https://xkcd.com/3095/
       发布日期: 2026-04-02
       分类: Comics, Science

  [2] [new] Volcano Fact
       博客: xkcd
       URL: https://xkcd.com/3094/
       发布日期: 2026-04-01
       分类: Comics
```

## 备注

- 未提供 `--feed-url` 时，会自动从博客首页发现 RSS/Atom 订阅源。
- 如果 RSS 失败且已配置 `--scrape-selector`，则回退到 HTML 抓取。
- RSS/Atom 订阅源中的分类会被存储，并可用于筛选文章。
- 支持从 Feedly、Inoreader、NewsBlur 等导出的 OPML 文件批量导入博客。
- 数据库默认存储在 `~/.blogwatcher-cli/blogwatcher-cli.db`（可通过 `--db` 或 `BLOGWATCHER_DB` 覆盖）。
- 使用 `blogwatcher-cli <command> --help` 查看所有可用参数和选项。