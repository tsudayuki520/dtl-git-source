# 代表队总分批量刷新 + 调整分 + 总积分榜 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `team.total_score` 通过「刷新所有代表队总分」按钮批量重算为 `SUM(result.points) + SUM(team_score_adjustment.delta_amount)`；新建 `team_score_adjustment` 表存加减分；新增「总积分榜」tab 跨组别按总分排序展示。

**Architecture:** 后端在现有 TeamMapper/Service/Controller 上加 `recalculateTotalScoreBySportsMeetingId`（一条 UPDATE 相关子查询）+ `/refreshTotalScore` 端点；新建 `TeamScoreAdjustment` 实体/Mapper/Service/Controller 走标准 CRUD；前端在 MeetingDetailView 加刷新按钮和「总积分榜」tab，在 TeamDetailView 加调整记录卡片。SQL 重算按项目惯例不写 Mapper 集成测试（项目无此基础设施），Service 层的纯校验写薄单测。

**Tech Stack:** Spring Boot + MyBatis（XML mapper，`map-underscore-to-camel-case`）+ MySQL 8；Vue 3 + TypeScript + Element Plus；JUnit 5。

**Spec:** [docs/superpowers/specs/2026-06-29-team-total-score-refresh-design.md](../specs/2026-06-29-team-total-score-refresh-design.md)

---

## 项目惯例速查（实施前必读）

- **包结构**：`Controller/admin/Admin*Controller.java`（路径前缀 `/api/admin/{module}`）/ `Service/` / `Service/Impl/` / `Mapper/` / `entity/` / `common/Result.java`。
- **响应封装**：Controller 返回 `Result.success(data)` / Service 抛 `RuntimeException("原因")` → `GlobalExceptionHandler` 转 HTTP 400。
- **MyBatis**：`map-underscore-to-camel-case: true`，DB 列 `snake_case` ↔ 实体 `camelCase` 自动映射；XML 在 `SportBackend/src/main/resources/mapper/`。
- **测试惯例**：只测**纯逻辑**（参考 `EventScheduleServiceImplTest` 测静态方法 `computeSyncPlan`）。**不测 Mapper SQL**（无 `@MybatisTest` / `@SpringBootTest` 集成测试基础设施）。
- **DDL 同步约定**（CLAUDE.md）：执行 DDL 后必须同步更新 `SportBackend/sql/` 下脚本。
- **执行任何 DDL/DML 前**必须向用户确认。
- **前端 API**：`src/api/*.ts` 用 `request` 实例（baseURL `/api`），所以前端写 `/admin/...`、后端写 `/api/admin/...`。

---

## File Structure

### 新建（10 个文件）

| 文件 | 职责 |
|------|------|
| `SportBackend/sql/team_score_adjustment.sql` | DDL 脚本（权威来源） |
| `SportBackend/src/main/java/com/dlust/sportbackend/entity/TeamScoreAdjustment.java` | 实体 |
| `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/TeamScoreAdjustmentMapper.java` | Mapper 接口 |
| `SportBackend/src/main/resources/mapper/TeamScoreAdjustmentMapper.xml` | SQL |
| `SportBackend/src/main/java/com/dlust/sportbackend/Service/TeamScoreAdjustmentService.java` | Service 接口 |
| `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/TeamScoreAdjustmentServiceImpl.java` | Service 实现（含 note 空校验） |
| `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminTeamScoreAdjustmentController.java` | Controller |
| `SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/TeamScoreAdjustmentServiceImplTest.java` | note 空校验纯逻辑单测 |
| `webManageProject/src/api/teamScoreAdjustment.ts` | 前端 API |
| （无其他新建） | |

### 修改（8 个文件）

| 文件 | 改动 |
|------|------|
| `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/TeamMapper.java` | 加 `recalculateTotalScoreBySportsMeetingId` 方法签名 |
| `SportBackend/src/main/resources/mapper/TeamMapper.xml` | 加 UPDATE 相关子查询 SQL |
| `SportBackend/src/main/java/com/dlust/sportbackend/Service/TeamService.java` | 加 `refreshTotalScoreBySportsMeetingId` |
| `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/TeamServiceImpl.java` | 实现 |
| `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminTeamController.java` | 加 `POST /refreshTotalScore` |
| `webManageProject/src/api/team.ts` | 加 `refreshTotalScore` 函数 |
| `webManageProject/src/views/MeetingDetailView.vue` | 刷新按钮 + 编辑弹窗只读 + 总积分榜 tab |
| `webManageProject/src/views/TeamDetailView.vue` | 调整记录卡片 + 编辑弹窗只读 |

