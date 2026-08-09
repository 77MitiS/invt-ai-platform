---
name: pptx
description: "PowerPoint 演示文稿（.pptx）的全能处理：创建幻灯片、商业演示、阅读解析文本、编辑修改、合并拆分、模板布局、演讲者备注与批注。触发词：幻灯片、演示、deck、.pptx。"
dependencies:
  commands:
    - python3
  tools:
    - skillScriptTool
    - skillFileTool
    - delegateToAgent
platforms:
  - macos
  - linux
  - windows
---

> **重要：** 所有 `scripts/` 路径均相对于本技能目录。
> 使用 `run_skill_script` 工具执行脚本，或通过以下命令运行：`cd {this_skill_dir} && python scripts/...`

# PPTX 技能

## 前置条件

- **markitdown[pptx]**：从演示文稿中提取文本
- **Pillow**：缩略图网格生成
- **pptxgenjs**（`npm install -g pptxgenjs`）：从零创建演示文稿
- **LibreOffice**（`soffice`）：演示文稿转 PDF
- **pdftoppm**（poppler-utils）：PDF 转图像，用于缩略图/可视化工作流
- 若 `pdftoppm` 不可用，Python 回退方案可使用 `pdf2image`。

## 快速参考

| 任务 | 指南 |
|------|------|
| 读取/分析内容 | `python -m markitdown presentation.pptx` |
| 基于模板编辑或创建 | 解包 → 修改 → 打包工作流 |
| 从零创建 | 使用 pptxgenjs (npm) |

---

## 读取内容

```bash
# 文本提取
python -m markitdown presentation.pptx

# 可视化概览（缩略图网格）
python scripts/thumbnail.py presentation.pptx

# 原始 XML 访问
python scripts/office/unpack.py presentation.pptx unpacked/
```

---

## 编辑工作流

1. 使用 `thumbnail.py` 分析模板
2. 解包：`python scripts/office/unpack.py presentation.pptx unpacked/`
3. 添加/删除幻灯片：`python scripts/add_slide.py unpacked/ --source <slide_num>`
4. 编辑 `unpacked/ppt/slides/` 中的 XML 内容
5. 清理孤立项：`python scripts/clean.py unpacked/`
6. 打包：`python scripts/office/pack.py unpacked/ output.pptx --original presentation.pptx`

### 添加幻灯片

```bash
# 复制已有幻灯片
python scripts/add_slide.py unpacked/ --source 2

# 从版式模板添加
python scripts/add_slide.py unpacked/ --layout 1
```

### 清理

```bash
# 移除孤立幻灯片、未引用的媒体文件，更新内容类型
python scripts/clean.py unpacked/
```

### 生成缩略图

```bash
# 创建全部幻灯片的缩略图网格
python scripts/thumbnail.py presentation.pptx

# 自定义输出
python scripts/thumbnail.py presentation.pptx --output thumbs.png --cols 4
```

---

## 从零创建

当无模板可用时，使用 `pptxgenjs`（Node.js）。安装：`npm install -g pptxgenjs`

---

## 设计理念

**不要制作无聊的幻灯片。** 白底加纯项目符号不会给任何人留下印象。

### 开始之前

- **选择大胆且贴合内容的配色方案**：应当让人觉得是为这个主题专门设计的
- **主次分明**：一种颜色应占主导地位（60-70%），搭配 1-2 种辅助色调
- **深/浅对比**：标题页和结尾页用深色背景，内容页用浅色背景
- **坚持一种视觉母题**：选择一种独特的元素并反复使用

### 配色方案

| 主题 | 主色 | 辅色 | 强调色 |
|-------|---------|-----------|--------|
| **午夜行政** | `1E2761`（藏青） | `CADCFC`（冰蓝） | `FFFFFF`（白） |
| **森林与苔藓** | `2C5F2D`（森林绿） | `97BC62`（苔藓绿） | `F5F5F5`（奶油） |
| **珊瑚活力** | `F96167`（珊瑚红） | `F9E795`（金） | `2F3C7E`（藏青） |
| **温暖赤陶** | `B85042`（赤陶） | `E7E8D1`（沙） | `A7BEAE`（鼠尾草绿） |
| **海洋渐变** | `065A82`（深海蓝） | `1C7293`（青） | `21295C`（午夜蓝） |
| **炭黑极简** | `36454F`（炭灰） | `F2F2F2`（米白） | `212121`（黑） |
| **蓝绿信赖** | `028090`（蓝绿） | `00A896`（海沫绿） | `02C39A`（薄荷） |
| **浆果与奶油** | `6D2E46`（浆果） | `A26769`（灰玫瑰） | `ECE2D0`（奶油） |
| **鼠尾草宁静** | `84B59F`（鼠尾草） | `69A297`（桉树） | `50808E`（板岩） |
| **樱桃大胆** | `990011`（樱桃红） | `FCF6F5`（米白） | `2F3C7E`（藏青） |

