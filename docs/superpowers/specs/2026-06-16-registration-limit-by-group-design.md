# 组别级报名限报规则 — 设计

- 日期:2026-06-16
- 关联:任务一(限报规则配置)+任务二(报名校验)合并为一个限报子系统

## 1. 背景与目标

现状:报名无「每人限报项目数」限制,参赛人员可无限报名项目。

目标:在**组别(group_type)级**配置限报规则——选中一批项目(按田/径/趣味赛分组多选,支持一键勾选某类)+ 一个统一上限 N;报名时按「每人」在选中项目集内已报的不同项目(event)数校验,超过 N 则拒绝。

关键决策(已与用户确认):

- 配置归属:**组别(group_type)级**,同组别所有代表队共用一套规则
- 限制对象:**每人**(参赛人员),非代表队总人次
- 上限粒度:**一个统一上限 N**(不按类别分别设)
- 计数单位:按 **event(项目)** 去重,同项目预赛+决赛算 1 项
- 数据建模:**group_type 表加 JSON 字段**(方案 A,不新建关联表)

## 2. 范围

做:

- 数据库:`group_type` 表加 `per_person_limit` + `limit_event_ids`(JSON)
- 后端:`GroupType`(Entity/Mapper/Service/Controller)加配置读写;`RegistrationService.add` 加报名校验;`RegistrationMapper` 加按 event 去重的计数查询
- 前端:组别管理页加「限报配置」弹窗(项目按 category 分组多选 + 全选本类 + 上限 N);报名操作接收后端限报错误并提示

不做(YAGNI):

- 不做「按类别分别设上限」(仅统一 N)
- 不做报名页「剩余可报数」实时显示(可作后续增强)
- 不改 `registration` / `event` / `event_schedule` 表结构
- 不对历史已超限报名做追溯处理(仅约束新报名)

## 3. 数据库改动

`group_type` 表新增字段:

```sql
ALTER TABLE group_type
  ADD COLUMN per_person_limit INT DEFAULT 0 COMMENT '每人限报项目数(0=不限)',
  ADD COLUMN limit_event_ids JSON DEFAULT NULL COMMENT '限报规则选中的项目ID数组(空/NULL=不限)';
```

- 同步更新 `SportBackend/sql/group_type.sql` 建表脚本
- **由用户手动执行 ALTER**(不直连数据库)
- 旧数据:`per_person_limit=0` 即不限,旧行为不变

## 4. 后端改动

### 4.1 Entity

- `GroupType.java`:加
  - `Integer perPersonLimit`
  - `List<Long> limitEventIds`(JSON,经 typeHandler 与库交互)

### 4.2 GroupTypeMapper

- `selectById` / `selectBySportsMeetingId`:返回值含新字段
- `insert` / `updateById`:写入新字段(JSON 序列化)

### 4.3 GroupTypeService

- 新增 `saveLimitConfig(Long groupTypeId, Integer perPersonLimit, List<Long> eventIds)`
- 新增 `getLimitConfig(Long groupTypeId)`(返回 `perPersonLimit` + `limitEventIds`)

### 4.4 RegistrationMapper

- 新增 `int countDistinctEventByParticipantInEvents(@Param("participantId") Long, @Param("eventIds") List<Long>, @Param("statuses") List<Integer>)`
- xml:`SELECT COUNT(DISTINCT event_id) FROM registration WHERE participant_id=? AND event_id IN(...) AND status IN(...)`

### 4.5 RegistrationService.add 校验

`add(participantId, eventId, scheduleId)` 内,`insert` 前:

1. 查 `participant → team → group_type`(`participant.team_id` → `team.group_type_id`)。无 team/group_type 则**跳过校验**(放行)。
2. 取 `group_type.per_person_limit`(N)与 `limit_event_ids`(项目集)。
3. 若 `N<=0` 或 项目集为空 或 本次 `eventId` 不在项目集 → **跳过**(不限)。
4. 已报数 = `countDistinctEventByParticipantInEvents(participantId, 项目集, [0,1])`。
   - status:`0 已报名`、`1 已晋级` 计入占用;`2 已取消` 不计。
5. 若本次 `eventId` 不在「已报的不同 event」中 → 已报数+1;否则 已报数。
6. 若结果 > N → 抛业务异常(「超出限报:每人最多报 N 项」)。
7. 否则 `insert`。

### 4.6 自动晋级报名

`autoRegisterNextSchedule` 为同 event,且直接调 `registrationMapper.insert` 不经 `add`,**不受校验影响**(同 event 本就不增计数)。

### 4.7 AdminGroupTypeController(或现有组别 controller)

- `GET /admin/groupType/limitConfig?groupTypeId=`
- `POST /admin/groupType/saveLimitConfig`(body:`groupTypeId`, `perPersonLimit`, `eventIds[]`)

## 5. 前端改动

### 5.1 api/groupType.ts

- `getLimitConfig(groupTypeId)`
- `saveLimitConfig({ groupTypeId, perPersonLimit, eventIds })`

### 5.2 组别管理页

- 每行组别加「限报配置」按钮
- 弹窗内容:
  - 项目多选:按 `event.category`(田赛/径赛/趣味赛)分组,每组 checkbox 列表,组标题旁「全选本类」快捷
  - 「每人限报项目数」数字输入(N,0=不限)
  - 保存调 `saveLimitConfig`
- 打开时:加载当前组别 `limitConfig` + 该运动会所有项目(按 category 分组)

### 5.3 报名操作

- 报名接口若返回限报错误(`message`),用 `ElMessage.error` 展示
- (可选增强,后置)报名页显示该人剩余可报数

## 6. 校验语义总结

- `N=0` 或 选中集空 → **完全不限制**
- 仅当「选中集非空 且 N>0 且 本次 event 在选中集」时才计数
- 计数 = 该人在选中集内已报的不同 event 数(`status∈{0,1}`),同 event 多赛次算 1
- 含本次后超过 N 则拒绝

## 7. 验证

- 配置:某组别选中径赛全部 + N=2
  - 报名第 1 个径赛项目 → 成功
  - 报名第 2 个不同径赛项目 → 成功(达上限)
  - 报名第 3 个不同径赛项目 → **拒绝**
  - 同一径赛项目报另一赛次 → 成功(同 event 不增计数;或经晋级自动报名)
- 田赛项目(未选中)→ 不受限,任意报
- `N=0` → 任意报
- 晋级自动报名决赛 → 不受限报影响

## 8. 风险/边界

- `participant` 无 team 或 team 无 group_type → 不校验(数据不全时放行,避免误伤;可日志告警)
- `limit_event_ids` 含已删除/不存在 event → 计数时无害(IN 不匹配)
- 仅约束新报名,历史超额数据不追溯
- **风险:低**。改动集中在 `group_type` 配置 + `add` 方法校验,不涉及复杂迁移