---

## Task 1: 新建 team_score_adjustment 表 + 同步 SQL 脚本

**Files:**
- Create: `SportBackend/sql/team_score_adjustment.sql`

- [ ] **Step 1: 写 SQL 脚本文件**

创建 `SportBackend/sql/team_score_adjustment.sql`：

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

- [ ] **Step 2: 向用户确认后执行 DDL**

Run（需用户确认）:
```bash
mysql -h localhost -uroot -p<password> --default-character-set=utf8mb4 sport_db < SportBackend/sql/team_score_adjustment.sql
```

验证：
```bash
mysql -h localhost -uroot -p<password> sport_db -e "DESCRIBE team_score_adjustment;"
```
Expected: 5 列（id, team_id, delta_amount, note, create_time）。

- [ ] **Step 3: Commit**

```bash
git add SportBackend/sql/team_score_adjustment.sql
git commit -m "feat: 新建 team_score_adjustment 表（代表队加减分记录）"
```

---

## Task 2: 后端 TeamScoreAdjustment 实体 + Mapper

**Files:**
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/entity/TeamScoreAdjustment.java`
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/TeamScoreAdjustmentMapper.java`
- Create: `SportBackend/src/main/resources/mapper/TeamScoreAdjustmentMapper.xml`

- [ ] **Step 1: 创建实体**

`SportBackend/src/main/java/com/dlust/sportbackend/entity/TeamScoreAdjustment.java`：

