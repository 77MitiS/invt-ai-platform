---

name: docx
version: "1.1.0"
description: "Word 文档（.docx）的全能处理。创建新文档时优先使用内置工具 `renderDocx`（Markdown 转 .docx，毫秒级生成）；编辑已有 .docx 时使用此技能（解包/编辑 XML/打包、修订追踪、批注、图片处理、查找替换、格式转换）。触发词：Word 文档、.docx、编辑/提取/重构 Word 文件。不适用于 PDF、电子表格或与文档生成无关的编码任务。"
dependencies:
  commands:
    - python3
    - node
  tools:
    - skillScriptTool
    - skillFileTool
platforms:
  - macos
  - linux
  - windows
---

> **重要提示：** 所有 `scripts/` 路径均相对于本技能目录。
> 请使用 `run_skill_script` 工具执行脚本，或通过以下方式运行：`cd {this_skill_dir} && python scripts/...`

# DOCX 创建、编辑与分析

## 快速入门 —— 选择合适的工具

| 任务 | 推荐工具 |
|------|----------|
| **创建**新文档（报告 / 简历 / 合同 / 备忘录） | `renderDocx()` —— 毫秒级渲染，无需子进程 |
| **编辑**已有 .docx（内容 / 格式） | 下方的解包 → 编辑 XML → 打包工作流 |
| 添加修订追踪 / 批注 | 下方的解包 → 编辑 XML → 打包工作流 |
| GB/T 9704 公文 | `writeGongwen()`（仅 BmacClaw） |

### 创建新文档（推荐方式）

调用进程内 Java 工具 —— 无需 Node.js 安装，无需 fork，无需磁盘往返：

```
renderDocx(
  markdown="# 标题\n\n正文段落...",
  filename="月度报告",
  pageSize="A4"
)
```

返回形如 `[月度报告.docx](/api/v1/files/generated/<uuid>)` 的可点击链接，有效期 10 分钟。用户点击即可下载 —— Agent 无需后续操作。

`renderDocx` 支持标题（`#` `##` `###`）、粗体（`**文本**`）、项目符号列表（`- 项目`）、编号列表（`1. 项目`）、管道表格和普通段落。如需图片、页眉/页脚或精确的 OOXML 控制，请回退到下方的 docx-js 工作流。

## 前置条件

- **python-docx**（`pip install python-docx`）：直接读取结构并进行轻量编辑（段落、样式、表格）
- **docx**（`npm install -g docx`）：创建新文档
- **LibreOffice**（`soffice`）：将 `.doc` 转为 `.docx`、接受修订追踪以及导出 PDF
- **pandoc**：文本提取
- **pdftoppm**（poppler-utils）：文档转图片工作流
- 若 `pdftoppm` 不可用，Python 回退方案可能使用 `pdf2image`。
- 在 Windows 上，必须安装依赖并使其在 `PATH` 中可用；若缺失，报告依赖问题并停止（不要反复重试）。

## 概述

.docx 文件是一个包含 XML 文件的 ZIP 归档。

## 快速参考

| 任务 | 方法 |
|------|------|
| 读取/分析内容 | `pandoc` 或解包以获取原始 XML |
| 创建新文档 | 使用 `docx-js` —— 参见下方"创建新文档" |
| 编辑已有文档 | 解包 → 编辑 XML → 重新打包 —— 参见下方"编辑已有文档" |

### 将 .doc 转换为 .docx

旧版 `.doc` 文件在编辑前必须先转换：

```bash
python scripts/office/soffice.py --headless --convert-to docx document.doc
```

### 读取内容

**方案 A：python-docx（推荐用于结构化访问）**

安装：`pip install python-docx`。无需解压 ZIP 即可直接访问段落、样式、表格和元数据。

```python
from docx import Document

doc = Document("document.docx")

# 带样式的段落
for para in doc.paragraphs:
    print(f"[{para.style.name}] {para.text}")

# 表格
for i, table in enumerate(doc.tables):
    print(f"表格 {i+1}：")
    for row in table.rows:
        print([cell.text for cell in row.cells])

# 段落内的内联样式
for para in doc.paragraphs:
    for run in para.runs:
        print(f"  run: bold={run.bold} italic={run.italic} text={run.text!r}")
```

当你需要读取或轻度修改内容时，使用 python-docx。对于复杂的结构修改，回退到解包/XML 工作流。

**方案 B：pandoc（纯文本提取）**
```bash
# 带修订追踪的文本提取
pandoc --track-changes=all document.docx -o output.md

# 原始 XML 访问
python scripts/office/unpack.py document.docx unpacked/
```

