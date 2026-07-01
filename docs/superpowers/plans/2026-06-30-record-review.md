# 破纪录候选标记 + 人工审核入册 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让成绩录入后赛次前 3 名自动标三角形「候选破纪录」,管理员在独立审核页逐条审核,通过则入册 record(历届档案,自动拼字段),拒绝则标记不再提示。

**Architecture:** `result` 表加 `record_status`(0/1/2)记审核结果;`record` 表加 `sports_meeting_id`/`category`/`score_value`/`result_id` 字段并删 `venue`,从「人工档案」升级为「历届档案」;三角形靠 rankMap 即时算前 3(不存候选状态);审核 endpoint `POST /api/admin/record/review` 在单事务内改 result.status + insert record;score 换算(毫秒→秒、厘米→米)抽静态方法做纯逻辑单测。

**Tech Stack:** Spring Boot + MyBatis(XML mapper, `map-underscore-to-camel-case`) + MySQL 8;Vue 3 + TypeScript + Element Plus;JUnit 5。

**Spec:** [docs/superpowers/specs/2026-06-30-record-review-design.md](../specs/2026-06-30-record-review-design.md)

---

## 项目惯例速查(实施前必读)

- **包结构**:`Controller/admin/Admin*Controller.java`(路径前缀 `/api/admin/{module}`) / `Service/` / `Service/Impl/` / `Mapper/` / `entity/` / `common/Result.java`。
- **响应封装**:Controller 返回 `Result.success(data)`;Service 抛 `RuntimeException("原因")` → `GlobalExceptionHandler` 转 HTTP 400。
- **MyBatis**:`map-underscore-to-camel-case: true`(DB `snake_case` ↔ 实体 `camelCase`);XML 在 `SportBackend/src/main/resources/mapper/`。
- **测试惯例**:只测**纯逻辑静态方法**(参考 `EventScheduleServiceImplTest` 测 `computeSyncPlan`、`TeamScoreAdjustmentServiceImplTest` 测 `validateAdd`)。**不测 Mapper SQL**(无 `@MybatisTest`/`@SpringBootTest` 真断言基础设施)。
- **DDL 同步约定**(CLAUDE.md):DDL 执行后必须同步 `SportBackend/sql/` 下脚本。
- **执行任何 DDL/DML 前必须向用户确认**。
- **前端 API**:`src/api/*.ts` 用 `request` 实例(baseURL `/api`),前端写 `/admin/...`、后端写 `/api/admin/...`。

---

## File Structure

### 新建(2 个文件)

| 文件 | 职责 |
|------|------|
| `webManageProject/src/views/RecordReviewView.vue` | 破纪录审核页(筛选 + 候选列表 + 通过/拒绝) |
| `SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/RecordServiceImplTest.java` | score 换算 + 审核参数校验纯逻辑单测 |

### 修改(13 个文件)

| 文件 | 改动 |
|------|------|
| `SportBackend/sql/result.sql` | 加 `record_status` 字段 |
| `SportBackend/sql/record.sql` | 加 4 字段(sports_meeting_id/category/score_value/result_id)、删 venue |
| `SportBackend/src/main/java/com/dlust/sportbackend/entity/Result.java` | 加 `recordStatus` |
| `SportBackend/src/main/java/com/dlust/sportbackend/entity/ResultVO.java` | 加 `recordStatus` |
| `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/ResultMapper.java` | 加 `selectById` + `updateRecordStatus` |
| `SportBackend/src/main/resources/mapper/ResultMapper.xml` | 3 个 selectVO 加 `r.record_status`;加 `selectById` + `updateRecordStatus` |
| `SportBackend/src/main/java/com/dlust/sportbackend/entity/Record.java` | 加 4 字段、删 venue |
| `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/RecordMapper.java` | `selectAll` 签名加 3 个筛选参数 |
| `SportBackend/src/main/resources/mapper/RecordMapper.xml` | selectAll 加 `<where>`;insert/update 含新字段去 venue |
| `SportBackend/src/main/java/com/dlust/sportbackend/Service/RecordService.java` | `getAll` 签名加筛选;加 `reviewRecord` |
| `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/RecordServiceImpl.java` | getAll 实现;reviewRecord(`@Transactional`)+ 注入多 Mapper + convertScoreValueToScore 静态 |
| `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminRecordController.java` | `/list` 加筛选参数;加 `POST /review` |
| `webManageProject/src/api/result.ts` | `ResultVO` 加 `recordStatus` |
| `webManageProject/src/api/record.ts` | `Record` 接口字段调整;加 `reviewRecord`;`getRecordList` 加 params |
| `webManageProject/src/views/EventDetailView.vue` | 成绩列加三角形(前 3 三色) |
| `webManageProject/src/views/RecordView.vue` | 去 venue;加类别/运动会列;弹窗 score_value 只读 |
| `webManageProject/src/router/index.ts` | 加 `/record-review` 路由 |
| `webManageProject/src/layouts/DefaultLayout.vue` | 侧边栏加「破纪录审核」菜单项 |

---

## Task 1: DDL + SQL 脚本同步

**Files:**
- Modify: `SportBackend/sql/result.sql`
- Modify: `SportBackend/sql/record.sql`