```java
package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TeamScoreAdjustment {
    private Long id;
    private Long teamId;
    private BigDecimal deltaAmount;
    private String note;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 创建 Mapper 接口**

`SportBackend/src/main/java/com/dlust/sportbackend/Mapper/TeamScoreAdjustmentMapper.java`：

```java
package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.TeamScoreAdjustment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeamScoreAdjustmentMapper {

    List<TeamScoreAdjustment> selectByTeamId(@Param("teamId") Long teamId);

    void insert(TeamScoreAdjustment adjustment);

    void deleteById(@Param("id") Long id);
}
```

- [ ] **Step 3: 创建 Mapper XML**

`SportBackend/src/main/resources/mapper/TeamScoreAdjustmentMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.dlust.sportbackend.Mapper.TeamScoreAdjustmentMapper">

    <select id="selectByTeamId" resultType="TeamScoreAdjustment">
        SELECT * FROM team_score_adjustment WHERE team_id = #{teamId}
        ORDER BY create_time DESC
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO team_score_adjustment (team_id, delta_amount, note)
        VALUES (#{teamId}, #{deltaAmount}, #{note})
    </insert>

    <delete id="deleteById">
        DELETE FROM team_score_adjustment WHERE id = #{id}
    </delete>

</mapper>
```

- [ ] **Step 4: 编译验证**

Run: `cd SportBackend && mvn -q compile`
Expected: 无报错退出。

- [ ] **Step 5: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/entity/TeamScoreAdjustment.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Mapper/TeamScoreAdjustmentMapper.java \
        SportBackend/src/main/resources/mapper/TeamScoreAdjustmentMapper.xml
git commit -m "feat: TeamScoreAdjustment 实体+Mapper（CRUD）"
```

---

## Task 3: 后端 TeamScoreAdjustmentService + Impl + 单测

**Files:**
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/Service/TeamScoreAdjustmentService.java`
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/TeamScoreAdjustmentServiceImpl.java`
- Create: `SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/TeamScoreAdjustmentServiceImplTest.java`

- [ ] **Step 1: 写 Service 接口**

`SportBackend/src/main/java/com/dlust/sportbackend/Service/TeamScoreAdjustmentService.java`：

```java
package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.TeamScoreAdjustment;
import java.math.BigDecimal;
import java.util.List;

public interface TeamScoreAdjustmentService {

    List<TeamScoreAdjustment> getByTeamId(Long teamId);

    void add(Long teamId, BigDecimal deltaAmount, String note);

    void delete(Long id);
}
```

- [ ] **Step 2: 写 Service 实现（含 note 空校验）**

`SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/TeamScoreAdjustmentServiceImpl.java`：

```java
package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.TeamScoreAdjustmentMapper;
import com.dlust.sportbackend.Service.TeamScoreAdjustmentService;
import com.dlust.sportbackend.entity.TeamScoreAdjustment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TeamScoreAdjustmentServiceImpl implements TeamScoreAdjustmentService {

    @Autowired
    private TeamScoreAdjustmentMapper teamScoreAdjustmentMapper;

    @Override
    public List<TeamScoreAdjustment> getByTeamId(Long teamId) {
        return teamScoreAdjustmentMapper.selectByTeamId(teamId);
    }

    @Override
    public void add(Long teamId, BigDecimal deltaAmount, String note) {
        validateAdd(teamId, deltaAmount, note);
        TeamScoreAdjustment adj = new TeamScoreAdjustment();
        adj.setTeamId(teamId);
        adj.setDeltaAmount(deltaAmount);
        adj.setNote(note);
        teamScoreAdjustmentMapper.insert(adj);
    }

    @Override
    public void delete(Long id) {
        teamScoreAdjustmentMapper.deleteById(id);
    }

    /**
     * 抽成包级静态方法以便单测（不依赖 Spring/Mapper）。
     * 校验：teamId/deltaAmount 非 null；note 非空。
     */
    static void validateAdd(Long teamId, BigDecimal deltaAmount, String note) {
        if (teamId == null) {
            throw new RuntimeException("代表队 ID 不能为空");
        }
        if (deltaAmount == null) {
            throw new RuntimeException("调整数额不能为空");
        }
        if (note == null || note.trim().isEmpty()) {
            throw new RuntimeException("调整原因不能为空");
        }
    }
}
```

- [ ] **Step 3: 写纯逻辑单测（先验证失败）**

`SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/TeamScoreAdjustmentServiceImplTest.java`：

```java
package com.dlust.sportbackend.Service.Impl;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamScoreAdjustmentServiceImplTest {

    @Test
    void validateAdd_nullTeamId_throws() {
        assertThrows(RuntimeException.class, () ->
            TeamScoreAdjustmentServiceImpl.validateAdd(null, new BigDecimal("5"), "作弊扣分"));
    }

    @Test
    void validateAdd_nullDelta_throws() {
        assertThrows(RuntimeException.class, () ->
            TeamScoreAdjustmentServiceImpl.validateAdd(1L, null, "作弊扣分"));
    }

    @Test
    void validateAdd_blankNote_throws() {
        assertThrows(RuntimeException.class, () ->
            TeamScoreAdjustmentServiceImpl.validateAdd(1L, new BigDecimal("5"), "  "));
    }

    @Test
    void validateAdd_negativeDelta_ok() {
        // 负数 delta（扣分）合法
        assertDoesNotThrow(() ->
            TeamScoreAdjustmentServiceImpl.validateAdd(1L, new BigDecimal("-5"), "作弊扣5分"));
    }

    @Test
    void validateAdd_validInput_ok() {
        assertDoesNotThrow(() ->
            TeamScoreAdjustmentServiceImpl.validateAdd(1L, new BigDecimal("3"), "精神文明+3"));
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd SportBackend && mvn -q test -Dtest=TeamScoreAdjustmentServiceImplTest`
Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Service/TeamScoreAdjustmentService.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/TeamScoreAdjustmentServiceImpl.java \
        SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/TeamScoreAdjustmentServiceImplTest.java
git commit -m "feat: TeamScoreAdjustmentService（CRUD+note 校验纯逻辑单测）"
```

---

## Task 4: 后端 AdminTeamScoreAdjustmentController

**Files:**
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminTeamScoreAdjustmentController.java`

- [ ] **Step 1: 写 Controller**

`SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminTeamScoreAdjustmentController.java`：

```java
package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.TeamScoreAdjustmentService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.TeamScoreAdjustment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/teamScoreAdjustment")
public class AdminTeamScoreAdjustmentController {

    @Autowired
    private TeamScoreAdjustmentService teamScoreAdjustmentService;

    @GetMapping("/list")
    public Result<List<TeamScoreAdjustment>> list(@RequestParam Long teamId) {
        return Result.success(teamScoreAdjustmentService.getByTeamId(teamId));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, Object> body) {
        Long teamId = body.get("teamId") == null ? null : Long.valueOf(body.get("teamId").toString());
        BigDecimal deltaAmount = body.get("deltaAmount") == null ? null
                : new BigDecimal(body.get("deltaAmount").toString());
        String note = body.get("note") == null ? null : body.get("note").toString();
        log.info("添加代表队调整分: teamId={}, delta={}, note={}", teamId, deltaAmount, note);
        teamScoreAdjustmentService.add(teamId, deltaAmount, note);
        return Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除代表队调整分: id={}", id);
        teamScoreAdjustmentService.delete(id);
        return Result.success("删除成功");
    }
}
```

> 说明：用 `Map<String, Object>` body 接 deltaAmount，避免 BigDecimal 反序列化在某些 Jackson 配置下的歧义；与 spec 第 4.4 节字段一致。

- [ ] **Step 2: 编译验证**

Run: `cd SportBackend && mvn -q compile`
Expected: 无报错。

- [ ] **Step 3: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminTeamScoreAdjustmentController.java
git commit -m "feat: AdminTeamScoreAdjustmentController（list/add/delete）"
```

---

## Task 5: 后端 TeamMapper 重算 SQL + Service + Controller endpoint

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/TeamMapper.java`
- Modify: `SportBackend/src/main/resources/mapper/TeamMapper.xml`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/TeamService.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/TeamServiceImpl.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminTeamController.java`

- [ ] **Step 1: TeamMapper.java 加方法签名**

在 `TeamMapper.java` 的 `void deleteById(@Param("id") Long id);` 上一行（接口末尾）加：

```java
    int recalculateTotalScoreBySportsMeetingId(@Param("sportsMeetingId") Long sportsMeetingId);
```

并在文件顶部 import 区加（如未有）：

```java
import org.apache.ibatis.annotations.Param;
```

（实际 `@Param` 已在用，无需重复 import）

- [ ] **Step 2: TeamMapper.xml 加 UPDATE SQL**

在 `TeamMapper.xml` 的 `</mapper>` 之前加：

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

- [ ] **Step 3: TeamService.java 加方法签名**

在 `TeamService.java` 接口末尾 `}` 之前加：

```java
    int refreshTotalScoreBySportsMeetingId(Long sportsMeetingId);
