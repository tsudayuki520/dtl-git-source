# 参赛人员账号体系改造设计

**日期**：2026-07-02
**状态**：设计已确认，待 review
**影响范围**：后端（认证/参赛人员/导入）、管理端 Vue、微信小程序三端

---

## 背景

当前系统：
- 登录用**微信静默登录**（`jscode2session` 换 openid → JWT），无账号密码体系
- `user` 表是纯微信用户载体（openid/nickname/avatar/phone），`participant` 表存参赛人员信息（user_code/name/phone/gender/college/major）
- **user 与 participant 完全无关联** —— 报名时按表单填的 `(user_code, sports_meeting_id)` 定位 participant，无鉴权，任何人可填他人学号代报
- 同一学号跨多个运动会有多条 participant 记录
- 无 POI、无 BCrypt 依赖

## 目标

1. 参赛人员成为登录主体：账号 = 学号/工号，密码可改
2. 管理端支持 Excel 批量导入参赛人员（默认密码 `dlust123456`，BCrypt 加密）
3. 管理员可重置密码
4. 小程序支持游客浏览，触发报名/查我的项目时才需登录

## 关键决策（已与用户确认）

| 决策点 | 选择 |
|---|---|
| 登录体系 | 完全改用账号密码登录，废弃微信静默登录 |
| 数据模型 | user 表做账号主体（账号 + 人，全局唯一），participant 精简为「某运动会的参赛身份」 |
| 历史数据 | 写迁移脚本保留数据（不清空） |

---

## 一、数据模型

### `user` 表改造（账号 + 人，全局唯一）

**移除字段**：`openid`、`union_id`、`nickname`、`avatar_url`、`session_key`
**新增字段**：`user_code`（唯一）、`password`（BCrypt）、`name`、`gender`、`college`、`major`
**保留**：`id`、`phone`、`create_time`、`update_time`

```sql
user_code VARCHAR(30) NOT NULL UNIQUE  -- 学号/工号，登录账号
password VARCHAR(100) NOT NULL         -- BCrypt 哈希
name     VARCHAR(50) NOT NULL
gender   VARCHAR(4) NOT NULL
phone    VARCHAR(20)                   -- 非必须
college  VARCHAR(100)
major    VARCHAR(100)                  -- 专业/单位
```

### `participant` 表精简（某运动会的参赛身份）

**移除字段**：`user_code`、`name`、`phone`、`gender`、`college`、`major`（移到 user）
**新增字段**：`user_id`（关联 user.id）
**保留**：`id`、`sports_meeting_id`、`team_id`、`create_time`、`update_time`
**新增约束**：`UNIQUE(sports_meeting_id, user_id)` —— 一人一运动会一条

### 关系链

- **登录**：`(user_code, password)` 校验 user → JWT（payload = userId）
- **报名**：JWT 取 userId → 当前运动会下 upsert participant(user_id + sports_meeting_id)
- **查 participant**：JOIN user 拿 name/gender/...（影响所有 participant 查询的 Mapper）

---

## 二、认证流程

### 登录接口
- `POST /api/auth/login`，body = `{ userCode, password }`
- 后端：按 userCode 查 user → `BCryptPasswordEncoder.matches(raw, hash)` → 发 JWT（payload = userId，7 天有效）
- **删除** `UserAuthController` 原微信登录方法（jscode2session + httpGet 调用）

### token / 拦截
- `JwtUtil` 不变（仍按 userId）
- `AuthInterceptor` 不变
- `WebMvcConfig` 拦截列表改为（需登录才列入）：
  ```
  /api/register/submit
  /api/register/cancel        （如有取消报名）
  /api/my/*
  /api/auth/change-password
  ```
- 其余路径（首页、详情、赛程、项目、报名人员列表、成绩、通知、登录接口）游客可访问

### 小程序改造
- **新增登录页** `pages/login`：学号 + 密码 + 登录按钮
- **`auth.js` 改造**：
  - 删除 `silentLogin`、`jscode2session` 相关
  - 新增 `login(userCode, password)`：调登录接口、存 token + userCode/name
  - 新增 `isLoggedIn()`：判断本地有无 token
  - `request()` 401 时：**清 token + 跳登录页**（不再自动重登）
