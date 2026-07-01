# 破纪录候选标记 + 人工审核入册 — 设计

- 日期:2026-06-30
- 关联:成绩录入(EventDetailView)、校运会记录(record 表)、比赛分类重构(田赛/径赛/团队赛)

## 1. 背景与目标

现状:

- `record` 表存在,但**完全人工维护**(RecordView 纯 CRUD),且字段语义模糊:
  - `score` DECIMAL(秒/米,人类可读)与 `result.score_value` INT(毫秒/厘米)**单位不一致**
  - **无 sports_meeting_id**(不分届)、**无 category**(不分径赛/田赛,无法判断方向)、无 result 追溯
- 学校**未提供往年历史数据**,record 表初始为空。
- `result.score_value` INT(径赛=毫秒、田赛=厘米)是成绩录入的权威值,已有 rankMap 名次计算。

目标:

- 让管理员能在「不依赖往年历史」的前提下,逐步建立校运会纪录档案。
- **成绩录入后,赛次前 3 名自动标三角形「候选破纪录」**(即时计算,不存候选状态)。
- 独立「破纪录审核」页,管理员逐条审核候选:**通过 → 入册 record(历届档案,追加)**;**拒绝 → 标记不再提示**。
- record 表扩展为「历届档案」语义(带届次/类别/统一单位),保留人类可读副本。

关键决策(已与用户确认):

- **触发逻辑**:赛次前 3 名都标(不对比历史;首届即建立基线,后续届同项目前 3 也标,审核时人工判断是否真破纪录)
- **三角形标记数**:前 3 名
- **record 表语义**:历届档案(追加,带 sports_meeting_id)
- **整体方案**:result 加 `record_status`(0/1/2);三角形即时算前 3;独立审核页(方案 A)
- **审核状态三态**:0=未审(默认)、1=通过已入册、2=拒绝
- **三角形三色**:🟡金黄(待审)、🟢绿(通过入册)、⚪灰(拒绝)
- **赛次范围**:预赛/决赛**都标**(各自赛次前 3)
- **审核页「当前纪录」列**:**不展示**(管理员凭经验判断,不系统对比)
- **venue 字段**:**删除**(record 表 DROP COLUMN,RecordView 同步去掉)
- **score_value 字段(新)在 RecordView 弹窗**:**只展示不可编辑**(自动从审核通过时填,管理员不手填)
- **审核 endpoint 归属**:`AdminRecordController` POST `/api/admin/record/review`

## 2. 范围

做:

- 数据库:
  - `result` 表加 `record_status` TINYINT
  - `record` 表加 `sports_meeting_id`/`category`/`score_value`/`result_id` 4 字段
  - `record` 表删 `venue` 字段
  - 同步 `SportBackend/sql/result.sql`、`SportBackend/sql/record.sql`
- 后端:
  - `Result` 实体加 `recordStatus`;`Record` 实体加 4 字段、删 venue
  - `RecordMapper.xml`:insert/update 含新字段;select 加 `<if>` 筛选(sportsMeetingId/eventName/category)
  - `RecordService` 加 `reviewRecord(Long resultId, String action)`(`@Transactional` 改 result + insert record)
  - `AdminRecordController` 加 `POST /review`;`/list` 加筛选参数
  - score 换算静态方法(毫秒→秒、厘米→米)+ 纯逻辑单测
- 前端:
  - `api/result.ts`(`ResultVO` 加 recordStatus)、`api/record.ts`(Record 接口字段调整、加 reviewRecord、list 加筛选参数)
  - 新建 `views/RecordReviewView.vue`(审核页)
  - `EventDetailView.vue`:成绩列旁加三角形(前 3 名 + 三色)
  - `RecordView.vue`:去 venue 列/输入;加类别列/运动会列;score_value 只读展示
  - 路由 + 侧边栏菜单加「破纪录审核」

不做(YAGNI):

- 不做自动对比破纪录(候选 = 赛次前 3 即时算,非对比 record)
- 不做审核历史审计(不记「谁审的、何时审的」,只记结果 0/1/2)
- 不做批量审核(逐条点)
- 不做「重审」流程(要重审 → 去 RecordView 删 record → 该 result 回 status=0 再审)
- 不做 record 删除级联(删 record 不会自动把 result.record_status 改回 0,需管理员手动协调)
- 不做小程序端(用户端)展示
- 不做跨赛次聚合前 3(每个赛次独立前 3)
- 不做审核页的「该项目当前纪录」对比列

