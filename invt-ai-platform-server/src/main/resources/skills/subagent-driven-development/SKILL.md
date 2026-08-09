```yaml
---
name: subagent-driven-development
description: 通过 delegate_task 子 Agent 执行开发计划（两阶段审查）。
version: 1.1.0
tags:
- delegation
- subagent
- implementation
- workflow
- parallel
author: ported
---
# 子 Agent 驱动开发

## 概述

通过每个任务派发全新的子 Agent，配合系统化的两阶段审查来执行实现计划。

**核心原则：** 每个任务全新子 Agent + 两阶段审查（先规格，后质量）= 高质量、快速迭代。

## 适用场景

以下情况使用本 Skill：
- 你有一个实现计划（来自 writing-plans Skill 或用户需求）
- 任务之间基本独立
- 质量和规格合规很重要
- 你希望在任务之间进行自动化审查

**对比手动执行：**
- 每个任务拥有全新上下文（不会因累积状态产生混淆）
- 自动化审查流程能及早发现问题
- 所有任务拥有一致的质量检查
- 子 Agent 可以在开始工作前提问

## 流程

### 1. 读取并解析计划

读取计划文件。一次性提取所有任务及其完整文本和上下文。创建一个待办列表：

```python
# Read the plan
read_file("docs/plans/feature-plan.md")

# Create todo list with all tasks
todo([
    {"id": "task-1", "content": "Create User model with email field", "status": "pending"},
    {"id": "task-2", "content": "Add password hashing utility", "status": "pending"},
    {"id": "task-3", "content": "Create login endpoint", "status": "pending"},
])
```

**要点：** 只读取一次计划。一次性提取所有内容。不要让子 Agent 去读计划文件——在上下文中直接提供完整的任务文本。

### 2. 逐任务工作流

对计划中的每个任务：

#### 第 1 步：派发实现子 Agent

使用 `delegate_task` 并附上完整上下文：

```python
delegate_task(
    goal="Implement Task 1: Create User model with email and password_hash fields",
    context="""
    TASK FROM PLAN:
    - Create: src/models/user.py
    - Add User class with email (str) and password_hash (str) fields
    - Use bcrypt for password hashing
    - Include __repr__ for debugging

    FOLLOW TDD:
    1. Write failing test in tests/models/test_user.py
    2. Run: pytest tests/models/test_user.py -v (verify FAIL)
    3. Write minimal implementation
    4. Run: pytest tests/models/test_user.py -v (verify PASS)
    5. Run: pytest tests/ -q (verify no regressions)
    6. Commit: git add -A && git commit -m "feat: add User model with password hashing"

    PROJECT CONTEXT:
    - Python 3.11, Flask app in src/app.py
    - Existing models in src/models/
    - Tests use pytest, run from project root
    - bcrypt already in requirements.txt
    """,
    toolsets=['terminal', 'file']
)
```

#### 第 2 步：派发规格合规审查子 Agent

实现完成后，对照原始规格进行验证：

```python
delegate_task(
    goal="Review if implementation matches the spec from the plan",
    context="""
    ORIGINAL TASK SPEC:
    - Create src/models/user.py with User class
    - Fields: email (str), password_hash (str)
    - Use bcrypt for password hashing
    - Include __repr__

    CHECK:
    - [ ] All requirements from spec implemented?
    - [ ] File paths match spec?
    - [ ] Function signatures match spec?
    - [ ] Behavior matches expected?
    - [ ] Nothing extra added (no scope creep)?

    OUTPUT: PASS or list of specific spec gaps to fix.
    """,
    toolsets=['file']
)
```

**如发现规格问题：** 修复差距，然后重新运行规格审查。只有规格合规后才继续。

#### 第 3 步：派发代码质量审查子 Agent

规格合规通过后：

```python
delegate_task(
    goal="Review code quality for Task 1 implementation",
    context="""
    FILES TO REVIEW:
    - src/models/user.py
    - tests/models/test_user.py

    CHECK:
    - [ ] Follows project conventions and style?
    - [ ] Proper error handling?
    - [ ] Clear variable/function names?
    - [ ] Adequate test coverage?
    - [ ] No obvious bugs or missed edge cases?
    - [ ] No security issues?

    OUTPUT FORMAT:
    - Critical Issues: [must fix before proceeding]
    - Important Issues: [should fix]
    - Minor Issues: [optional]
    - Verdict: APPROVED or REQUEST_CHANGES
    """,
    toolsets=['file']
)
```

**如发现质量问题：** 修复问题，重新审查。只有审批通过后才继续。

#### 第 4 步：标记完成

```python
todo([{"id": "task-1", "content": "Create User model with email field", "status": "completed"}], merge=True)
```

### 3. 最终审查

所有任务完成后，派发一个最终的集成审查子 Agent：

```python
delegate_task(
    goal="Review the entire implementation for consistency and integration issues",
    context="""
    All tasks from the plan are complete. Review the full implementation:
    - Do all components work together?
    - Any inconsistencies between tasks?
    - All tests passing?
    - Ready for merge?
    """,
    toolsets=['terminal', 'file']
)
```

### 4. 验证并提交

```bash
# Run full test suite
pytest tests/ -q

# Review all changes
git diff --stat