- **报名触发登录**：报名表单页 `onLoad` 检测未登录 → `wx.navigateTo` 登录页（带 `redirect` 回跳参数）→ 登录后回跳继续
- **`profile`（我的）页**：
  - 未登录：「请登录」按钮 → 跳登录页
  - 已登录：顶部显示学号/姓名/学院/专业；菜单 = 我的参赛项目 / 修改密码 / 退出

### token 失效 / 退出
- JWT 无状态，401 时清本地 token + 跳登录页
- 退出 = 清本地存储

---

## 三、Excel 批量导入

### 前端入口
- 参赛人员管理 tab（`MeetingDetailView`），工具栏新增「批量导入」+「下载模板」按钮
- 针对当前运动会（请求带 `sportsMeetingId`）

### 依赖
`pom.xml` 新增 `easyexcel`（阿里，轻量、API 友好）

### Excel 模板字段（6 列）
| 学号/工号 | 姓名 | 性别 | 电话(非必须) | 学院 | 专业/单位 |
- 代表队：不在表里，后续在代表队管理分配
- 密码：导入时统一 `BCrypt(dlust123456)`

### 接口
`POST /api/admin/participant/import`
- multipart：Excel 文件 + `sportsMeetingId`
- 返回：`{ successCount, failedRows: [{ row, reason }] }`

### 导入逻辑（幂等，逐行）
1. 按 `user_code` 查 user：
   - 不存在 → 新建（password = `BCrypt(dlust123456)`）
   - 已存在 → 用 Excel 非空字段更新（空值不覆盖，避免清已有数据）
2. 按 `(sports_meeting_id, user_id)` 查 participant：
   - 不存在 → 新建（user_id + sports_meeting_id，team_id 空）
   - 已存在 → 跳过
3. 失败行（格式错误、必填空）记录行号 + 原因，继续后续行

### 模板下载
`GET /api/admin/participant/import-template` → 后端用 EasyExcel 生成含表头的空 Excel 返回下载（字段与导入一致）

---

## 四、密码管理

### 管理员重置密码
- **入口**：参赛人员列表每行「重置密码」按钮
- **接口**：`POST /api/admin/user/reset-password`，body = `{ userId }`
- **逻辑**：`user.password = BCrypt("dlust123456")`
- **提示**：弹窗告知「已重置为默认密码 dlust123456」

### 用户改密码（小程序）
- **入口**：「我的」页 → 修改密码
- **接口**：`POST /api/auth/change-password`，body = `{ oldPassword, newPassword }`
- **逻辑**：JWT 取 userId → 校验 oldPassword 匹配 → 更新为 `BCrypt(newPassword)`

---

## 五、管理端参赛人员管理适配

当前参赛人员列表/新增/编辑直接操作 participant 的 user_code/name 等字段，改造后这些字段在 user 表：

- **列表查询**：`ParticipantMapper` JOIN user，返回带 user 信息的 VO（participantId/userId/userCode/name/gender/phone/college/major/teamName）
- **单条新增**：按 user_code 查/建 user + 创建 participant 关联（同 Excel 逻辑的单条版）
- **编辑**：编辑 user 信息（name/phone/gender/college/major）
- **删除**：删 participant 关联（**user 账号保留**，因为是全局账号，可能跨运动会）

---

## 六、我的参赛项目

### 接口
`GET /api/my/registrations`（拦截）
- JWT 取 userId → 查所有 participant(user_id) → JOIN registration + event + sports_meeting
- 返回按运动会分组：`[{ sportsMeeting, items: [{ eventName, scheduleName, category }] }]`

### 小程序新页面 `pages/my-registrations`
- 顶部摘要：累计参赛 N 个项目
- 按运动会分组展示，每组列该运动会报名的项目

---

## 七、数据迁移脚本

**形式**：Java 一次性 `CommandLineRunner`（profile= migrate 启用，跑完即删）

**原因**：BCrypt 加密 SQL 做不了，且去重逻辑复杂，需 Java 实现。

### 步骤
1. **DDL 准备**：
   - `user` 表：清空旧微信数据；ALTER 加 `user_code`(unique)/`password`/`name`/`gender`/`college`/`major`；删 `openid`/`union_id`/`nickname`/`avatar_url`/`session_key`
   - `participant` 表：ALTER 加 `user_id` 列（可空）
