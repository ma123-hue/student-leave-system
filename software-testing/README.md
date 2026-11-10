# 学生请销假系统软件测试项目

本目录把现有学生请销假系统整理为一套可演示、可复现、可用于软件测试实习投递的测试项目。被测系统采用 Vue 3 + Spring Boot + MySQL，测试工程采用 Playwright，同时覆盖接口测试和 Web UI 自动化测试。

## 项目成果

- 需求与风险分析：学生登录、强制改密、请假/返校/续假、管理审批、角色切换和统计查询。
- 测试设计：等价类、边界值、场景法、错误推测和权限矩阵。
- 接口自动化：认证、会话、权限边界、分页和响应契约。
- UI 自动化：登录校验、错误反馈、路由保护和管理端导航。
- 缺陷管理：记录复现步骤、预期/实际结果、严重程度、原因定位和修复建议。
- 测试报告：保留 HTML 报告、失败截图、Trace 和视频，便于回归与定位。

## 目录结构

```text
software-testing/
├─ docs/
│  ├─ test-plan.md          # 测试计划与策略
│  ├─ test-cases.md         # 功能/接口/安全用例库
│  ├─ bug-report.md         # 已确认缺陷
│  ├─ test-report.md        # 本轮执行结论
│  └─ resume-description.md # 简历与面试表述
├─ tests/
│  ├─ api/                  # API 自动化
│  └─ ui/                   # UI 自动化
├─ playwright.config.js
└─ package.json
```

## 环境要求

- JDK 8、Maven 3.6+
- Node.js 18+
- MySQL 5.7/8.x，账号配置与 `backend/src/main/resources/application.yml` 一致
- 被测数据库仅使用测试数据，禁止连接生产数据库执行测试

## 安装与执行

```powershell
cd software-testing
npm install
npx playwright install chromium
npm test
```

测试配置会自动启动前后端；如果服务已运行，会直接复用。也可以分别执行：

```powershell
npm run test:api
npm run test:ui
npm run report
```

## 测试账号

| 端 | 账号 | 初始密码 | 说明 |
| --- | --- | --- | --- |
| 学生端 | `2024000001` | `123456` | 首次登录需要修改密码 |
| 管理端 | `admin001` | `123456` | 测试辅导员账号 |

如需连接其他环境：

```powershell
$env:BASE_URL='http://test-host:5173'
$env:API_BASE_URL='http://test-host:8080'
$env:SKIP_WEBSERVER='1'
npm test
```

## 缺陷回归

首轮测试发现的管理端无鉴权、学号规则不一致和 CORS 风险已经修复，相关用例已转为普通回归测试。当前自动化共 27 条，全部通过。

详细结论见 [测试报告](docs/test-report.md) 和 [缺陷报告](docs/bug-report.md)。