### 转换为图片

```bash
python scripts/office/soffice.py --headless --convert-to pdf document.docx
pdftoppm -jpeg -r 150 document.pdf page
```

### 接受修订追踪

生成一个所有修订均已接受的干净文档（需要 LibreOffice）：

```bash
python scripts/accept_changes.py input.docx output.docx
```

---

## 创建新文档

使用 JavaScript 生成 .docx 文件，然后进行验证。安装：`npm install -g docx`

### 设置
```javascript
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, ImageRun,
        Header, Footer, AlignmentType, PageOrientation, LevelFormat, ExternalHyperlink,
        TableOfContents, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign, PageNumber, PageBreak } = require('docx');

const doc = new Document({ sections: [{ children: [/* 内容 */] }] });
Packer.toBuffer(doc).then(buffer => fs.writeFileSync("doc.docx", buffer));
```

### 验证
创建文件后，请进行验证。若验证失败，则解包，修复 XML，然后重新打包。
```bash
python scripts/office/validate.py doc.docx
```

### 页面大小

```javascript
// 关键：docx-js 默认 A4，而非 US Letter
// 始终显式设置页面大小以获得一致的结果
sections: [{
  properties: {
    page: {
      size: {
        width: 12240,   // 8.5 英寸（DXA 单位）
        height: 15840   // 11 英寸（DXA 单位）
      },
      margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } // 1 英寸页边距
    }
  },
  children: [/* 内容 */]
}]
```

**常见页面大小（DXA 单位，1440 DXA = 1 英寸）：**

| 纸张 | 宽度 | 高度 | 内容宽度（1 英寸页边距） |
|-------|-------|--------|---------------------------|
| US Letter | 12,240 | 15,840 | 9,360 |
| A4（默认） | 11,906 | 16,838 | 9,026 |

**横向方向：** docx-js 在内部会交换宽高，因此传入纵向尺寸，让它自行处理交换：
```javascript
size: {
  width: 12240,   // 将短边作为宽度传入
  height: 15840,  // 将长边作为高度传入
  orientation: PageOrientation.LANDSCAPE  // docx-js 会在 XML 中交换它们
},
// 内容宽度 = 15840 - 左边距 - 右边距（使用长边）
```

### 样式（覆盖内置标题样式）

使用 Arial 作为默认字体（普遍支持）。标题保持黑色以提高可读性。

```javascript
const doc = new Document({
  styles: {
    default: { document: { run: { font: "Arial", size: 24 } } }, // 12pt 默认
    paragraphStyles: [
      // 重要：使用精确的 ID 覆盖内置样式
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 32, bold: true, font: "Arial" },
        paragraph: { spacing: { before: 240, after: 240 }, outlineLevel: 0 } }, // TOC 需要 outlineLevel
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 28, bold: true, font: "Arial" },
        paragraph: { spacing: { before: 180, after: 180 }, outlineLevel: 1 } },
    ]
  },
  sections: [{
    children: [
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("标题")] }),
    ]
  }]
});
```

### 列表（绝不使用 Unicode 项目符号）

```javascript
// 正确 —— 使用 numbering 配置与 LevelFormat.BULLET
const doc = new Document({
  numbering: {
    config: [
      { reference: "bullets",
        levels: [{ level: 0, format: LevelFormat.BULLET, text: "\u2022", alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
      { reference: "numbers",
        levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
    ]
  },
  sections: [{
    children: [
      new Paragraph({ numbering: { reference: "bullets", level: 0 },
        children: [new TextRun("项目符号项")] }),
      new Paragraph({ numbering: { reference: "numbers", level: 0 },
        children: [new TextRun("编号项")] }),
    ]
  }]
});

// 每个 reference 创建独立的编号序列
// 相同 reference = 延续（1,2,3 然后 4,5,6）
// 不同 reference = 重新开始（1,2,3 然后 1,2,3）
```

### 表格

**关键：表格需要双重宽度设置** —— 同时在表格上设置 `columnWidths` 并在每个单元格上设置 `width`。

```javascript
// 关键：使用 ShadingType.CLEAR（而非 SOLID）以防止黑色背景
const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const borders = { top: border, bottom: border, left: border, right: border };

new Table({
  width: { size: 9360, type: WidthType.DXA }, // 始终使用 DXA
  columnWidths: [4680, 4680], // 必须与表格宽度总和一致（DXA：1440 = 1 英寸）
  rows: [
    new TableRow({
      children: [
        new TableCell({
          borders,
          width: { size: 4680, type: WidthType.DXA }, // 每个单元格也要设置
          shading: { fill: "D5E8F0", type: ShadingType.CLEAR }, // 使用 CLEAR 而非 SOLID
          margins: { top: 80, bottom: 80, left: 120, right: 120 },
          children: [new Paragraph({ children: [new TextRun("单元格")] })]
        })
      ]
    })
  ]
})
```

