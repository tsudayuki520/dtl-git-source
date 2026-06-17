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
```
启动前需复制 `application.yml.template` 为 `application.yml` 并填入数据库密码和微信小程序凭证。

### 管理端前端 (webManageProject)
```bash
cd webManageProject
npm install
npm run dev                   # 启动开发服务器 (端口 5173，自动代理 /api 到 localhost:8080)
npm run build                 # TypeScript 类型检查 + 构建
```

### 小程序端 (sportdocu)
微信开发者工具导入 `sportdocu/` 目录即可预览和调试。

## 架构

### 后端分层结构
`Controller/ -> Service/ -> Mapper/ -> entity/`，MyBatis XML 映射在 `resources/mapper/`。

Controller 按角色分包：
- `Controller/admin/` — 管理端接口，路径 `/api/admin/{module}/{action}`
- `Controller/user/` — 小程序端接口，路径 `/api/{module}/{action}`

认证：`AuthInterceptor` 对部分 user 路由做 JWT 校验（Bearer token），admin 路由目前无拦截。

### 管理端前端
Vue 3 + TypeScript + Element Plus + Pinia + Vue Router。API 模块在 `src/api/`，每个后端实体一个文件。Axios 实例在 `src/utils/request.ts` 中自动注入 token。路由嵌套在 `DefaultLayout` 下（侧边栏深色主题）。

### 小程序端
原生微信小程序框架，`utils/auth.js` 封装了静默登录（`wx.login` -> 后端换 token）。所有请求走自定义 `request()` 函数，自动附带 token，401 时自动重新登录。

### 数据库
MySQL `sport_db`，建表脚本在 `SportBackend/sql/`。核心表：`sports_meeting`、`schedule`、`event`、`participant`、`registration`、`team`、`group_type`、`banner`、`notice`、`user`。表间通过逻辑外键关联（无物理外键约束）。MyBatis 配置了 `map-underscore-to-camel-case: true`。

### 第三方集成
- **华为云 OBS**：图片存储（轮播图、头像），工具类 `OBSUtil.java`
- **微信登录**：后端调用 `jscode2session` 换取 openid，JWT 有效期 7 天