```

- [ ] **Step 4: TeamServiceImpl.java 加实现**

在 `TeamServiceImpl.java` 的 `delete` 方法后、`}` 之前加：

```java
    @Override
    public int refreshTotalScoreBySportsMeetingId(Long sportsMeetingId) {
        if (sportsMeetingId == null) {
            throw new RuntimeException("运动会 ID 不能为空");
        }
        return teamMapper.recalculateTotalScoreBySportsMeetingId(sportsMeetingId);
    }
```

- [ ] **Step 5: AdminTeamController.java 加 endpoint**

在 `AdminTeamController.java` 的 `delete` 方法后、`}` 之前加：

```java
    @PostMapping("/refreshTotalScore")
    public Result<java.util.Map<String, Object>> refreshTotalScore(@RequestParam Long sportsMeetingId) {
        log.info("刷新代表队总分: sportsMeetingId={}", sportsMeetingId);
        int count = teamService.refreshTotalScoreBySportsMeetingId(sportsMeetingId);
        return Result.success(java.util.Map.of("refreshedCount", count));
    }
```

- [ ] **Step 6: 编译 + 全测试**

Run: `cd SportBackend && mvn -q compile && mvn -q test`
Expected: 全绿（包含 Task 3 的 5 个新测试 + 现有 `EventScheduleServiceImplTest` 6 个）。

- [ ] **Step 7: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Mapper/TeamMapper.java \
        SportBackend/src/main/resources/mapper/TeamMapper.xml \
        SportBackend/src/main/java/com/dlust/sportbackend/Service/TeamService.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/TeamServiceImpl.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminTeamController.java
git commit -m "feat: TeamMapper.recalculateTotalScoreBySportsMeetingId + /refreshTotalScore 端点"
```

---

## Task 6: 前端 API 文件

**Files:**
- Modify: `webManageProject/src/api/team.ts`
- Create: `webManageProject/src/api/teamScoreAdjustment.ts`

- [ ] **Step 1: team.ts 加 refreshTotalScore**

在 `webManageProject/src/api/team.ts` 末尾（`deleteTeam` 函数后）加：

```ts
export function refreshTotalScore(sportsMeetingId: number) {
  return request.post('/admin/team/refreshTotalScore', null, { params: { sportsMeetingId } })
}
```

