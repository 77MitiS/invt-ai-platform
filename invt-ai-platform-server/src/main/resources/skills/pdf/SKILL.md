```markdown
---
name: pdf
description: "PDF 文件的全能处理：读取与提取文字/表格、合并拆分、旋转页面、添加水印、创建新 PDF、填写表单、加解密、提取图片、OCR 扫描件文字识别。只要涉及 .pdf 文件即可触发此技能。"
dependencies:
  commands:
    - python3
  tools:
    - skillScriptTool
    - skillFileTool
platforms:
  - macos
  - linux
  - windows
---

> **重要提示：** 所有 `scripts/` 路径均相对于本技能目录。
> 使用 `run_skill_script` 工具执行脚本，或在终端中运行：`cd {this_skill_dir} && python scripts/...`

# PDF 处理指南

## 前置依赖

- **pypdf**：核心 PDF 读写
- **pdfplumber**：文本与表格提取
- **reportlab**：PDF 创建
- **pdftotext**（poppler-utils）：命令行文本提取
- **pdftoppm**（poppler-utils）：PDF 转图片
- **qpdf**：PDF 操作（合并、拆分、旋转、解密）

## 工具选择决策表

开始前请选择合适的方案：

| 输入 | 条件 | 推荐工具 |
|-------|-----------|-----------------|
| URL | 可通过 URL 访问的 PDF | `web_extract(url)` — 最快，无需下载 |
| 本地文件 | 原生文本型 PDF（软件生成） | `pymupdf` — 约 25 MB 安装，即时提取 |
| 本地文件 | 扫描件/纯图片 PDF（无可选中文字） | `marker-pdf` — 带布局保留的 OCR（约 5 GB，需 GPU 或 CPU） |
| 本地文件 | 表单填写或页面操作 | `pypdf` / `pdfplumber` + 表单脚本 |
| 本地文件 | NLP 编辑或语义搜索 | `nano-pdf` — 句子级操作 |

**URL 优先原则**：如果用户提供了 URL，始终先尝试 URL 提取，再考虑下载。

## 概述

本指南涵盖使用 Python 库和命令行工具进行的基本 PDF 处理操作。

## URL 优先提取

如果用户提供了指向 PDF 的 URL，无需下载即可提取：

```
web_extract(url="https://example.com/report.pdf")
```

仅在 `web_extract` 返回空或报错时，才回退到下载 + 本地处理。

---

## 快速提取：pymupdf (fitz)

**适用场景**：原生文本型 PDF（数字生成，非扫描件）。安装：`pip install pymupdf`（约 25 MB）。

```python
import fitz  # pymupdf

doc = fitz.open("document.pdf")
print(f"页面数：{doc.page_count}")

# 提取全部文本（快速）
full_text = "\n".join(page.get_text() for page in doc)

# 带布局块提取（表格、列）
for page in doc:
    blocks = page.get_text("blocks")  # (x0,y0,x1,y1,text,block_no,block_type)
    for block in blocks:
        print(block[4])  # 文本内容

# 提取图片
for page in doc:
    for img in page.get_images():
        xref = img[0]
        base = doc.extract_image(xref)
        with open(f"img_{xref}.{base['ext']}", "wb") as f:
            f.write(base["image"])
```

pymupdf 的文本提取速度比 pypdf 快 5-10 倍，且布局保留效果更好。

---

## OCR 提取：marker-pdf

**适用场景**：扫描版 PDF、纯图片 PDF，或 `pymupdf` 返回乱码的文档。
安装：`pip install marker-pdf`（约 5 GB，含模型）。

```bash
# 单个文件
marker_single document.pdf output_dir/ --batch_multiplier 2

# 批量处理
marker input_dir/ output_dir/ --workers 4
```

输出保留标题、表格和代码块的 Markdown 文件。

**判断依据**：先运行 `pymupdf`。如果提取的文本中可打印字符占比不足 50% 或明显为乱码，则切换到 `marker-pdf`。

---

## NLP 编辑：nano-pdf

**适用场景**：语义搜索、句子级编辑、原生文本型 PDF 中的关键词替换。
安装：`pip install nano-pdf`。

```python
from nano_pdf import NanoPDF

doc = NanoPDF("document.pdf")

# 搜索句子
results = doc.search("termination clause", top_k=5)
for r in results:
    print(r.page, r.text, r.score)

# 替换文本（生成新 PDF）
doc.replace("old phrase", "new phrase", output="modified.pdf")
```

---

## Python 库

### pypdf — 基础操作

#### 合并 PDF
```python
from pypdf import PdfWriter, PdfReader

writer = PdfWriter()
for pdf_file in ["doc1.pdf", "doc2.pdf", "doc3.pdf"]:
    reader = PdfReader(pdf_file)
    for page in reader.pages:
        writer.add_page(page)

with open("merged.pdf", "wb") as output:
    writer.write(output)
```

#### 拆分 PDF
```python
reader = PdfReader("input.pdf")
for i, page in enumerate(reader.pages):
    writer = PdfWriter()
    writer.add_page(page)
    with open(f"page_{i+1}.pdf", "wb") as output:
        writer.write(output)
```

#### 提取元数据
```python
reader = PdfReader("document.pdf")
meta = reader.metadata
print(f"标题：{meta.title}")
print(f"作者：{meta.author}")
```

#### 旋转页面
```python
reader = PdfReader("input.pdf")
writer = PdfWriter()
page = reader.pages[0]
page.rotate(90)  # 顺时针旋转 90 度
writer.add_page(page)
with open("rotated.pdf", "wb") as output:
    writer.write(output)