**宽度规则：**
- **始终使用 `WidthType.DXA`** —— 绝不使用 `WidthType.PERCENTAGE`
- 表格宽度必须等于 `columnWidths` 的总和
- 单元格 `width` 必须与对应的 `columnWidth` 匹配

### 图片

```javascript
// 关键：type 参数是必需的
new Paragraph({
  children: [new ImageRun({
    type: "png", // 必需：png、jpg、jpeg、gif、bmp、svg
    data: fs.readFileSync("image.png"),
    transformation: { width: 200, height: 150 },
    altText: { title: "标题", description: "描述", name: "名称" } // 三项全部必填
  })]
})
```

### 分页符

```javascript
// PageBreak 必须放在 Paragraph 内部
new Paragraph({ children: [new PageBreak()] })
```

### 目录

```javascript
// 标题必须仅使用 HeadingLevel —— 不能使用自定义样式
new TableOfContents("目录", { hyperlink: true, headingStyleRange: "1-3" })
```

### 页眉/页脚

```javascript
sections: [{
  properties: {
    page: { margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } }
  },
  headers: {
    default: new Header({ children: [new Paragraph({ children: [new TextRun("页眉")] })] })
  },
  footers: {
    default: new Footer({ children: [new Paragraph({
      children: [new TextRun("第 "), new TextRun({ children: [PageNumber.CURRENT] }), new TextRun(" 页")]
    })] })
  },
  children: [/* 内容 */]
}]
```

### docx-js 的关键规则

- **显式设置页面大小** —— docx-js 默认 A4
- **横向：传入纵向尺寸** —— docx-js 在内部交换宽高
- **绝不使用 `\n`** —— 使用单独的 Paragraph 元素
- **绝不使用 Unicode 项目符号** —— 使用 `LevelFormat.BULLET` 配合 numbering 配置
- **PageBreak 必须放在 Paragraph 中**
- **ImageRun 需要 `type` 参数**
- **始终使用 DXA 设置表格 `width`** —— 绝不使用 `WidthType.PERCENTAGE`
- **表格需要双重宽度** —— `columnWidths` 数组与单元格 `width` 两者皆需
- **使用 `ShadingType.CLEAR`** —— 表格底纹绝不使用 SOLID
- **TOC 仅需要 HeadingLevel**
- **覆盖内置样式** —— 使用精确 ID："Heading1"、"Heading2" 等
- **包含 `outlineLevel`** —— TOC 需要（H1 为 0，H2 为 1，以此类推）

---

## 编辑已有文档

**严格按以下 3 个步骤顺序执行。**

### 第 1 步：解包
```bash
python scripts/office/unpack.py document.docx unpacked/
```
提取 XML，格式化，合并相邻 run，并将智能引号转换为 XML 实体。使用 `--merge-runs false` 跳过 run 合并。

### 第 2 步：编辑 XML

编辑 `unpacked/word/` 中的文件。请参阅下方 XML 参考中的模式。

**使用 "Invt" 作为作者** 进行修订追踪和批注，除非用户明确要求使用其他名称。

**关键：新内容使用智能引号：**
```xml
<w:t>这里是引号：&#x201C;你好&#x201D;</w:t>
```
| 实体 | 字符 |
|--------|-----------|
| `&#x2018;` | '（左单引号） |
| `&#x2019;` | '（右单引号 / 撇号） |
| `&#x201C;` | "（左双引号） |
| `&#x201D;` | "（右双引号） |

**添加批注：** 使用 `comment.py` 处理样板代码：
```bash
python scripts/comment.py unpacked/ 0 "批注文本，含 &amp; 和 &#x2019;"
python scripts/comment.py unpacked/ 1 "回复文本" --parent 0  # 回复批注 0
python scripts/comment.py unpacked/ 0 "文本" --author "自定义作者"
```
然后在 document.xml 中添加标记（参见 XML 参考中的"批注"部分）。

### 第 3 步：打包
```bash
python scripts/office/pack.py unpacked/ output.docx --original document.docx
```
验证并自动修复、压缩 XML，创建 DOCX。使用 `--validate false` 跳过验证。

