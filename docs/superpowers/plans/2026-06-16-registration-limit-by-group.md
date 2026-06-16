# 组别级报名限报规则 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在组别(group_type)级配置限报规则(选中项目集 + 每人上限 N),报名时按每人已报不同 event 数校验,超限拒绝。

**Architecture:** `group_type` 表加 `per_person_limit`(INT) + `limit_event_ids`(JSON,实体用 String 承载、service 层 Jackson 解析,不引入 typeHandler)。配置接口挂在 `AdminGroupTypeController`;报名校验加在 `RegistrationServiceImpl.add`(链路 participant→team→group_type)。前端在 `MeetingDetailView` 组别管理 tab 加配置弹窗(项目按 category 分组多选 + 全选本类)。

**Tech Stack:** Spring Boot + MyBatis(XML) + MySQL8(JSON) + Vue3 + Element Plus + TypeScript。

**测试策略:** 项目无业务单测/前端无测试框架(沿用既有模式)。每任务用 `mvn compile` / `npm run build` 做编译/类型验证,关键逻辑用启动服务 + curl/UI 手动验证。最后 Task 10 端到端验证。

**关联 spec:** `docs/superpowers/specs/2026-06-16-registration-limit-by-group-design.md`

---

## 文件结构

后端:
- 修改 `SportBackend/sql/group_type.sql` — 建表脚本加字段
- 修改 `SportBackend/src/main/java/com/dlust/sportbackend/entity/GroupType.java` — 加两字段
- 修改 `SportBackend/src/main/resources/mapper/GroupTypeMapper.xml` — insert/update 写入新字段
- 修改 `SportBackend/src/main/java/com/dlust/sportbackend/Service/GroupTypeService.java` — 加两方法签名
- 修改 `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/GroupTypeServiceImpl.java` — 实现 + JSON 解析
- 修改 `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminGroupTypeController.java` — 加两接口
- 修改 `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/RegistrationMapper.java` — 加计数方法
- 修改 `SportBackend/src/main/resources/mapper/RegistrationMapper.xml` — 计数 SQL
- 修改 `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/RegistrationServiceImpl.java` — add 校验

前端:
- 修改 `webManageProject/src/api/groupType.ts` — 加字段 + 两 API
- 修改 `webManageProject/src/views/MeetingDetailView.vue` — 配置弹窗 + 组别管理入口按钮
- 检查 `webManageProject/src/views/EventDetailView.vue` — 报名 catch 错误提示

---

### Task 1: 数据库加字段 + 建表脚本

**Files:**
- Modify: `SportBackend/sql/group_type.sql`
- DB: 执行 ALTER(由用户/计划执行者确认后跑)

- [ ] **Step 1: 执行 ALTER**

```sql
ALTER TABLE group_type
  ADD COLUMN per_person_limit INT DEFAULT 0 COMMENT '每人限报项目数(0=不限)',
  ADD COLUMN limit_event_ids JSON DEFAULT NULL COMMENT '限报规则选中的项目ID数组(空/NULL=不限)';
```

执行(密码用环境变量,不落命令行参数):

```bash
MYSQL_PWD="<你的root密码>" mysql -h localhost -P 3306 -u root sport_db -e "ALTER TABLE group_type ADD COLUMN per_person_limit INT DEFAULT 0 COMMENT '每人限报项目数(0=不限)', ADD COLUMN limit_event_ids JSON DEFAULT NULL COMMENT '限报规则选中的项目ID数组(空/NULL=不限)';"
```

- [ ] **Step 2: 复验 DESC**

```bash
MYSQL_PWD="<你的root密码>" mysql -h localhost -P 3306 -u root sport_db -e "DESC group_type;"
```

预期:出现 `per_person_limit`(int) 与 `limit_event_ids`(json) 两行。

- [ ] **Step 3: 同步建表脚本**

打开 `SportBackend/sql/group_type.sql`,在 CREATE TABLE 的字段列表里(name 之后、create_time 之前)加:

```sql
  `per_person_limit` INT DEFAULT 0 COMMENT '每人限报项目数(0=不限)',
  `limit_event_ids` JSON DEFAULT NULL COMMENT '限报规则选中的项目ID数组(空/NULL=不限)',
```

- [ ] **Step 4: 提交**

```bash
git add SportBackend/sql/group_type.sql
git commit -m "feat: group_type表增加每人限报字段(per_person_limit+limit_event_ids)"
```