2. **数据迁移**（Java 逻辑）：
   - 从 participant 按 `user_code` 分组，每组取 `id` 最大那条（最新信息）
   - 插入 user：`user_code` + 取自 participant 的 name/gender/phone/college/major + `password = BCrypt("dlust123456")`
   - 回填 `participant.user_id`：按 `(user_code)` 关联 `user.id`
   - 脏数据（user_code 为空）记日志跳过
3. **DDL 收尾**：
   - participant 删 `user_code`/`name`/`phone`/`gender`/`college`/`major`
   - participant 加 `UNIQUE(sports_meeting_id, user_id)`
4. **执行前备份** `user`/`participant`/`registration` 表（按 feedback_db_policy，执行前向用户确认）

### 风险与边界
- 同一 `user_code` 跨 participant 记录信息冲突（如不同运动会填了不同电话）：取 id 最大那条，其余忽略
- user_code 重复但因大小写/空格不一致：脚本内 trim 后比较，但仍可能冲突 —— 冲突记日志跳过，人工处理
- registration 表的 `participant_id` 不变（仍指向原 participant 记录），迁移后 participant 保留 id，关联不断

---

## 八、影响面清单

### 后端
- `pom.xml`：加 `easyexcel`、`spring-security-crypto`
- `entity/User.java`：重写（去微信字段，加账号字段）
- `entity/Participant.java`：去 user_code/name 等，加 userId
- `UserMapper.java/.xml`：重写（账号查询、改密码、重置密码）
- `ParticipantMapper.xml`：所有查询加 JOIN user
- `RegistrationMapper.xml`：报名 VO 的 participantName 等改从 user JOIN
- `ResultMapper.xml`：成绩 VO 的参赛者信息改从 user JOIN
- `UserAuthController`：登录方法重写（账号密码）
- `AdminParticipantController`：加 import / import-template；新增/编辑适配 user 关联
- 新增 `AdminUserController`：reset-password
- `UserRegisterController`：报名接口改为取 JWT userId，去掉前端传 userCode
- `WebMvcConfig`：拦截列表更新
- 新增 `PasswordService`（BCrypt 工具封装）
- 新增迁移 CommandLineRunner

### 管理端（Vue）
- `api/user.ts`、`api/participant.ts`：接口调整
- `MeetingDetailView.vue` 参赛人员 tab：列表/新增/编辑适配新 VO；加「批量导入」「下载模板」「重置密码」按钮；导入结果弹窗
- 新增批量导入对话框组件

### 小程序
- 新增 `pages/login`（登录页）
- 新增 `pages/my-registrations`（我的参赛项目）
- `utils/auth.js`：改造（去微信登录，加账号登录）
- `pages/profile`：改造（登录态判断、菜单）
- `pages/register`、`pages/register-form`：触发登录
- `app.json`：注册新页面
- `UserAuthController` 登录响应字段调整，前端对应处理

---

## 验证

1. **后端**：`mvn compile && mvn test` 通过；新增 BCrypt 单测、导入逻辑单测
2. **前端**：`npm run build` TypeScript 通过
3. **手测**：
   - 管理端：Excel 导入 5 人 → 参赛人员列表显示；重置密码 → 小程序用默认密码登录成功；用户改密码后用新密码登录
   - 小程序：游客浏览首页/详情/赛程/项目正常；点报名跳登录；登录后报名成功；「我的」显示参赛项目；退出后跳登录页
   - 迁移：脚本跑完后，原 participant 数据可通过新账号登录、原报名记录仍可查
4. **数据库**：迁移后 `user.user_code` 唯一、`participant(user_id, sports_meeting_id)` 唯一、原 registration 记录的 participant_id 仍有效

---

## 注意事项

- 本次是认证体系大改，建议按「迁移 → 后端认证 → 管理端导入 → 小程序」顺序分阶段实施，每阶段独立提交可回滚
- user 账号保留原则：删 participant 不删 user（账号是全局的，可能跨运动会复用）
- BCrypt 加密强度用默认（10 轮），足够且性能可接受
- EasyExcel 读大文件用监听器模式（避免 OOM）；本场景文件不大，用同步读即可
