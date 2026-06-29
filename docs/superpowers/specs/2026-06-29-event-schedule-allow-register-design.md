# 开放报名下沉到「项目×轮次」粒度 — 设计文档

- 日期：2026-06-29
- 分支：feat/result-by-schedule
- 状态：待审核

## 1. 背景与目标

### 问题
当前「是否开放报名」由 `event.allow_register` 单一字段控制，挂在**项目**粒度。一个项目（如 100 米）关联多个轮次（预赛/复赛/决赛）时，`allow_register=1` 意味着整个项目开放，无法表达「只开放预赛、决赛不开放」的语义。

### 目标
将开放报名开关下沉到 `event_schedule`（项目×轮次）粒度：
- 新增项目勾选多个轮次时，**自动只把轮次最低（`schedule.sort` 最小）的那个设为开放**，其余关闭。
- 后续可在轮次详情页**手动切换**某个轮次的开关（如预赛比完后开放决赛报名）。

### 非目标（YAGNI）
- 不做轮次间互斥（开新轮次不自动关旧轮次）。
- 不改晋级自动报名逻辑（`autoRegisterNextSchedule` 已按 `schedule.sort` 正确工作）。
- 不改「限报规则」相关逻辑。

## 2. 现状分析（关键代码位置）

| 关注点 | 位置 | 说明 |
|--------|------|------|
| 轮次定义 | `schedule` 表，`sort` 字段（越小越靠前） | 预赛 sort=1 < 复赛 < 决赛 |
| 项目总开关 | `event.allow_register` | 1=支持报名，0=仅晋级 |
| 项目×轮次关联 | `event_schedule` 表 | 当前无 allow_register 字段 |
| 轮次排序查询 | [EventScheduleMapper.xml:16-21](../../../SportBackend/src/main/resources/mapper/EventScheduleMapper.xml) `selectScheduleIdsByEventId` | 已按 `schedule.sort ASC` 排序 |
| 报名默认轮次 | [UserRegisterController.java:53-61](../../../SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserRegisterController.java) | 前端不传 scheduleId 时取 `scheduleIds.get(0)`（最低轮次） |
| 晋级自动报名 | [RegistrationServiceImpl.java:155](../../../SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/RegistrationServiceImpl.java) `autoRegisterNextSchedule` | 按 sort 顺序报下一轮次，已正确 |
| 小程序报名过滤 | [register.js:22](../../../sportdocu/pages/register/register.js) `filter(e => e.allowRegister)` | 项目级过滤 |
| 管理端项目弹窗 | [ScheduleDetailView.vue:202-227](../../../webManageProject/src/views/ScheduleDetailView.vue) | 多选轮次 + allowRegister 总开关 |
| 关联保存（全删重建） | [EventScheduleServiceImpl.java:44-50](../../../SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java) `saveEventSchedules` | 需改造为增量同步 |

## 3. 数据模型变更

### event_schedule 加字段
```sql
ALTER TABLE event_schedule
  ADD COLUMN allow_register TINYINT NOT NULL DEFAULT 0
  COMMENT '该轮次是否开放报名：1-开放，0-关闭';
```

- 同步更新 [event_schedule.sql](../../../SportBackend/sql/event_schedule.sql)（项目强制约定：DDL 变更必须同步建表脚本）。
- [EventSchedule.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/entity/EventSchedule.java) 加 `private Integer allowRegister;`

### event.allow_register 保留
语义明确为「项目总闸」：1=该项目支持报名，0=仅晋级（不开放任何轮次）。注释已存在，无需改动。

## 4. 业务规则

### 4.1 「可报名」统一判定
某项目某轮次允许报名 ⟺ `event.allow_register = 1` **且** `event_schedule.allow_register = 1`。

### 4.2 新增项目时的自动逻辑
保存项目轮次关联时（`saveEventSchedules`）：
- 若 `event.allow_register = 1`：所选 scheduleIds 按 `schedule.sort` 升序，仅最小的那条 `allow_register=1`，其余=0。
- 若 `event.allow_register = 0`：全部=0。
- 仅选 1 个轮次且总闸开：那条=1。

→ 满足「选了预赛+复赛+决赛，自动只开预赛」。

### 4.3 编辑项目时的行为（增量同步，保留状态）
`saveEventSchedules` 从「全删重建」改为**增量 diff**：
- 已存在且仍被选中的关联：**保留** `allow_register` 不变。
- 新增的关联：`allow_register = 0`（不自动开）。
- 被取消选中的关联：软删除。

→ 用户手动开的决赛不会被「改个项目名」重置。

