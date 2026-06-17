# 成绩按赛次独立存储 — 设计

- 日期:2026-06-15
- 关联:在项目报名页(EventDetailView)展示/录入成绩的诉求引发

## 1. 背景与目标

现状:`result` 表成绩是「项目级」(仅 `event_id` + `participant_id`,无赛次维度),导致预赛/决赛报名页看到同一条成绩。

目标:`result` 表经 `event_schedule` 关联赛次,使预赛与决赛成绩各自独立;并在项目报名页展示/录入当前赛次成绩。

关键决策(已与用户确认):

- 关联字段:`event_schedule_id`(关联 `event_schedule.id`)
- 无旧成绩数据需要迁移
- 完整范围:数据库 + 后端 + 前端(含 MeetingDetailView 连带)一次做全

## 2. 范围

做:

- 数据库:`result` 表加 `event_schedule_id`
- 后端:`Result` / `ResultVO` / `EventScheduleMapper` / `ResultMapper` / `ResultService` / `AdminResultController`
- 前端:`result.ts`、`EventDetailView`(成绩列)、`MeetingDetailView`(成绩 tab 连带)

不做:

- 不改 `event_schedule`、`schedule`、`registration` 等其他表结构
- 不新增成绩删除能力(沿用现有 `deleteResult`)
- 不做批量录入

## 3. 数据库改动

`result` 表新增字段:

```sql
ALTER TABLE result
  ADD COLUMN event_schedule_id BIGINT DEFAULT NULL
  COMMENT '项目-赛程关联ID(关联event_schedule.id)';
```

- 同步更新 `SportBackend/sql/result.sql` 建表脚本
- **由用户手动执行 ALTER**(我不直连数据库)
- 无旧数据,新列对现有行可为 NULL

## 4. 后端改动

### 4.1 Entity

- `Result.java`:加 `private Long eventScheduleId;`
  - 另加 `private Long scheduleId;`(非数据库字段,仅用于接收前端赛次参数,Service 内转换为 `eventScheduleId`)
- `ResultVO.java`:加 `eventScheduleId`、`scheduleId`、`scheduleName`

### 4.2 EventScheduleMapper

- 新增 `EventSchedule selectByEventIdAndScheduleId(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId)`
- xml:`SELECT * FROM event_schedule WHERE event_id = #{eventId} AND schedule_id = #{scheduleId} AND is_deleted = 0`

### 4.3 ResultMapper

- 新增 `List<ResultVO> selectVOByEventAndSchedule(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId)`
- xml:JOIN `event_schedule es` + `schedule s`,WHERE `es.event_id = ? AND es.schedule_id = ? AND es.is_deleted = 0`,选 `r.*`、`s.id AS schedule_id`、`s.name AS schedule_name`、`es.id AS event_schedule_id`;`ORDER BY r.score ASC`
- `insert` 加 `event_schedule_id` 列与值
- `updateById` 的 `<set>` 加 `event_schedule_id`

### 4.4 ResultService / Impl

- 新增 `getByEventAndSchedule(eventId, scheduleId)` → 调 `selectVOByEventAndSchedule`
- `add(Result)`:
  - 若 `eventScheduleId` 为空且 `scheduleId` 非空 → 调 `EventScheduleMapper.selectByEventIdAndScheduleId` 查出 id 填入 `eventScheduleId`
  - 若查不到对应 `event_schedule`(该项目未配置该赛次)→ 抛业务异常/返回错误
  - 调 `insert`
- `update(Result)`:同样支持按 `scheduleId` 转 `eventScheduleId`(若传了 `scheduleId`)

### 4.5 AdminResultController

- 新增 `GET /listByEventAndSchedule?eventId=&scheduleId=` → `getByEventAndSchedule`
- `/add`、`/update` 接收的 `Result` 可带 `scheduleId`,Service 内转换(对前端透明)
- 保留原 `/listByEvent`(返回项目下所有赛次成绩,MeetingDetailView 用)

## 5. 前端改动

### 5.1 result.ts

- `ResultVO` 加 `eventScheduleId`、`scheduleId`、`scheduleName`
- `ResultItem` 加 `scheduleId?`、`eventScheduleId?`
- 新增 `getResultsByEventAndSchedule(eventId, scheduleId)` → `GET /admin/result/listByEventAndSchedule`
- `addResult` / `updateResult` 可传 `scheduleId`

### 5.2 EventDetailView(报名页成绩列)

- `fetchResults` 改用 `getResultsByEventAndSchedule(eventId, scheduleId)`,只取当前赛次成绩
- `resultMap` 按 `participantId` 建(数据源 = 当前赛次)
- 录入:`addResult({ sportsMeetingId, eventId, participantId, scheduleId, score })`
- 修改:`updateResult({ id, scheduleId, score })`
- 行内编辑交互:有成绩显示数值、无成绩显示「录入」;点击变输入框;回车/失焦保存、Esc 取消;非数字提示「请输入有效成绩」并保持编辑态;空输入视为取消
- 列位置:「赛次」列之后、「状态」列之前

### 5.3 MeetingDetailView(成绩 tab 连带)

- 录入成绩对话框新增「赛次」下拉(`el-select`):选项 = 该项目已配置的赛次
- 成绩列表新增「赛次」列(显示 `scheduleName`)
- 录入时传 `scheduleId`;查询沿用 `/listByEvent` 或 `/list`(显示该项目所有赛次成绩)

## 6. 验证

数据库:

- ALTER 执行后,`DESC result` 显示 `event_schedule_id`

后端:

- `GET /listByEventAndSchedule?eventId=X&scheduleId=Y` 仅返回该赛次成绩

前端 EventDetailView:

- 预赛页为参赛者 A 录入成绩 → 决赛页参赛者 A 无成绩(独立),反之亦然
- 行内编辑:录入 / 修改 / 取消 / 非数字校验均符合预期

前端 MeetingDetailView:

- 录入成绩必须选赛次;列表显示赛次
- 经 MeetingDetailView 录入的成绩,在对应赛次报名页(EventDetailView)可见

## 7. 风险

中等。跨三层改动;数据库 ALTER 需手动执行;MeetingDetailView 改动较大。

边界:

- `event_schedule` 被软删除后,关联成绩在赛次视图查不到——业务上应避免「先删 event_schedule 再录成绩」。
- 录入时若 `(eventId, scheduleId)` 在 `event_schedule` 中不存在,后端返回错误提示,前端展示。