### 每张幻灯片

**每张幻灯片都需要一个视觉元素**——图片、图表、图标或形状。

**布局选项：**
- 双栏（左侧文字，右侧插图）
- 图标 + 文字行（彩色圆形中的图标、加粗标题、下方描述）
- 2×2 或 2×3 网格
- 半幅出血图片叠加内容

**数据展示：**
- 大数据标注（60-72pt 的大数字，下方配小字标签）
- 对比列（之前 vs 之后、优点 vs 缺点）
- 时间线或流程图（编号步骤、箭头）

### 排版

| 标题字体 | 正文字体 |
|-------------|-----------|
| Georgia | Calibri |
| Arial Black | Arial |
| Calibri | Calibri Light |
| Cambria | Calibri |
| Trebuchet MS | Calibri |

| 元素 | 字号 |
|------|------|
| 幻灯片标题 | 36-44pt 加粗 |
| 章节标题 | 20-24pt 加粗 |
| 正文 | 14-16pt |
| 注释 | 10-12pt 弱化 |

### 间距

- 最小边距 0.5 英寸
- 内容块之间 0.3-0.5 英寸
- 留出呼吸空间

### 避免（常见错误）

- 不要在不同幻灯片中重复相同的布局
- 不要将正文居中——段落和列表应左对齐
- 不要吝啬字号对比
- 不要默认使用蓝色——选择贴合主题的颜色
- 不要制作纯文字幻灯片——添加视觉元素
- 不要忘记文本框内边距
- **绝不要**在标题下方使用强调线——这是 AI 生成幻灯片的标志

---

## QA（必须执行）

**假设一定有问题。你的工作就是找到它们。**

### 内容 QA

```bash
python -m markitdown output.pptx
```

检查缺失内容、错别字、顺序错误。检查是否有残留的占位符文本：

```bash
python -m markitdown output.pptx | grep -iE "xxxx|lorem|ipsum"
```

### 视觉 QA

将幻灯片转换为图片，然后检查：

```bash
python scripts/office/soffice.py --headless --convert-to pdf output.pptx
pdftoppm -jpeg -r 150 output.pdf slide
```

检查以下问题：元素重叠、文本溢出、低对比度文本、间距不均匀、边距不足。

### 子代理视觉 QA（全新视角）

对于高规格演示文稿，将视觉检查委派给一个独立代理——该代理**未曾**参与创建过程。全新视角能发现制作者遗漏的问题。

```
delegateToAgent(
  agentName="strong-agent",
  task="[视觉 QA 请求] 以全新审阅者的身份检查附带的演示文稿幻灯片。
你对这些幻灯片的制作过程一无所知——当作第一次看到它们来对待。

幻灯片位置：<幻灯片图片或 pptx 的路径>

检查以下事项：
1. 任何文本被截断或溢出边框的幻灯片
2. 低对比度（例如浅色文字在浅色背景上）
3. 重复布局——超过 2 张幻灯片结构完全相同
4. 没有任何视觉元素的纯文字幻灯片
5. 幻灯片标题下方的强调线（AI 生成幻灯片的标志）
6. 任何残留的占位符文本（XXXX、lorem、[在此插入]）
7. 正文字号低于 14pt

对每个问题，说明：幻灯片编号、问题类型、你看到了什么。
如果一切正常，请明确说明。"
)
```

在声明演示文稿完成之前，根据子代理的发现采取行动。

### 验证循环

1. 生成幻灯片 -> 转换为图片 -> 检查
2. 列出发现的问题
3. 修复问题
4. 重新验证受影响的幻灯片
5. 重复直至干净
6. （高规格场景）运行子代理视觉 QA，进行全新视角检查

---

## 转换为图片

```bash
python scripts/office/soffice.py --headless --convert-to pdf output.pptx
pdftoppm -jpeg -r 150 output.pdf slide
```

生成 `slide-01.jpg`、`slide-02.jpg` 等。