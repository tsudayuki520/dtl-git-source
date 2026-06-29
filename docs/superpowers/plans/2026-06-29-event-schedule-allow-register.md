# 开放报名下沉到「项目×轮次」粒度 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将「开放报名」开关从项目(event)粒度下沉到 项目×轮次(event_schedule)粒度，新增项目选多个轮次时自动只开放最低轮次，并支持在轮次详情页手动切换。

**Architecture:** `event_schedule` 表加 `allow_register` 列；`event.allow_register` 保留为项目总闸。新增项目时按 `schedule.sort` 升序只把最低轮次设为开放；编辑项目时增量同步保留已有开关；轮次详情页用 el-switch 切换；报名时取「开放且 sort 最小」的轮次。

**Tech Stack:** Spring Boot 4.0.6 / MyBatis / MySQL 8 / Vue 3 + Element Plus / 原生微信小程序

**测试策略（务实）：** 核心增量 diff 逻辑抽成纯静态方法 + JUnit 单测；MyBatis/Controller/前端/小程序改动用具体的 curl 命令与前端操作做手动验证（项目无前端测试框架、无 Mockito 先例，不强行引入）。

**对应设计文档：** [docs/superpowers/specs/2026-06-29-event-schedule-allow-register-design.md](../specs/2026-06-29-event-schedule-allow-register-design.md)

---

## 文件结构（创建/修改清单）

**后端**
- Modify: `SportBackend/sql/event_schedule.sql`（加列）
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/EventSchedule.java`（加 allowRegister）
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/Event.java`（加 currentOpenScheduleId 非持久化字段）
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/EventScheduleMapper.java`（加 3 个方法）
- Modify: `SportBackend/src/main/resources/mapper/EventScheduleMapper.xml`（batchInsert 带列 + 2 个新查询/更新）
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/EventScheduleService.java`（加 2 个方法签名）
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java`（增量化 + 静态 syncPlan + 新方法）
- Create: `SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImplTest.java`（纯逻辑单测）
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminEventScheduleController.java`（加 allow 接口）
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserEventController.java`（装配 currentOpenScheduleId）
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserRegisterController.java`（取开放轮次）

**管理端**
- Modify: `webManageProject/src/api/eventSchedule.ts`（加字段 + toggleAllow）
- Modify: `webManageProject/src/api/event.ts`（Event 接口加 currentOpenScheduleId）
- Modify: `webManageProject/src/views/ScheduleDetailView.vue`（开关列 + fetchEvents 装配）

**小程序**
- Modify: `sportdocu/pages/register/register.js`（过滤条件加 currentOpenScheduleId）

---

## Task 1: 数据库 DDL + 数据回填

> ⚠️ 本任务涉及数据库写操作（DDL + DML）。按项目约定（CLAUDE.md / memory），**执行任何 SQL 前必须先向用户确认**。下面把「写 SQL 文件」和「执行 SQL」分开。

**Files:**
- Modify: `SportBackend/sql/event_schedule.sql`

- [ ] **Step 1: 同步建表脚本（加 allow_register 列）**

修改 [event_schedule.sql](../../../SportBackend/sql/event_schedule.sql)，在 `schedule_id` 行后加一行 `allow_register`，完整内容如下：

```sql
CREATE TABLE IF NOT EXISTS event_schedule (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    event_id      BIGINT   NOT NULL COMMENT '项目ID',
    schedule_id   BIGINT   NOT NULL COMMENT '赛程ID',
    allow_register TINYINT NOT NULL DEFAULT 0 COMMENT '该轮次是否开放报名：1-开放，0-关闭',
    is_deleted    TINYINT  DEFAULT 0 COMMENT '软删除：0-正常，1-已删除',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_event_schedule (event_id, schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-赛程关联表';
```

- [ ] **Step 2: 准备 DDL 迁移语句（仅准备，不执行）**

将以下语句整理好准备执行（下一步向用户确认后执行）：

```sql
ALTER TABLE event_schedule
  ADD COLUMN allow_register TINYINT NOT NULL DEFAULT 0
  COMMENT '该轮次是否开放报名：1-开放，0-关闭'
  AFTER schedule_id;
```

- [ ] **Step 3: 向用户确认并执行 DDL**

向用户说明：需要在 `sport_db` 上执行上面的 `ALTER TABLE` 加列。征得同意后在 MySQL 执行。

- [ ] **Step 4: 准备数据回填语句（仅准备，不执行）**

回填规则：每个 event 取其关联轮次中 `schedule.sort` 最小者，`allow_register = event.allow_register`；其余 0。