---

### Task 2: 后端 GroupType 实体 + Mapper 读写新字段

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/GroupType.java`
- Modify: `SportBackend/src/main/resources/mapper/GroupTypeMapper.xml`

- [ ] **Step 1: 改 GroupType.java**

整体替换为:

```java
package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupType {
    private Long id;
    private Long sportsMeetingId;
    private String name;
    private Integer perPersonLimit;   // 每人限报项目数(0=不限)
    private String limitEventIds;     // 限报选中项目ID数组的JSON字符串(如"[1,2,3]"),DB为JSON列
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 改 GroupTypeMapper.xml 的 insert**

把现有 insert(16-19 行)替换为:

```xml
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO group_type (sports_meeting_id, name, per_person_limit, limit_event_ids)
        VALUES (#{sportsMeetingId}, #{name},
                <choose><when test="perPersonLimit != null">#{perPersonLimit}</when><otherwise>0</otherwise></choose>,
                #{limitEventIds})
    </insert>
```

- [ ] **Step 3: 改 GroupTypeMapper.xml 的 updateById**

把现有 updateById(21-27 行)替换为:

```xml
    <update id="updateById">
        UPDATE group_type
        <set>
            <if test="name != null">name = #{name},</if>
            <if test="perPersonLimit != null">per_person_limit = #{perPersonLimit},</if>
            <if test="limitEventIds != null">limit_event_ids = #{limitEventIds},</if>
        </set>
        WHERE id = #{id}
    </update>
```

注:selectBySportsMeetingId / selectById 用 `SELECT *`,配合 `map-underscore-camel-case` 自动把 `per_person_limit`→`perPersonLimit`、`limit_event_ids`→`limitEventIds` 映射好,无需改。

- [ ] **Step 4: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

预期:BUILD SUCCESS。

- [ ] **Step 5: 提交**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/entity/GroupType.java SportBackend/src/main/resources/mapper/GroupTypeMapper.xml
git commit -m "feat: GroupType实体与Mapper支持限报字段读写"
```

---

### Task 3: 后端 GroupTypeService 配置读写(JSON 解析)

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/GroupTypeService.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/GroupTypeServiceImpl.java`

- [ ] **Step 1: 接口加方法签名**

把 `GroupTypeService.java` 整体替换为:

```java
package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.GroupType;
import java.util.List;

public interface GroupTypeService {
    List<GroupType> getBySportsMeetingId(Long sportsMeetingId);
    GroupType getById(Long id);
    void add(GroupType groupType);
    void update(GroupType groupType);
    void delete(Long id);

    // 限报配置
    GroupType getLimitConfig(Long groupTypeId);
    void saveLimitConfig(Long groupTypeId, Integer perPersonLimit, List<Long> eventIds);
}
```

- [ ] **Step 2: 实现类加方法 + JSON 解析**

把 `GroupTypeServiceImpl.java` 整体替换为:

```java
package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.GroupTypeMapper;
import com.dlust.sportbackend.Service.GroupTypeService;
import com.dlust.sportbackend.entity.GroupType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupTypeServiceImpl implements GroupTypeService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private GroupTypeMapper groupTypeMapper;

    @Override
    public List<GroupType> getBySportsMeetingId(Long sportsMeetingId) {
        return groupTypeMapper.selectBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public GroupType getById(Long id) {
        return groupTypeMapper.selectById(id);
    }

    @Override
    public void add(GroupType groupType) {
        groupTypeMapper.insert(groupType);
    }

    @Override
    public void update(GroupType groupType) {
        groupTypeMapper.updateById(groupType);
    }

    @Override
    public void delete(Long id) {
        groupTypeMapper.deleteById(id);
    }

    @Override
    public GroupType getLimitConfig(Long groupTypeId) {
        return groupTypeMapper.selectById(groupTypeId);
    }

    @Override
    public void saveLimitConfig(Long groupTypeId, Integer perPersonLimit, List<Long> eventIds) {
        GroupType gt = new GroupType();
        gt.setId(groupTypeId);
        gt.setPerPersonLimit(perPersonLimit == null ? 0 : perPersonLimit);
        try {
            // 空列表存 null,语义=不限
            gt.setLimitEventIds((eventIds == null || eventIds.isEmpty()) ? null : MAPPER.writeValueAsString(eventIds));
        } catch (Exception e) {
            throw new RuntimeException("限报项目配置序列化失败", e);
        }
        groupTypeMapper.updateById(gt);
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

预期:BUILD SUCCESS。

- [ ] **Step 4: 提交**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Service/GroupTypeService.java SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/GroupTypeServiceImpl.java
git commit -m "feat: GroupTypeService增加限报配置读写(JSON解析)"
```

---

### Task 4: 后端 AdminGroupTypeController 配置接口

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminGroupTypeController.java`

- [ ] **Step 1: 加两个接口**

把 `AdminGroupTypeController.java` 整体替换为:

```java
package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.GroupTypeService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.GroupType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/group-type")
public class AdminGroupTypeController {

    @Autowired
    private GroupTypeService groupTypeService;

    @GetMapping("/list")
    public Result<List<GroupType>> list(@RequestParam Long sportsMeetingId) {
        return Result.success(groupTypeService.getBySportsMeetingId(sportsMeetingId));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody GroupType groupType) {
        log.info("添加组别: name={}", groupType.getName());
        groupTypeService.add(groupType);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody GroupType groupType) {
        log.info("更新组别: id={}", groupType.getId());
        groupTypeService.update(groupType);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除组别: id={}", id);
        groupTypeService.delete(id);
        return Result.success("删除成功");
    }

    // ===== 限报配置 =====

    @GetMapping("/limitConfig")
    public Result<GroupType> limitConfig(@RequestParam Long groupTypeId) {
        return Result.success(groupTypeService.getLimitConfig(groupTypeId));
    }

    @PostMapping("/saveLimitConfig")
    public Result<String> saveLimitConfig(@RequestBody Map<String, Object> params) {
        Long groupTypeId = Long.valueOf(params.get("groupTypeId").toString());
        Integer perPersonLimit = params.get("perPersonLimit") != null
                ? Integer.valueOf(params.get("perPersonLimit").toString()) : 0;
        List<Long> eventIds = ((List<?>) params.get("eventIds")).stream()
                .map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
        log.info("保存限报配置: groupTypeId={}, perPersonLimit={}, eventIds={}", groupTypeId, perPersonLimit, eventIds);
        groupTypeService.saveLimitConfig(groupTypeId, perPersonLimit, eventIds);
        return Result.success("保存成功");
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

预期:BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminGroupTypeController.java
git commit -m "feat: AdminGroupTypeController增加限报配置查询/保存接口"
```

---

### Task 5: 后端 RegistrationMapper 按 event 去重计数

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/RegistrationMapper.java`
- Modify: `SportBackend/src/main/resources/mapper/RegistrationMapper.xml`

- [ ] **Step 1: Mapper 接口加方法**

在 `RegistrationMapper.java` 的 `deleteById` 方法前(35 行附近)加:

```java
    int countDistinctEventByParticipantInEvents(@Param("participantId") Long participantId,
                                                 @Param("eventIds") List<Long> eventIds,
                                                 @Param("statuses") List<Integer> statuses);
```

- [ ] **Step 2: xml 加 SQL**

在 `RegistrationMapper.xml` 的 `deleteById`(68-70 行)之前加:

```xml
    <select id="countDistinctEventByParticipantInEvents" resultType="int">
        SELECT COUNT(DISTINCT event_id) FROM registration
        WHERE participant_id = #{participantId}
        AND event_id IN
        <foreach collection="eventIds" item="eid" open="(" separator="," close=")">#{eid}</foreach>
        AND status IN
        <foreach collection="statuses" item="st" open="(" separator="," close=")">#{st}</foreach>
    </select>
```

- [ ] **Step 3: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

预期:BUILD SUCCESS。

- [ ] **Step 4: 提交**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Mapper/RegistrationMapper.java SportBackend/src/main/resources/mapper/RegistrationMapper.xml
git commit -m "feat: RegistrationMapper增加按event去重的限报计数查询"
```

---

### Task 6: 后端 RegistrationService.add 报名校验

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/RegistrationServiceImpl.java`

- [ ] **Step 1: 注入三个 Mapper + 改 add + 加校验方法**

把 `RegistrationServiceImpl.java` 整体替换为:

```java
package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.GroupTypeMapper;
import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.RegistrationMapper;
import com.dlust.sportbackend.Mapper.TeamMapper;
import com.dlust.sportbackend.Service.EventScheduleService;
import com.dlust.sportbackend.Service.RegistrationService;
import com.dlust.sportbackend.entity.GroupType;
import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.Registration;
import com.dlust.sportbackend.entity.RegistrationVO;
import com.dlust.sportbackend.entity.Team;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private EventScheduleService eventScheduleService;

    @Autowired
    private ParticipantMapper participantMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private GroupTypeMapper groupTypeMapper;

    @Override
    public List<RegistrationVO> getBySportsMeetingId(Long sportsMeetingId) {
        return registrationMapper.selectVOBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<RegistrationVO> getByEventId(Long eventId) {
        return registrationMapper.selectVOByEventId(eventId);
    }

    @Override
    public void add(Long participantId, Long eventId, Long scheduleId) {
        checkRegisterLimit(participantId, eventId);
        Registration reg = new Registration();
        reg.setParticipantId(participantId);
        reg.setEventId(eventId);
        reg.setScheduleId(scheduleId);
        reg.setStatus(0);
        registrationMapper.insert(reg);
    }

    /**
     * 每人限报校验:participant → team → group_type,取组别限报规则;
     * 仅当 N>0 且选中项目集非空 且 本次 event 在选中集时才计数。
     * 计数=该人在选中集内已报的不同event数(status∈{0,1}),同event多赛次算1。
     */
    private void checkRegisterLimit(Long participantId, Long eventId) {
        Participant p = participantMapper.selectById(participantId);
        if (p == null || p.getTeamId() == null) return;
        Team team = teamMapper.selectById(p.getTeamId());
        if (team == null || team.getGroupTypeId() == null) return;
        GroupType gt = groupTypeMapper.selectById(team.getGroupTypeId());
        if (gt == null) return;

        Integer n = gt.getPerPersonLimit();
        if (n == null || n <= 0) return;

        List<Long> limitEventIds = parseLongList(gt.getLimitEventIds());
        if (limitEventIds == null || limitEventIds.isEmpty()) return;
        if (!limitEventIds.contains(eventId)) return;  // 本次项目不在限制范围

        // 已报的不同event数(本次尚未insert,故不含本次)
        int counted = registrationMapper.countDistinctEventByParticipantInEvents(
                participantId, limitEventIds, Arrays.asList(0, 1));
        // 若同event已有报名记录(另一赛次),则本次不新增占用
        Registration existing = registrationMapper.selectByParticipantIdAndEventId(participantId, eventId);
        int after = (existing != null) ? counted : counted + 1;
        if (after > n) {
            throw new RuntimeException("超出限报:每人最多报 " + n + " 个项目");
        }
    }

    private List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("解析limitEventIds失败: {}", json, e);
            return null;
        }
    }

    @Override
    public void update(Long id, Integer status) {
        Registration reg = registrationMapper.selectById(id);
        Integer oldStatus = (reg != null) ? reg.getStatus() : null;

        registrationMapper.updateStatus(id, status);

        if (status == 1) {
            autoRegisterNextSchedule(id);
        }

        if (oldStatus != null && oldStatus == 1 && status != 1) {
            removeNextScheduleRegistration(reg);
        }
    }

    private void autoRegisterNextSchedule(Long registrationId) {
        Registration reg = registrationMapper.selectById(registrationId);
        if (reg == null || reg.getEventId() == null || reg.getScheduleId() == null) return;

        List<Long> scheduleIds = eventScheduleService.getScheduleIdsByEventId(reg.getEventId());
        if (scheduleIds == null || scheduleIds.isEmpty()) return;

        int currentIndex = scheduleIds.indexOf(reg.getScheduleId());
        if (currentIndex < 0 || currentIndex >= scheduleIds.size() - 1) return;

        Long nextScheduleId = scheduleIds.get(currentIndex + 1);

        Registration existing = registrationMapper.selectByParticipantIdEventIdScheduleId(
                reg.getParticipantId(), reg.getEventId(), nextScheduleId);
        if (existing != null) return;

        Registration newReg = new Registration();
        newReg.setParticipantId(reg.getParticipantId());
        newReg.setEventId(reg.getEventId());
        newReg.setScheduleId(nextScheduleId);
        newReg.setStatus(0);
        registrationMapper.insert(newReg);
        log.info("晋级自动报名: participantId={}, eventId={}, nextScheduleId={}",
                reg.getParticipantId(), reg.getEventId(), nextScheduleId);
    }

    private void removeNextScheduleRegistration(Registration reg) {
        if (reg == null || reg.getEventId() == null || reg.getScheduleId() == null) return;

        List<Long> scheduleIds = eventScheduleService.getScheduleIdsByEventId(reg.getEventId());
        if (scheduleIds == null || scheduleIds.isEmpty()) return;

        int currentIndex = scheduleIds.indexOf(reg.getScheduleId());
        if (currentIndex < 0 || currentIndex >= scheduleIds.size() - 1) return;

        Long nextScheduleId = scheduleIds.get(currentIndex + 1);

        Registration existing = registrationMapper.selectByParticipantIdEventIdScheduleId(
                reg.getParticipantId(), reg.getEventId(), nextScheduleId);
        if (existing != null) {
            registrationMapper.deleteById(existing.getId());
            log.info("取消晋级，删除下一赛次报名: participantId={}, eventId={}, nextScheduleId={}",
                    reg.getParticipantId(), reg.getEventId(), nextScheduleId);
        }
    }

    @Override
    public void delete(Long id) {
        registrationMapper.deleteById(id);
    }
}
```

注意:`autoRegisterNextSchedule` 直接调 `registrationMapper.insert`,不经 `add`,故晋级自动报名不受限报校验影响(同 event 本就不增计数)。

- [ ] **Step 2: 编译验证**

```bash
cd SportBackend && mvn -q compile
```

预期:BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/RegistrationServiceImpl.java
git commit -m "feat: 报名add增加每人限报校验(participant→team→group_type链路)"
```

---

### Task 7: 前端 groupType.ts 加字段 + 配置 API

**Files:**
- Modify: `webManageProject/src/api/groupType.ts`

- [ ] **Step 1: 整体替换**

```ts
import request from '@/utils/request'

export interface GroupType {
  id: number
  sportsMeetingId: number
  name: string
  perPersonLimit?: number
  limitEventIds?: string | null
}

export function getGroupTypeList(sportsMeetingId: number) {
  return request.get('/admin/group-type/list', { params: { sportsMeetingId } })
}

export function addGroupType(data: Partial<GroupType>) {
  return request.post('/admin/group-type/add', data)
}

export function updateGroupType(data: Partial<GroupType>) {
  return request.put('/admin/group-type/update', data)
}

export function deleteGroupType(id: number) {
  return request.delete(`/admin/group-type/${id}`)
}

export function getLimitConfig(groupTypeId: number) {
  return request.get('/admin/group-type/limitConfig', { params: { groupTypeId } })
}

export function saveLimitConfig(data: { groupTypeId: number; perPersonLimit: number; eventIds: number[] }) {
  return request.post('/admin/group-type/saveLimitConfig', data)
}
```

- [ ] **Step 2: 类型检查 + 构建**

```bash
cd webManageProject && npm run build
```

预期:构建成功,无 TS 错误。

- [ ] **Step 3: 提交**

```bash
git add webManageProject/src/api/groupType.ts
git commit -m "feat: groupType.ts增加限报字段与配置API"
```

---

### Task 8: 前端 MeetingDetailView 限报配置弹窗

**Files:**
- Modify: `webManageProject/src/views/MeetingDetailView.vue`

前置:确认 `webManageProject/src/api/event.ts` 的 `Event` 接口含 `category` 字段(DESC 显示 event 表有 category 列)。若接口缺字段需补 `category: string`。

- [ ] **Step 1: 改 import(第 21-22 行附近)**

在现有 groupType import 行后追加配置 API 与 Event:

```ts
import { getGroupTypeList, addGroupType, updateGroupType, deleteGroupType, getLimitConfig, saveLimitConfig } from '@/api/groupType'
import type { GroupType } from '@/api/groupType'
import { getEventList } from '@/api/event'
import type { Event } from '@/api/event'
```

(若 `getEventList` / `Event` 已 import 则不重复)

同时确认顶部已 import `computed`(若没有,在已有的 `import { ref } from 'vue'` 处补 `computed`):

```ts
import { ref, computed } from 'vue'
```

- [ ] **Step 2: 加 script 状态与方法**

在组别管理相关方法区(`openTeamAdd` 附近,约 191 行后)插入:

```ts
// ============ 限报配置 ============
const limitDialogVisible = ref(false)
const limitForm = ref({ groupTypeId: 0, perPersonLimit: 0, eventIds: [] as number[] })
const allEvents = ref<Event[]>([])
const eventsByCategory = computed(() => {
  const m: Record<string, Event[]> = {}
  for (const e of allEvents.value) {
    const cat = e.category || '其他'
    ;(m[cat] = m[cat] || []).push(e)
  }
  return m
})

function isCategoryAllChecked(cat: string) {
  const ids = (eventsByCategory.value[cat] || []).map(e => e.id)
  return ids.length > 0 && ids.every(id => limitForm.value.eventIds.includes(id))
}

function toggleCategoryAll(cat: string, checked: any) {
  const ids = (eventsByCategory.value[cat] || []).map(e => e.id)
  if (checked) {
    limitForm.value.eventIds = Array.from(new Set([...limitForm.value.eventIds, ...ids]))
  } else {
    limitForm.value.eventIds = limitForm.value.eventIds.filter(id => !ids.includes(id))
  }
}

async function openLimitConfig(gt: GroupType) {
  limitForm.value.groupTypeId = gt.id
  if (allEvents.value.length === 0) {
    const res: any = await getEventList(meetingId)
    allEvents.value = res.data || res || []
  }
  const res: any = await getLimitConfig(gt.id)
  const cfg = res.data || res
  limitForm.value.perPersonLimit = cfg?.perPersonLimit || 0
  try {
    limitForm.value.eventIds = cfg?.limitEventIds ? JSON.parse(cfg.limitEventIds) : []
  } catch {
    limitForm.value.eventIds = []
  }
  limitDialogVisible.value = true
}

async function saveLimit() {
  await saveLimitConfig({
    groupTypeId: limitForm.value.groupTypeId,
    perPersonLimit: limitForm.value.perPersonLimit,
    eventIds: limitForm.value.eventIds
  })
  ElMessage.success('限报配置已保存')
  limitDialogVisible.value = false
}
```

注:`meetingId` / `ElMessage` 已是本组件既有变量/引用,直接复用。

- [ ] **Step 3: 模板加「限报配置」按钮**

在组别 collapse-item 标题区(约 574-575 行,展示「N 个代表队」的 `<span class="collapse-count">` 旁边),加按钮:

```html
<span class="collapse-count">{{ getTeamsByGroupType(gt.id).length }} 个代表队</span>
<el-button size="small" link type="primary" @click.stop="openLimitConfig(gt)">限报配置</el-button>
```

(`@click.stop` 防止触发 collapse 展开/收起)

- [ ] **Step 4: 模板加弹窗**

在组别编辑弹窗(`gtDialogVisible` 的 el-dialog,约 715 行)之后,加限报配置弹窗:

```html
    <!-- ======== 弹窗：限报配置 ======== -->
    <el-dialog v-model="limitDialogVisible" title="限报配置" width="640px" destroy-on-close>
      <div style="margin-bottom:8px;color:#999">
        勾选受限项目（按类别分组，可一键全选某类），并设置每人最多可报项目数。
      </div>
      <div v-for="(evs, cat) in eventsByCategory" :key="cat" style="margin-bottom:16px">
        <div style="margin-bottom:6px">
          <el-checkbox :model-value="isCategoryAllChecked(cat)" @change="(v:any)=>toggleCategoryAll(cat,v)">{{ cat }}（全选）</el-checkbox>
        </div>
        <el-checkbox-group v-model="limitForm.eventIds" style="margin-left:24px">
          <el-checkbox v-for="ev in evs" :key="ev.id" :label="ev.id">{{ ev.name }}</el-checkbox>
        </el-checkbox-group>
      </div>
      <el-form-item label="每人限报项目数" style="margin-top:8px">
        <el-input-number v-model="limitForm.perPersonLimit" :min="0" />
        <span style="margin-left:8px;color:#999">0 = 不限</span>
      </el-form-item>
      <template #footer>
        <el-button @click="limitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLimit">保存</el-button>
      </template>
    </el-dialog>
```

- [ ] **Step 5: 类型检查 + 构建**

```bash
cd webManageProject && npm run build
```

预期:构建成功。若报 `category` 不存在 → 给 `webManageProject/src/api/event.ts` 的 `Event` 接口补 `category: string`。

- [ ] **Step 6: 提交**

```bash
git add webManageProject/src/views/MeetingDetailView.vue webManageProject/src/api/event.ts
git commit -m "feat: 组别管理增加限报配置弹窗(项目按类别分组多选+全选)"
```

---

### Task 9: 前端报名失败错误提示

**Files:**
- Check/Modify: `webManageProject/src/views/EventDetailView.vue`(约 176-185 行的报名 try/catch)

- [ ] **Step 1: 检查现有 catch 块**

读取 `EventDetailView.vue` 第 174-190 行,确认 `try { ... addRegistration ... }` 后的 `catch` 是否已 `ElMessage.error(err?.response?.data?.message || '报名失败')`。

- [ ] **Step 2: 若未提示 message,补全 catch**

把报名那段(176 行起)改为类似:

```ts
  try {
    for (const pid of addSelectedIds.value) {
      await addRegistration({ participantId: pid, eventId, scheduleId })
    }
    ElMessage.success(`成功添加 ${addSelectedIds.value.length} 名参赛人员`)
    addDialogVisible.value = false
    fetchRegistrations()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '报名失败')
    fetchRegistrations()
  }
```

(若现有 catch 已等价,此步跳过)

- [ ] **Step 3: 类型检查 + 构建**

```bash
cd webManageProject && npm run build
```

预期:构建成功。

- [ ] **Step 4: 提交(若有改动)**

```bash
git add webManageProject/src/views/EventDetailView.vue
git commit -m "fix: 报名失败显示后端限报错误信息"
```

---

### Task 10: 端到端验证

**Files:** 无(纯验证)

- [ ] **Step 1: 启动后端**

```bash
cd SportBackend && mvn spring-boot:run
```

预期:无异常启动(无 SQL 列错误)。

- [ ] **Step 2: 启动前端**

```bash
cd webManageProject && npm run dev
```

- [ ] **Step 3: 配置限报规则**

在浏览器组别管理 tab,对某组别点「限报配置」→ 勾选径赛全部(点「径赛（全选）」)→ 每人限报项目数填 2 → 保存。刷新后重新打开,确认回显:径赛项目全选中、N=2。

- [ ] **Step 4: 验证计数边界**

为该组别某代表队下一名参赛人员,依次报径赛项目:
1. 报第 1 个径赛项目 → 成功
2. 报第 2 个不同径赛项目 → 成功(达上限 2)
3. 报第 3 个不同径赛项目 → **失败**,提示「超出限报:每人最多报 2 个项目」
4. 报一个田赛项目(未勾选) → 成功(不受限)
5. 对已报的某径赛项目报另一赛次(或经晋级自动报名) → 不受影响

- [ ] **Step 5: 验证未配置不限**

对另一个未配置限报的组别,报名任意数量项目 → 均成功。

- [ ] **Step 6: 提交验证记录(可选)**

如验证中发现并修复了问题,按修复内容提交。否则无需提交。

---

## Self-Review(计划自检)

**Spec 覆盖:** 
- §3 数据库改动 → Task 1 ✓
- §4.1 Entity → Task 2 ✓
- §4.2 GroupTypeMapper 读写 → Task 2 ✓
- §4.3 Service 配置读写 → Task 3 ✓
- §4.4 RegistrationMapper 计数 → Task 5 ✓
- §4.5 RegistrationService.add 校验 → Task 6 ✓
- §4.6 自动晋级绕过校验 → Task 6(代码注释+不经过add)✓
- §4.7 Controller 接口 → Task 4 ✓
- §5.1 groupType.ts → Task 7 ✓
- §5.2 组别管理弹窗 → Task 8 ✓
- §5.3 报名错误提示 → Task 9 ✓
- §7 验证清单 → Task 10 ✓

**占位扫描:** Task 1 的 `<你的root密码>` 为执行者本地值,属必要占位(不硬编码密码),非计划缺陷。其余无 TBD/TODO。

**类型一致性:** 
- `getLimitConfig` / `saveLimitConfig` 在 Service(Task3)、Controller(Task4)、前端(Task7)签名一致 ✓
- `countDistinctEventByParticipantInEvents` Mapper 接口(Task5)与 xml(Task5)与调用(Task6)参数名一致(participantId/eventIds/statuses)✓
- `perPersonLimit` / `limitEventIds` 命名贯穿实体/SQL/前端 ✓
- 项目根包名为 `com.dlust.sportbackend`,各 Task 的 `git add` 路径均已据此使用 `com/dlust/...` ✓