- [ ] **Step 1: 改 result.sql 加 record_status**

把 `SportBackend/sql/result.sql` 的 `points` 行后加 `record_status`:

```sql
-- 成绩表
CREATE TABLE IF NOT EXISTS result (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成绩ID',
    sports_meeting_id BIGINT       NOT NULL COMMENT '所属运动会ID',
    event_id          BIGINT       NOT NULL COMMENT '比赛项目ID（关联event表）',
    event_schedule_id BIGINT       DEFAULT NULL COMMENT '项目-赛程关联ID（关联event_schedule.id）',
    participant_id    BIGINT       NOT NULL COMMENT '参赛人员ID（关联participant表）',
    score_value       INT          DEFAULT NULL COMMENT '成绩值（径赛=毫秒数，田赛=厘米数，按event.category区分）',
    points            INT          DEFAULT 0 COMMENT '积分（用于代表队总分计算）',
    record_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '破纪录审核状态（0=未审，1=通过已入册，2=拒绝）',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';
```

- [ ] **Step 2: 改 record.sql 加 4 字段、删 venue**

把 `SportBackend/sql/record.sql` 整体替换为:

```sql
-- 运动会记录表（历届档案）
CREATE TABLE IF NOT EXISTS record (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    sports_meeting_id BIGINT       DEFAULT NULL COMMENT '所属运动会ID',
    group_type        VARCHAR(50)  DEFAULT NULL COMMENT '组别',
    event_name        VARCHAR(100) DEFAULT NULL COMMENT '项目',
    category          VARCHAR(20)  DEFAULT NULL COMMENT '项目类别（径赛/田赛/团队赛）',
    unit              VARCHAR(100) DEFAULT NULL COMMENT '单位（代表队）',
    name              VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
    score             DECIMAL(10,2) DEFAULT NULL COMMENT '成绩（人类可读：径赛秒，田赛米）',
    score_value       INT          DEFAULT NULL COMMENT '成绩值（径赛=毫秒，田赛=厘米，对齐result.score_value）',
    result_id         BIGINT       DEFAULT NULL COMMENT '源成绩ID（审核通过时追溯）',
    record_time       DATETIME     DEFAULT NULL COMMENT '时间',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动会记录表（历届档案）';
```

- [ ] **Step 3: Commit**

```bash
cd c:/Users/dtsud/Desktop/sportProject
git add SportBackend/sql/result.sql SportBackend/sql/record.sql
git commit -m "feat: DDL 脚本——result 加 record_status，record 加 4 字段删 venue"
```

> DDL 执行推迟到 Task 9 端到端验证前由 controller 向用户确认。

---

## Task 2: 后端 result 模块改动(实体 + Mapper)

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/Result.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/ResultVO.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/ResultMapper.java`
- Modify: `SportBackend/src/main/resources/mapper/ResultMapper.xml`

- [ ] **Step 1: Result.java 加 recordStatus**

在 `Result.java` 的 `points` 字段后加:

```java
    private Integer recordStatus;
```

- [ ] **Step 2: ResultVO.java 加 recordStatus**

在 `ResultVO.java` 的 `private Integer points;` 后加:

```java
    // 破纪录审核状态：0=未审，1=通过已入册，2=拒绝
    private Integer recordStatus;
```

- [ ] **Step 3: ResultMapper.java 加 selectById + updateRecordStatus**

在 `ResultMapper.java` 的 `void insert(Result result);` 之前加:

```java
    Result selectById(@Param("id") Long id);

    int updateRecordStatus(@Param("id") Long id, @Param("status") Integer status);
```

- [ ] **Step 4: ResultMapper.xml 三个 selectVO 加 r.record_status**

三个 selectVO(`selectVOBySportsMeetingId`、`selectVOByEventId`、`selectVOByEventAndSchedule`)的 SELECT 列表里,把:

```
               r.participant_id, r.score_value, r.points, r.create_time, r.update_time,
```

改为:

```
               r.participant_id, r.score_value, r.points, r.record_status, r.create_time, r.update_time,
```

(三处都改,加 `r.record_status,`)

- [ ] **Step 5: ResultMapper.xml 加 selectById + updateRecordStatus**

在 `<insert id="insert">` 之前加:

```xml
    <select id="selectById" resultType="Result">
        SELECT * FROM result WHERE id = #{id}
    </select>

    <update id="updateRecordStatus">
        UPDATE result SET record_status = #{status} WHERE id = #{id}
    </update>
```

- [ ] **Step 6: 编译验证**

Run: `cd SportBackend && mvn -q compile`
Expected: 无报错。

- [ ] **Step 7: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/entity/Result.java \
        SportBackend/src/main/java/com/dlust/sportbackend/entity/ResultVO.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Mapper/ResultMapper.java \
        SportBackend/src/main/resources/mapper/ResultMapper.xml
git commit -m "feat: Result/ResultVO 加 recordStatus + ResultMapper selectById/updateRecordStatus"
```

---

## Task 3: 后端 record 模块改动(实体 + Mapper + Service.getAll + Controller /list)

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/Record.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/RecordMapper.java`
- Modify: `SportBackend/src/main/resources/mapper/RecordMapper.xml`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/RecordService.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/RecordServiceImpl.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminRecordController.java`