```sql
-- 把每个 event 的最低轮次关联的 allow_register 设为该项目总闸值
UPDATE event_schedule es
JOIN (
    SELECT es2.event_id, es2.id AS es_id, es2.schedule_id
    FROM event_schedule es2
    JOIN schedule s ON es2.schedule_id = s.id
    WHERE es2.is_deleted = 0 AND s.is_deleted = 0
      AND s.sort = (
          SELECT MIN(s2.sort)
          FROM event_schedule es3
          JOIN schedule s2 ON es3.schedule_id = s2.id
          WHERE es3.event_id = es2.event_id AND es3.is_deleted = 0 AND s2.is_deleted = 0
      )
) lowest ON es.id = lowest.es_id
JOIN event e ON es.event_id = e.id
SET es.allow_register = e.allow_register;
```

- [ ] **Step 5: 向用户确认并执行回填**

向用户说明回填语义（最低轮次继承项目总闸，其余 0），确认后执行。

- [ ] **Step 6: 验证回填结果**

执行查询确认：

```sql
SELECT es.event_id, s.name AS round_name, s.sort, es.allow_register, e.allow_register AS event_total
FROM event_schedule es
JOIN schedule s ON es.schedule_id = s.id
JOIN event e ON es.event_id = e.id
WHERE es.is_deleted = 0
ORDER BY es.event_id, s.sort;
```

Expected: 每个 event 的 sort 最小轮次 `allow_register` 等于 `event.allow_register`，其余为 0。

- [ ] **Step 7: Commit**

```bash
git add SportBackend/sql/event_schedule.sql
git commit -m "feat: event_schedule 表增加 allow_register 字段（按轮次控制开放报名）"
```

---

## Task 2: EventSchedule entity + Mapper 层

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/EventSchedule.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/EventScheduleMapper.java`
- Modify: `SportBackend/src/main/resources/mapper/EventScheduleMapper.xml`

- [ ] **Step 1: EventSchedule.java 加 allowRegister 字段**

修改 [EventSchedule.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/entity/EventSchedule.java)，在 `scheduleId` 字段后加：

```java
    private Long scheduleId;
    private Integer allowRegister; // 该轮次是否开放报名：1-开放，0-关闭
    private Integer isDeleted;
```

- [ ] **Step 2: EventScheduleMapper.java 加 3 个方法**

修改 [EventScheduleMapper.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Mapper/EventScheduleMapper.java)，在 `batchInsert` 之后、`deleteByEventId` 之前加：

```java
    void batchInsertWithAllow(@Param("list") List<EventSchedule> list);

    void updateAllowRegister(@Param("eventId") Long eventId,
                             @Param("scheduleId") Long scheduleId,
                             @Param("allowRegister") Integer allowRegister);

    /** 取该 event 下 allow_register=1 且 sort 最小的 scheduleId；无则返回 null */
    Long selectOpenScheduleIdByEventId(@Param("eventId") Long eventId);
