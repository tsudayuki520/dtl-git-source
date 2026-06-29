# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

运动会管理系统，包含三个子项目：Spring Boot 后端、Vue 3 管理端、微信小程序端。所有 API 响应统一使用 `Result<T>` 封装（`{code, message, data}`）。

## 常用命令

### 后端 (SportBackend)
```bash
cd SportBackend
mvn spring-boot:run          # 启动后端服务 (端口 8080)
mvn compile                   # 编译
mvn test                      # 运行测试
mvn test -Dtest=BannerTest    # 运行单个测试类
```
环境要求：JDK 21、Maven 3.9+、MySQL 8.x（详见 `SportBackend/doc/01-项目说明.md`）。
启动前需复制 `application.yml.template` 为 `application.yml` 并填入数据库密码和微信小程序凭证（`wechat.appid` / `wechat.secret`）。

### 管理端前端 (webManageProject)
```bash
cd webManageProject
npm install
npm run dev                   # 启动开发服务器 (端口 5173，自动代理 /api 到 localhost:8080)
npm run build                 # TypeScript 类型检查 + 构建
```

### 小程序端 (sportdocu)
微信开发者工具导入 `sportdocu/` 目录即可预览和调试。开发阶段需在「详情 → 本地设置」中勾选 **「不校验合法域名…」**，否则无法请求 `localhost:8080`。`utils/auth.js` 中 `BASE_URL` 硬编码为 `http://localhost:8080`，切换环境时需同步修改。

## 架构

### 后端分层结构
`Controller/ -> Service/ -> Mapper/ -> entity/`，MyBatis XML 映射在 `resources/mapper/`。

Controller 按角色分包：
- `Controller/admin/` — 管理端接口，路径 `/api/admin/{module}/{action}`
- `Controller/user/` — 小程序端接口，路径 `/api/{module}/{action}`

认证：`AuthInterceptor` 仅拦截 `WebMvcConfig` 中显式登记的 user 路径（如 `/api/notice/**`、`/api/auth/phone`、`/api/auth/info` 等），admin 路由目前无拦截。校验通过后 `userId` 被注入到 request attribute，Controller 中以 `@RequestAttribute("userId") Long userId` 获取；token 无效返回 HTTP 401。前端 401 时小程序端会自动静默重登，管理端不会（管理端 token 由 `localStorage` 注入，无自动续期）。

异常处理：`GlobalExceptionHandler` 捕获所有 `RuntimeException`，统一返回 **HTTP 400 + `Result.error(400, message)`**。因此 Controller/Service 中遇到业务错误直接 `throw new RuntimeException("原因")` 即可，前端 axios 拦截器会从 `error.response.data.message` 取到提示。

### 管理端前端
Vue 3 + TypeScript + Element Plus + Pinia + Vue Router。API 模块在 `src/api/`，每个后端实体一个文件。Axios 实例在 `src/utils/request.ts` 中自动从 `localStorage` 注入 Bearer token，响应拦截器直接返回 `response.data`（即 `Result` 体）。路由全部嵌套在 `DefaultLayout` 下（侧边栏深色主题），按业务层级组织：`/home` → `/meeting/:id` → `/meeting/:meetingId/schedule/:scheduleId` → `/meeting/:meetingId/schedule/:scheduleId/event/:eventId`，新增页面应遵循此层级并在 `src/router/index.ts` 注册。

### 小程序端
原生微信小程序框架，`utils/auth.js` 封装了静默登录（`wx.login` -> 后端换 token）。所有请求走自定义 `request()` 函数，自动附带 token，401 时自动重新登录。

### 数据库
MySQL `sport_db`（字符集 `utf8mb4`），建表脚本在 `SportBackend/sql/`（权威来源，字段说明见 `SportBackend/doc/02-数据库设计.md`）。核心表：`sports_meeting`、`schedule`、`event`、`event_schedule`、`participant`、`registration`、`team`、`group_type`、`result`、`record`、`banner`、`notice`、`user`。表间通过逻辑外键关联（无物理外键约束）。MyBatis 配置了 `map-underscore-to-camel-case: true`（DB 列 `snake_case` ↔ 实体 `camelCase` 自动映射）。

#### Schema 同步约定（强制）
每次对数据库执行 **DDL 结构变更**（`ALTER TABLE` 加列/改列/改类型/删列、`CREATE TABLE`、`DROP TABLE` 等）后，**必须立即同步更新** `SportBackend/sql/` 下对应的建表脚本（如 `event.sql`、`schedule.sql`、`group_type.sql`、`participant.sql`），保持脚本与实际表结构一致，不得遗漏。

- 仅 DDL（结构变更）触发此约定；纯数据操作（`INSERT`/`UPDATE`/`DELETE`/数据回填）不涉及。
- 同步时保持现有手写格式（中文 `COMMENT`、`IF NOT EXISTS`、对齐缩进），**不要用 `mysqldump` 覆盖**（其输出格式与手写脚本不一致）。
- 执行任何数据库写操作（DDL 或 DML）前，须先向用户确认。

### 第三方集成
- **华为云 OBS**：图片存储（轮播图、头像），工具类 `OBSUtil.java`
- **微信登录**：后端调用 `jscode2session` 换取 openid，JWT 有效期 7 天