- [ ] **Step 1: Record.java 加 4 字段删 venue**

把 `Record.java` 整体替换为:

```java
package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Record {
    private Long id;
    private Long sportsMeetingId;
    private String groupType;
    private String eventName;
    private String category;
    private String unit;
    private String name;
    private BigDecimal score;
    private Integer scoreValue;
    private Long resultId;
    private LocalDateTime recordTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

(去掉 venue,加 sportsMeetingId/category/scoreValue/resultId)

- [ ] **Step 2: RecordMapper.java selectAll 签名加筛选参数**

把 `RecordMapper.java` 的 `List<Record> selectAll();` 改为:

```java
    List<Record> selectAll(@Param("sportsMeetingId") Long sportsMeetingId,
                           @Param("eventName") String eventName,
                           @Param("category") String category);
```

- [ ] **Step 3: RecordMapper.xml selectAll 加 where + insert/update 改字段**

把 `RecordMapper.xml` 的 `selectAll`、`insert`、`updateById` 替换为(其他不变):

```xml
    <select id="selectAll" resultType="Record">
        SELECT * FROM record
        <where>
            <if test="sportsMeetingId != null">AND sports_meeting_id = #{sportsMeetingId}</if>
            <if test="eventName != null and eventName != ''">AND event_name = #{eventName}</if>
            <if test="category != null and category != ''">AND category = #{category}</if>
        </where>
        ORDER BY record_time DESC, create_time DESC
    </select>
```

```xml
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO record (sports_meeting_id, group_type, event_name, category, unit, name, score, score_value, result_id, record_time)
        VALUES (#{sportsMeetingId}, #{groupType}, #{eventName}, #{category}, #{unit}, #{name}, #{score}, #{scoreValue}, #{resultId}, #{recordTime})
    </insert>
```

```xml
    <update id="updateById">
        UPDATE record
        <set>
            <if test="sportsMeetingId != null">sports_meeting_id = #{sportsMeetingId},</if>
            <if test="groupType != null">group_type = #{groupType},</if>
            <if test="eventName != null">event_name = #{eventName},</if>
            <if test="category != null">category = #{category},</if>
            <if test="unit != null">unit = #{unit},</if>
            <if test="name != null">name = #{name},</if>
            <if test="score != null">score = #{score},</if>
            <if test="scoreValue != null">score_value = #{scoreValue},</if>
            <if test="resultId != null">result_id = #{resultId},</if>
            <if test="recordTime != null">record_time = #{recordTime},</if>
        </set>
        WHERE id = #{id}
    </update>
```

(去 venue,加新字段)

- [ ] **Step 4: RecordService.java getAll 签名加筛选**

把 `RecordService.java` 的 `List<Record> getAll();` 改为:

```java
    List<Record> getAll(Long sportsMeetingId, String eventName, String category);
```

- [ ] **Step 5: RecordServiceImpl.java getAll 实现同步**

把 `RecordServiceImpl.java` 的 `getAll` 方法改为:

```java
    @Override
    public List<Record> getAll(Long sportsMeetingId, String eventName, String category) {
        return recordMapper.selectAll(sportsMeetingId, eventName, category);
    }
```

- [ ] **Step 6: AdminRecordController.java /list 加筛选参数**

把 `AdminRecordController.java` 的 `list` 方法改为:

```java
    @GetMapping("/list")
    public Result<List<Record>> list(@RequestParam(required = false) Long sportsMeetingId,
                                     @RequestParam(required = false) String eventName,
                                     @RequestParam(required = false) String category) {
        log.info("查询记录: sportsMeetingId={}, eventName={}, category={}", sportsMeetingId, eventName, category);
        return Result.success(recordService.getAll(sportsMeetingId, eventName, category));
    }
```

- [ ] **Step 7: 编译验证**

Run: `cd SportBackend && mvn -q compile`
Expected: 无报错。

- [ ] **Step 8: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/entity/Record.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Mapper/RecordMapper.java \
        SportBackend/src/main/resources/mapper/RecordMapper.xml \
        SportBackend/src/main/java/com/dlust/sportbackend/Service/RecordService.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/RecordServiceImpl.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminRecordController.java
git commit -m "feat: Record 实体加 4 字段删 venue + Mapper/Service/Controller list 加筛选"
```

---

## Task 4: 后端审核功能(reviewRecord + score 换算 + 单测 + /review endpoint)

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/RecordService.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/RecordServiceImpl.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminRecordController.java`
- Create: `SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/RecordServiceImplTest.java`

- [ ] **Step 1: RecordService.java 加 reviewRecord 方法签名**

在 `RecordService.java` 接口末尾 `}` 之前加:

```java
    void reviewRecord(Long resultId, String action);
