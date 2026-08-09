```markdown
---
name: xlsx
description: "电子表格文件的创建、读取、编辑与格式转换。覆盖 .xlsx、.xlsm、.csv、.tsv 等格式，支持从零创建、从其他数据源生成、清洗与重构杂乱表格数据。最终交付物为电子表格文件。"
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
> 使用 `run_skill_script` 工具执行脚本，或通过以下方式运行：`cd {this_skill_dir} && python scripts/...`

# 输出要求

## 所有 Excel 文件

### 专业字体
- 除非另有指示，否则使用统一、专业的字体（如 Arial、Times New Roman）

### 零公式错误
- 每个 Excel 模型交付时必须为零公式错误（#REF!、#DIV/0!、#VALUE!、#N/A、#NAME?）

### 保留现有模板
- 修改文件时，研究并**完全匹配**现有的格式、样式和约定
- 现有模板约定始终优先于本指南

## 财务模型

### 颜色编码标准

- **蓝色文字 (0,0,255)**：硬编码输入
- **黑色文字 (0,0,0)**：所有公式和计算
- **绿色文字 (0,128,0)**：来自其他工作表的链接
- **红色文字 (255,0,0)**：指向其他文件的外部链接
- **黄色背景 (255,255,0)**：需要注意的关键假设

### 数字格式标准

- **年份**：格式化为文本字符串（"2024"，而非"2,024"）
- **货币**：使用 $#,##0 格式；在表头中注明单位（"收入（百万美元）"）
- **零值**：格式化为 "-"，包括百分比
- **百分比**：默认使用 0.0% 格式
- **倍数**：格式化为 0.0x
- **负数**：使用括号 (123) 而非减号 -123

### 公式构建规则

- 将所有假设放在独立的假设单元格中
- 使用单元格引用，而非硬编码数值
- 示例：使用 `=B5*(1+$B$6)` 而非 `=B5*1.05`

# XLSX 创建、编辑与分析

## 前置条件

- **openpyxl**：Excel 文件创建和编辑
- **pandas**：数据分析和批量操作
- **LibreOffice** (`soffice`)：通过 `scripts/recalc.py` 进行公式重算

## 关键：使用公式，而非硬编码数值

**始终使用 Excel 公式，而非在 Python 中计算后硬编码数值。**

### 错误做法 — 硬编码
```python
total = df['Sales'].sum()
sheet['B10'] = total  # 错误：硬编码为 5000
```

### 正确做法 — 使用公式
```python
sheet['B10'] = '=SUM(B2:B9)'
```

## 常见工作流

1. **选择工具**：数据处理用 pandas，公式/格式用 openpyxl
2. **创建/加载**：创建新工作簿或加载现有文件
3. **修改**：添加/编辑数据、公式和格式
4. **保存**：写入文件
5. **重算公式（若使用公式则必须执行）**：
   ```bash
   python scripts/recalc.py output.xlsx
   ```
6. **验证并修复错误**：
   - 若 `status` 为 `errors_found`，查看 `error_summary` 中的具体错误
   - 修复已识别的错误并再次重算

## 读取与分析数据

### 使用 pandas 进行数据分析
```python
import pandas as pd

df = pd.read_excel('file.xlsx')                          # 默认：第一个工作表
all_sheets = pd.read_excel('file.xlsx', sheet_name=None) # 所有工作表，以字典形式返回

df.head()      # 预览数据
df.info()      # 列信息
df.describe()  # 统计信息

df.to_excel('output.xlsx', index=False)
```

## Excel 文件工作流

### 创建新的 Excel 文件
```python
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment

wb = Workbook()
sheet = wb.active

sheet['A1'] = 'Hello'
sheet['B1'] = 'World'
sheet.append(['Row', 'of', 'data'])

sheet['B2'] = '=SUM(A1:A10)'

sheet['A1'].font = Font(bold=True, color='FF0000')
sheet['A1'].fill = PatternFill('solid', start_color='FFFF00')
sheet['A1'].alignment = Alignment(horizontal='center')

sheet.column_dimensions['A'].width = 20

wb.save('output.xlsx')
```

### 编辑现有 Excel 文件
```python
from openpyxl import load_workbook

wb = load_workbook('existing.xlsx')
sheet = wb.active

sheet['A1'] = 'New Value'
sheet.insert_rows(2)
sheet.delete_cols(3)

new_sheet = wb.create_sheet('NewSheet')
new_sheet['A1'] = 'Data'

wb.save('modified.xlsx')
```

## 解包/打包工作流（高级 XML 编辑）

用于通过原始 XML 进行高级 Excel 操作：

```bash
# 解包
python scripts/office/unpack.py spreadsheet.xlsx unpacked/

# 在 unpacked/xl/worksheets/、unpacked/xl/sharedStrings.xml 等目录中编辑 XML

# 打包
python scripts/office/pack.py unpacked/ output.xlsx
```

## 重算公式

```bash
python scripts/recalc.py <excel_file> [timeout_seconds]
```

该脚本会：
- 首次运行时自动配置 LibreOffice 宏
- 重算所有工作表中的所有公式
- 扫描所有单元格中的 Excel 错误
- 返回 JSON，包含详细的错误位置和数量
- 支持 Linux、macOS 和 Windows

### 解读 recalc.py 输出
```json
{
  "status": "success",
  "total_errors": 0,
  "total_formulas": 42,
  "error_summary": {}
}
```

## 公式验证清单

### 基本验证
- 在构建完整模型之前，先测试 2-3 个示例引用
- 确认 Excel 列映射（第 64 列 = BL，而非 BK）
- 记住 Excel 行从 1 开始索引（DataFrame 第 5 行 = Excel 第 6 行）

### 常见易错点
- NaN 处理：使用 `pd.notna()` 检查空值
- 除零错误：在公式中使用 `/` 之前检查分母
- 引用错误：验证所有单元格引用是否指向预期单元格
- 跨工作表引用：使用正确格式（`Sheet1!A1`）

## 最佳实践

### 库的选择
- **pandas**：最适合数据分析、批量操作和简单数据导出
- **openpyxl**：最适合复杂格式、公式和 Excel 特有功能

### 使用 openpyxl
- 单元格索引从 1 开始
- 使用 `data_only=True` 读取计算后的值
- **警告**：`data_only=True` + 保存 = 公式永久丢失
- 公式会被保留但不会被求值 — 使用 `scripts/recalc.py` 更新数值
```