## 3. 数据库改动

### 3.1 `result` 表加字段

```sql
ALTER TABLE result ADD COLUMN record_status TINYINT NOT NULL DEFAULT 0
    COMMENT '破纪录审核状态（0=未审，1=通过已入册，2=拒绝）' AFTER points;
```

### 3.2 `record` 表加字段

```sql
ALTER TABLE record
    ADD COLUMN sports_meeting_id BIGINT       DEFAULT NULL COMMENT '所属运动会ID' AFTER id,
    ADD COLUMN category          VARCHAR(20)  DEFAULT NULL COMMENT '项目类别（径赛/田赛/团队赛）' AFTER event_name,
    ADD COLUMN score_value       INT          DEFAULT NULL COMMENT '成绩值（径赛=毫秒，田赛=厘米，对齐result.score_value）' AFTER score,
    ADD COLUMN result_id         BIGINT       DEFAULT NULL COMMENT '源成绩ID（审核通过时追溯）' AFTER score_value;
```

### 3.3 `record` 表删 venue

```sql
ALTER TABLE record DROP COLUMN venue;
```

### 3.4 同步 SQL 脚本

更新 `SportBackend/sql/result.sql`(加 record_status 字段)、`SportBackend/sql/record.sql`(加 4 字段、删 venue)。

## 4. 后端

### 4.1 实体

`Result.java` 加字段:

```java
private Integer recordStatus;
```

`Record.java` 改动:

```java
// 新增字段
private Long sportsMeetingId;
private String category;
private Integer scoreValue;
private Long resultId;
// 删除 venue 字段
```

### 4.2 Mapper

`ResultMapper.xml`:所有 select 自动含 record_status(列映射,无需改 SQL,`SELECT *` 即可)。

`RecordMapper.xml`:

- `selectAll` 改名为 `selectByConditions` 或保留 `selectAll` 但加 `<if>` 条件(sportsMeetingId/eventName/category)
- `insert` 加新字段(sports_meeting_id/category/score_value/result_id);去掉 venue
- `updateById` 同步新字段;去掉 venue

```xml
<select id="selectAll" resultType="Record">
    SELECT * FROM record
    <where>
        <if test="sportsMeetingId != null">AND sports_meeting_id = #{sportsMeetingId}</if>
        <if test="eventName != null and eventName != ''">AND event_name = #{eventName}</if>
        <if test="category != null and category != ''">AND category = #{category}</if>
    </where>
    ORDER BY create_time DESC
</select>
```

`RecordMapper.java`:`selectAll` 签名改为 `selectAll(@Param("sportsMeetingId") Long sportsMeetingId, @Param("eventName") String eventName, @Param("category") String category)`(3 个可选筛选参数,均可 null)。

`ResultMapper.java` 加方法:`int updateRecordStatus(@Param("id") Long id, @Param("status") Integer status);`。

```xml
<update id="updateRecordStatus">
    UPDATE result SET record_status = #{status} WHERE id = #{id}
</update>
```

### 4.3 Service

`RecordService` 加方法:

```java
void reviewRecord(Long resultId, String action);  // action: "approve" | "reject"
```

`RecordServiceImpl.reviewRecord`(`@Transactional`):

1. 参数校验:resultId 非空,action ∈ {approve, reject}
2. 查 result(`resultMapper.selectById`)→ 不存在抛 `RuntimeException("成绩不存在")`
3. 查 event(`eventMapper.selectById`)、participant(`participantMapper.selectById`)、team(`teamMapper.selectById` by participant.teamId)、group_type(`groupTypeMapper.selectById` by event.groupTypeId)
4. action = approve:
   - `resultMapper.updateRecordStatus(resultId, 1)`
   - 拼 Record 对象(字段映射见 4.4),`recordMapper.insert(record)`
5. action = reject:`resultMapper.updateRecordStatus(resultId, 2)`

> 跨模块依赖:`RecordServiceImpl` 注入 `ResultMapper`/`EventMapper`/`ParticipantMapper`/`TeamMapper`/`GroupTypeMapper`(项目惯例允许 Service 注入多 Mapper)。

### 4.4 score 换算静态方法(纯逻辑,可单测)

抽到 `RecordServiceImpl` 的包级静态方法:

```java
/**
 * 将 result.score_value（INT 内部单位）换算为 record.score（DECIMAL 人类可读）。
 * 径赛/团队赛: 毫秒 → 秒（÷1000）
 * 田赛: 厘米 → 米（÷100）
 */
static BigDecimal convertScoreValueToScore(Integer scoreValue, String category) {
    if (scoreValue == null) return null;
    if ("田赛".equals(category)) {
        return BigDecimal.valueOf(scoreValue).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
    // 径赛/团队赛
    return BigDecimal.valueOf(scoreValue).divide(BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP);
}
```

### 4.5 Controller

`AdminRecordController`:

```java
@PostMapping("/review")
public Result<String> review(@RequestBody Map<String, Object> body) {
    Long resultId = body.get("resultId") == null ? null : Long.valueOf(body.get("resultId").toString());
    String action = body.get("action") == null ? null : body.get("action").toString();
    log.info("审核破纪录候选: resultId={}, action={}", resultId, action);
    recordService.reviewRecord(resultId, action);
    return Result.success("审核成功");
}
```

`/list` 加可选筛选参数:

```java
@GetMapping("/list")
public Result<List<Record>> list(
        @RequestParam(required = false) Long sportsMeetingId,
        @RequestParam(required = false) String eventName,
        @RequestParam(required = false) String category) {
    return Result.success(recordService.getAll(sportsMeetingId, eventName, category));
}
```

### 4.6 通过时 record 字段映射

| record 字段 | 来源 |
|---|---|
| `sportsMeetingId` | result.sportsMeetingId |
| `category` | event.category |
| `scoreValue` | result.scoreValue |
| `score` | convertScoreValueToScore(result.scoreValue, event.category) |
| `resultId` | result.id |
| `eventName` | event.name |
| `groupType` | group_type.name(event.groupTypeId 关联) |
| `unit` | team.name(participant.teamId 关联) |
| `name` | participant.name |
| `recordTime` | result.createTime |
| ~~venue~~ | (字段已删) |

## 5. 前端

### 5.1 API

`api/result.ts`:`ResultVO` 接口加 `recordStatus: number`。

`api/record.ts`:

```ts
export interface Record {
  id: number
  sportsMeetingId: number | null
  groupType: string
  eventName: string
  category: string
  unit: string
  name: string
  score: number | null
  scoreValue: number | null
  resultId: number | null
  recordTime: string
  createTime: string
  updateTime: string
}
// 删除 venue

export function getRecordList(params?: { sportsMeetingId?: number; eventName?: string; category?: string }) {
  return request.get('/admin/record/list', { params })
}

export function reviewRecord(resultId: number, action: 'approve' | 'reject') {
  return request.post('/admin/record/review', { resultId, action })
}
// addRecord/updateRecord/deleteRecord 保留
```

### 5.2 新建 RecordReviewView.vue(审核页)

路由 `/record-review`,侧边栏新增菜单「破纪录审核」。

**筛选**:

- 运动会下拉(必选,默认当前)
- 项目下拉(依运动会,可选「全部」)
- 赛次下拉(依项目,可选「全部」)
- 状态(默认「待审」,可选全部/通过/拒绝)

**数据源**(复用现有 API,不新建 endpoint):
- 项目下拉:`getEventList({ sportsMeetingId })`
- 赛次下拉:从 `getRegistrationListByEvent(eventId)` 的结果取 scheduleId 集合,或复用 EventDetailView 的赛次加载逻辑
- 候选列表:`getResultsByEventAndSchedule(eventId, scheduleId)` 返回该赛次所有 result(含 recordStatus),前端按 category 排序取前 3 名标候选(复用 EventDetailView 的 rankMap 排序逻辑)
- 默认筛选状态:运动会=当前、项目=全部时提示「请选择项目」,选项目后默认赛次=第一个赛次

候选列表每条显示:参赛者/代表队/项目/赛次/格式化成绩/状态 tag/操作(通过/拒绝按钮,仅 status=0 显示)。

操作调 `reviewRecord(resultId, 'approve'|'reject')` → toast → 刷新列表。

### 5.3 EventDetailView.vue 三角形标记

成绩列单元格旁,前 3 名显示三角形图标:

```html
<el-table-column label="成绩" width="120">
  <template #default="{ row }">
    <el-button link type="primary" size="small" @click="openScoreEdit(row)">
      {{ formatScore(resultMap.get(row.participantId)) }}
    </el-button>
    <el-tooltip v-if="rankMap.get(row.participantId) <= 3 && rankMap.get(row.participantId) != null"
      :content="recordTooltip(resultMap.get(row.participantId))">
      <span :class="['record-flag', recordFlagClass(resultMap.get(row.participantId))]">▲</span>
    </el-tooltip>
  </template>
</el-table-column>
```