```

- [ ] **Step 2: RecordServiceImpl.java 注入多 Mapper + reviewRecord + convertScoreValueToScore + validateReviewAction

把 `RecordServiceImpl.java` 整体替换为:

```java
package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.*;
import com.dlust.sportbackend.Service.RecordService;
import com.dlust.sportbackend.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RecordServiceImpl implements RecordService {

    @Autowired
    private RecordMapper recordMapper;
    @Autowired
    private ResultMapper resultMapper;
    @Autowired
    private EventMapper eventMapper;
    @Autowired
    private ParticipantMapper participantMapper;
    @Autowired
    private TeamMapper teamMapper;
    @Autowired
    private GroupTypeMapper groupTypeMapper;

    @Override
    public List<Record> getAll(Long sportsMeetingId, String eventName, String category) {
        return recordMapper.selectAll(sportsMeetingId, eventName, category);
    }

    @Override
    @Transactional
    public void reviewRecord(Long resultId, String action) {
        validateReviewAction(resultId, action);
        Result result = resultMapper.selectById(resultId);
        if (result == null) {
            throw new RuntimeException("成绩不存在");
        }
        if (result.getRecordStatus() != null && result.getRecordStatus() != 0) {
            throw new RuntimeException("该成绩已审核");
        }
        int status = "approve".equals(action) ? 1 : 2;
        resultMapper.updateRecordStatus(resultId, status);
        if (status == 1) {
            // 拼装 record 入册
            Event event = eventMapper.selectById(result.getEventId());
            Participant participant = participantMapper.selectById(result.getParticipantId());
            Team team = participant != null && participant.getTeamId() != null
                    ? teamMapper.selectById(participant.getTeamId()) : null;
            GroupType groupType = event != null && event.getGroupTypeId() != null
                    ? groupTypeMapper.selectById(event.getGroupTypeId()) : null;

            Record record = new Record();
            record.setSportsMeetingId(result.getSportsMeetingId());
            record.setCategory(event != null ? event.getCategory() : null);
            record.setScoreValue(result.getScoreValue());
            record.setScore(convertScoreValueToScore(result.getScoreValue(), event != null ? event.getCategory() : null));
            record.setResultId(result.getId());
            record.setEventName(event != null ? event.getName() : null);
            record.setGroupType(groupType != null ? groupType.getName() : null);
            record.setUnit(team != null ? team.getName() : null);
            record.setName(participant != null ? participant.getName() : null);
            record.setRecordTime(result.getCreateTime());
            recordMapper.insert(record);
        }
    }

    /**
     * 将 result.score_value（INT 内部单位）换算为 record.score（DECIMAL 人类可读）。
     * 径赛/团队赛: 毫秒 → 秒（÷1000）
     * 田赛: 厘米 → 米（÷100）
     * category 为 null 时默认按径赛算。
     */
    static BigDecimal convertScoreValueToScore(Integer scoreValue, String category) {
        if (scoreValue == null) return null;
        if ("田赛".equals(category)) {
            return BigDecimal.valueOf(scoreValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(scoreValue).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
    }

    /**
     * 审核参数校验（抽静态以便单测）。
     */
    static void validateReviewAction(Long resultId, String action) {
        if (resultId == null) {
            throw new RuntimeException("成绩 ID 不能为空");
        }
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new RuntimeException("审核动作非法（必须是 approve 或 reject）");
        }
    }
}
```

- [ ] **Step 3: AdminRecordController.java 加 POST /review**

在 `AdminRecordController.java` 的 `delete` 方法后、类 `}` 之前加:

```java
    @PostMapping("/review")
    public Result<String> review(@RequestBody java.util.Map<String, Object> body) {
        Long resultId = body.get("resultId") == null ? null : Long.valueOf(body.get("resultId").toString());
        String action = body.get("action") == null ? null : body.get("action").toString();
        log.info("审核破纪录候选: resultId={}, action={}", resultId, action);
        recordService.reviewRecord(resultId, action);
        return Result.success("审核成功");
    }