```

注意 import `java.util.List` 已存在。

- [ ] **Step 3: EventScheduleMapper.xml 替换 batchInsert 并新增 SQL**

修改 [EventScheduleMapper.xml](../../../SportBackend/src/main/resources/mapper/EventScheduleMapper.xml)：

把旧的 `batchInsert` 替换为（支持 allow_register）：

```xml
    <insert id="batchInsertWithAllow">
        INSERT INTO event_schedule (event_id, schedule_id, allow_register)
        VALUES
        <foreach collection="list" item="item" separator=",">
            (#{item.eventId}, #{item.scheduleId}, #{item.allowRegister})
        </foreach>
    </insert>
```

在 `deleteByEventId` 之前新增：

```xml
    <update id="updateAllowRegister">
        UPDATE event_schedule
        SET allow_register = #{allowRegister}
        WHERE event_id = #{eventId} AND schedule_id = #{scheduleId} AND is_deleted = 0
    </update>

    <select id="selectOpenScheduleIdByEventId" resultType="java.lang.Long">
        SELECT es.schedule_id FROM event_schedule es
        INNER JOIN schedule s ON es.schedule_id = s.id
        WHERE es.event_id = #{eventId} AND es.is_deleted = 0 AND s.is_deleted = 0
          AND es.allow_register = 1
        ORDER BY s.sort ASC, s.create_time ASC
        LIMIT 1
    </select>
```

注意：旧的 `batchInsert`（无 allow_register 版本）删除，由 `batchInsertWithAllow` 替代。`selectByEventId` 是 `SELECT *`，会自动带出新列，无需改。

- [ ] **Step 4: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

Expected: BUILD SUCCESS，无编译错误。

- [ ] **Step 5: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/entity/EventSchedule.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Mapper/EventScheduleMapper.java \
        SportBackend/src/main/resources/mapper/EventScheduleMapper.xml
git commit -m "feat: EventSchedule entity/Mapper 支持 allow_register 字段"
```

---

## Task 3: 增量同步纯逻辑 + 单元测试（TDD）

> 把「保留/删除/新增」与「新增项目时只开最低轮次」的判定抽成纯静态方法，先写测试。

**Files:**
- Create: `SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImplTest.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java`

- [ ] **Step 1: 写失败测试**

创建 [EventScheduleServiceImplTest.java](../../../SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImplTest.java)（同包，可访问 package-private 静态方法，**不加** `@SpringBootTest` 以避免连数据库）：

```java
package com.dlust.sportbackend.Service.Impl;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventScheduleServiceImplTest {

    /**
     * syncPlan：计算关联同步方案。
     * 返回 Map：scheduleId -> 目标 allowRegister。
     * 现有但未出现在 selected 中的 scheduleId 不在返回 Map 里（调用方据此删除）。
     */
    private static Map<Long, Integer> callSyncPlan(Map<Long, Integer> existing,
                                                   List<Long> selectedSorted,
                                                   boolean eventAllowRegister) {
        return EventScheduleServiceImpl.syncPlanForTest(existing, selectedSorted, eventAllowRegister);
    }

    @Test
    void newProject_multipleRounds_totalOn_onlyLowestOpen() {
        // 新增项目（existing 空），选了预赛/复赛/决赛（已按 sort 升序），总闸开 → 只有预赛=1
        Map<Long, Integer> plan = callSyncPlan(new LinkedHashMap<>(),
                List.of(10L, 20L, 30L), true);
        assertEquals(1, plan.get(10L));
        assertEquals(0, plan.get(20L));
        assertEquals(0, plan.get(30L));
    }

    @Test
    void newProject_singleRound_totalOn_thatRoundOpen() {
        Map<Long, Integer> plan = callSyncPlan(new LinkedHashMap<>(), List.of(10L), true);
        assertEquals(1, plan.get(10L));
    }

    @Test
    void newProject_totalOff_allClosed() {
        Map<Long, Integer> plan = callSyncPlan(new LinkedHashMap<>(), List.of(10L, 20L), false);
        assertEquals(0, plan.get(10L));
        assertEquals(0, plan.get(20L));
    }

    @Test
    void editProject_preservesExistingSwitch_newRoundsDefaultClosed() {
        // 编辑：预赛已=1（之前手动），新增决赛关联 → 预赛保留 1，决赛默认 0
        Map<Long, Integer> existing = new LinkedHashMap<>();
        existing.put(10L, 1); // 预赛已开放
        Map<Long, Integer> plan = callSyncPlan(existing, List.of(10L, 30L), true);
        assertEquals(1, plan.get(10L));  // 保留
        assertEquals(0, plan.get(30L));  // 新增默认关
    }

    @Test
    void editProject_droppedRoundAbsentFromPlan() {
        // 编辑：取消选中复赛 → 复赛不在 plan 里（调用方据此删除）
        Map<Long, Integer> existing = new LinkedHashMap<>();
        existing.put(10L, 1);
        existing.put(20L, 0);
        Map<Long, Integer> plan = callSyncPlan(existing, List.of(10L), true);
        assertTrue(plan.containsKey(10L));
        assertFalse(plan.containsKey(20L));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd SportBackend && mvn -q test -Dtest=EventScheduleServiceImplTest
```

Expected: 编译失败（`syncPlanForTest` 方法不存在）。

- [ ] **Step 3: 实现纯静态方法**

在 [EventScheduleServiceImpl.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java) 类体内（与 `parseLongList` 同级位置无需，直接加在类内）新增：

```java
    /**
     * 计算关联同步方案（纯逻辑，便于单测）。
     * - existing 为空（首次创建）：总闸开时只把 selected 第一个（sort 最小）设 1，其余 0；总闸关全 0。
     * - existing 非空（编辑）：保留已有关联的 allow_register；新增的默认 0。
     * - existing 中不在 selected 里的 scheduleId 不出现在返回 Map（调用方据此删除）。
     *
     * @param existing           现有关联 scheduleId -> allow_register
     * @param selectedSorted     新选中的 scheduleIds（须已按 schedule.sort 升序）
     * @param eventAllowRegister 项目总闸
     */
    static Map<Long, Integer> syncPlanForTest(Map<Long, Integer> existing,
                                              List<Long> selectedSorted,
                                              boolean eventAllowRegister) {
        Map<Long, Integer> plan = new LinkedHashMap<>();
        boolean isNew = existing == null || existing.isEmpty();
        for (int i = 0; i < selectedSorted.size(); i++) {
            Long sid = selectedSorted.get(i);
            Integer cur = (existing == null) ? null : existing.get(sid);
            if (cur != null) {
                plan.put(sid, cur); // 保留
            } else if (isNew && eventAllowRegister && i == 0) {
                plan.put(sid, 1);   // 首次创建：最低轮次开放
            } else {
                plan.put(sid, 0);   // 编辑新增：默认关
            }
        }
        return plan;
    }
```

顶部如缺 import，补：

```java
import java.util.LinkedHashMap;
import java.util.Map;
```

- [ ] **Step 4: 跑测试确认通过**

```bash
cd SportBackend && mvn -q test -Dtest=EventScheduleServiceImplTest
```

Expected: Tests run: 5, Failures: 0, Errors: 0。

- [ ] **Step 5: Commit**

```bash
git add SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImplTest.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java
git commit -m "feat: 抽取关联同步纯逻辑 syncPlan 并补单测"
```

---

## Task 4: saveEventSchedules 增量化改造

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/EventScheduleService.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java`

- [ ] **Step 1: Service 接口加 updateAllowRegister、getOpenScheduleIdByEventId 签名**

修改 [EventScheduleService.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Service/EventScheduleService.java)，在末尾加：

```java
    void updateAllowRegister(Long eventId, Long scheduleId, Integer allowRegister);

    /** 取该 event 当前开放且 sort 最小的 scheduleId，无则 null */
    Long getOpenScheduleIdByEventId(Long eventId);
```

`saveEventSchedules` 签名保持不变。

- [ ] **Step 2: Impl 改造 saveEventSchedules 为增量同步**

修改 [EventScheduleServiceImpl.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java) 的 `saveEventSchedules`，替换为：

```java
    @Override
    @Transactional
    public void saveEventSchedules(Long eventId, List<Long> scheduleIds) {
        // 1. 现有关联（已带 allow_register）
        List<EventSchedule> existing = eventScheduleMapper.selectByEventId(eventId);
        Map<Long, Integer> existingMap = new LinkedHashMap<>();
        for (EventSchedule es : existing) {
            existingMap.put(es.getScheduleId(), es.getAllowRegister() == null ? 0 : es.getAllowRegister());
        }

        // 2. 计算同步方案（selected 须按 sort 升序，复用既有排序查询）
        List<Long> selectedSorted = (scheduleIds == null) ? List.of() : scheduleIds;
        if (!selectedSorted.isEmpty()) {
            // 借用 selectScheduleIdsByEventId 的排序逻辑：按 schedule.sort 升序
            // 这里 selected 是新选集合，需自行按 sort 排序
            selectedSorted = sortByScheduleSort(selectedSorted);
        }
        boolean eventAllowRegister = isEventAllowRegister(eventId);
        Map<Long, Integer> plan = syncPlanForTest(existingMap, selectedSorted, eventAllowRegister);

        // 3. 删除：现有但不在 plan 里的
        for (Long sid : existingMap.keySet()) {
            if (!plan.containsKey(sid)) {
                // 软删除单条：用 scheduleId 维度（该 event 下）
                eventScheduleMapper.deleteByScheduleIdForEvent(eventId, sid);
            }
        }

        // 4. 新增：plan 里 existing 没有的；保留的无需动作
        List<EventSchedule> toInsert = new java.util.ArrayList<>();
        for (Map.Entry<Long, Integer> e : plan.entrySet()) {
            if (!existingMap.containsKey(e.getKey())) {
                EventSchedule es = new EventSchedule();
                es.setEventId(eventId);
                es.setScheduleId(e.getKey());
                es.setAllowRegister(e.getValue());
                toInsert.add(es);
            }
        }
        if (!toInsert.isEmpty()) {
            eventScheduleMapper.batchInsertWithAllow(toInsert);
        }
    }
```

- [ ] **Step 3: Impl 补辅助方法与依赖**

在 [EventScheduleServiceImpl.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java) 类内补充（顶部注入 `EventMapper`）：

新增 import：
```java
import com.dlust.sportbackend.Mapper.EventMapper;
import com.dlust.sportbackend.entity.Event;
import java.util.ArrayList;
import java.util.Comparator;
```

注入 EventMapper（在 `eventScheduleMapper` 字段下）：
```java
    @Autowired
    private EventMapper eventMapper;
```

辅助方法：
```java
    /** 按 schedule.sort 升序排列给定的 scheduleIds */
    private List<Long> sortByScheduleSort(List<Long> scheduleIds) {
        // 复用 mapper：取该项目所有关联的 (scheduleId, sort) 排序，再过滤出选中的
        List<Long> ordered = eventScheduleMapper.selectScheduleIdsByEventIdOrdered(scheduleIds);
        return ordered;
    }

    private boolean isEventAllowRegister(Long eventId) {
        Event e = eventMapper.selectById(eventId);
        return e != null && e.getAllowRegister() != null && e.getAllowRegister() == 1;
    }
```

> 说明：`selectScheduleIdsByEventIdOrdered` 接收一组任意 scheduleId，按 `schedule.sort` 升序返回——下一步在 Mapper 加。

- [ ] **Step 4: Mapper 加排序查询 + 单条软删除**

在 [EventScheduleMapper.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Mapper/EventScheduleMapper.java) 加：

```java
    /** 按给定 scheduleIds 集合，依 schedule.sort 升序返回 */
    List<Long> selectScheduleIdsByEventIdOrdered(@Param("scheduleIds") List<Long> scheduleIds);

    /** 软删除某 event 下的某个 schedule 关联 */
    void deleteByScheduleIdForEvent(@Param("eventId") Long eventId, @Param("scheduleId") Long scheduleId);
```

在 [EventScheduleMapper.xml](../../../SportBackend/src/main/resources/mapper/EventScheduleMapper.xml) 加：

```xml
    <select id="selectScheduleIdsByEventIdOrdered" resultType="java.lang.Long">
        SELECT s.id FROM schedule s
        WHERE s.is_deleted = 0
          AND s.id IN
          <foreach collection="scheduleIds" item="sid" open="(" separator="," close=")">
              #{sid}
          </foreach>
        ORDER BY s.sort ASC, s.create_time ASC
    </select>

    <update id="deleteByScheduleIdForEvent">
        UPDATE event_schedule SET is_deleted = 1
        WHERE event_id = #{eventId} AND schedule_id = #{scheduleId} AND is_deleted = 0
    </update>
```

- [ ] **Step 5: Impl 实现 updateAllowRegister 与 getOpenScheduleIdByEventId**

在 [EventScheduleServiceImpl.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java) 加：

```java
    @Override
    public void updateAllowRegister(Long eventId, Long scheduleId, Integer allowRegister) {
        eventScheduleMapper.updateAllowRegister(eventId, scheduleId, allowRegister == null ? 0 : allowRegister);
    }

    @Override
    public Long getOpenScheduleIdByEventId(Long eventId) {
        return eventScheduleMapper.selectOpenScheduleIdByEventId(eventId);
    }
```

- [ ] **Step 6: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 7: 跑单测确认未被破坏**

```bash
cd SportBackend && mvn -q test -Dtest=EventScheduleServiceImplTest
```

Expected: 5 个测试全过。

- [ ] **Step 8: 启动后端做手动验证**

启动后端（`mvn spring-boot:run`，需 application.yml 配好）。用 curl 验证「新增项目选多轮次只开最低」：

先准备：确保某运动会下有 3 个轮次（schedule）记录（sort 升序，如预赛=1/复赛=2/决赛=3）。记下三个 scheduleId，假设 10/20/30。

```bash
# 新增一个项目（allowRegister=1），关联三个轮次
curl -X POST http://localhost:8080/api/admin/event/add \
  -H "Content-Type: application/json" \
  -d '{"sportsMeetingId":1,"name":"T-测试100米","category":"径赛","gender":"不限","allowRegister":1,"registerLimit":0,"status":0}'
# 记下返回的 eventId，假设 999

curl -X POST http://localhost:8080/api/admin/event-schedule/save \
  -H "Content-Type: application/json" \
  -d '{"eventId":999,"scheduleIds":[10,20,30]}'
```

查库验证：
```sql
SELECT schedule_id, allow_register FROM event_schedule WHERE event_id = 999 ORDER BY schedule_id;
```
Expected: schedule_id=10 的 allow_register=1，20 和 30 为 0。

清理测试数据（软删除该项目即可）：
```bash
curl -X DELETE http://localhost:8080/api/admin/event/999
```

- [ ] **Step 9: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Service/EventScheduleService.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/EventScheduleServiceImpl.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Mapper/EventScheduleMapper.java \
        SportBackend/src/main/resources/mapper/EventScheduleMapper.xml
git commit -m "feat: saveEventSchedules 增量同步保留开关+新增项目自动只开最低轮次"
```

---

## Task 5: 管理端切换接口（PUT /allow）

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminEventScheduleController.java`

- [ ] **Step 1: Controller 加 allow 接口**

在 [AdminEventScheduleController.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminEventScheduleController.java) 的 `delete` 方法之后加：

```java
    @PutMapping("/allow")
    public Result<String> toggleAllow(@RequestBody EventScheduleAllowRequest request) {
        log.info("切换轮次开放报名: eventId={}, scheduleId={}, allowRegister={}",
                request.getEventId(), request.getScheduleId(), request.getAllowRegister());
        eventScheduleService.updateAllowRegister(
                request.getEventId(), request.getScheduleId(), request.getAllowRegister());
        return Result.success("更新成功");
    }
```

并在文件底部 `EventScheduleSaveRequest` 静态内部类之后新增：

```java
    @Data
    public static class EventScheduleAllowRequest {
        private Long eventId;
        private Long scheduleId;
        private Integer allowRegister;
    }
```

- [ ] **Step 2: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: 启动后端手动验证**

启动后端。沿用 Task 4 验证用的数据（或新建）。对某 event_schedule 切换：

```bash
curl -X PUT http://localhost:8080/api/admin/event-schedule/allow \
  -H "Content-Type: application/json" \
  -d '{"eventId":999,"scheduleId":30,"allowRegister":1}'
```

查库：
```sql
SELECT schedule_id, allow_register FROM event_schedule WHERE event_id = 999 AND schedule_id = 30;
```
Expected: allow_register=1。

- [ ] **Step 4: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminEventScheduleController.java
git commit -m "feat: 新增 PUT /admin/event-schedule/allow 接口切换轮次开放报名"
```

---

## Task 6: Event 列表装配 currentOpenScheduleId

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/Event.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserEventController.java`

- [ ] **Step 1: Event.java 加非持久化字段**

在 [Event.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/entity/Event.java) 末尾（`updateTime` 后）加：

```java
    private Long currentOpenScheduleId; // 当前开放且 sort 最小的轮次ID（非持久化，VO 用，null 表示无开放轮次）
```

- [ ] **Step 2: UserEventController 装配字段**

修改 [UserEventController.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserEventController.java) 的 `getList`：在两个返回 events 的分支前统一填充。把方法体改为：

```java
    @GetMapping("/list")
    public Result<List<Event>> getList(@RequestParam(required = false) Long scheduleId,
                                        @RequestParam(required = false) Long sportsMeetingId,
                                        @RequestParam(required = false) Long groupTypeId) {
        log.info("获取项目列表: scheduleId={}, sportsMeetingId={}, groupTypeId={}", scheduleId, sportsMeetingId, groupTypeId);
        List<Event> events;
        if (scheduleId != null) {
            List<Long> eventIds = eventScheduleService.getEventIdsByScheduleId(scheduleId);
            if (eventIds.isEmpty()) {
                return Result.success(List.of());
            }
            events = eventIds.stream()
                    .map(eventService::getById)
                    .filter(e -> e != null)
                    .filter(e -> groupTypeId == null || groupTypeId.equals(e.getGroupTypeId()))
                    .collect(Collectors.toList());
        } else if (sportsMeetingId != null) {
            events = eventService.getBySportsMeetingId(sportsMeetingId);
        } else {
            return Result.error(400, "请传入 scheduleId 或 sportsMeetingId");
        }
        // 装配 currentOpenScheduleId（当前开放且 sort 最小的轮次）
        for (Event e : events) {
            e.setCurrentOpenScheduleId(eventScheduleService.getOpenScheduleIdByEventId(e.getId()));
        }
        return Result.success(events);
    }
```

- [ ] **Step 3: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 4: 手动验证**

启动后端，对某运动会查询：

```bash
curl "http://localhost:8080/api/event/list?sportsMeetingId=1"
```

Expected: 返回的每个 event 对象含 `currentOpenScheduleId` 字段；至少有一个项目该字段为 Task 4/5 中设过 allow_register=1 的 scheduleId。

- [ ] **Step 5: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/entity/Event.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserEventController.java
git commit -m "feat: event 列表装配 currentOpenScheduleId 供小程序精确过滤"
```

---

## Task 7: 报名 submit 取开放轮次

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserRegisterController.java`

- [ ] **Step 1: 改 submit 的 scheduleId 取值逻辑**

修改 [UserRegisterController.java](../../../SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserRegisterController.java)。把现有的「取 scheduleId」代码块（约 52-61 行）：

```java
        // 获取 scheduleId：前端传或取项目第一个赛次
        Long scheduleId = null;
        if (body.get("scheduleId") != null) {
            scheduleId = Long.valueOf(body.get("scheduleId").toString());
        } else {
            List<Long> scheduleIds = eventScheduleService.getScheduleIdsByEventId(eventId);
            if (scheduleIds != null && !scheduleIds.isEmpty()) {
                scheduleId = scheduleIds.get(0);
            }
        }
```

替换为：

```java
        // 获取 scheduleId：前端传，或取该项目「当前开放且 sort 最小」的轮次
        Long scheduleId = null;
        if (body.get("scheduleId") != null) {
            scheduleId = Long.valueOf(body.get("scheduleId").toString());
        } else {
            scheduleId = eventScheduleService.getOpenScheduleIdByEventId(eventId);
            if (scheduleId == null) {
                return Result.error(400, "该项目当前未开放报名");
            }
        }
```

- [ ] **Step 2: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: 手动验证**

启动后端。准备一个「总闸开 + 某轮次 allow_register=1」的项目（eventId=999，开放 scheduleId=10）。

```bash
# 正常：能取到开放轮次（报名校验会继续走原有限报逻辑，这里只验证 scheduleId 被正确写入）
curl -X POST http://localhost:8080/api/register/submit \
  -H "Content-Type: application/json" \
  -d '{"sportsMeetingId":1,"eventId":999,"userCode":"TEST001","name":"测试员","phone":"13800000000","gender":"男","college":"","major":""}'
```

查库：
```sql
SELECT schedule_id FROM registration WHERE event_id = 999 AND participant_id = (SELECT id FROM participant WHERE user_code='TEST001');
```
Expected: schedule_id = 10（开放的那个）。

把该 event 所有轮次 allow_register 改为 0 后再报：
```bash
curl -X PUT http://localhost:8080/api/admin/event-schedule/allow \
  -H "Content-Type: application/json" \
  -d '{"eventId":999,"scheduleId":10,"allowRegister":0}'

curl -X POST http://localhost:8080/api/register/submit \
  -H "Content-Type: application/json" \
  -d '{"sportsMeetingId":1,"eventId":999,"userCode":"TEST002","name":"测试员2","phone":"13800000001","gender":"男","college":"","major":""}'
```
Expected: 第二次返回 `{"code":400,"message":"该项目当前未开放报名"}`。

清理测试数据（手动删除 TEST001/TEST002 的 participant 与 registration）。

- [ ] **Step 4: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserRegisterController.java
git commit -m "feat: 报名 submit 取当前开放轮次，无开放则拒绝"
```

---

## Task 8: 管理端轮次详情页开关列

**Files:**
- Modify: `webManageProject/src/api/eventSchedule.ts`
- Modify: `webManageProject/src/api/event.ts`
- Modify: `webManageProject/src/views/ScheduleDetailView.vue`

- [ ] **Step 1: api/eventSchedule.ts 加字段与方法**

修改 [eventSchedule.ts](../../../webManageProject/src/api/eventSchedule.ts)：

接口加 `allowRegister`：
```typescript
export interface EventSchedule {
  id: number
  eventId: number
  scheduleId: number
  allowRegister: number
}
```

文件末尾加方法：
```typescript
export function toggleEventScheduleAllow(eventId: number, scheduleId: number, allowRegister: number) {
  return request.put('/admin/event-schedule/allow', { eventId, scheduleId, allowRegister })
}
```

- [ ] **Step 2: api/event.ts 加 currentOpenScheduleId**

修改 [event.ts](../../../webManageProject/src/api/event.ts) 的 `Event` 接口，在 `status` 后加：
```typescript
  status: number
  currentOpenScheduleId: number | null
```

- [ ] **Step 3: ScheduleDetailView.vue 装配 allowRegister 到项目列表**

修改 [ScheduleDetailView.vue](../../../webManageProject/src/views/ScheduleDetailView.vue)。

在 `<script setup>` 顶部 import 增补 `toggleEventScheduleAllow`：
```typescript
import { getEventSchedulesBySchedule, getEventSchedules, saveEventSchedules, toggleEventScheduleAllow } from '@/api/eventSchedule'
```

新增一个 `allowMap`（eventId -> allowRegister）并改造 `fetchEvents` 末尾装配。把 `fetchEvents` 整体替换为：

```typescript
async function fetchEvents() {
  try {
    const assocRes: any = await getEventSchedulesBySchedule(scheduleId)
    const associations: any[] = assocRes.data || assocRes || []
    const eventIds = new Set(associations.map((a: any) => a.eventId))
    // 建立 eventId -> allowRegister 映射（当前轮次下的开关状态）
    const allowMap: Record<number, number> = {}
    associations.forEach((a: any) => { allowMap[a.eventId] = a.allowRegister ?? 0 })
    allowRegisterMap.value = allowMap
    if (eventIds.size === 0) {
      events.value = []
      return
    }
    const res: any = await getEventList({ sportsMeetingId: meetingId })
    events.value = (res.data || res || []).filter((e: Event) => eventIds.has(e.id))
  } catch { /* ignore */ }
}
```

在 `regCountMap` 声明附近新增：
```typescript
const allowRegisterMap = ref<Record<number, number>>({})
```

- [ ] **Step 4: ScheduleDetailView.vue 加切换方法**

在 `toggleScheduleStatus` 方法附近新增：

```typescript
async function handleToggleAllow(eventId: number, val: number) {
  try {
    await toggleEventScheduleAllow(eventId, scheduleId, val)
    ElMessage.success(val === 1 ? '已开放该轮次报名' : '已关闭该轮次报名')
    allowRegisterMap.value[eventId] = val
  } catch {
    // 失败时回滚 UI
    allowRegisterMap.value[eventId] = val === 1 ? 0 : 1
    ElMessage.error('操作失败')
  }
}
```

- [ ] **Step 5: ScheduleDetailView.vue 改「开放报名」列为开关**

把模板里的「开放报名」列（约 179-181 行）：

```html
        <el-table-column label="开放报名" width="80">
          <template #default="{ row }">{{ row.allowRegister === 1 ? '是' : '否' }}</template>
        </el-table-column>
```

替换为：

```html
        <el-table-column label="开放报名" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="allowRegisterMap[row.id] === 1"
              :disabled="row.allowRegister === 0"
              @update:model-value="(v: boolean) => handleToggleAllow(row.id, v ? 1 : 0)"
            />
          </template>
        </el-table-column>
```

> 说明：`row.allowRegister` 是项目总闸（event.allow_register）。总闸关（仅晋级项目）时开关禁用。`allowRegisterMap[row.id]` 才是该轮次的实际开关。

- [ ] **Step 6: 类型检查 + 构建**

```bash
cd webManageProject && npm run build
```

Expected: 构建成功，无 TypeScript 报错。

- [ ] **Step 7: 前端手动联调**

启动后端 + `npm run dev`。进入某轮次详情页（`/meeting/:id/schedule/:scheduleId`）：
- 列表「开放报名」列显示为 switch。
- 总闸开的项目：switch 可切换，切换后刷新仍保持。
- 总闸关（仅晋级）的项目：switch 禁用。

- [ ] **Step 8: Commit**

```bash
git add webManageProject/src/api/eventSchedule.ts \
        webManageProject/src/api/event.ts \
        webManageProject/src/views/ScheduleDetailView.vue
git commit -m "feat: 轮次详情页开放报名列改为可切换开关"
```

---

## Task 9: 小程序报名页过滤

**Files:**
- Modify: `sportdocu/pages/register/register.js`

- [ ] **Step 1: 过滤条件加 currentOpenScheduleId**

修改 [register.js:22](../../../sportdocu/pages/register/register.js) 的过滤：

把：
```javascript
          const events = (res.data.data || []).filter(e => e.allowRegister)
```

改为：
```javascript
          const events = (res.data.data || []).filter(e => e.allowRegister && e.currentOpenScheduleId)
```

- [ ] **Step 2: 手动验证**

微信开发者工具打开小程序，进入某运动会报名页：
- 总闸开且某轮次开放的项目：正常显示。
- 总闸开但所有轮次 allow_register=0 的项目：不再显示。

验证方式：在后端把某项目所有轮次 allow_register 置 0，下拉刷新报名页，确认该项目消失。

- [ ] **Step 3: Commit**

```bash
git add sportdocu/pages/register/register.js
git commit -m "feat: 小程序报名页按 currentOpenScheduleId 过滤可报名项目"
```

---

## Task 10: 端到端联调

- [ ] **Step 1: 完整流程验证**

在管理端某运动会下，确保有 3 个轮次（预赛/复赛/决赛，sort 升序）：

1. **新增项目**：新建「100米」勾选预赛+复赛+决赛，allowRegister=1，保存。
   - 查库：仅预赛关联 allow_register=1，其余 0。
2. **小程序报名**：报名页能看到「100米」；报名成功，registration.schedule_id = 预赛 id。
3. **切换**：进「决赛」轮次详情页，把「100米」的开放报名开关打开；进「预赛」页可关掉（不互斥，可同时开）。
4. **再报名**：如只开决赛、关预赛，小程序报名「100米」落到 schedule_id = 决赛 id。
5. **关闭所有轮次**：报名页「100米」消失；直接调 submit 返回「该项目当前未开放报名」。
6. **编辑项目**：编辑「100米」改个项目名，保存。查库各轮次 allow_register 保持原值（不被重置）。

- [ ] **Step 2: 回归检查**

- 晋级自动报名下一轮次（`autoRegisterNextSchedule`）仍正常（未改动该逻辑）。
- 限报规则仍生效。

---

## Self-Review（计划自检）

**1. Spec 覆盖**
- 数据模型变更 → Task 1（DDL）+ Task 2（entity）
- 可报名判定（event + event_schedule 双开关）→ Task 7（submit）+ Task 9（小程序过滤）
- 新增项目自动只开最低 → Task 3（纯逻辑）+ Task 4（saveEventSchedules）
- 编辑项目增量保留 → Task 3 + Task 4（syncPlan + 增量同步）
- 轮次详情页切换 → Task 5（接口）+ Task 8（前端）
- 报名链路 currentOpenScheduleId → Task 6（装配）+ Task 7 + Task 9
- 数据迁移 → Task 1（回填）
- event.allow_register 保留总闸 → Task 8 Step 5（总闸关时禁用 switch）
- 无遗漏。

**2. 类型/命名一致性**
- `updateAllowRegister(eventId, scheduleId, allowRegister)`：Service（Task 4 Step 1）↔ Controller（Task 5）↔ Mapper（Task 2）签名一致。
- `getOpenScheduleIdByEventId`：Service ↔ Controller（Task 6）↔ Mapper（Task 2 `selectOpenScheduleIdByEventId`）一致。
- `currentOpenScheduleId`：Event.java（Task 6）↔ 前端 api/event.ts（Task 8）↔ register.js（Task 9）一致。
- `syncPlanForTest`：测试（Task 3）↔ Impl（Task 3/Task 4）一致。
- `batchInsertWithAllow`：Mapper 接口 ↔ XML（Task 2）↔ Impl 调用（Task 4）一致。

**3. 占位符扫描**：无 TBD/TODO；每个代码步骤含完整代码。

**4. 已知风险点**
- Task 4 Step 3 的 `selectScheduleIdsByEventIdOrdered` 用 `IN (...)` 排序，scheduleIds 非空（调用前已判空），foreach 安全。
- `event_schedule.sql` 的 UNIQUE KEY `(event_id, schedule_id)` 与软删除共存：历史软删除行可能冲突新插入。本计划沿用现有「软删除」模式未改，若 Task 4 验证时遇到唯一键冲突，需排查历史脏数据（属既有问题，非本次引入）。
