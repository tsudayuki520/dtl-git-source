# 代表队总分批量刷新 + 调整分 + 总积分榜 — 设计

- 日期:2026-06-29
- 关联:积分系统补齐(原 `team.total_score` 字段为人工维护,本次改为按队员成绩 + 调整分自动重算)

## 1. 背景与目标

现状:

- `team.total_score` DECIMAL(8,2) 字段存在,但**完全人工维护**——管理端代表队编辑弹窗里手填,与队员实际成绩脱钩。
- `result.points` INT 字段记录每条成绩对应的积分,录入成绩时手动填写。
- `participant.team_id` 关联到 team,但改队后不影响 total_score。
- 全项目**无事件总线/消息机制**,无定时任务。

目标:

- 让 `team.total_score` 自动反映「该队所有成员的所有 `result.points` 之和」+「该队的加减调整分之和」。
- 通过组别管理页的「刷新所有代表队总分」按钮触发**批量重算**整个运动会所有代表队(覆盖语义,旧值丢弃)。
- 新增 `team_score_adjustment` 关联表,记录加减分及原因(如「xxx作弊扣5分」「精神文明+3」),一个队可多条。
- 新增「总积分榜」tab,跨组别按 `total_score` 降序展示,Top3 金银铜高亮。

关键决策(已与用户确认):

- **存储策略**:手动刷新(保留 `team.total_score` 字段,不实时同步)
- **刷新口径**:覆盖(刷新后旧值丢弃)
- **赛次口径**:全部累加(预赛/决赛/复赛所有赛次 result.points 都计入,不去重)
- **触发粒度**:批量刷新(运动会层面一个按钮重算所有队)
- **调整分存储**:关联表 `team_score_adjustment`(一个队可多条调整,每条独立备注)
- **排行榜位置**:独立「总积分榜」tab(跨组别总榜)
- **代表队编辑弹窗的总分输入框**:**去掉**,改为只读展示 + 提示语(与覆盖语义自洽,避免「手填被悄悄覆盖」的混淆)

## 2. 范围

做:

- 数据库:新建 `team_score_adjustment` 表(`team` 表无变更)
- 后端:
  - 实体/Mapper/Service/Controller 完整新增 `TeamScoreAdjustment` CRUD
  - `TeamMapper` 加 `recalculateTotalScoreBySportsMeetingId`(一条 UPDATE 相关子查询)
  - `TeamService` 加 `refreshTotalScoreBySportsMeetingId`
  - `TeamController` 加 `POST /admin/team/refreshTotalScore`
- 前端:
  - `api/team.ts` 加 `refreshTotalScore`
  - 新建 `api/teamScoreAdjustment.ts`
  - `MeetingDetailView`:组别管理 tab 加「刷新所有代表队总分」按钮;代表队编辑弹窗去掉「总分」输入框改只读+提示;新增「总积分榜」tab(表格 + Top3 高亮)
  - `TeamDetailView`:info-bar 总分旁加「(刷新重算)」提示;编辑弹窗去掉「总分」输入框;新增「调整记录」卡片 + 添加/删除弹窗

不做(YAGNI):

- 不做实时同步(result 录入/删除、adjustment 增删、participant 改队都不立即更新 total_score,统一靠刷新按钮)
- 不做单队刷新(仅批量刷新一个按钮)
- 不做调整记录的修改(只能增/删,不能改;改 = 删后重建)
- 不做排行榜的导出/筛选/分组别排行(仅跨组别总榜,后续可扩展)
- 不做历史 total_score 的归档/审计
- 不做「按代表队成员历史归属」追溯积分(participant 改队后,旧成绩按当前 team_id 归属新队,刷新即反映)

## 3. 数据库改动

新建 `team_score_adjustment` 表:

```sql
CREATE TABLE IF NOT EXISTS team_score_adjustment (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id       BIGINT NOT NULL COMMENT '所属代表队',
    delta_amount  DECIMAL(8,2) NOT NULL COMMENT '调整数额（正=加分，负=扣分）',
    note          VARCHAR(255) NOT NULL COMMENT '调整原因，如：xxx作弊扣5分',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_team_id (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代表队总分调整记录';
```

同步脚本到 `SportBackend/sql/team_score_adjustment.sql`(按 CLAUDE.md DDL 同步约定)。

`team` 表无变更,继续用 `total_score` 字段(覆盖语义)。

## 4. 后端

### 4.1 实体

`TeamScoreAdjustment.java`:

```java
@Data
public class TeamScoreAdjustment {
    private Long id;
    private Long teamId;
    private BigDecimal deltaAmount;
    private String note;
    private LocalDateTime createTime;
}
```

### 4.2 Mapper

`TeamScoreAdjustmentMapper`(`TeamScoreAdjustmentMapper.xml`):