```

- [ ] **Step 4: 写纯逻辑单测**

创建 `SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/RecordServiceImplTest.java`:

```java
package com.dlust.sportbackend.Service.Impl;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RecordServiceImplTest {

    @Test
    void convertScoreValueToScore_trackSeconds() {
        // 径赛 10250 毫秒 → 10.25 秒
        assertEquals(new BigDecimal("10.25"),
                RecordServiceImpl.convertScoreValueToScore(10250, "径赛"));
    }

    @Test
    void convertScoreValueToScore_fieldMeters() {
        // 田赛 680 厘米 → 6.80 米
        assertEquals(new BigDecimal("6.80"),
                RecordServiceImpl.convertScoreValueToScore(680, "田赛"));
    }

    @Test
    void convertScoreValueToScore_teamEvent() {
        // 团队赛 70000 毫秒 → 70.00 秒（同径赛逻辑）
        assertEquals(new BigDecimal("70.00"),
                RecordServiceImpl.convertScoreValueToScore(70000, "团队赛"));
    }

    @Test
    void convertScoreValueToScore_nullScoreValue() {
        assertNull(RecordServiceImpl.convertScoreValueToScore(null, "径赛"));
    }

    @Test
    void convertScoreValueToScore_nullCategoryDefaultsTrack() {
        // category null 默认按径赛（÷1000）
        assertEquals(new BigDecimal("10.25"),
                RecordServiceImpl.convertScoreValueToScore(10250, null));
    }

    @Test
    void validateReviewAction_nullResultId_throws() {
        assertThrows(RuntimeException.class, () ->
                RecordServiceImpl.validateReviewAction(null, "approve"));
    }

    @Test
    void validateReviewAction_invalidAction_throws() {
        assertThrows(RuntimeException.class, () ->
                RecordServiceImpl.validateReviewAction(1L, "maybe"));
    }

    @Test
    void validateReviewAction_approveOk() {
        assertDoesNotThrow(() ->
                RecordServiceImpl.validateReviewAction(1L, "approve"));
    }

    @Test
    void validateReviewAction_rejectOk() {
        assertDoesNotThrow(() ->
                RecordServiceImpl.validateReviewAction(1L, "reject"));
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd SportBackend && mvn -q test -Dtest=RecordServiceImplTest`
Expected: 9 tests pass.

- [ ] **Step 6: 全测试验证**

Run: `cd SportBackend && mvn -q test`
Expected: 全绿(含 RecordServiceImplTest 9 个 + TeamScoreAdjustmentServiceImplTest 5 个 + EventScheduleServiceImplTest 6 个 + BannerTest 1 + SportBackendApplicationTests 1)。

- [ ] **Step 7: Commit**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Service/RecordService.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/RecordServiceImpl.java \
        SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminRecordController.java \
        SportBackend/src/test/java/com/dlust/sportbackend/Service/Impl/RecordServiceImplTest.java
git commit -m "feat: RecordService.reviewRecord（@Transactional 审核+入册）+ score 换算纯逻辑单测"
```

---

## Task 5: 前端 API 文件

**Files:**
- Modify: `webManageProject/src/api/result.ts`
- Modify: `webManageProject/src/api/record.ts`

- [ ] **Step 1: result.ts ResultVO 加 recordStatus**

在 `webManageProject/src/api/result.ts` 的 `ResultVO` 接口里(在 `points` 后)加:

```ts
  recordStatus: number
```

> 实际接口字段顺序参考现有文件;字段加在 `points` 之后即可。

- [ ] **Step 2: record.ts 字段调整 + reviewRecord + getRecordList 加 params**

把 `webManageProject/src/api/record.ts` 整体替换为:

```ts
import request from '@/utils/request'

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

export function getRecordList(params?: { sportsMeetingId?: number; eventName?: string; category?: string }) {
  return request.get('/admin/record/list', { params })
}

export function addRecord(data: Partial<Record>) {
  return request.post('/admin/record/add', data)
}

export function updateRecord(data: Partial<Record>) {
  return request.put('/admin/record/update', data)
}

export function deleteRecord(id: number) {
  return request.delete(`/admin/record/${id}`)
}

export function reviewRecord(resultId: number, action: 'approve' | 'reject') {
  return request.post('/admin/record/review', { resultId, action })
}
```

- [ ] **Step 3: 构建验证**

Run: `cd webManageProject && npm run build`
Expected: `✓ built in ...`(无 TS 错误)。

- [ ] **Step 4: Commit**

```bash
git add webManageProject/src/api/result.ts webManageProject/src/api/record.ts
git commit -m "feat: 前端 API——result.ts 加 recordStatus + record.ts 字段调整/reviewRecord"
```

---

## Task 6: 前端 EventDetailView 三角形标记

**Files:**
- Modify: `webManageProject/src/views/EventDetailView.vue`

> 当前成绩列模板(在 `<el-table-column label="成绩" width="110">` 的 `#default` 里)只有一个 `<el-button>` 调 `openScoreEdit`。`rankMap`(line 137-143)给名次,`resultMap` 给 ResultVO(含 recordStatus)。先 Read 确认实际行号。

- [ ] **Step 1: 加辅助函数 recordFlagClass + recordTooltip**

在 `EventDetailView.vue` 的 `formatScore` 函数附近加:

```ts
function recordFlagClass(r?: ResultVO): string {
  if (!r) return ''
  if (r.recordStatus === 1) return 'record-flag-approved'
  if (r.recordStatus === 2) return 'record-flag-rejected'
  return 'record-flag-pending'
}
function recordTooltip(r?: ResultVO): string {
  if (!r) return ''
  if (r.recordStatus === 1) return '已通过审核，已入册校运会纪录'
  if (r.recordStatus === 2) return '已拒绝（非破纪录）'
  return '破纪录候选（赛次前 3），待审核'
}
```

- [ ] **Step 2: 成绩列加三角形**

把成绩列的 `#default`:

```html
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openScoreEdit(row)">
              {{ formatScore(resultMap.get(row.participantId)) }}
            </el-button>
          </template>
```

改为:

```html
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openScoreEdit(row)">
              {{ formatScore(resultMap.get(row.participantId)) }}
            </el-button>
            <el-tooltip
              v-if="rankMap.get(row.participantId) != null && rankMap.get(row.participantId)! <= 3"
              :content="recordTooltip(resultMap.get(row.participantId))">
              <span :class="['record-flag', recordFlagClass(resultMap.get(row.participantId))]">▲</span>
            </el-tooltip>
          </template>
```

- [ ] **Step 3: 加三角形 CSS**

在 `EventDetailView.vue` 的 `<style scoped>` 区(任意合适位置,建议 `.event-detail-page` 之后)加:

```css
.record-flag { margin-left: 4px; font-size: 12px; font-weight: 600; }
.record-flag-pending { color: #e6a23c; }   /* 金黄：待审 */
.record-flag-approved { color: #67c23a; }  /* 绿：已通过入册 */
.record-flag-rejected { color: #c0c4cc; }  /* 灰：已拒绝 */
```

- [ ] **Step 4: 构建验证**

Run: `cd webManageProject && npm run build`
Expected: `✓ built in ...`。

- [ ] **Step 5: Commit**

```bash
git add webManageProject/src/views/EventDetailView.vue
git commit -m "feat: EventDetailView 成绩列加三角形（赛次前3 三色标记）"
```

---

## Task 7: 前端 RecordReviewView 审核页 + 路由 + 菜单

**Files:**
- Create: `webManageProject/src/views/RecordReviewView.vue`
- Modify: `webManageProject/src/router/index.ts`
- Modify: `webManageProject/src/layouts/DefaultLayout.vue`

- [ ] **Step 1: 新建 RecordReviewView.vue**

创建 `webManageProject/src/views/RecordReviewView.vue`:

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMeetingList } from '@/api/meeting'
import type { SportsMeeting } from '@/api/meeting'
import { getEventList } from '@/api/event'
import type { Event } from '@/api/event'
import { getRegistrationListByEvent } from '@/api/registration'
import { getResultsByEventAndSchedule } from '@/api/result'
import type { ResultVO } from '@/api/result'
import { reviewRecord } from '@/api/record'

const meetings = ref<SportsMeeting[]>([])
const events = ref<Event[]>([])
const schedules = ref<{ id: number; name: string }[]>([])
const results = ref<ResultVO[]>([])

const meetingId = ref<number | undefined>(undefined)
const eventId = ref<number | undefined>(undefined)
const scheduleId = ref<number | undefined>(undefined)
const statusFilter = ref<'all' | 0 | 1 | 2>('all')

async function fetchMeetings() {
  try {
    const res: any = await getMeetingList()
    meetings.value = res.data || res || []
    if (meetings.value.length > 0) {
      meetingId.value = meetings.value[0].id
      fetchEvents()
    }
  } catch { ElMessage.error('加载运动会失败') }
}

async function fetchEvents() {
  if (!meetingId.value) return
  eventId.value = undefined
  scheduleId.value = undefined
  schedules.value = []
  results.value = []
  try {
    const res: any = await getEventList({ sportsMeetingId: meetingId.value })
    events.value = res.data || res || []
  } catch { events.value = [] }
}

async function fetchSchedules() {
  if (!eventId.value) return
  scheduleId.value = undefined
  results.value = []
  try {
    const res: any = await getRegistrationListByEvent(eventId.value)
    const regs = res.data || res || []
    const map = new Map<number, string>()
    regs.forEach((r: any) => {
      if (r.scheduleId && !map.has(r.scheduleId)) map.set(r.scheduleId, r.scheduleName || `赛次${r.scheduleId}`)
    })
    schedules.value = [...map.entries()].map(([id, name]) => ({ id, name }))
  } catch { schedules.value = [] }
}

async function fetchResults() {
  if (!eventId.value || !scheduleId.value) return
  try {
    const res: any = await getResultsByEventAndSchedule(eventId.value, scheduleId.value)
    results.value = res.data || res || []
  } catch { results.value = [] }
}

const category = computed(() => {
  const ev = events.value.find(e => e.id === eventId.value)
  return ev?.category || ''
})

// 前 3 名 id 集合（results 已按成绩排序，取前 3 有成绩的）
const top3Ids = computed(() =>
  results.value.filter(r => r.scoreValue != null).slice(0, 3).map(r => r.id)
)

const filteredResults = computed(() => {
  if (statusFilter.value === 'all') return results.value
  return results.value.filter(r => (r.recordStatus ?? 0) === statusFilter.value)
})

function isTop3(id: number) {
  return top3Ids.value.includes(id)
}

function formatScore(r?: ResultVO): string {
  if (!r || r.scoreValue == null) return '-'
  if (category.value === '田赛') return `${(r.scoreValue / 100).toFixed(2)}米`
  const totalMs = r.scoreValue
  const totalSeconds = Math.floor(totalMs / 1000)
  const ms = totalMs % 1000
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  if (minutes > 0) return `${minutes}:${String(seconds).padStart(2, '0')}.${String(ms).padStart(3, '0')}`
  return `${seconds}.${String(ms).padStart(3, '0')}秒`
}

const statusMap: Record<number, string> = { 0: '待审', 1: '已通过', 2: '已拒绝' }
const statusType: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }

async function handleReview(id: number, action: 'approve' | 'reject') {
  try {
    await reviewRecord(id, action)
    ElMessage.success(action === 'approve' ? '已通过并入册' : '已拒绝')
    await fetchResults()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  }
}

onMounted(fetchMeetings)
</script>

<template>
  <div class="record-review-page">
    <div class="content-card">
      <div class="tab-toolbar">
        <div class="tab-toolbar-left">
          <el-select v-model="meetingId" placeholder="运动会" style="width:180px" @change="fetchEvents">
            <el-option v-for="m in meetings" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
          <el-select v-model="eventId" placeholder="项目" style="width:160px" @change="fetchSchedules">
            <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
          <el-select v-model="scheduleId" placeholder="赛次" style="width:120px" @change="fetchResults">
            <el-option v-for="s in schedules" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-select v-model="statusFilter" style="width:110px">
            <el-option label="全部状态" value="all" />
            <el-option label="待审" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </div>
        <span class="toolbar-hint">前 3 名自动标三角形候选；通过即入册校运会纪录档案</span>
      </div>

      <el-table v-if="eventId && scheduleId" :data="filteredResults" stripe border size="small">
        <el-table-column label="" width="40" align="center">
          <template #default="{ row }">
            <el-tooltip
              v-if="isTop3(row.id)"
              :content="row.recordStatus === 1 ? '已通过入册' : row.recordStatus === 2 ? '已拒绝' : '破纪录候选（赛次前3）'">
              <span :class="['record-flag',
                row.recordStatus === 1 ? 'record-flag-approved' :
                row.recordStatus === 2 ? 'record-flag-rejected' : 'record-flag-pending']">▲</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="participantName" label="参赛者" width="120" />
        <el-table-column prop="eventName" label="项目" />
        <el-table-column prop="scheduleName" label="赛次" width="90" />
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">{{ formatScore(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType[row.recordStatus ?? 0] as any" size="small">
              {{ statusMap[row.recordStatus ?? 0] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="(row.recordStatus ?? 0) === 0">
              <el-button link type="success" size="small" @click="handleReview(row.id, 'approve')">通过</el-button>
              <el-button link type="danger" size="small" @click="handleReview(row.id, 'reject')">拒绝</el-button>
            </template>
            <span v-else style="color:#999;font-size:12px">已审核</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="请选择项目和赛次查看候选" />
    </div>
  </div>
</template>

<style scoped>
.record-review-page { padding: 20px; }
.content-card { background:#fff; border-radius:8px; padding:16px; box-shadow:0 1px 3px rgba(0,0,0,0.08); }
.tab-toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.tab-toolbar-left { display:flex; gap:8px; }
.toolbar-hint { color:#999; font-size:12px; }
.record-flag { font-size:14px; font-weight:600; }
.record-flag-pending { color: #e6a23c; }
.record-flag-approved { color: #67c23a; }
.record-flag-rejected { color: #c0c4cc; }
</style>
```

- [ ] **Step 2: router/index.ts 加 /record-review 路由**

在 `router/index.ts` 的 `record` 路由(line 47-52)之后加:

```ts
      {
        path: 'record-review',
        name: 'RecordReview',
        component: () => import('@/views/RecordReviewView.vue'),
        meta: { title: '破纪录审核' },
      },
```

- [ ] **Step 3: DefaultLayout.vue 侧边栏加菜单项**

在 `DefaultLayout.vue` 的 `<el-menu-item index="/record">...</el-menu-item>` 之后加:

```html
        <el-menu-item index="/record-review">
          <span>破纪录审核</span>
        </el-menu-item>
```

- [ ] **Step 4: 构建验证**

Run: `cd webManageProject && npm run build`
Expected: `✓ built in ...`。

- [ ] **Step 5: Commit**

```bash
git add webManageProject/src/views/RecordReviewView.vue \
        webManageProject/src/router/index.ts \
        webManageProject/src/layouts/DefaultLayout.vue
git commit -m "feat: 新增「破纪录审核」页（筛选+候选列表+通过/拒绝）+ 路由菜单"
```

---

## Task 8: 前端 RecordView 字段调整

**Files:**
- Modify: `webManageProject/src/views/RecordView.vue`

> 当前 RecordView.vue 表格列:组别/项目/单位/姓名/成绩/时间/地点/操作。弹窗字段:组别/项目/单位/姓名/成绩/时间/地点。先 Read 确认。

- [ ] **Step 1: 表格去 venue 列、加类别列**

把 `<el-table-column prop="venue" label="地点" />` 删除。

在 `<el-table-column prop="eventName" label="项目" width="120" />` 之后加:

```html
      <el-table-column prop="category" label="类别" width="90" />
```

> 列顺序建议:组别/项目/类别/单位/姓名/成绩/score_value(内部)/时间/操作。

- [ ] **Step 2: 表格加 score_value 只读展示列**

在「成绩」列(`<el-table-column label="成绩" ...>` 那个)之后加:

```html
      <el-table-column label="内部值" width="100">
        <template #default="{ row }">{{ row.scoreValue ?? '-' }}</template>
      </el-table-column>
```

- [ ] **Step 3: 弹窗去 venue 输入**

把弹窗里的:

```html
        <el-form-item label="地点">
          <el-input v-model="form.venue" />
        </el-form-item>
```

整段删除。

- [ ] **Step 4: 弹窗加类别下拉 + score_value 只读展示**

在弹窗的「项目」`<el-form-item>` 之后加:

```html
        <el-form-item label="类别">
          <el-select v-model="form.category" placeholder="选择类别" style="width:100%">
            <el-option label="径赛" value="径赛" />
            <el-option label="田赛" value="田赛" />
            <el-option label="团队赛" value="团队赛" />
          </el-select>
        </el-form-item>
```

在弹窗的「成绩」`<el-form-item>` 之后加(score_value 只读):

```html
        <el-form-item label="内部值">
          <span>{{ form.scoreValue ?? '-' }}（{{ form.scoreValue == null ? '手动档案无内部值' : '毫秒/厘米' }}）</span>
        </el-form-item>
```

- [ ] **Step 5: openAdd 默认值同步**

把 `openAdd` 函数里的默认对象:

```ts
  form.value = { groupType: '', eventName: '', unit: '', name: '', score: null, recordTime: '', venue: '' }
```

改为:

```ts
  form.value = { groupType: '', eventName: '', category: '', unit: '', name: '', score: null, scoreValue: null, recordTime: '' }
```

- [ ] **Step 6: 构建验证**

Run: `cd webManageProject && npm run build`
Expected: `✓ built in ...`。

- [ ] **Step 7: Commit**

```bash
git add webManageProject/src/views/RecordView.vue
git commit -m "feat: RecordView 去 venue、加类别列+内部值只读展示"
```

---

## Task 9: 端到端验证(DDL 执行 + 手测)

**Files:** 无(验证任务)

> **DDL 执行必须先向用户确认**(controller 负责,不在 subagent 任务内)。

- [ ] **Step 1: 向用户确认后执行 DDL**

执行(CLAUDE.md 约定,需用户确认):

```bash
mysql -h localhost -uroot -p<password> --default-character-set=utf8mb4 sport_db -e "
ALTER TABLE result ADD COLUMN record_status TINYINT NOT NULL DEFAULT 0
    COMMENT '破纪录审核状态（0=未审，1=通过已入册，2=拒绝）' AFTER points;
ALTER TABLE record
    ADD COLUMN sports_meeting_id BIGINT DEFAULT NULL COMMENT '所属运动会ID' AFTER id,
    ADD COLUMN category VARCHAR(20) DEFAULT NULL COMMENT '项目类别（径赛/田赛/团队赛）' AFTER event_name,
    ADD COLUMN score_value INT DEFAULT NULL COMMENT '成绩值（径赛=毫秒，田赛=厘米）' AFTER score,
    ADD COLUMN result_id BIGINT DEFAULT NULL COMMENT '源成绩ID' AFTER score_value;
ALTER TABLE record DROP COLUMN venue;
"
```

验证:

```bash
mysql -h localhost -uroot -p<password> sport_db -e "DESCRIBE result; DESCRIBE record;"
```

Expected: result 含 record_status;record 含新 4 字段、无 venue。

- [ ] **Step 2: 后端全测试 + 前端构建**

```bash
cd SportBackend && mvn -q test
cd ../webManageProject && npm run build
```

Expected: 后端全绿,前端 `✓ built in ...`。

- [ ] **Step 3: 启动服务手测**

```bash
# 终端 1
cd SportBackend && mvn spring-boot:run
# 终端 2
cd webManageProject && npm run dev
```

手测清单(打开 `http://localhost:5173`):

1. 进入某项目详情页(EventDetailView),录几条成绩(含前 3 名)→ 前 3 名成绩旁出现**金黄三角形** ▲,hover 显示「破纪录候选(赛次前3),待审核」。
2. 侧边栏点「**破纪录审核**」→ 进入审核页。
3. 选运动会/项目/赛次 → 列表显示该赛次成绩,前 3 名有金黄三角形 + 「通过/拒绝」按钮。
4. 点「通过」→ toast「已通过并入册」→ 该行状态变「已通过」绿色 tag,三角形变绿,操作列变「已审核」。
5. 点「拒绝」→ 状态变「已拒绝」灰色 tag,三角形变灰。
6. 回 EventDetailView → 刚审核过的成绩旁三角形颜色更新(绿/灰)。
7. 侧边栏点「记录管理」→ 表格出现刚入册的 record(含类别/内部值列,无地点列)。
8. 重复审核同一成绩 → toast 报错「该成绩已审核」(防重复入册)。
9. 录入新成绩挤掉前 3 → 原第 3 名失去三角形,新第 3 名获得(三角形即时算)。

- [ ] **Step 4: 最终 commit(如有零散改动)**

```bash
git status
# 若有未提交改动:
git add -A && git commit -m "chore: 端到端联调微调"
```

---

## 验证总览

- **后端编译 + 测试**:`mvn -q compile && mvn -q test` 全绿(新增 9 个 `RecordServiceImplTest` + 现有 12 个测试)。
- **前端构建**:`npm run build` 无 TS 错误。
- **数据库**:result 加 record_status;record 加 4 字段删 venue,DESCRIBE 验证。
- **功能手测**:Task 9 步骤 3 的 9 项全部通过。

## 注意事项

- DDL(Task 9 Step 1)**必须先向用户确认**再执行(CLAUDE.md 约定)。
- record 表当前为空(用户确认无历史),无需 DML 迁移;result.record_status 默认 0,现有成绩视为「未审」。
- SQL 重算不写 Mapper 集成测试(项目无该基础设施,参考 TeamScoreAdjustmentServiceImplTest 只测纯逻辑)。
- `reviewRecord` 跨模块注入 5 个 Mapper(Result/Event/Participant/Team/GroupType),项目惯例允许 Service 注入多 Mapper。
- score 换算依赖 event.category:径赛/团队赛 ÷1000(秒),田赛 ÷100(米),category 为 null 默认按径赛。
- 前端三角形靠 rankMap 即时算(已含名次),后端 selectVO 返回 recordStatus 自动映射。