```

### pdfplumber — 文本与表格提取

#### 带布局提取文本
```python
import pdfplumber

with pdfplumber.open("document.pdf") as pdf:
    for page in pdf.pages:
        text = page.extract_text()
        print(text)
```

#### 提取表格
```python
with pdfplumber.open("document.pdf") as pdf:
    for i, page in enumerate(pdf.pages):
        tables = page.extract_tables()
        for j, table in enumerate(tables):
            print(f"第 {i+1} 页的第 {j+1} 个表格：")
            for row in table:
                print(row)
```

#### 高级表格提取
```python
import pandas as pd

with pdfplumber.open("document.pdf") as pdf:
    all_tables = []
    for page in pdf.pages:
        tables = page.extract_tables()
        for table in tables:
            if table:
                df = pd.DataFrame(table[1:], columns=table[0])
                all_tables.append(df)

if all_tables:
    combined_df = pd.concat(all_tables, ignore_index=True)
    combined_df.to_excel("extracted_tables.xlsx", index=False)
```

### reportlab — 创建 PDF

#### 基础 PDF 创建
```python
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas

c = canvas.Canvas("hello.pdf", pagesize=letter)
width, height = letter
c.drawString(100, height - 100, "Hello World!")
c.line(100, height - 140, 400, height - 140)
c.save()
```

#### 下标与上标

**重要提示**：切勿在 ReportLab PDF 中使用 Unicode 下标/上标字符。内置字体不包含这些字形，会导致它们渲染为实心黑块。

请改用 ReportLab 的 XML 标记标签：
```python
from reportlab.platypus import Paragraph
from reportlab.lib.styles import getSampleStyleSheet
styles = getSampleStyleSheet()
chemical = Paragraph("H<sub>2</sub>O", styles['Normal'])
squared = Paragraph("x<super>2</super> + y<super>2</super>", styles['Normal'])
```

## PDF 表单处理

### 检查 PDF 是否包含可填写字段
```bash
python scripts/check_fillable_fields.py document.pdf
```

### 提取表单字段信息
```bash
python scripts/extract_form_field_info.py document.pdf
```

### 提取表单结构（非可填写 PDF）
```bash
python scripts/extract_form_structure.py document.pdf
```

### 填写表单字段
```bash
python scripts/fill_fillable_fields.py document.pdf output.pdf --fields '{"field_name": "value"}'
```

### 以注释方式填写（非可填写 PDF）
```bash
python scripts/fill_pdf_form_with_annotations.py document.pdf output.pdf --data '{"x,y": "text"}'
```

### 校验边界框
```bash
python scripts/check_bounding_boxes.py document.pdf
```

### 将 PDF 转换为图片
```bash
python scripts/convert_pdf_to_images.py document.pdf output_dir/ --dpi 150
```

### 创建带叠层的校验图片
```bash
python scripts/create_validation_image.py document.pdf output.png
```

## 命令行工具

### pdftotext（poppler-utils）
```bash
pdftotext input.pdf output.txt              # 提取文本
pdftotext -layout input.pdf output.txt      # 保留布局
pdftotext -f 1 -l 5 input.pdf output.txt   # 第 1-5 页
```

### qpdf
```bash
qpdf --empty --pages file1.pdf file2.pdf -- merged.pdf     # 合并
qpdf input.pdf --pages . 1-5 -- pages1-5.pdf               # 拆分
qpdf input.pdf output.pdf --rotate=+90:1                    # 旋转
qpdf --password=mypassword --decrypt encrypted.pdf out.pdf  # 解密
```

## 常见任务

### 从扫描 PDF 中提取文本（OCR）
```python
import pytesseract
from pdf2image import convert_from_path

images = convert_from_path('scanned.pdf')
text = ""
for i, image in enumerate(images):
    text += f"第 {i+1} 页：\n"
    text += pytesseract.image_to_string(image)
    text += "\n\n"
```

### 添加水印
```python
from pypdf import PdfReader, PdfWriter

watermark = PdfReader("watermark.pdf").pages[0]
reader = PdfReader("document.pdf")
writer = PdfWriter()

for page in reader.pages:
    page.merge_page(watermark)
    writer.add_page(page)

with open("watermarked.pdf", "wb") as output:
    writer.write(output)
```

### 密码保护
```python
from pypdf import PdfReader, PdfWriter

reader = PdfReader("input.pdf")
writer = PdfWriter()
for page in reader.pages:
    writer.add_page(page)
writer.encrypt("userpassword", "ownerpassword")
with open("encrypted.pdf", "wb") as output:
    writer.write(output)
```

## 速查表

| 任务 | 最佳工具 | 命令/代码 |
|------|-----------|--------------|
| URL → 文本 | web_extract | `web_extract(url=...)` |
| 快速文本提取 | pymupdf | `fitz.open(...).get_text()` |
| 扫描件 / OCR | marker-pdf | `marker_single doc.pdf out/` |
| 语义搜索/编辑 | nano-pdf | `NanoPDF(...).search(...)` |
| 合并 PDF | pypdf | `writer.add_page(page)` |
| 拆分 PDF | pypdf | 每页一个文件 |
| 提取文本（带布局） | pdfplumber | `page.extract_text()` |
| 提取表格 | pdfplumber | `page.extract_tables()` |
| 创建 PDF | reportlab | Canvas 或 Platypus |
| 填写表单 | scripts | `fill_fillable_fields.py` |
```