- `selectByTeamId(Long teamId) ORDER BY create_time DESC`
- `insert(TeamScoreAdjustment)`
- `deleteById(Long id)`

`TeamMapper` 新增方法:

```xml
<update id="recalculateTotalScoreBySportsMeetingId">
    UPDATE team t
    SET t.total_score = COALESCE((
        SELECT SUM(r.points) FROM result r
        JOIN participant p ON r.participant_id = p.id
        WHERE p.team_id = t.id
    ), 0) + COALESCE((
        SELECT SUM(a.delta_amount) FROM team_score_adjustment a
        WHERE a.team_id = t.id
    ), 0)
    WHERE t.sports_meeting_id = #{sportsMeetingId}
</update>
```

返回影响行数(int),前端用于 toast「已刷新 N 个代表队总分」。

### 4.3 Service

`TeamScoreAdjustmentService` / `TeamScoreAdjustmentServiceImpl`:

- `List<TeamScoreAdjustment> getByTeamId(Long teamId)`
- `void add(Long teamId, BigDecimal deltaAmount, String note)` — note 空校验
- `void delete(Long id)`

`TeamService` 加方法:

- `int refreshTotalScoreBySportsMeetingId(Long sportsMeetingId)` — 调 mapper 返回影响行数

### 4.4 Controller(admin 包)

`TeamController` 加方法:

- `POST /api/admin/team/refreshTotalScore?sportsMeetingId=XX` → `Result.ok(Map.of("refreshedCount", n))`

新建 `TeamScoreAdjustmentController`:

- `GET /api/admin/teamScoreAdjustment/list?teamId=XX` → `Result.ok(List<TeamScoreAdjustment>)`
- `POST /api/admin/teamScoreAdjustment/add` (body: `{teamId, deltaAmount, note}`) → `Result.ok()`
- `DELETE /api/admin/teamScoreAdjustment/{id}` → `Result.ok()`

## 5. 前端

### 5.1 API

`api/team.ts` 加:

```ts
export function refreshTotalScore(sportsMeetingId: number) {
  return request.post('/admin/team/refreshTotalScore', null, { params: { sportsMeetingId } })
}
```

新建 `api/teamScoreAdjustment.ts`:

```ts
export interface TeamScoreAdjustment {
  id: number
  teamId: number
  deltaAmount: number
  note: string
  createTime: string
}
export function getAdjustmentList(teamId: number) {
  return request.get('/admin/teamScoreAdjustment/list', { params: { teamId } })
}
export function addAdjustment(data: { teamId: number; deltaAmount: number; note: string }) {
  return request.post('/admin/teamScoreAdjustment/add', data)
}
export function deleteAdjustment(id: number) {
  return request.delete(`/admin/teamScoreAdjustment/${id}`)
}
```

### 5.2 MeetingDetailView 改动

**组别管理 tab toolbar**:右侧加按钮

```html
<el-button type="success" size="small" :loading="refreshing" @click="handleRefreshTotalScore">
  刷新所有代表队总分
</el-button>
```

`handleRefreshTotalScore`:调 `refreshTotalScore(meetingId)` → toast `已刷新 N 个代表队总分` → `fetchTeams()` 重拉。

**代表队编辑弹窗**:去掉「总分」`el-form-item` 的 `el-input-number`,改为只读展示:

```html
<el-form-item label="总分">
  <span>{{ teamForm.totalScore ?? 0 }} 分</span>
  <span style="color:#999;font-size:12px;margin-left:8px">
    （点击组别管理页「刷新所有代表队总分」按钮重算）
  </span>
</el-form-item>
```

**新增「总积分榜」tab**(与「组别管理」并列,放到 tabs 合适位置):

```html
<el-tab-pane label="总积分榜" name="ranking">
  <el-table :data="rankingList" stripe border>
    <el-table-column label="排名" width="80">
      <template #default="{ row, $index }">
        <span :class="['rank-badge', `rank-${$index + 1}`]" v-if="$index < 3">{{ $index + 1 }}</span>
        <span v-else>{{ $index + 1 }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="name" label="代表队" />
    <el-table-column label="组别">
      <template #default="{ row }">{{ getGroupTypeName(row.groupTypeId) }}</template>
    </el-table-column>
    <el-table-column prop="totalScore" label="总分" width="120" />
  </el-table>
</el-tab-pane>
```

`rankingList` = `computed(() => [...teams.value].sort((a, b) => (b.totalScore ?? 0) - (a.totalScore ?? 0)))`,复用已加载的 teams 数据。

`getGroupTypeName(groupTypeId)` 是新增的辅助函数,从已加载的 `groupTypes` 列表中按 id 查 name(若未加载返回空串)。

Top3 高亮用 CSS 类 `.rank-1`/`.rank-2`/`.rank-3`(金银铜背景色)。

