# 成绩按赛次独立存储 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `result` 表经 `event_schedule` 关联赛次,使预赛/决赛成绩独立,并在项目报名页(EventDetailView)展示/录入当前赛次成绩。

**Architecture:** `result` 表加 `event_schedule_id`(关联 `event_schedule.id`)。后端接口对前端透明——用 `eventId + scheduleId`,Service 内部查出 `event_schedule_id` 再持久化。三个查询 SQL 都 JOIN `event_schedule` + `schedule` 带出 `scheduleId/scheduleName`。前端 EventDetailView 用新接口按赛次查/录;MeetingDetailView 成绩 tab 录入加赛次选择、分组改用成绩自带的 `scheduleId`。

**Tech Stack:** Spring Boot + MyBatis(XML)、Vue 3 + TypeScript + Element Plus、MySQL。

**测试说明:** 本项目无单测/前端测试框架(后端仅有空 context-load 测试,前端 package.json 无 test 脚本)。遵循项目现有模式,验证以「后端 `mvn compile` + 前端 `npm run build`(含 vue-tsc 类型检查)+ 手动验证」为主,**非 TDD**。每个任务给出明确的验证命令与预期结果。

**参考 spec:** `docs/superpowers/specs/2026-06-15-result-by-schedule-design.md`

---

## 文件结构

**数据库脚本:**
- 修改:`SportBackend/sql/result.sql`

**后端:**
- 修改:`SportBackend/src/main/java/com/dlust/sportbackend/entity/Result.java`
- 修改:`SportBackend/src/main/java/com/dlust/sportbackend/entity/ResultVO.java`
- 修改:`SportBackend/src/main/java/com/dlust/sportbackend/Mapper/EventScheduleMapper.java`
- 修改:`SportBackend/src/main/resources/mapper/EventScheduleMapper.xml`
- 修改:`SportBackend/src/main/java/com/dlust/sportbackend/Mapper/ResultMapper.java`
- 修改:`SportBackend/src/main/resources/mapper/ResultMapper.xml`
- 修改:`SportBackend/src/main/java/com/dlust/sportbackend/Service/ResultService.java`
- 修改:`SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/ResultServiceImpl.java`
- 修改:`SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminResultController.java`

**前端:**
- 修改:`webManageProject/src/api/result.ts`
- 修改:`webManageProject/src/views/EventDetailView.vue`
- 修改:`webManageProject/src/views/MeetingDetailView.vue`

---

## Task 1: 数据库 — result 表加 event_schedule_id

**Files:**
- Modify: `SportBackend/sql/result.sql`

- [ ] **Step 1: 更新建表脚本,加 event_schedule_id 字段**

把 `SportBackend/sql/result.sql` 整体替换为:

```sql
-- 成绩表
CREATE TABLE IF NOT EXISTS result (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成绩ID',
    sports_meeting_id BIGINT       NOT NULL COMMENT '所属运动会ID',
    event_id          BIGINT       NOT NULL COMMENT '比赛项目ID（关联event表）',
    event_schedule_id BIGINT       DEFAULT NULL COMMENT '项目-赛程关联ID（关联event_schedule.id）',
    participant_id    BIGINT       NOT NULL COMMENT '参赛人员ID（关联participant表）',
    score             DECIMAL(10,2) DEFAULT NULL COMMENT '成绩（小数类型，如秒数、米数）',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';
```

- [ ] **Step 2: 向用户提供 ALTER 语句,由用户在 MySQL 中执行**

提供以下语句给用户(db_policy:数据库操作由用户确认并执行):

```sql
ALTER TABLE result
  ADD COLUMN event_schedule_id BIGINT DEFAULT NULL
  COMMENT '项目-赛程关联ID（关联event_schedule.id）'
  AFTER event_id;
```

- [ ] **Step 3: 用户执行后验证字段存在**

用户在 MySQL 执行:`DESC result;`
预期:输出包含 `event_schedule_id` 行,类型 `bigint`,`Default NULL`。