### 4.4 后续切换（轮次详情页，不互斥）
- 管理员在轮次详情页切换某项目某轮次的开关。
- 开新轮次**不自动关**旧轮次，允许同一项目多个轮次同时开放。

## 5. 后端接口变更

### 5.1 改造现有
- `saveEventSchedules(eventId, scheduleIds)`：**保持 Controller 调用签名不变**，Service 内部读取 `event.allow_register` 后按 4.2/4.3 处理。`batchInsert` 需支持带 allow_register 列。
- `getByScheduleId` / `selectByScheduleId`：返回带 allow_register（轮次详情页列表要用）。

### 5.2 新增接口
`PUT /api/admin/event-schedule/allow`
```json
{ "eventId": 1, "scheduleId": 2, "allowRegister": 1 }
```
→ 更新对应 event_schedule.allow_register。在 `AdminEventScheduleController` + `EventScheduleService` + Mapper 新增 `updateAllowRegister` 方法。

### 5.3 event list 接口增强
为支持小程序精确过滤，event 列表每项增加派生字段 `currentOpenScheduleId`：该项目 `event_schedule.allow_register=1` 且 `schedule.sort` 最小的轮次 id，无则 null。在 **Service 层装配**（先查 event 列表，再批量查各 event 的开放轮次取最小），避免给 EventMapper 单表查询加复杂 JOIN。

## 6. 前端变更（管理端）

### 6.1 轮次详情页开关 [ScheduleDetailView.vue:179-181](../../../webManageProject/src/views/ScheduleDetailView.vue)
「开放报名」列：纯文字 → `el-switch`，绑定该行在该轮次的 allow_register，change 时调用新接口 `PUT /event-schedule/allow`。
- 需调整 `fetchEvents`：从 `getEventSchedulesBySchedule` 拿到每个 eventId 对应的 allow_register（关联表已带该字段）。

### 6.2 项目编辑弹窗
保持现有 UI（多选轮次 + allowRegister 总开关）。提交逻辑不变（后端按 4.2 自动处理）。可选：多选轮次下方加提示「保存后将自动只开放最低轮次（预赛），其余可在各轮次详情页手动开启」。

## 7. 小程序端联动

- [register.js:22](../../../sportdocu/pages/register/register.js) 过滤条件：`e.allowRegister && e.currentOpenScheduleId`，避免显示「总闸开但无开放轮次」的项目。
- 报名 submit（[UserRegisterController.java:53-61](../../../SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserRegisterController.java)）：把「取 get(0)」改为「取该项目 `event_schedule.allow_register=1` 且 sort 最小的轮次」；无则返回 `Result.error(400, "该项目当前未开放报名")`。
- detail 页「可报名/仅晋级」徽章（基于 event.allowRegister）不变。

## 8. 数据迁移

执行 DDL 加列后，回填老数据（DML）：
- 每个 event 取其关联轮次中 `schedule.sort` 最小者，`allow_register = event.allow_register`；
- 其余关联 `allow_register = 0`。

迁移后行为与现状等价（向后兼容）。

> 注意：按项目约定，DDL 与 DML 执行前均需向用户确认。

## 9. 已确认的决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 开关粒度 | 下沉到 event_schedule（项目×轮次） | 唯一能表达分轮次控制的方案 |
| 切换入口 | 轮次详情页加 el-switch | 贴合「比完预赛→进决赛页→开放」工作流 |
| 编辑时行为 | 保留现有开关（增量同步） | 不破坏用户手动切换结果 |
| 切换互斥性 | 不互斥，自由控制 | 灵活，管理员可控 |
| event.allow_register | 保留作总闸 | 沿用「仅晋级」语义，兼容现有数据 |

## 10. 影响范围清单

**后端**
- DDL：event_schedule 加列 + 同步 event_schedule.sql
- entity：EventSchedule.java
- Mapper：EventScheduleMapper.xml（batchInsert 带列、新增 updateAllowRegister、查询带列）
- Service：EventScheduleService/Impl（saveEventSchedules 增量化、新增 updateAllowRegister）
- Controller：AdminEventScheduleController（新增 allow 接口）
- EventMapper/Service：list 带 currentOpenScheduleId
- UserRegisterController：submit 取轮次逻辑

**管理端**
- ScheduleDetailView.vue（开关列 + fetchEvents 调整）
- api/eventSchedule.ts（新增 toggleAllow 方法）

**小程序**
- register.js（过滤条件）
- register.wxml 无需改

**数据库**
- 迁移脚本（DDL + 回填 DML，执行前需用户确认）