首次切到 ranking tab 时若 teams 未加载则触发 `fetchTeams()`。

### 5.3 TeamDetailView 改动

**info-bar**:`<span>总分：{{ team.totalScore ?? 0 }}</span>` 旁加灰字「(刷新重算)」。

**编辑弹窗**:同 5.2,去掉「总分」`el-input-number` 改只读。

**新增「调整记录」卡片**(在「队员列表」卡片下方):

```html
<div class="content-card">
  <div class="tab-toolbar">
    <span class="toolbar-hint">加减分调整（如作弊扣分、精神文明加分），重算总分时累加</span>
    <el-button type="primary" size="small" @click="openAdjustmentAdd">+ 添加调整</el-button>
  </div>
  <el-table v-if="adjustments.length > 0" :data="adjustments" stripe border size="small">
    <el-table-column label="数额" width="100">
      <template #default="{ row }">
        <span :style="{color: row.deltaAmount >= 0 ? '#67c23a' : '#f56c6c'}">
          {{ row.deltaAmount >= 0 ? '+' : '' }}{{ row.deltaAmount }}
        </span>
      </template>
    </el-table-column>
    <el-table-column prop="note" label="原因" />
    <el-table-column label="时间" width="170">
      <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
    </el-table-column>
    <el-table-column label="操作" width="80" fixed="right">
      <template #default="{ row }">
        <el-button link type="danger" size="small" @click="handleAdjustmentDelete(row.id)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
  <el-empty v-else description="暂无调整记录" />
</div>
```

**新增「添加调整」弹窗**:

```html
<el-dialog v-model="adjustmentDialogVisible" title="添加调整" width="480px" destroy-on-close>
  <el-form :model="adjustmentForm" label-width="80px">
    <el-form-item label="数额" required>
      <el-input-number v-model="adjustmentForm.deltaAmount" :precision="2" :step="0.5" />
      <span style="margin-left:8px;color:#999">正=加分，负=扣分</span>
    </el-form-item>
    <el-form-item label="原因" required>
      <el-input v-model="adjustmentForm.note" placeholder="如：xxx作弊扣5分" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="adjustmentDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleAdjustmentSubmit">确定</el-button>
  </template>
</el-dialog>
```

提交后调 `addAdjustment({teamId, deltaAmount, note})` → toast → `fetchAdjustments()` 重拉。

## 6. 数据流

所有积分相关数据变更**都不立即同步** `total_score`:

- 录入/删除 result → 用户点「刷新所有代表队总分」→ 重算
- 增加/删除 adjustment → 用户点「刷新所有代表队总分」→ 重算
- participant 改队 → 用户点「刷新所有代表队总分」→ 重算

语义单一、可预测。这是「手动刷新」策略的核心权衡:牺牲实时性换实现简单 + 性能可控。

## 7. 错误处理

沿用项目约定:

- Service 抛 `RuntimeException("原因")`
- `GlobalExceptionHandler` 捕获 → HTTP 400 + `Result.error(400, message)`
- 前端 axios 拦截器从 `error.response.data.message` 取提示
- Controller 校验 `sportsMeetingId` 非空、`teamId` 非空、`note` 非空等边界

## 8. 测试

`TeamMapper.recalculateTotalScoreBySportsMeetingId` 单测覆盖:

- 空队(无成员无 adjustment) → `total_score = 0`
- 单人单赛次 → `total_score = 该 result.points`
- 单人跨多赛次(预赛+决赛) → 全部累加
- 跨队成员(A队成员的 result 不算到 B 队)
- `result.points = NULL` → 按 0 算
- 含正 adjustment(加分) → `total = sum(points) + delta`
- 含负 adjustment(扣分) → `total = sum(points) - |delta|`
- 多条 adjustment → 全部累加
- 同次刷新只影响指定 `sports_meeting_id` 的队(其他运动会的队不动)

`TeamScoreAdjustmentMapper` 基本 CRUD 单测(insert / selectByTeamId / deleteById)。

`TeamScoreAdjustmentService.add` 的 note 空校验单测。

后端 `mvn test` 全绿。

## 9. 数据迁移

- **DDL**:新建 `team_score_adjustment` 表(执行前向用户确认)
- **DML**:无需迁移。现有手动 `total_score` 在用户首次点「刷新」按钮时被覆盖为正确值(用户已确认接受丢弃)。

## 10. 实施顺序建议

1. DDL 建表 + 同步 SQL 脚本(需用户确认)
2. 后端实体/Mapper/Service/Controller + TeamMapper 重算 SQL + 单测
3. 前端 API 文件 + MeetingDetailView(刷新按钮 + 编辑弹窗只读 + 总积分榜 tab)
4. 前端 TeamDetailView(调整记录卡片 + 添加弹窗 + 编辑弹窗只读)
5. 端到端联调验证