- [ ] **Step 4: Commit**

```bash
git add SportBackend/sql/result.sql
git commit -m "feat: result表增加event_schedule_id字段关联赛次"
```

---

## Task 2: 后端 entity — Result 与 ResultVO 加字段

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/Result.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/ResultVO.java`

- [ ] **Step 1: Result.java 加 eventScheduleId 与 scheduleId**

整体替换 `Result.java` 为:

```java
package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Result {
    private Long id;
    private Long sportsMeetingId;
    private Long eventId;
    private Long eventScheduleId;
    // 非数据库字段：仅用于接收前端赛次参数，Service 内转换为 eventScheduleId
    private Long scheduleId;
    private Long participantId;
    private BigDecimal score;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: ResultVO.java 加 eventScheduleId、scheduleId、scheduleName**

整体替换 `ResultVO.java` 为:

```java
package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ResultVO {
    private Long id;
    private Long sportsMeetingId;
    private Long eventId;
    private Long eventScheduleId;
    private Long scheduleId;
    private Long participantId;
    private BigDecimal score;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 关联查询字段
    private String participantName;
    private String eventName;
    private String scheduleName;
}
```

- [ ] **Step 3: 验证编译**

Run: `cd SportBackend && mvn compile -q`
Expected: BUILD SUCCESS,无报错。

- [ ] **Step 4: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/entity/Result.java SportBackend/src/main/java/com/dlust/sportbackend/entity/ResultVO.java
git commit -m "feat: Result/ResultVO增加赛次关联字段"
```

---

## Task 3: 后端 EventScheduleMapper — 加按 (eventId, scheduleId) 查询

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/EventScheduleMapper.java`
- Modify: `SportBackend/src/main/resources/mapper/EventScheduleMapper.xml`

- [ ] **Step 1: EventScheduleMapper.java 加方法**

在 `selectBySportsMeetingId` 方法下方新增(约第 20 行后):

```java
    EventSchedule selectByEventIdAndScheduleId(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId);
```

- [ ] **Step 2: EventScheduleMapper.xml 加对应 select**

在 `selectBySportsMeetingId` 的 `<select>` 之后新增:

```xml
    <select id="selectByEventIdAndScheduleId" resultType="EventSchedule">
        SELECT * FROM event_schedule
        WHERE event_id = #{eventId} AND schedule_id = #{scheduleId} AND is_deleted = 0
    </select>
```

- [ ] **Step 3: 验证编译**

Run: `cd SportBackend && mvn compile -q`
Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Mapper/EventScheduleMapper.java SportBackend/src/main/resources/mapper/EventScheduleMapper.xml
git commit -m "feat: EventScheduleMapper增加按项目和赛次查询"
```

---

## Task 4: 后端 ResultMapper — 查询 JOIN 赛次 + insert/update 加字段

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/ResultMapper.java`
- Modify: `SportBackend/src/main/resources/mapper/ResultMapper.xml`

- [ ] **Step 1: ResultMapper.java 加 selectVOByEventAndSchedule**

在 `selectVOByEventId` 方法下方新增(约第 15 行后):

```java
    List<ResultVO> selectVOByEventAndSchedule(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId);
```

- [ ] **Step 2: ResultMapper.xml 整体替换**

把 `ResultMapper.xml` 整体替换为(三个 select 都 JOIN event_schedule + schedule 带 scheduleId/scheduleName,insert/update 加 event_schedule_id):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.dlust.sportbackend.Mapper.ResultMapper">

    <select id="selectVOBySportsMeetingId" resultType="ResultVO">
        SELECT r.id, r.sports_meeting_id, r.event_id, r.event_schedule_id,
               r.participant_id, r.score, r.create_time, r.update_time,
               es.schedule_id AS schedule_id,
               p.name AS participant_name,
               e.name AS event_name,
               s.name AS schedule_name
        FROM result r
        LEFT JOIN event_schedule es ON r.event_schedule_id = es.id
        LEFT JOIN schedule s ON es.schedule_id = s.id
        LEFT JOIN participant p ON r.participant_id = p.id
        LEFT JOIN event e ON r.event_id = e.id
        WHERE r.sports_meeting_id = #{sportsMeetingId}
        ORDER BY r.create_time DESC
    </select>

    <select id="selectVOByEventId" resultType="ResultVO">
        SELECT r.id, r.sports_meeting_id, r.event_id, r.event_schedule_id,
               r.participant_id, r.score, r.create_time, r.update_time,
               es.schedule_id AS schedule_id,
               p.name AS participant_name,
               e.name AS event_name,
               s.name AS schedule_name
        FROM result r
        LEFT JOIN event_schedule es ON r.event_schedule_id = es.id
        LEFT JOIN schedule s ON es.schedule_id = s.id
        LEFT JOIN participant p ON r.participant_id = p.id
        LEFT JOIN event e ON r.event_id = e.id
        WHERE r.event_id = #{eventId}
        ORDER BY r.score ASC
    </select>

    <select id="selectVOByEventAndSchedule" resultType="ResultVO">
        SELECT r.id, r.sports_meeting_id, r.event_id, r.event_schedule_id,
               r.participant_id, r.score, r.create_time, r.update_time,
               es.schedule_id AS schedule_id,
               p.name AS participant_name,
               e.name AS event_name,
               s.name AS schedule_name
        FROM result r
        INNER JOIN event_schedule es ON r.event_schedule_id = es.id
        LEFT JOIN schedule s ON es.schedule_id = s.id
        LEFT JOIN participant p ON r.participant_id = p.id
        LEFT JOIN event e ON r.event_id = e.id
        WHERE r.event_id = #{eventId} AND es.schedule_id = #{scheduleId} AND es.is_deleted = 0
        ORDER BY r.score ASC
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO result (sports_meeting_id, event_id, event_schedule_id, participant_id, score)
        VALUES (#{sportsMeetingId}, #{eventId}, #{eventScheduleId}, #{participantId}, #{score})
    </insert>

    <update id="updateById">
        UPDATE result
        <set>
            <if test="eventScheduleId != null">event_schedule_id = #{eventScheduleId},</if>
            <if test="eventId != null">event_id = #{eventId},</if>
            <if test="participantId != null">participant_id = #{participantId},</if>
            <if test="score != null">score = #{score},</if>
        </set>
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM result WHERE id = #{id}
    </delete>

</mapper>
```

说明:`selectVOByEventAndSchedule` 用 `INNER JOIN event_schedule` 确保只返回有赛次关联的成绩;另两个查询用 `LEFT JOIN` 保留兼容(旧数据 event_schedule_id 为 NULL 时 scheduleId/scheduleName 为 NULL)。

- [ ] **Step 3: 验证编译**

Run: `cd SportBackend && mvn compile -q`
Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Mapper/ResultMapper.java SportBackend/src/main/resources/mapper/ResultMapper.xml
git commit -m "feat: ResultMapper查询关联赛次并新增按赛次查询"
```

---

## Task 5: 后端 Service + Controller — 转换逻辑与新接口

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/ResultService.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/ResultServiceImpl.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminResultController.java`

- [ ] **Step 1: ResultService.java 加方法签名**

在 `getByEventId` 方法下方新增(约第 11 行后):

```java
    List<ResultVO> getByEventAndSchedule(Long eventId, Long scheduleId);
```

- [ ] **Step 2: ResultServiceImpl.java 整体替换**

```java
package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.EventScheduleMapper;
import com.dlust.sportbackend.Mapper.ResultMapper;
import com.dlust.sportbackend.Service.ResultService;
import com.dlust.sportbackend.entity.EventSchedule;
import com.dlust.sportbackend.entity.Result;
import com.dlust.sportbackend.entity.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResultServiceImpl implements ResultService {

    @Autowired
    private ResultMapper resultMapper;

    @Autowired
    private EventScheduleMapper eventScheduleMapper;

    @Override
    public List<ResultVO> getBySportsMeetingId(Long sportsMeetingId) {
        return resultMapper.selectVOBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<ResultVO> getByEventId(Long eventId) {
        return resultMapper.selectVOByEventId(eventId);
    }

    @Override
    public List<ResultVO> getByEventAndSchedule(Long eventId, Long scheduleId) {
        return resultMapper.selectVOByEventAndSchedule(eventId, scheduleId);
    }

    @Override
    public void add(Result result) {
        resolveEventScheduleId(result);
        resultMapper.insert(result);
    }

    @Override
    public void update(Result result) {
        resolveEventScheduleId(result);
        resultMapper.updateById(result);
    }

    @Override
    public void delete(Long id) {
        resultMapper.deleteById(id);
    }

    // 前端传 scheduleId（赛次），后端查出对应的 event_schedule.id 填入 eventScheduleId
    private void resolveEventScheduleId(Result result) {
        if (result.getEventScheduleId() == null && result.getScheduleId() != null) {
            EventSchedule es = eventScheduleMapper.selectByEventIdAndScheduleId(
                    result.getEventId(), result.getScheduleId());
            if (es == null) {
                throw new RuntimeException("该项目未配置该赛次");
            }
            result.setEventScheduleId(es.getId());
        }
    }
}
```

- [ ] **Step 3: AdminResultController.java 加 listByEventAndSchedule 并给 add/update 加异常处理**

整体替换 `AdminResultController.java` 为:

```java
package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.ResultService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/result")
public class AdminResultController {

    @Autowired
    private ResultService resultService;

    @GetMapping("/list")
    public Result<List<ResultVO>> list(@RequestParam Long sportsMeetingId) {
        log.info("查询成绩列表: sportsMeetingId={}", sportsMeetingId);
        return Result.success(resultService.getBySportsMeetingId(sportsMeetingId));
    }

    @GetMapping("/listByEvent")
    public Result<List<ResultVO>> listByEvent(@RequestParam Long eventId) {
        log.info("按项目查询成绩: eventId={}", eventId);
        return Result.success(resultService.getByEventId(eventId));
    }

    @GetMapping("/listByEventAndSchedule")
    public Result<List<ResultVO>> listByEventAndSchedule(@RequestParam Long eventId, @RequestParam Long scheduleId) {
        log.info("按项目和赛次查询成绩: eventId={}, scheduleId={}", eventId, scheduleId);
        return Result.success(resultService.getByEventAndSchedule(eventId, scheduleId));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody com.dlust.sportbackend.entity.Result result) {
        log.info("添加成绩: eventId={}, participantId={}, score={}", result.getEventId(), result.getParticipantId(), result.getScore());
        try {
            resultService.add(result);
            return Result.success("添加成功");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody com.dlust.sportbackend.entity.Result result) {
        log.info("更新成绩: id={}", result.getId());
        try {
            resultService.update(result);
            return Result.success("更新成功");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除成绩: id={}", id);
        resultService.delete(id);
        return Result.success("删除成功");
    }
}
```

- [ ] **Step 4: 验证编译**

Run: `cd SportBackend && mvn compile -q`
Expected: BUILD SUCCESS。

- [ ] **Step 5: 启动后端,调接口验证**

Run: `cd SportBackend && mvn spring-boot:run`(端口 8080)

用 curl/Postman 验证新接口(替换真实 id):
```
GET http://localhost:8080/api/admin/result/listByEventAndSchedule?eventId=<某项目ID>&scheduleId=<某赛次ID>
```
预期:返回 `{"code":200,"message":"success","data":[...]}`(数组可为空,因暂无成绩)。

- [ ] **Step 6: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Service/ResultService.java SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/ResultServiceImpl.java SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminResultController.java
git commit -m "feat: 成绩Service转换event_schedule_id并新增按赛次查询接口"
```

---

## Task 6: 前端 result.ts — 加字段与新查询方法

**Files:**
- Modify: `webManageProject/src/api/result.ts`

- [ ] **Step 1: 整体替换 result.ts**

```typescript
import request from '@/utils/request'

export interface ResultVO {
  id: number
  sportsMeetingId: number
  eventId: number
  eventScheduleId: number | null
  scheduleId: number | null
  participantId: number
  score: number | null
  createTime: string
  updateTime: string
  participantName: string
  eventName: string
  scheduleName: string
}

export interface ResultItem {
  id?: number
  sportsMeetingId: number
  eventId: number
  scheduleId?: number
  eventScheduleId?: number
  participantId: number
  score: number | null
}

export function getResultList(sportsMeetingId: number) {
  return request.get('/admin/result/list', { params: { sportsMeetingId } })
}

export function getResultListByEvent(eventId: number) {
  return request.get('/admin/result/listByEvent', { params: { eventId } })
}

export function getResultsByEventAndSchedule(eventId: number, scheduleId: number) {
  return request.get('/admin/result/listByEventAndSchedule', { params: { eventId, scheduleId } })
}

export function addResult(data: Partial<ResultItem>) {
  return request.post('/admin/result/add', data)
}

export function updateResult(data: Partial<ResultItem>) {
  return request.put('/admin/result/update', data)
}

export function deleteResult(id: number) {
  return request.delete(`/admin/result/${id}`)
}
```

- [ ] **Step 2: 验证类型检查**

Run: `cd webManageProject && npm run build`
Expected: 构建成功,无 TS 报错。

- [ ] **Step 3: Commit**

```bash
git add webManageProject/src/api/result.ts
git commit -m "feat: result.ts增加赛次字段和按赛次查询方法"
```

---

## Task 7: 前端 EventDetailView — 成绩列与行内编辑

**Files:**
- Modify: `webManageProject/src/views/EventDetailView.vue`

- [ ] **Step 1: 改 `<script setup>` 的 import 与新增成绩相关逻辑**

把文件开头的 import 块(第 1-10 行)替换为:

```typescript
<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEventList } from '@/api/event'
import type { Event } from '@/api/event'
import { getRegistrationListByEvent, addRegistration, updateRegistration, deleteRegistration } from '@/api/registration'
import type { RegistrationVO } from '@/api/registration'
import { getParticipantList } from '@/api/participant'
import type { Participant } from '@/api/participant'
import { getResultsByEventAndSchedule, addResult, updateResult } from '@/api/result'
import type { ResultVO } from '@/api/result'
```

- [ ] **Step 2: 在 `const regStatusType` 行(第 23 行)之后,新增成绩相关状态与方法**

在 `const regStatusType: Record<number, string> = { 0: 'primary', 1: 'success', 2: 'info' }` 之后插入:

```typescript

// ============ 成绩（行内编辑） ============
// 注意：resultMap 必须用 computed 依赖响应式 results，
// 否则成绩保存后(只更新 results)模板的成绩列不会重渲染。
const results = ref<ResultVO[]>([])
const resultMap = computed(() => new Map<number, ResultVO>(results.value.map(r => [r.participantId, r])))
const editingId = ref<number | null>(null)
const editingScore = ref('')

// 自动聚焦指令：el-input 渲染时聚焦内部 input
const vFocus = {
  mounted: (el: HTMLElement) => {
    el.querySelector('input')?.focus()
  }
}

async function fetchResults() {
  try {
    const res: any = await getResultsByEventAndSchedule(eventId, scheduleId)
    results.value = res.data || res || []
  } catch { /* ignore */ }
}

function startEdit(row: RegistrationVO) {
  editingId.value = row.participantId
  const existing = resultMap.value.get(row.participantId)
  editingScore.value = existing?.score != null ? String(existing.score) : ''
}

function cancelEdit() {
  editingId.value = null
}

async function saveScore(row: RegistrationVO) {
  // 防止回车后 blur 重复触发
  if (editingId.value !== row.participantId) return
  const val = editingScore.value.trim()
  if (val === '') {
    editingId.value = null
    return
  }
  if (!Number.isFinite(Number(val))) {
    ElMessage.warning('请输入有效成绩')
    return
  }
  const existing = resultMap.value.get(row.participantId)
  try {
    if (existing) {
      await updateResult({ id: existing.id, scheduleId, score: Number(val) })
    } else {
      await addResult({ sportsMeetingId: meetingId, eventId, participantId: row.participantId, scheduleId, score: Number(val) })
    }
    ElMessage.success('保存成功')
    editingId.value = null
    fetchResults()
  } catch {
    ElMessage.error('保存失败')
  }
}
```

- [ ] **Step 3: onMounted 中追加 fetchResults**

把 `onMounted`(第 127-130 行)替换为:

```typescript
onMounted(() => {
  fetchEvent()
  fetchRegistrations()
  fetchResults()
})
```

- [ ] **Step 4: 模板中在「赛次」列后、「状态」列前插入「成绩」列**

找到 `<el-table-column prop="scheduleName" label="赛次" width="90" />`(第 166 行),在其**之后**插入:

```html
        <el-table-column label="成绩" width="110">
          <template #default="{ row }">
            <el-input
              v-if="editingId === row.participantId"
              v-model="editingScore"
              v-focus
              size="small"
              style="width:90px"
              @keyup.enter="saveScore(row)"
              @keyup.esc="cancelEdit"
              @blur="saveScore(row)"
            />
            <el-button v-else link type="primary" size="small" @click="startEdit(row)">
              {{ resultMap.get(row.participantId)?.score ?? '录入' }}
            </el-button>
          </template>
        </el-table-column>
```

- [ ] **Step 5: 验证类型检查 + 构建**

Run: `cd webManageProject && npm run build`
Expected: 构建成功,无 TS 报错。

- [ ] **Step 6: 手动验证**

启动前端 `npm run dev` 与后端,打开一个有报名记录的项目报名页:
1. 无成绩参赛者显示「录入」→ 点击 → 输入数字 → 回车,显示成绩。
2. 有成绩参赛者 → 点击 → 修改 → 回车,更新。
3. 输入 `abc` → 提示「请输入有效成绩」,保持编辑态。
4. Esc → 取消,不保存。
5. 录入后,切换到同项目另一赛次报名页,该参赛者应**无**成绩(独立)。

- [ ] **Step 7: Commit**

```bash
git add webManageProject/src/views/EventDetailView.vue
git commit -m "feat: 项目报名页成绩列行内编辑(按赛次)"
```

---

## Task 8: 前端 MeetingDetailView — 录入选赛次 + 分组用成绩自带 scheduleId

**Files:**
- Modify: `webManageProject/src/views/MeetingDetailView.vue`

- [ ] **Step 1: 新增 getScheduleNameById 辅助函数**

在 `getScheduleNameByEventId` 函数(约第 399-404 行)之后新增:

```typescript

function getScheduleNameById(scheduleId: number | null): string {
  if (scheduleId == null) return '未分类'
  const sch = schedules.value.find(s => s.id === scheduleId)
  return sch ? sch.name : '未分类'
}
```

- [ ] **Step 2: 改写 getResultsGroupedBySchedule,用成绩自带 scheduleId 分组**

把 `getResultsGroupedBySchedule` 函数(约第 419-433 行)整体替换为:

```typescript
function getResultsGroupedBySchedule(): { scheduleId: number | null, scheduleName: string, items: ResultVO[] }[] {
  const groups: Record<string, ResultVO[]> = {}
  for (const r of filteredResults.value) {
    const schId = r.scheduleId ?? null
    const key = String(schId)
    if (!groups[key]) groups[key] = []
    groups[key].push(r)
  }
  return Object.entries(groups).map(([key, items]) => ({
    scheduleId: key === 'null' ? null : Number(key),
    scheduleName: items[0].scheduleName || getScheduleNameById(items[0].scheduleId),
    items,
  }))
}
```

- [ ] **Step 3: 录入对话框加「赛次」表单项**

找到成绩录入弹窗里的「比赛项目」`el-form-item`(约第 803-807 行),在其**之后**(「参赛人员」`el-form-item` 之前)插入:

```html
        <el-form-item label="赛次" required>
          <el-select v-model="resultForm.scheduleId" placeholder="请选择赛次" style="width:100%" :disabled="!!resultForm.id">
            <el-option
              v-for="schId in getScheduleIdsByEventId(resultForm.eventId)"
              :key="schId"
              :label="getScheduleNameById(schId)"
              :value="schId"
            />
          </el-select>
        </el-form-item>
```

- [ ] **Step 4: openResultAdd 初始值加 scheduleId**

把 `openResultAdd`(约第 435-438 行)替换为:

```typescript
function openResultAdd() {
  resultForm.value = { sportsMeetingId: meetingId, eventId: undefined as any, scheduleId: undefined, participantId: undefined as any, score: null }
  resultDialogVisible.value = true
}
```

- [ ] **Step 5: openResultEdit 带上 scheduleId**

把 `openResultEdit`(约第 439-442 行)替换为:

```typescript
function openResultEdit(row: ResultVO) {
  resultForm.value = { id: row.id, sportsMeetingId: row.sportsMeetingId, eventId: row.eventId, scheduleId: row.scheduleId ?? undefined, participantId: row.participantId, score: row.score }
  resultDialogVisible.value = true
}
```

- [ ] **Step 6: 验证类型检查 + 构建**

Run: `cd webManageProject && npm run build`
Expected: 构建成功,无 TS 报错。

- [ ] **Step 7: 手动验证**

启动前端,打开运动会详情 → 成绩管理 tab:
1. 点「+ 录入成绩」→ 选项目后,赛次下拉显示该项目配置的赛次 → 选赛次 → 填成绩 → 确定,成功。
2. 成绩列表按赛次分组显示,组名 = scheduleName。
3. 录入的成绩,在对应赛次的项目报名页(EventDetailView)可见;在另一赛次报名页不可见。

- [ ] **Step 8: Commit**

```bash
git add webManageProject/src/views/MeetingDetailView.vue
git commit -m "feat: 成绩录入选择赛次,列表按成绩自带赛次分组"
```

---

## Task 9: 端到端验证

- [ ] **Step 1: 预赛/决赛独立性**

选一个有「预赛 + 决赛」两个赛次的项目:
1. 在预赛报名页为参赛者 A 录入成绩 11.20 → 保存。
2. 切换到决赛报名页 → 参赛者 A(若也报名决赛)显示「录入」(无成绩),独立。
3. 为决赛的参赛者 B 录入成绩 11.50。
4. MeetingDetailView 成绩 tab → 看到「预赛」组有 A、「决赛」组有 B。

- [ ] **Step 2: 边界 — 项目未配置该赛次**

若手动构造请求(项目未关联该赛次)录入成绩 → 后端返回 `{code:400, message:"该项目未配置该赛次"}`,前端 toast 错误。

- [ ] **Step 3: 最终构建确认**

Run: `cd webManageProject && npm run build`
Expected: 构建成功。

Run: `cd SportBackend && mvn compile -q`
Expected: BUILD SUCCESS。

---

## Self-Review 记录

- **Spec 覆盖**:数据库(Task 1)、entity(Task 2)、EventScheduleMapper(Task 3)、ResultMapper 三 select + insert/update(Task 4)、Service 转换 + Controller 新接口 + 异常(Task 5)、result.ts(Task 6)、EventDetailView 成绩列(Task 7)、MeetingDetailView 连带(Task 8)、端到端(Task 9)——spec 各节均有对应任务。
- **占位符扫描**:无 TBD/TODO,所有代码步骤含完整代码。
- **类型一致性**:`eventScheduleId`、`scheduleId`、`scheduleName`、`getResultsByEventAndSchedule`、`selectVOByEventAndSchedule`、`selectByEventIdAndScheduleId`、`getByEventAndSchedule`、`resolveEventScheduleId`、`getScheduleNameById` 等命名前后一致。