- [ ] **Step 2: 新建 teamScoreAdjustment.ts**

创建 `webManageProject/src/api/teamScoreAdjustment.ts`：

```ts
import request from '@/utils/request'

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

- [ ] **Step 3: 构建验证（类型检查）**

Run: `cd webManageProject && npm run build`
Expected: `✓ built in ...`（无 TS 错误）。

- [ ] **Step 4: Commit**

```bash
git add webManageProject/src/api/team.ts webManageProject/src/api/teamScoreAdjustment.ts
git commit -m "feat: 前端 API——refreshTotalScore + teamScoreAdjustment CRUD"
```

---

## Task 7: 前端 MeetingDetailView — 刷新按钮 + 编辑弹窗只读

**Files:**
- Modify: `webManageProject/src/views/MeetingDetailView.vue`

> 当前组别管理 tab toolbar 在 line 674-677（`<div class="tab-toolbar">` ... `+ 新增组别` button）。代表队编辑弹窗在 line 893-912（`<el-dialog v-model="teamDialogVisible" ...>`）。

- [ ] **Step 1: 加 import + refreshing 状态 + 处理函数**

在 MeetingDetailView.vue 的 `import { getTeamList, addTeam, updateTeam } from '@/api/team'`（约 line 19）改成：

```ts
import { getTeamList, addTeam, updateTeam, refreshTotalScore } from '@/api/team'
```

在 `teams` ref 定义附近（约 line 129 `const teams = ref<Team[]>([])`）加：

```ts
const refreshing = ref(false)
async function handleRefreshTotalScore() {
  refreshing.value = true
  try {
    const res: any = await refreshTotalScore(meetingId)
    const count = res?.data?.refreshedCount ?? res?.refreshedCount ?? 0
    ElMessage.success(`已刷新 ${count} 个代表队总分`)
    fetchTeams()
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}
```

- [ ] **Step 2: toolbar 加刷新按钮**

把 line 674-677 的 tab-toolbar：

```html
        <div class="tab-toolbar">
          <span class="toolbar-hint">展开组别查看并管理其下的代表队</span>
          <el-button type="primary" size="small" @click="openGtAdd">+ 新增组别</el-button>
        </div>
```

改为：

```html
        <div class="tab-toolbar">
          <span class="toolbar-hint">展开组别查看并管理其下的代表队</span>
          <div style="display:flex;gap:8px">
            <el-button type="success" size="small" :loading="refreshing" @click="handleRefreshTotalScore">刷新所有代表队总分</el-button>
            <el-button type="primary" size="small" @click="openGtAdd">+ 新增组别</el-button>
          </div>
        </div>
```

- [ ] **Step 3: 代表队编辑弹窗改只读**

> 注：MeetingDetailView **当前没有 `openTeamEdit` 入口**，此弹窗仅 `openTeamAdd` 触发（即只用于新增代表队，title `:title="teamForm.id ? '编辑' : '新增'"` 永远显示「新增」）。但仍改此弹窗，原因：(a) 与 TeamDetailView 编辑弹窗保持一致；(b) 新增代表队时不再允许手填初始总分（合理：新队无成绩无调整，初始应为 0，靠后续刷新累加）。

把 line 904-906 的「总分」表单项：

```html
        <el-form-item label="总分">
          <el-input-number v-model="teamForm.totalScore" :precision="2" :min="0" />
        </el-form-item>
```

改为：

```html
        <el-form-item label="总分">
          <span>{{ teamForm.totalScore ?? 0 }} 分</span>
          <span style="color:#999;font-size:12px;margin-left:8px">（点击组别管理页「刷新所有代表队总分」按钮重算）</span>
        </el-form-item>
```

- [ ] **Step 4: 构建验证**

Run: `cd webManageProject && npm run build`
Expected: `✓ built in ...`。

- [ ] **Step 5: Commit**

```bash
git add webManageProject/src/views/MeetingDetailView.vue
git commit -m "feat: 组别管理 tab 加「刷新所有代表队总分」按钮+编辑弹窗总分只读"
```

---

## Task 8: 前端 MeetingDetailView — 总积分榜 tab

**Files:**
- Modify: `webManageProject/src/views/MeetingDetailView.vue`

> 当前 tabs 在 line 641（`<el-tabs v-model="activeTab" @tab-change="onTabChange">`）。`onTabChange` 在 line 599-607。组别管理 tab pane 在 line 673-714。

- [ ] **Step 1: 加 rankingList computed + getGroupTypeName 辅助**

在 `teams` ref 附近（紧跟 Task 7 加的 `handleRefreshTotalScore` 之后）加：

```ts
const rankingList = computed(() =>
  [...teams.value].sort((a, b) => (b.totalScore ?? 0) - (a.totalScore ?? 0))
)
function getGroupTypeName(groupTypeId: number | null): string {
  if (groupTypeId == null) return ''
  const gt = groupTypes.value.find(g => g.id === groupTypeId)
  return gt?.name || ''
}
```

> 注意：MeetingDetailView.vue line 2 已是 `import { ref, computed, onMounted, watch } from 'vue'`，`computed` 无需补 import。`groupTypes` ref 在文件中已存在（fetchGroupTypes 加载）。

- [ ] **Step 2: onTabChange 加 ranking 加载**

把 line 599-607 的 `onTabChange` 函数中（紧跟 `else if (tab === 'groupType') ...`）加一行：

```ts
  else if (tab === 'ranking') { fetchTeams(); fetchGroupTypes() }
```

完整修改后的 `onTabChange`：

```ts
function onTabChange(tab: string) {
  if (loadedTabs.has(tab)) return
  loadedTabs.add(tab)
  if (tab === 'schedule') { fetchSchedules(); fetchEvents(); fetchEventSchedules() }
  else if (tab === 'groupType') { fetchGroupTypes(); fetchTeams(); fetchParticipants() }
  else if (tab === 'ranking') { fetchTeams(); fetchGroupTypes() }
  else if (tab === 'participant') fetchParticipants()
  else if (tab === 'notice') fetchNotices()
  else if (tab === 'result') { fetchResults(); fetchSchedules(); fetchEvents(); fetchEventSchedules(); fetchParticipants() }
}
```

- [ ] **Step 3: 加 ranking tab pane 模板**

在组别管理 tab pane 的 `</el-tab-pane>`（line 714）之后、参赛人员 tab pane 之前加：

```html
      <!-- ======== 总积分榜 ======== -->
      <el-tab-pane label="总积分榜" name="ranking">
        <el-table :data="rankingList" stripe border size="small">
          <el-table-column label="排名" width="80" align="center">
            <template #default="{ $index }">
              <span v-if="$index < 3" :class="['rank-badge', `rank-${$index + 1}`]">{{ $index + 1 }}</span>
              <span v-else>{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="代表队" />
          <el-table-column label="组别" width="140">
            <template #default="{ row }">{{ getGroupTypeName(row.groupTypeId) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="totalScore" label="总分" width="120" align="right" />
        </el-table>
        <el-empty v-if="rankingList.length === 0" description="暂无代表队" />
      </el-tab-pane>
```

- [ ] **Step 4: 加 Top3 高亮 CSS**

在 MeetingDetailView.vue 的 `<style scoped>` 区（找一处合适位置，建议放在 `.team-card-info` 之后）加：

```css
/* 总积分榜 Top3 徽章 */
.rank-badge {
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
  border-radius: 50%;
  color: #fff;
  font-weight: 600;
  font-size: 13px;
}
.rank-1 { background: #f5c518; }  /* 金 */
.rank-2 { background: #b8b8b8; }  /* 银 */
.rank-3 { background: #cd7f32; }  /* 铜 */
```

- [ ] **Step 5: 构建验证**

Run: `cd webManageProject && npm run build`
Expected: `✓ built in ...`。

- [ ] **Step 6: Commit**

```bash
git add webManageProject/src/views/MeetingDetailView.vue
git commit -m "feat: 新增「总积分榜」tab 跨组别按总分排序，Top3 金银铜徽章"
```

---

## Task 9: 前端 TeamDetailView — 调整记录卡片 + 编辑弹窗只读

**Files:**
- Modify: `webManageProject/src/views/TeamDetailView.vue`

> 当前 TeamDetailView 编辑弹窗 line 180-199 含「总分」`el-input-number`（line 191-193）。info-bar line 149-154 含总分 span（line 152）。队员列表卡片 line 158-177。

- [ ] **Step 1: 加 import + adjustments 状态 + CRUD 函数**

把 line 7-8 的 import：

```ts
import { getTeamList, updateTeam, deleteTeam } from '@/api/team'
import type { Team } from '@/api/team'
```

保持不变。在 import 区末尾加：

```ts
import { getAdjustmentList, addAdjustment, deleteAdjustment } from '@/api/teamScoreAdjustment'
import type { TeamScoreAdjustment } from '@/api/teamScoreAdjustment'
```

在 `participants` ref 定义后（约 line 21）加：

```ts
const adjustments = ref<TeamScoreAdjustment[]>([])
const adjustmentDialogVisible = ref(false)
const adjustmentForm = ref({ deltaAmount: 0, note: '' })

async function fetchAdjustments() {
  try {
    const res: any = await getAdjustmentList(teamId)
    adjustments.value = res.data || res || []
  } catch { /* ignore */ }
}

function openAdjustmentAdd() {
  adjustmentForm.value = { deltaAmount: 0, note: '' }
  adjustmentDialogVisible.value = true
}

async function handleAdjustmentSubmit() {
  if (!adjustmentForm.value.note.trim()) {
    ElMessage.warning('请填写调整原因')
    return
  }
  try {
    await addAdjustment({
      teamId,
      deltaAmount: adjustmentForm.value.deltaAmount,
      note: adjustmentForm.value.note.trim(),
    })
    ElMessage.success('添加成功')
    adjustmentDialogVisible.value = false
    fetchAdjustments()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '添加失败')
  }
}

async function handleAdjustmentDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该调整记录？', '提示', { type: 'warning' })
    await deleteAdjustment(id)
    ElMessage.success('删除成功')
    fetchAdjustments()
  } catch { /* cancel */ }
}
```

- [ ] **Step 2: onMounted 加 fetchAdjustments**

把 line 127-131 的 `onMounted`：

```ts
onMounted(() => {
  fetchMeeting()
  fetchTeam()
  fetchParticipants()
})
```

改为：

```ts
onMounted(() => {
  fetchMeeting()
  fetchTeam()
  fetchParticipants()
  fetchAdjustments()
})
```

- [ ] **Step 3: info-bar 总分加提示**

把 line 152：

```html
        <span>总分：{{ team.totalScore ?? 0 }}</span>
```

改为：

```html
        <span>总分：{{ team.totalScore ?? 0 }} <span style="color:#999;font-size:12px">（刷新重算）</span></span>
```

- [ ] **Step 4: 编辑弹窗改只读**

把 line 191-193 的「总分」表单项：

```html
        <el-form-item label="总分">
          <el-input-number v-model="teamEditForm.totalScore" :precision="2" :min="0" />
        </el-form-item>
```

改为：

```html
        <el-form-item label="总分">
          <span>{{ teamEditForm.totalScore ?? 0 }} 分</span>
          <span style="color:#999;font-size:12px;margin-left:8px">（点击组别管理页「刷新所有代表队总分」按钮重算）</span>
        </el-form-item>
```

- [ ] **Step 5: 加调整记录卡片模板**

在队员列表卡片的 `</div>`（line 177）之后、编辑弹窗之前（line 180 `<el-dialog v-model="teamEditVisible"` 之前）加：

```html
    <!-- 调整记录 -->
    <div class="content-card" style="margin-top:20px">
      <div class="tab-toolbar">
        <span class="toolbar-hint">加减分调整（如作弊扣分、精神文明加分），重算总分时累加</span>
        <el-button type="primary" size="small" @click="openAdjustmentAdd">+ 添加调整</el-button>
      </div>
      <el-table v-if="adjustments.length > 0" :data="adjustments" stripe border size="small">
        <el-table-column label="数额" width="100">
          <template #default="{ row }">
            <span :style="{color: row.deltaAmount >= 0 ? '#67c23a' : '#f56c6c', fontWeight: 600}">
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
      <el-empty v-else description="暂无调整记录" :image-size="60" />
    </div>
```

> 注意：TeamDetailView 当前**没有** `formatDate` 函数（已 grep 确认），必须在 script 区添加（放在 `goBack` 函数附近）：

```ts
function formatDate(d: string) {
  if (!d) return ''
  return d.substring(0, 16).replace('T', ' ')
}
```

- [ ] **Step 6: 加「添加调整」弹窗模板**

在编辑弹窗（line 180-199 `<el-dialog v-model="teamEditVisible">... </el-dialog>`）之后加：

```html
    <!-- 弹窗：添加调整 -->
    <el-dialog v-model="adjustmentDialogVisible" title="添加调整" width="480px" destroy-on-close>
      <el-form :model="adjustmentForm" label-width="80px">
        <el-form-item label="数额" required>
          <el-input-number v-model="adjustmentForm.deltaAmount" :precision="2" :step="0.5" />
          <span style="margin-left:8px;color:#999;font-size:12px">正=加分，负=扣分</span>
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

- [ ] **Step 7: 构建验证**

Run: `cd webManageProject && npm run build`
Expected: `✓ built in ...`。

- [ ] **Step 8: Commit**

```bash
git add webManageProject/src/views/TeamDetailView.vue
git commit -m "feat: TeamDetailView 加调整记录卡片+添加弹窗+编辑弹窗总分只读"
```

---

## Task 10: 端到端手测 + 最终验证

**Files:** 无（验证任务）

- [ ] **Step 1: 后端启动**

```bash
cd SportBackend && mvn spring-boot:run
```
确认无报错启动在 8080 端口。

- [ ] **Step 2: 前端启动**

另开终端：
```bash
cd webManageProject && npm run dev
```

- [ ] **Step 3: 手测清单**

打开管理端 `http://localhost:5173`，进入某运动会详情页：

1. **组别管理 tab** → 顶部右侧应看到绿色「刷新所有代表队总分」按钮（与「+ 新增组别」并排）。
2. 点击「刷新所有代表队总分」→ 成功 toast「已刷新 N 个代表队总分」→ 卡片上的「XX 分」tag 数值更新。
3. 点击某代表队卡片进 TeamDetailView：
   - info-bar 显示「总分：XX（刷新重算）」。
   - 调整记录卡片存在，初始可能为空。
   - 点「+ 添加调整」→ 弹窗输入「数额=-5，原因=作弊扣5分」→ 确定 → 列表出现红色「-5」。
   - 再加一条「数额=3，原因=精神文明+3」→ 列表出现绿色「+3」。
   - 点删除 → 二次确认 → 记录消失。
   - 点「编辑」按钮 → 弹窗中「总分」是只读文字「XX 分（点击组别管理页...）」。
4. 回组别管理 tab → 再点「刷新所有代表队总分」→ 总分应反映上述调整（旧分 + 调整）。
5. 切到「总积分榜」tab：
   - 表格列出所有代表队，按总分降序。
   - Top3 行的排名数字是金银铜圆形徽章。
   - 组别列显示组别名（不是 ID）。
6. **MeetingDetailView 的代表队编辑弹窗**：组别管理 tab 下展开某组别，点代表队卡片**右键不点**（避免跳转），找入口编辑——若 groupType tab 内有「编辑代表队」入口则验证；若无入口（卡片整体跳转），跳过此步只验 TeamDetailView 的编辑弹窗。
7. **积分累加验证**：在 EventDetailView 录一条新成绩带 `points=10` → 回组别管理点刷新 → 该队员所在代表队总分应 +10。

- [ ] **Step 4: 全测试 + 构建**

```bash
cd SportBackend && mvn -q test
cd ../webManageProject && npm run build
```
Expected: 后端全绿，前端 `✓ built in ...`。

- [ ] **Step 5: 最终 commit（如有零散改动）**

```bash
git status
# 若有未提交改动：
git add -A && git commit -m "chore: 端到端联调微调"
```

---

## 验证总览

- **后端编译 + 测试**：`mvn -q compile && mvn -q test` 全绿（新增 5 个 `TeamScoreAdjustmentServiceImplTest` 测试 + 现有 6 个 EventSchedule 测试）。
- **前端构建**：`npm run build` 无 TS 错误。
- **数据库**：`team_score_adjustment` 表已建，`DESCRIBE` 验证 5 列。
- **功能手测**：Task 10 步骤 3 的 7 项全部通过。

## 注意事项

- DDL（Task 1 Step 2）**必须先向用户确认**再执行（CLAUDE.md 约定）。
- 现有手动 `total_score` 数据：用户首次点「刷新」按钮即被覆盖（用户已确认接受）。
- SQL 重算**不写 Mapper 集成测试**（项目无 `@MybatisTest`/`@SpringBootTest` 真断言测试基础设施，参考 `EventScheduleServiceImplTest` 只测纯逻辑）。
- Mapper XML 中 `team_score_adjustment` 表名要与 SQL 脚本完全一致（含下划线）。