辅助函数:

```ts
function recordFlagClass(r?: ResultVO): string {
  if (!r) return ''
  // 0=未审金黄、1=通过绿、2=拒绝灰
  if (r.recordStatus === 1) return 'record-flag-approved'
  if (r.recordStatus === 2) return 'record-flag-rejected'
  return 'record-flag-pending'  // 0 或 null
}
function recordTooltip(r?: ResultVO): string {
  if (!r) return ''
  if (r.recordStatus === 1) return '已通过审核，已入册校运会纪录'
  if (r.recordStatus === 2) return '已拒绝（非破纪录）'
  return '破纪录候选（赛次前 3），待审核'
}
```

CSS:

```css
.record-flag { margin-left:4px; font-size:12px; font-weight:600; }
.record-flag-pending { color: #e6a23c; }    /* 金黄 */
.record-flag-approved { color: #67c23a; }    /* 绿 */
.record-flag-rejected { color: #c0c4cc; }    /* 灰 */
```

> rankMap 已含名次(`rankMap.get(participantId) <= 3` 即前 3)。resultMap 含 recordStatus(后端 select 返回)。

### 5.4 RecordView.vue 调整

- 表格列:去掉「地点」;新增「类别」「运动会」列
- 弹窗:去掉 venue 输入;新增 score_value **只读展示**(`disabled` 或纯文本);category 下拉(径赛/田赛/团队赛)
- 仍支持人工 CRUD(管理员手动维护历届档案)

### 5.5 路由 + 菜单

`router/index.ts` 加 `/record-review` → `RecordReviewView`。
侧边栏菜单(在 DefaultLayout 或菜单配置)加「破纪录审核」项,与「记录管理」并列。

## 6. 数据流

- **三角形**:成绩录入/修改 → 后端返回 result(含 recordStatus)→ 前端 rankMap 算前 3 → 前 3 名显示三角形(颜色按 recordStatus)→ 即时更新,无需同步
- **审核通过**:`POST /record/review { resultId, 'approve' }` → Service @Transactional:UPDATE result.record_status=1 + INSERT record(自动拼字段)→ 前端刷新
- **审核拒绝**:`POST /record/review { resultId, 'reject' }` → UPDATE result.record_status=2 → 前端刷新,三角形变灰
- **重审**:管理员去 RecordView 删 record → 该 record 删除(但 result.record_status 仍为 1,需手动改回 0 或直接在审核页拒绝路径外处理)→ 不在本次范围(见不做清单)

## 7. 错误处理

沿用项目约定:

- Service 抛 `RuntimeException("原因")` → `GlobalExceptionHandler` → HTTP 400
- 参数校验:`reviewRecord` 校验 resultId 非空、action ∈ {approve, reject},否则抛异常
- result 不存在 → 抛 `RuntimeException("成绩不存在")`
- result.record_status 已是 1 或 2(重复审核)→ 抛 `RuntimeException("该成绩已审核")`(避免重复入册 record)

## 8. 测试

`RecordServiceImplTest` 纯逻辑单测(项目惯例不测 Mapper SQL):

- `convertScoreValueToScore` 单测:
  - 径赛 10250 毫秒 → 10.25 秒
  - 田赛 680 厘米 → 6.80 米
  - 团队赛 70000 毫秒 → 70.00 秒(1分10秒)
  - scoreValue = null → null
  - category = null → 默认按径赛算(或抛异常,二选一,推荐默认径赛)
- `validateReviewAction`(若抽成静态):resultId 空/action 非法 → 抛异常

后端 `mvn test` 全绿。

## 9. 数据迁移

- **DDL**:result 加列、record 加 4 列删 venue(执行前向用户确认)
- **DML**:无需迁移。record 表当前为空(用户确认无历史);result.record_status 默认 0,现有成绩一律视为「未审」(管理员可后续在审核页补审)。

## 10. 实施顺序建议

1. DDL(result 加列、record 加 4 列删 venue)+ 同步 SQL 脚本(需用户确认)
2. 后端实体/Mapper/Service/Controller + score 换算纯逻辑单测
3. 前端 API(result.ts/record.ts)+ RecordReviewView 审核页 + 路由菜单
4. 前端 EventDetailView 三角形标记 + RecordView 字段调整
5. 端到端联调验证