**自动修复将处理：**
- `durableId` >= 0x7FFFFFFF（重新生成有效 ID）
- 包含空白字符的 `<w:t>` 缺少 `xml:space="preserve"`

### 常见陷阱

- **替换整个 `<w:r>` 元素**：添加修订追踪时，替换整个 `<w:r>...</w:r>` 块。
- **保留 `<w:rPr>` 格式**：将原始 run 的 `<w:rPr>` 块复制到你的修订追踪 run 中。

---

## XML 参考

### 模式合规

- **`<w:pPr>` 中的元素顺序**：`<w:pStyle>`、`<w:numPr>`、`<w:spacing>`、`<w:ind>`、`<w:jc>`，`<w:rPr>` 放在最后
- **空白字符**：为包含前导/尾随空格的 `<w:t>` 添加 `xml:space="preserve"`
- **RSID**：必须为 8 位十六进制数字（例如 `00AB1234`）

### 修订追踪

**插入：**
```xml
<w:ins w:id="1" w:author="Invt" w:date="2025-01-01T00:00:00Z">
  <w:r><w:t>插入的文本</w:t></w:r>
</w:ins>
```

**删除：**
```xml
<w:del w:id="2" w:author="Invt" w:date="2025-01-01T00:00:00Z">
  <w:r><w:delText>删除的文本</w:delText></w:r>
</w:del>
```

**在 `<w:del>` 内部**：使用 `<w:delText>` 替代 `<w:t>`，使用 `<w:delInstrText>` 替代 `<w:instrText>`。

**最小化编辑** —— 仅标记发生变更的部分：
```xml
<!-- 将 "30 天" 改为 "60 天" -->
<w:r><w:t>期限为 </w:t></w:r>
<w:del w:id="1" w:author="Invt" w:date="...">
  <w:r><w:delText>30</w:delText></w:r>
</w:del>
<w:ins w:id="2" w:author="Invt" w:date="...">
  <w:r><w:t>60</w:t></w:r>
</w:ins>
<w:r><w:t> 天。</w:t></w:r>
```

**删除整个段落** —— 将段落标记标记为已删除：
```xml
<w:p>
  <w:pPr>
    <w:rPr>
      <w:del w:id="1" w:author="Invt" w:date="2025-01-01T00:00:00Z"/>
    </w:rPr>
  </w:pPr>
  <w:del w:id="2" w:author="Invt" w:date="2025-01-01T00:00:00Z">
    <w:r><w:delText>正在删除的整个段落内容...</w:delText></w:r>
  </w:del>
</w:p>
```

**拒绝另一作者的插入：**
```xml
<w:ins w:author="Jane" w:id="5">
  <w:del w:author="Invt" w:id="10">
    <w:r><w:delText>他们插入的文本</w:delText></w:r>
  </w:del>
</w:ins>
```

**恢复另一作者的删除：**
```xml
<w:del w:author="Jane" w:id="5">
  <w:r><w:delText>已删除的文本</w:delText></w:r>
</w:del>
<w:ins w:author="Invt" w:id="10">
  <w:r><w:t>已删除的文本</w:t></w:r>
</w:ins>
```

### 批注

运行 `comment.py` 后，在 document.xml 中添加标记：

**关键：`<w:commentRangeStart>` 和 `<w:commentRangeEnd>` 是 `<w:r>` 的同级元素，绝不能放在 `<w:r>` 内部。**

```xml
<w:commentRangeStart w:id="0"/>
<w:r><w:t>被批注的文本</w:t></w:r>
<w:commentRangeEnd w:id="0"/>
<w:r><w:rPr><w:rStyle w:val="CommentReference"/></w:rPr><w:commentReference w:id="0"/></w:r>
```

### 图片

1. 将图片文件添加到 `word/media/`
2. 在 `word/_rels/document.xml.rels` 中添加关系：
```xml
<Relationship Id="rId5" Type=".../image" Target="media/image1.png"/>
```
3. 在 `[Content_Types].xml` 中添加内容类型：
```xml
<Default Extension="png" ContentType="image/png"/>
```
4. 在 document.xml 中引用：
```xml
<w:drawing>
  <wp:inline>
    <wp:extent cx="914400" cy="914400"/>  <!-- EMU：914400 = 1 英寸 -->
    <a:graphic>
      <a:graphicData uri=".../picture">
        <pic:pic>
          <pic:blipFill><a:blip r:embed="rId5"/></pic:blipFill>
        </pic:pic>
      </a:graphicData>
    </a:graphic>
  </wp:inline>
</w:drawing>
```