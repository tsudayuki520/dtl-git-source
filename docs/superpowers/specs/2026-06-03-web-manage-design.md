# Web 管理端完善设计文档

## 概述

完善 `webManageProject`（Vue 3 + Element Plus + TypeScript）管理端，实现运动会管理系统的完整后台管理功能。

## 导航结构

- 面包屑 + 独立页面模式
- 首页展示运动会卡片列表，点击「进入管理」进入运动会详情页
- 详情页使用 Tab 标签切换各管理模块

## 页面结构

### 1. 首页 `/`
- 顶部：搜索框 + 状态筛选下拉 + 「新增运动会」按钮
- 主体：卡片网格展示运动会（2列布局）
- 卡片左侧彩色边框区分状态：蓝=报名中、绿=进行中、红=已结束
- 卡片内容：名称、状态标签、地点、日期、联系电话
- 操作：进入管理 / 编辑 / 删除
- 新增/编辑使用 Element Plus Dialog 弹窗表单
- 删除使用确认弹窗

### 2. 运动会详情页 `/meeting/:id`
- 顶部信息栏：运动会名称、地点、日期、状态、主办方、联系电话
- 面包屑：首页 / 运动会名称
- Tab 标签切换 5 个管理模块
- 每个 Tab：搜索/筛选条件 + 数据表格 + 新增/编辑/删除操作
- 新增/编辑使用 Dialog 弹窗表单

### 3. 各 Tab 管理模块

#### 比赛项目管理
- 表格列：名称、类别（田赛/径赛/趣味赛）、性别、组别、是否可报名、人数上限
- 筛选：类别、性别、组别
- 表单字段：名称、类别(select)、性别(select)、组别(select)、所属轮次(select)、是否开放报名(switch)、报名上限(number)
- API：GET/POST/PUT/DELETE `/api/admin/event/*`

#### 参赛人员管理
- 表格列：学号/工号、姓名、性别、电话、学院、专业
- 筛选：姓名搜索、学院筛选
- 表单字段：学号/工号、姓名、性别(radio)、电话、学院、专业
- API：GET/POST/PUT/DELETE `/api/admin/participant/*`（需新增后端）

#### 赛程轮次管理
- 表格列：轮次名称、状态
- 筛选：状态
- 表单字段：轮次名称
- API：GET/POST/PUT/DELETE `/api/admin/schedule/*`

#### 公告通知管理
- 表格列：标题、内容摘要、创建时间
- 无筛选
- 表单字段：标题、内容(textarea)
- API：GET/POST/PUT/DELETE `/api/admin/notice/*`

#### 报名记录管理
- 表格列：参赛者姓名、项目名称、状态（已报名/已晋级/已取消）、报名时间
- 筛选：项目筛选、状态筛选
- 操作：修改状态、删除
- API：GET/PUT/DELETE `/api/admin/registration/*`（需新增后端）

## 后端新增 API

需在 `SportBackend` 中补充以下管理端接口：

### 参赛人员管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/participant/list?sportsMeetingId=` | 查询参赛人员列表 |
| POST | `/api/admin/participant/add` | 新增参赛人员 |
| PUT | `/api/admin/participant/update` | 更新参赛人员 |
| DELETE | `/api/admin/participant/{id}` | 删除参赛人员 |

### 报名记录管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/registration/list?sportsMeetingId=` | 查询报名记录列表 |
| PUT | `/api/admin/registration/update` | 更新报名状态 |
| DELETE | `/api/admin/registration/{id}` | 删除报名记录 |

## 前端文件结构

```
src/
├── api/                    # API 接口层
│   ├── meeting.ts          # 运动会 API
│   ├── event.ts            # 比赛项目 API
│   ├── participant.ts      # 参赛人员 API
│   ├── schedule.ts         # 赛程轮次 API
│   ├── notice.ts           # 公告通知 API
│   └── registration.ts     # 报名记录 API
├── views/
│   ├── HomeView.vue        # 首页（运动会卡片列表）
│   └── MeetingDetailView.vue  # 运动会详情页（Tab管理）
├── router/
│   └── index.ts            # 路由配置
├── layouts/
│   └── DefaultLayout.vue   # 布局（需更新侧边栏）
├── utils/
│   └── request.ts          # Axios 实例（已有）
└── stores/
    └── user.ts             # 用户状态（已有）
```

## 技术选型

- 框架：Vue 3 + TypeScript + Vite
- UI 库：Element Plus（已安装）
- 路由：Vue Router（已配置）
- HTTP：Axios（已配置，baseURL=/api，代理到 localhost:8080）
- 状态：Pinia（已有 user store）

## 状态映射

- 运动会状态：0=报名中、1=进行中、2=已结束
- 赛程/项目状态：0=进行中、1=已结束
- 报名状态：0=已报名、1=已晋级、2=已取消