# Final commit if needed
git add -A && git commit -m "feat: complete [feature name] implementation"
```

## 任务粒度

**每个任务 = 2-5 分钟的专注工作。**

**太大：**
- "实现用户认证系统"

**合适大小：**
- "创建带 email 和 password 字段的 User 模型"
- "添加密码哈希函数"
- "创建登录端点"
- "添加 JWT token 生成"
- "创建注册端点"

## 红线——绝对不要做

- 没有计划就开始实现
- 跳过审查（规格合规或代码质量任一）
- 存在未修复的严重/重要问题仍继续
- 为涉及相同文件的任务派发多个实现子 Agent
- 让子 Agent 去读计划文件（改为在上下文中提供完整文本）
- 跳过场景设置上下文（子 Agent 需要了解任务在整个项目中的位置）
- 忽略子 Agent 的提问（先回答再让其继续）
- 接受规格合规上的"差不多就行"
- 跳过审查循环（审查者发现问题 → 实现者修复 → 再次审查）
- 让实现者自审查替代真正的审查（两者都需要）
- **在规格合规为 PASS 之前就开始代码质量审查**（顺序错误）
- 在任一审查还有未解决问题时就进入下一个任务

## 问题处理

### 如果子 Agent 提出问题

- 清晰完整地回答
- 如需要，提供额外上下文
- 不要催促他们进入实现阶段

### 如果审查者发现问题

- 实现子 Agent（或一个新的子 Agent）修复这些问题
- 审查者再次审查
- 反复直到审批通过
- 不要跳过重新审查

### 如果子 Agent 执行任务失败

- 派发一个新的修复子 Agent，附上关于出了什么问题的具体说明
- 不要在控制器会话中手动修复（会污染上下文）

## 效率要点

**为什么每个任务要用全新子 Agent：**
- 防止累积状态导致的上下文污染
- 每个子 Agent 获得干净、聚焦的上下文
- 不会因先前任务的代码或推理产生混淆

**为什么采用两阶段审查：**
- 规格审查能在早期发现构建不足或过度构建
- 质量审查确保实现是良好构建的
- 在问题跨任务累积之前就发现它们

**成本权衡：**
- 更多的子 Agent 调用（每个任务：实现者 + 2 个审查者）
- 但能尽早发现问题（比后续调试累积问题更便宜）

## 与其他 Skill 的集成

### 与 writing-plans 配合

本 Skill 执行 writing-plans Skill 创建的计划：
1. 用户需求 → writing-plans → 实现计划
2. 实现计划 → subagent-driven-development → 可运行的代码

### 与 test-driven-development 配合

实现子 Agent 应遵循 TDD：
1. 先写失败的测试
2. 实现最小化代码
3. 验证测试通过
4. 提交

在每个实现者的上下文中包含 TDD 指令。

### 与 requesting-code-review 配合

两阶段审查过程本身就是代码审查。对于最终集成审查，使用 requesting-code-review Skill 的审查维度。

### 与 systematic-debugging 配合

如果子 Agent 在实现过程中遇到 Bug：
1. 遵循 systematic-debugging 流程
2. 在修复前找到根因
3. 编写回归测试
4. 恢复实现

## 工作流示例

```
[读取计划: docs/plans/auth-feature.md]
[创建包含 5 个任务的待办列表]

--- 任务 1：创建 User 模型 ---
[派发实现子 Agent]
  实现者："email 需要唯一吗？"
  你："是的，email 必须唯一"
  实现者：已实现，3/3 测试通过，已提交。

[派发规格审查者]
  规格审查者：✅ PASS — 所有需求已满足

[派发质量审查者]
  质量审查者：✅ APPROVED — 代码整洁，测试良好

[标记任务 1 完成]

--- 任务 2：密码哈希 ---
[派发实现子 Agent]
  实现者：无问题，已实现，5/5 测试通过。

[派发规格审查者]
  规格审查者：❌ 缺失：密码强度验证（规格要求"最少 8 字符"）

[实现者修复]
  实现者：已添加验证，7/7 测试通过。

[再次派发规格审查者]
  规格审查者：✅ PASS

[派发质量审查者]
  质量审查者：重要：魔法数字 8，提取为常量
  实现者：已提取 MIN_PASSWORD_LENGTH 常量
  质量审查者：✅ APPROVED

[标记任务 2 完成]

... （继续处理所有任务）

[所有任务完成后：派发最终集成审查者]
[运行完整测试套件：全部通过]
[完成！]
```

## 牢记

```
每个任务全新子 Agent
每次都做两阶段审查
规格合规优先
代码质量其次
绝不跳过审查
尽早发现问题
```

**质量不是偶然的。它是系统化流程的结果。**

## 延伸阅读（需要时加载）

当编排涉及显著的上下文消耗、冗长的审查循环或复杂的验证关卡时，加载对应方向的以下参考资料：

- **`references/context-budget-discipline.md`** — 四级上下文退化模型（PEAK / GOOD / DEGRADING / POOR），随上下文窗口大小缩放的阅读深度规则，以及静默退化的早期预警信号。当一次运行明显会消耗大量上下文时加载（多阶段计划、大量子 Agent、大型产物）。
- **`references/gates-taxonomy.md`** — 四种规范关卡类型（Pre-flight、Revision、Escalation、Abort）及其行为、恢复方式和示例。在设计或审查任何有验证关卡的工作流时加载——显式使用这些词汇，使每个关卡都有定义的入口、失败行为和恢复规则。

两份参考资料均改编自 gsd-build/get-shit-done（MIT © 2025 Lex Christopherson）。
```