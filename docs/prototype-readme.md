# 产品原型与交互设计

本目录展示项目从需求到页面的产品设计产物。原型重点不是视觉装饰，而是说明角色、任务、信息层级和异常状态如何被转化为可开发页面。

## 原型交付物

| 文件 | 说明 | 对应能力 |
| --- | --- | --- |
| [01-product-flow.png](prototype/01-product-flow.png) | 学生请销假完整流程 | 业务流程梳理、状态设计 |
| [02-student-apply-wireframe.png](prototype/02-student-apply-wireframe.png) | 学生发起请假 | 表单设计、字段分组、提交校验 |
| [03-student-detail-wireframe.png](prototype/03-student-detail-wireframe.png) | 请假申请详情 | 状态时间线、操作入口、异常反馈 |
| [04-admin-approval-wireframe.png](prototype/04-admin-approval-wireframe.png) | 管理端审批 | 待办优先级、审核信息层级、决策操作 |
| [05-admin-statistics-wireframe.png](prototype/05-admin-statistics-wireframe.png) | 数据统计 | 指标定义、筛选条件、数据可视化 |

## 关键交互说明

### 学生端

- 首屏展示当前最重要的申请状态，降低学生反复进入详情页查询的成本。
- 请假表单按“时间—事由—目的地—材料”组织，提交前进行必填项和时间冲突校验。
- 已通过、审批中、已驳回和已完成使用不同状态反馈，避免只用颜色传达信息。
- 续假和返校入口只在满足业务状态时出现，并给出不可操作原因。

### 管理端

- 审批列表优先展示待办数量、学生身份、时间范围和事由，减少逐层点击。
- 同意和驳回是高风险操作，保留二次确认与审批意见记录。
- 统计页面展示指标口径、筛选范围和空数据状态，避免图表“看起来有数据但无法解释”。

### AI 交互预留

AI 辅助审核应放在审批详情的决策区域旁边，展示摘要、风险证据和可编辑建议；不使用“AI 已判定”这类替代人工责任的文案。模型不可用时，页面仍能完成普通审批。

## 从原型到实现

原型中的页面和流程已落地到 Vue 3 前端，并通过 Playwright 对登录、申请、审批、权限和关键状态流转进行回归验证。在线 Demo 使用脱敏模拟数据，适合招聘方直接体验：<https://ma123-hue.github.io/student-leave-system/>。
