# 参赛人员账号体系改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让参赛人员成为登录主体（学号/工号 + 密码），废弃微信静默登录；管理端支持 Excel 批量导入；小程序游客模式 + 触发登录。

**Architecture:** user 表改造为「账号 + 人」（全局唯一），participant 精简为「某运动会的参赛身份」(user_id 关联)。JWT 认证保留（payload=userId），拦截列表收紧到「需登录才放行」。历史数据用 Java 一次性 CommandLineRunner（profile=migrate）迁移。

**Tech Stack:** Spring Boot + MyBatis + MySQL；BCryptPasswordEncoder（spring-security-crypto）；EasyExcel（Excel 导入）；Vue 3 + Element Plus；原生微信小程序。

---

## 阶段概览

| 阶段 | 任务 | 说明 |
|---|---|---|
| A. 依赖与密码工具 | Task 1 | pom 加依赖 + PasswordService |
| B. 实体与 Mapper | Task 2-5 | User/Participant 实体重构 + Mapper JOIN 改造 |
| C. 数据迁移 | Task 6 | CommandLineRunner 跑一次 |
| D. 后端认证 | Task 7 | 登录重写 + 拦截列表 |
| E. 报名接口 | Task 8 | JWT userId 取代 userCode |
| F. 管理端后端 | Task 9-10 | 重置密码 + 单条 CRUD + Excel 导入 |
| G. 管理端 Vue | Task 11-12 | 参赛人员 tab + 批量导入对话框 |
| H. 小程序登录 | Task 13 | 登录页 + auth.js + 报名触发 |
| I. 小程序我的 | Task 14 | profile + 我的参赛项目 |

执行顺序：A → B → C → D → E → F → G → H → I。每个 Task 独立提交可回滚。

---

## 阶段 A：依赖与密码工具

### Task 1: 加 easyexcel + spring-security-crypto，建 PasswordService

**Files:**
- Modify: `SportBackend/pom.xml`
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/util/PasswordService.java`
- Create: `SportBackend/src/test/java/com/dlust/sportbackend/util/PasswordServiceTest.java`

- [ ] **Step 1: pom.xml 加依赖**

在 `SportBackend/pom.xml` `<dependencies>` 末尾加：
```xml
<!-- 密码加密 -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
    <version>6.3.1</version>
</dependency>
<!-- Excel 导入 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>3.3.4</version>
</dependency>
```

- [ ] **Step 2: 写 PasswordServiceTest（先失败）**

```java
package com.dlust.sportbackend.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {
    private final PasswordService svc = new PasswordService();

    @Test
    void encode_thenMatches() {
        String hash = svc.encode("dlust123456");
        assertNotEquals("dlust123456", hash);
        assertTrue(svc.matches("dlust123456", hash));
        assertFalse(svc.matches("wrong", hash));
    }

    @Test
    void encode_generatesDifferentHashesForSameInput() {
        // BCrypt 每次加盐，同一明文应产生不同哈希
        assertNotEquals(svc.encode("x"), svc.encode("x"));
    }
}
```

- [ ] **Step 3: 跑测试确认失败**

```bash
cd SportBackend && mvn -q test -Dtest=PasswordServiceTest
```
Expected: 编译失败（PasswordService 不存在）

- [ ] **Step 4: 实现 PasswordService**

```java
package com.dlust.sportbackend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String raw) { return encoder.encode(raw); }
    public boolean matches(String raw, String hash) { return encoder.matches(raw, hash); }
}
```

- [ ] **Step 5: 跑测试确认通过**

```bash
mvn -q test -Dtest=PasswordServiceTest
```
Expected: 2 tests PASS

- [ ] **Step 6: Commit**

```bash
git add SportBackend/pom.xml SportBackend/src/main/java/com/dlust/sportbackend/util/PasswordService.java SportBackend/src/test/java/com/dlust/sportbackend/util/PasswordServiceTest.java
git commit -m "feat: 加 PasswordService(BCrypt) + easyexcel 依赖"
```

---

## 阶段 B：实体与 Mapper 重构

### Task 2: User / Participant 实体重构 + 建表脚本同步

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/User.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/entity/Participant.java`
- Modify: `SportBackend/sql/user.sql`
- Modify: `SportBackend/sql/participant.sql`

- [ ] **Step 1: 重写 User.java**

```java
package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String userCode;   // 学号/工号，登录账号（全局唯一）
    private String password;   // BCrypt 哈希
    private String name;
    private String gender;
    private String phone;
    private String college;
    private String major;      // 专业/单位
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 重写 Participant.java（去 user_code/name 等，加 userId）**

```java
package com.dlust.sportbackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Participant {
    private Long id;
    private Long sportsMeetingId;
    private Long userId;       // 关联 user.id（替代原 userCode/name/...）
    private Long teamId;       // 代表队（后续分配）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 以下为 JOIN 查询时的扩展字段（非 participant 表字段）
    private String userCode;   // JOIN user
    private String name;       // JOIN user
    private String gender;     // JOIN user
    private String phone;      // JOIN user
    private String college;    // JOIN user
    private String major;      // JOIN user
    private String teamName;   // JOIN team
}
```

> 注：保留 userCode/name/gender/phone/college/major/teamName 作为 VO 扩展字段，避免再建独立 VO 类，Mapper SELECT 时用别名填充。

- [ ] **Step 3: 同步 user.sql 建表脚本（权威源，DDL 实际变更在 Task 6 迁移脚本里做）**

```sql
-- 用户表（账号主体）
CREATE TABLE IF NOT EXISTS user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    user_code    VARCHAR(30)  NOT NULL COMMENT '学号/工号，登录账号',
    password     VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
    name         VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender       VARCHAR(4)   NOT NULL COMMENT '性别',
    phone        VARCHAR(20)  DEFAULT NULL COMMENT '电话（非必须）',
    college      VARCHAR(100) DEFAULT NULL COMMENT '学院',
    major        VARCHAR(100) DEFAULT NULL COMMENT '专业/单位',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_code (user_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（账号主体）';
```

- [ ] **Step 4: 同步 participant.sql 建表脚本**

```sql
-- 参赛人员表（某运动会的参赛身份）
CREATE TABLE IF NOT EXISTS participant (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '参赛人员ID',
    sports_meeting_id BIGINT NOT NULL COMMENT '所属运动会ID',
    user_id           BIGINT NOT NULL COMMENT '关联用户ID',
    team_id           BIGINT DEFAULT NULL COMMENT '代表队ID（关联team表）',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_meeting_user (sports_meeting_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参赛人员表（运动会参赛身份）';
```

- [ ] **Step 5: 编译确认**

```bash
cd SportBackend && mvn -q compile
```
Expected: 编译失败于 Mapper/UserController（引用了已删的 user 字段如 openid）—— 这是预期的，Task 3+ 修复。本 Task 只保证 entity 本身编译过。先不 commit，Task 3-5 一起完成后统一编译通过再 commit；或本 Task 用 `git stash` 暂存。

> 决策：本 Task 与 Task 3-5 强耦合（entity 改了所有引用方都得改），合并为同一提交。先完成 Task 3-5 再一起编译 + commit。

- [ ] **Step 6: 暂不 commit，继续 Task 3**

---

### Task 3: UserMapper 重写（登录查询、改密码、重置密码）

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Mapper/UserMapper.java`
- Modify: `SportBackend/src/main/resources/mapper/UserMapper.xml`

- [ ] **Step 1: 重写 UserMapper.java**

```java
package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User selectByUserCode(@Param("userCode") String userCode);
    User selectById(@Param("id") Long id);
    int insert(User user);
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    int updateProfile(User user);  // 更新 name/gender/phone/college/major（不含 userCode/password）
    // 迁移用
    int insertForMigration(User user);
}
```

- [ ] **Step 2: 重写 UserMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.dlust.sportbackend.Mapper.UserMapper">

    <select id="selectByUserCode" resultType="com.dlust.sportbackend.entity.User">
        SELECT id, user_code, password, name, gender, phone, college, major, create_time, update_time
        FROM user WHERE user_code = #{userCode}
    </select>

    <select id="selectById" resultType="com.dlust.sportbackend.entity.User">
        SELECT id, user_code, password, name, gender, phone, college, major, create_time, update_time
        FROM user WHERE id = #{id}
    </select>

    <insert id="insert" parameterType="com.dlust.sportbackend.entity.User" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user (user_code, password, name, gender, phone, college, major)
        VALUES (#{userCode}, #{password}, #{name}, #{gender}, #{phone}, #{college}, #{major})
    </insert>

    <insert id="insertForMigration" parameterType="com.dlust.sportbackend.entity.User" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user (user_code, password, name, gender, phone, college, major)
        VALUES (#{userCode}, #{password}, #{name}, #{gender}, #{phone}, #{college}, #{major})
    </insert>

    <update id="updatePassword">
        UPDATE user SET password = #{password} WHERE id = #{id}
    </update>

    <update id="updateProfile" parameterType="com.dlust.sportbackend.entity.User">
        UPDATE user
        SET name = #{name}, gender = #{gender}, phone = #{phone},
            college = #{college}, major = #{major}
        WHERE id = #{id}
    </update>

</mapper>
```

- [ ] **Step 3: 暂不 commit（依赖 Task 5 完成后整体编译通过）**

---

### Task 4: ParticipantMapper.xml 改造（所有查询 JOIN user）

**Files:**
- Modify: `SportBackend/src/main/resources/mapper/ParticipantMapper.xml`

- [ ] **Step 1: 重写 ParticipantMapper.xml**

所有原本读 `user_code/name/phone/gender/college/major` 的 SELECT 改为 JOIN user，列别名对齐 Participant.java 扩展字段。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.dlust.sportbackend.Mapper.ParticipantMapper">

    <select id="selectBySportsMeetingId" resultType="com.dlust.sportbackend.entity.Participant">
        SELECT p.id, p.sports_meeting_id, p.user_id, p.team_id, p.create_time, p.update_time,
               u.user_code, u.name, u.gender, u.phone, u.college, u.major,
               t.name AS team_name
        FROM participant p
        LEFT JOIN user u ON p.user_id = u.id
        LEFT JOIN team t ON p.team_id = t.id
        WHERE p.sports_meeting_id = #{sportsMeetingId}
        ORDER BY u.user_code ASC
    </select>

    <select id="selectByTeamId" resultType="com.dlust.sportbackend.entity.Participant">
        SELECT p.id, p.sports_meeting_id, p.user_id, p.team_id, p.create_time, p.update_time,
               u.user_code, u.name, u.gender, u.phone, u.college, u.major
        FROM participant p
        LEFT JOIN user u ON p.user_id = u.id
        WHERE p.team_id = #{teamId}
        ORDER BY u.user_code ASC
    </select>

    <select id="selectByUserIdAndSportsMeetingId" resultType="com.dlust.sportbackend.entity.Participant">
        SELECT id, sports_meeting_id, user_id, team_id, create_time, update_time
        FROM participant
        WHERE user_id = #{userId} AND sports_meeting_id = #{sportsMeetingId}
    </select>

    <insert id="insert" parameterType="com.dlust.sportbackend.entity.Participant" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO participant (sports_meeting_id, user_id, team_id)
        VALUES (#{sportsMeetingId}, #{userId}, #{teamId})
    </insert>

    <update id="updateTeamId">
        UPDATE participant SET team_id = #{teamId} WHERE id = #{id}
    </update>

    <update id="clearTeamId">
        UPDATE participant SET team_id = NULL WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM participant WHERE id = #{id}
    </delete>

    <!-- 迁移用：查所有旧 participant（迁移期专用，迁移后删） -->
    <select id="selectAllForMigration" resultType="java.util.Map">
        SELECT id, sports_meeting_id, team_id, user_code, name, phone, gender, college, major
        FROM participant
    </select>

    <update id="updateUserIdForMigration">
        UPDATE participant SET user_id = #{userId} WHERE id = #{id}
    </update>

</mapper>
```

> 注：`selectAllForMigration` / `updateUserIdForMigration` 是迁移期临时方法，Task 6 完成后从 XML 和 Mapper 接口删除。

- [ ] **Step 2: 同步更新 ParticipantMapper.java 接口**

确保接口方法签名与 XML 对齐（`selectByUserIdAndSportsMeetingId(@Param("userId") Long, @Param("sportsMeetingId") Long)` 等）。删掉原 `selectByUserCodeAndSportsMeetingId`（不再用）。

- [ ] **Step 3: 暂不 commit**

---

### Task 5: RegistrationMapper.xml + ResultMapper.xml JOIN 改造

**Files:**
- Modify: `SportBackend/src/main/resources/mapper/RegistrationMapper.xml`
- Modify: `SportBackend/src/main/resources/mapper/ResultMapper.xml`

- [ ] **Step 1: RegistrationMapper.xml 改 JOIN**

原 SELECT VO 里 `p.name AS participant_name, p.user_code` 等改为从 user 取。例：

```xml
<!-- 改前 -->
SELECT r.*, p.name AS participant_name, p.user_code, ...
FROM registration r
LEFT JOIN participant p ON r.participant_id = p.id

<!-- 改后 -->
SELECT r.*, u.name AS participant_name, u.user_code, ...,
       p.team_id, t.name AS team_name
FROM registration r
LEFT JOIN participant p ON r.participant_id = p.id
LEFT JOIN user u ON p.user_id = u.id
LEFT JOIN team t ON p.team_id = t.id
```

> RegistrationMapper.xml 里所有 selectVO（BySportsMeetingId / ByEventId / ByParticipantId）都要把 `JOIN participant p` 扩展为 `JOIN participant p LEFT JOIN user u ON p.user_id = u.id`，把 participant_name / user_code / gender / phone / college / major 改从 u 取。team_name 仍从 team 取。

- [ ] **Step 2: ResultMapper.xml 同样改造**

成绩 VO（ResultVO）含 `participantName`，原 JOIN participant p 改为 JOIN participant p LEFT JOIN user u，participantName 从 u.name 取。

- [ ] **Step 3: 编译全部后端代码**

```bash
cd SportBackend && mvn -q compile
```
Expected: 仍有 Controller/Service 引用旧字段或旧方法的编译错误（Task 7-10 修），但 Mapper/XML/entity 本身应一致。如果 Mapper 接口方法签名有改动导致 Service 调用编译失败，本 Task 不修 Service（Task 7+ 修）。

> 决策：Task 2-5 是"数据层重构"，Task 7+ 是"业务层适配"。中间编译失败是预期的。**Task 2-5 + Task 7-10 必须作为一个"可编译里程碑"——前 5 个 Task 写完后先不 commit，Task 7-10 完成后整体编译通过再统一 commit。** 但这样颗粒太大，不符合"每 Task 独立提交"。

> 替代方案：Task 2-5 写完后用 `git stash` 暂存，先做 Task 7-10 的 Service/Controller 改造（基于新 entity 签名），最后一起 unstash + commit。但 stash 跨 Task 难追踪。

> **最终决策：把 Task 2-5 + Task 7-10 视为一个"重构批次"，按 Task 顺序写代码，但只在 Task 10 末尾做一次 commit（feat: 参赛人员账号化数据层+业务层重构）。中间 Task 标注"暂不 commit"。** 这违反"频繁提交"但在大规模重构时务实——因为中间态编译不过，提交无意义。

- [ ] **Step 4: 暂不 commit**

---

## 阶段 C：数据迁移

### Task 6: MigrationRunner（一次性 CommandLineRunner）

**Files:**
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/migration/MigrationRunner.java`

> 前置：Task 2-5 已完成（entity/Mapper 重构就绪）。本 Task 在 Task 5 编译通过前**不能跑**（依赖新结构）。实际执行顺序：先完成 Task 2-5 + Task 7-10 让代码编译通过，再回来执行迁移。

- [ ] **Step 1: 写 MigrationRunner**

```java
package com.dlust.sportbackend.migration;

import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.UserMapper;
import com.dlust.sportbackend.entity.User;
import com.dlust.sportbackend.util.PasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@Profile("migrate")  // 仅 profile=migrate 时启用，避免正常启动误跑
public class MigrationRunner implements CommandLineRunner {

    @Autowired private UserMapper userMapper;
    @Autowired private ParticipantMapper participantMapper;
    @Autowired private PasswordService passwordService;

    @Override
    public void run(String... args) {
        log.warn("=== 参赛人员账号化迁移开始 ===");

        // 1. 查所有旧 participant（迁移期 XML 临时保留 user_code/name 列）
        List<Map<String, Object>> rows = participantMapper.selectAllForMigration();
        log.info("旧 participant 记录数：{}", rows.size());

        // 2. 按 user_code 分组，每组取 id 最大那条（最新信息）
        Map<String, Map<String, Object>> latest = new HashMap<>();
        for (Map<String, Object> r : rows) {
            String code = (String) r.get("user_code");
            if (code == null || code.trim().isEmpty()) {
                log.warn("跳过 user_code 为空的记录 id={}", r.get("id"));
                continue;
            }
            code = code.trim();
            Long id = ((Number) r.get("id")).longValue();
            Map<String, Object> prev = latest.get(code);
            if (prev == null || id > ((Number) prev.get("id")).longValue()) {
                latest.put(code, r);
            }
        }
        log.info("去重后 user 数：{}", latest.size());

        // 3. 插入 user，建 code->userId 映射
        String defaultHash = passwordService.encode("dlust123456");
        Map<String, Long> codeToUserId = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : latest.entrySet()) {
            Map<String, Object> r = e.getValue();
            User u = new User();
            u.setUserCode(e.getKey());
            u.setPassword(defaultHash);
            u.setName((String) r.get("name"));
            u.setGender(((String) r.get("gender")));
            u.setPhone((String) r.get("phone"));
            u.setCollege((String) r.get("college"));
            u.setMajor((String) r.get("major"));
            try {
                userMapper.insertForMigration(u);
                codeToUserId.put(e.getKey(), u.getId());
            } catch (Exception ex) {
                log.error("插入 user 失败 code={} : {}", e.getKey(), ex.getMessage());
            }
        }

        // 4. 回填 participant.user_id（按 user_code 关联）
        int updated = 0;
        for (Map<String, Object> r : rows) {
            String code = (String) r.get("user_code");
            if (code == null) continue;
            Long userId = codeToUserId.get(code.trim());
            if (userId == null) continue;
            Long pid = ((Number) r.get("id")).longValue();
            participantMapper.updateUserIdForMigration(pid, userId);
            updated++;
        }
        log.info("回填 participant.user_id 数：{}", updated);
        log.warn("=== 迁移完成。请手动执行 DDL 收尾（见 plan Task 6 Step 3）后，删除本 Runner ===");
    }
}
```

- [ ] **Step 2: 迁移前备份（执行前向用户确认）**

```bash
mysqldump -h localhost -u root -p sport_db user participant registration > backup_before_account_migration.sql
```
> 按项目 feedback_db_policy，**执行任何 DDL/DML 前必须向用户确认**。

- [ ] **Step 3: 执行 DDL 准备（向用户确认后）**

```sql
-- user 表：清空旧微信数据 + 改造结构
TRUNCATE TABLE user;
ALTER TABLE user ADD COLUMN user_code VARCHAR(30) NOT NULL COMMENT '学号/工号';
ALTER TABLE user ADD COLUMN password VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希';
ALTER TABLE user ADD COLUMN name VARCHAR(50) NOT NULL COMMENT '姓名';
ALTER TABLE user ADD COLUMN gender VARCHAR(4) NOT NULL COMMENT '性别';
ALTER TABLE user ADD COLUMN college VARCHAR(100) DEFAULT NULL COMMENT '学院';
ALTER TABLE user ADD COLUMN major VARCHAR(100) DEFAULT NULL COMMENT '专业/单位';
-- phone 已存在，保留
ALTER TABLE user DROP COLUMN openid;
ALTER TABLE user DROP COLUMN union_id;
ALTER TABLE user DROP COLUMN nickname;
ALTER TABLE user DROP COLUMN avatar_url;
ALTER TABLE user DROP COLUMN session_key;
ALTER TABLE user ADD UNIQUE KEY uk_user_code (user_code);

-- participant 表：加 user_id 列（先可空，迁移后改 NOT NULL）
ALTER TABLE participant ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT '关联用户ID';
```

- [ ] **Step 4: 跑迁移**

```bash
cd SportBackend && mvn spring-boot:run -Dspring-boot.run.profiles=migrate
```
观察日志确认 user 插入数 + participant.user_id 回填数符合预期。Ctrl+C 停止。

- [ ] **Step 5: DDL 收尾（向用户确认后）**

```sql
-- 检查是否有 user_id 为空的 participant（脏数据）
SELECT COUNT(*) FROM participant WHERE user_id IS NULL;
-- 人工处理后，删旧字段 + 加约束
ALTER TABLE participant DROP COLUMN user_code;
ALTER TABLE participant DROP COLUMN name;
ALTER TABLE participant DROP COLUMN phone;
ALTER TABLE participant DROP COLUMN gender;
ALTER TABLE participant DROP COLUMN college;
ALTER TABLE participant DROP COLUMN major;
ALTER TABLE participant MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '关联用户ID';
ALTER TABLE participant ADD UNIQUE KEY uk_meeting_user (sports_meeting_id, user_id);
```

- [ ] **Step 6: 删除迁移代码（迁移期临时方法）**

- 删 `MigrationRunner.java`
- 删 ParticipantMapper.xml 里的 `selectAllForMigration` / `updateUserIdForMigration`
- 删 ParticipantMapper.java 接口对应方法
- 删 UserMapper.java/xml 的 `insertForMigration`（与 insert 重复）

- [ ] **Step 7: 验证迁移结果**

```sql
SELECT COUNT(*) FROM user;  -- 应 = 去重后学号数
SELECT COUNT(*) FROM participant WHERE user_id IS NULL;  -- 应 = 0
SELECT u.user_code, u.name, p.sports_meeting_id FROM user u JOIN participant p ON u.id=p.user_id LIMIT 10;
```

- [ ] **Step 8: Commit（迁移代码已删，只 commit SQL 脚本同步）**

```bash
git add SportBackend/sql/user.sql SportBackend/sql/participant.sql
git commit -m "chore: user/participant 表结构同步（账号化改造）"
```

---

## 阶段 D：后端认证

### Task 7: UserAuthController 登录重写 + WebMvcConfig 拦截列表

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserAuthController.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/config/WebMvcConfig.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/UserService.java`（如存在）
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/UserServiceImpl.java`（如存在）

- [ ] **Step 1: UserService 加 loginByUserCode 方法**

```java
// UserService.java
User loginByUserCode(String userCode, String password);
```

```java
// UserServiceImpl.java
@Autowired private UserMapper userMapper;
@Autowired private PasswordService passwordService;

@Override
public User loginByUserCode(String userCode, String password) {
    User u = userMapper.selectByUserCode(userCode);
    if (u == null) throw new RuntimeException("账号不存在");
    if (!passwordService.matches(password, u.getPassword()))
        throw new RuntimeException("密码错误");
    return u;
}
```

- [ ] **Step 2: 重写 UserAuthController 登录方法**

删除原 `POST /api/auth/login`（jscode2session）方法，替换为：

```java
@PostMapping("/login")
public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
    String userCode = body.get("userCode");
    String password = body.get("password");
    if (userCode == null || password == null)
        return Result.error(400, "账号密码不能为空");
    User u = userService.loginByUserCode(userCode, password);
    String token = jwtUtil.generateToken(u.getId());
    Map<String, Object> data = new HashMap<>();
    data.put("token", token);
    data.put("userId", u.getId());
    data.put("userCode", u.getUserCode());
    data.put("name", u.getName());
    return Result.success(data);
}
```

删除 `httpGet`（微信 jscode2session 调用）私有方法、`WxLoginResponse` 内部类（如有）。

- [ ] **Step 3: 加改密码接口**

在 UserAuthController 加：
```java
@PostMapping("/change-password")
public Result<String> changePassword(@RequestAttribute("userId") Long userId,
                                     @RequestBody Map<String, String> body) {
    String oldPwd = body.get("oldPassword");
    String newPwd = body.get("newPassword");
    if (oldPwd == null || newPwd == null) return Result.error(400, "参数缺失");
    User u = userMapper.selectById(userId);
    if (u == null || !passwordService.matches(oldPwd, u.getPassword()))
        return Result.error(400, "原密码错误");
    userMapper.updatePassword(userId, passwordService.encode(newPwd));
    return Result.success("修改成功");
}
```

- [ ] **Step 4: WebMvcConfig 拦截列表更新**

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
        .addPathPatterns(
            "/api/register/submit",
            "/api/register/cancel",
            "/api/my/**",
            "/api/auth/change-password"
        );
}
```
> 删除原微信用户信息接口的拦截（/api/auth/phone, /api/auth/info, /api/auth/avatar, /api/auth/nickname, /api/notice/*/read 等）—— 这些接口改造后要么废弃要么改为参赛人员信息接口。

- [ ] **Step 5: 编译 + commit（与 Task 2-5 + Task 8-10 一起，里程碑提交）**

暂不 commit，继续 Task 8。

---

## 阶段 E：报名接口

### Task 8: UserRegisterController 改为取 JWT userId

**Files:**
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/UserRegisterController.java`

- [ ] **Step 1: 改 submit 方法签名**

```java
@PostMapping("/submit")
public Result<String> submit(@RequestAttribute("userId") Long userId,
                             @RequestBody Map<String, Object> body) {
    Long sportsMeetingId = ((Number) body.get("sportsMeetingId")).longValue();
    Long eventId = ((Number) body.get("eventId")).longValue();
    // 不再从前端取 userCode/name/phone/gender/college/major —— 从 JWT userId 推导
    // 1. upsert participant(user_id + sports_meeting_id)
    Participant p = participantMapper.selectByUserIdAndSportsMeetingId(userId, sportsMeetingId);
    if (p == null) {
        p = new Participant();
        p.setSportsMeetingId(sportsMeetingId);
        p.setUserId(userId);
        participantMapper.insert(p);
    }
    // 2. 后续报名逻辑用 p.getId() 作为 participantId（与原逻辑一致）
    // ... 原有的 schedule/event 限额校验、registration insert 逻辑保留
}
```

- [ ] **Step 2: 删除报名体里的 userCode/name/phone 等参数读取**

前端不再传这些字段。RegistrationServiceImpl 里 `checkRegisterLimit` 原本按 participant 查的，现在 participant 已确定（p.getId()），逻辑不变。

- [ ] **Step 3: 暂不 commit（与 Task 2-5、7、9-10 一起）**

---

## 阶段 F：管理端后端

### Task 9: AdminUserController + AdminParticipantController 单条 CRUD 适配

**Files:**
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminUserController.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminParticipantController.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/ParticipantService.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/ParticipantServiceImpl.java`

- [ ] **Step 1: AdminUserController（重置密码）**

```java
package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.UserService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.util.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {
    @Autowired private UserService userService;
    @Autowired private PasswordService passwordService;

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        if (userId == null) return Result.error(400, "userId 缺失");
        userService.resetPassword(userId, passwordService.encode("dlust123456"));
        return Result.success("已重置为默认密码 dlust123456");
    }
}
```

UserService 加 `void resetPassword(Long userId, String hash)`，Impl 里 `userMapper.updatePassword(userId, hash)`。

- [ ] **Step 2: AdminParticipantController 单条新增/编辑适配**

原 `POST /add` 直接接收 Participant（含 userCode/name/...）。改造后：前端传 `{sportsMeetingId, userCode, name, gender, phone, college, major}`，后端：
```java
@PostMapping("/add")
public Result<String> add(@RequestBody Map<String, Object> body) {
    Long sportsMeetingId = ((Number) body.get("sportsMeetingId")).longValue();
    String userCode = (String) body.get("userCode");
    // 1. upsert user
    User u = userMapper.selectByUserCode(userCode);
    if (u == null) {
        u = new User();
        u.setUserCode(userCode);
        u.setPassword(passwordService.encode("dlust123456"));  // 新建默认密码
        // set name/gender/phone/college/major from body
        userMapper.insert(u);
    } else {
        // 更新非空字段
        // set name/gender/phone/college/major from body
        // userMapper.updateProfile(u)
    }
    // 2. 创建 participant 关联
    Participant p = participantMapper.selectByUserIdAndSportsMeetingId(u.getId(), sportsMeetingId);
    if (p == null) {
        p = new Participant();
        p.setSportsMeetingId(sportsMeetingId);
        p.setUserId(u.getId());
        participantMapper.insert(p);
    }
    return Result.success("添加成功");
}
```

> 编辑接口 `/update` 改为更新 user 的 name/phone/gender/college/major（participant 只有 team_id 可改，team_id 走代表队管理）。

- [ ] **Step 3: 暂不 commit**

---

### Task 10: Excel 导入 + 模板下载

**Files:**
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/Service/ImportService.java`
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/Service/Impl/ImportServiceImpl.java`
- Modify: `SportBackend/src/main/java/com/dlust/sportbackend/Controller/admin/AdminParticipantController.java`
- Create: `SportBackend/src/main/java/com/dlust/sportbackend/dto/ParticipantImportRow.java`

- [ ] **Step 1: ParticipantImportRow DTO（EasyExcel 行模型）**

```java
package com.dlust.sportbackend.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ParticipantImportRow {
    @ExcelProperty("学号/工号") private String userCode;
    @ExcelProperty("姓名") private String name;
    @ExcelProperty("性别") private String gender;
    @ExcelProperty("电话") private String phone;
    @ExcelProperty("学院") private String college;
    @ExcelProperty("专业/单位") private String major;
}
```

- [ ] **Step 2: ImportService 接口 + 实现**

```java
// 接口
package com.dlust.sportbackend.Service;
import com.dlust.sportbackend.dto.ParticipantImportRow;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface ImportService {
    Map<String, Object> importParticipants(MultipartFile file, Long sportsMeetingId);
}
```

```java
// 实现（核心逻辑）
@Override
public Map<String, Object> importParticipants(MultipartFile file, Long sportsMeetingId) {
    List<ParticipantImportRow> rows;
    try {
        rows = EasyExcel.read(file.getInputStream())
                .head(ParticipantImportRow.class).sheet().doReadSync();
    } catch (Exception e) {
        throw new RuntimeException("Excel 解析失败：" + e.getMessage());
    }
    String defaultHash = passwordService.encode("dlust123456");
    int success = 0;
    List<Map<String, Object>> failed = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
        int rowNum = i + 2; // Excel 行号（含表头）
        ParticipantImportRow row = rows.get(i);
        try {
            if (row.getUserCode() == null || row.getUserCode().trim().isEmpty())
                throw new RuntimeException("学号为空");
            if (row.getName() == null || row.getName().trim().isEmpty())
                throw new RuntimeException("姓名为空");
            if (row.getGender() == null || row.getGender().trim().isEmpty())
                throw new RuntimeException("性别为空");
            String code = row.getUserCode().trim();
            // 1. upsert user
            User u = userMapper.selectByUserCode(code);
            if (u == null) {
                u = new User();
                u.setUserCode(code);
                u.setPassword(defaultHash);
                fillUserFromRow(u, row);
                userMapper.insert(u);
            } else {
                fillUserFromRowIfPresent(u, row); // 只更新非空字段
                userMapper.updateProfile(u); // 需加此方法
            }
            // 2. upsert participant
            Participant p = participantMapper.selectByUserIdAndSportsMeetingId(u.getId(), sportsMeetingId);
            if (p == null) {
                p = new Participant();
                p.setSportsMeetingId(sportsMeetingId);
                p.setUserId(u.getId());
                participantMapper.insert(p);
            }
            success++;
        } catch (Exception ex) {
            Map<String, Object> f = new HashMap<>();
            f.put("row", rowNum);
            f.put("reason", ex.getMessage());
            failed.add(f);
        }
    }
    Map<String, Object> result = new HashMap<>();
    result.put("successCount", success);
    result.put("failedRows", failed);
    return result;
}

private void fillUserFromRow(User u, ParticipantImportRow row) {
    u.setName(row.getName().trim());
    u.setGender(row.getGender().trim());
    u.setPhone(row.getPhone());
    u.setCollege(row.getCollege());
    u.setMajor(row.getMajor());
}

private void fillUserFromRowIfPresent(User u, ParticipantImportRow row) {
    // 仅当 Excel 单元格非空才更新（空值不覆盖已有数据）
    if (row.getName() != null && !row.getName().trim().isEmpty()) u.setName(row.getName().trim());
    if (row.getGender() != null && !row.getGender().trim().isEmpty()) u.setGender(row.getGender().trim());
    if (row.getPhone() != null && !row.getPhone().trim().isEmpty()) u.setPhone(row.getPhone());
    if (row.getCollege() != null && !row.getCollege().trim().isEmpty()) u.setCollege(row.getCollege());
    if (row.getMajor() != null && !row.getMajor().trim().isEmpty()) u.setMajor(row.getMajor());
}
```

> UserMapper 需加 `updateProfile(User u)` 方法更新 name/gender/phone/college/major（除 userCode/password 外）。

- [ ] **Step 3: Controller 接口**

```java
@Autowired private ImportService importService;

@PostMapping("/import")
public Result<Map<String, Object>> importParticipants(
        @RequestParam("file") MultipartFile file,
        @RequestParam("sportsMeetingId") Long sportsMeetingId) {
    return Result.success(importService.importParticipants(file, sportsMeetingId));
}

@GetMapping("/import-template")
public void downloadTemplate(HttpServletResponse response) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=participant-template.xlsx");
    EasyExcel.write(response.getOutputStream(), ParticipantImportRow.class)
            .sheet("参赛人员导入").doWrite(new ArrayList<>());
}
```

- [ ] **Step 4: 整体编译（里程碑）**

```bash
cd SportBackend && mvn -q compile && mvn -q test
```
Expected: 编译通过 + 所有测试通过（含 PasswordServiceTest）

- [ ] **Step 5: 里程碑 Commit（Task 2-10 合并）**

```bash
git add SportBackend/
git commit -m "feat: 参赛人员账号化改造（数据层+业务层重构）

- User 实体重写为账号主体（user_code/password/...）
- Participant 精简为运动会参赛身份（user_id 关联）
- UserMapper/ParticipantMapper/RegistrationMapper/ResultMapper 全部 JOIN user
- UserAuthController 登录改为账号密码 + 改密码接口
- UserRegisterController 报名取 JWT userId
- AdminUserController 重置密码；AdminParticipantController 单条 CRUD 适配 user 关联
- ImportService Excel 批量导入（EasyExcel，幂等）+ 模板下载"
```

---

## 阶段 G：管理端 Vue

### Task 11: api 接口调整 + MeetingDetailView 参赛人员 tab 改造（含重置密码）

**Files:**
- Modify: `webManageProject/src/api/user.ts`（如不存在则创建）
- Modify: `webManageProject/src/api/participant.ts`
- Modify: `webManageProject/src/views/MeetingDetailView.vue`

- [ ] **Step 1: api/user.ts 加重置密码**

```ts
import request from '@/utils/request'

export function resetPassword(userId: number) {
  return request.post('/admin/user/reset-password', { userId })
}
```

- [ ] **Step 2: api/participant.ts 加导入接口**

```ts
export function importParticipants(file: File, sportsMeetingId: number) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('sportsMeetingId', String(sportsMeetingId))
  return request.post('/admin/participant/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function downloadImportTemplate() {
  return request.get('/admin/participant/import-template', { responseType: 'blob' })
}
```

- [ ] **Step 3: MeetingDetailView 参赛人员 tab 改造**

- 列表列保持（userCode/name/gender/phone/college/major/teamName），数据从新 VO 取（字段名不变，后端 JOIN 别名对齐）
- 工具栏加「批量导入」按钮（Task 12 实现弹窗），先放占位
- 单条新增/编辑表单：保留字段，提交时调改造后的 `/add`（body = {sportsMeetingId, userCode, name, gender, phone, college, major}）
- 操作列加「重置密码」按钮 → `resetPassword(row.userId)` → toast「已重置为 dlust123456」

- [ ] **Step 4: 构建验证**

```bash
cd webManageProject && npm run build
```
Expected: TypeScript 通过

- [ ] **Step 5: Commit**

```bash
git add webManageProject/src/api/ webManageProject/src/views/MeetingDetailView.vue
git commit -m "feat: 管理端参赛人员 tab 适配账号化（重置密码 + 新增/编辑走 user 关联）"
```

---

### Task 12: 批量导入对话框组件

**Files:**
- Modify: `webManageProject/src/views/MeetingDetailView.vue`

- [ ] **Step 1: 加导入对话框**

工具栏「批量导入」按钮 → 打开对话框：
- 文件选择（accept .xlsx）
- 「下载模板」链接
- 确认导入 → 调 `importParticipants(file, meetingId)` → 显示结果（成功 N 条，失败行列表）

```vue
<el-dialog v-model="importDialogVisible" title="批量导入参赛人员" width="500px">
  <div style="margin-bottom:12px">
    <el-link type="primary" @click="handleDownloadTemplate">下载导入模板</el-link>
  </div>
  <input type="file" accept=".xlsx" @change="(e:any) => importFile = e.target.files[0]" />
  <div v-if="importResult" style="margin-top:16px">
    <el-alert :title="`成功 ${importResult.successCount} 条`" type="success" :closable="false" />
    <div v-if="importResult.failedRows.length" style="margin-top:8px;color:#f56c6c;font-size:13px">
      失败行：<span v-for="f in importResult.failedRows" :key="f.row">第{{ f.row }}行({{f.reason}}) </span>
    </div>
  </div>
  <template #footer>
    <el-button @click="importDialogVisible = false">关闭</el-button>
    <el-button type="primary" :disabled="!importFile" :loading="importing" @click="handleImport">导入</el-button>
  </template>
</el-dialog>
```

```ts
const importDialogVisible = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<any>(null)
const importing = ref(false)

async function handleImport() {
  importing.value = true
  try {
    const res: any = await importParticipants(importFile.value!, meetingId as unknown as number)
    importResult.value = res.data || res
    ElMessage.success(`成功导入 ${importResult.value.successCount} 条`)
    fetchParticipants() // 刷新列表
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '导入失败')
  } finally { importing.value = false }
}

async function handleDownloadTemplate() {
  const res: any = await downloadImportTemplate()
  const blob = new Blob([res.data || res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'participant-template.xlsx'; a.click()
  URL.revokeObjectURL(url)
}
```

- [ ] **Step 2: 构建验证 + Commit**

```bash
npm run build && cd .. && git add webManageProject/src/views/MeetingDetailView.vue
git commit -m "feat: 管理端参赛人员批量导入对话框（EasyExcel 模板下载 + 结果汇总）"
```

---

## 阶段 H：小程序登录

### Task 13: 登录页 + auth.js 改造 + 报名触发登录

**Files:**
- Create: `sportdocu/pages/login/login.wxml`
- Create: `sportdocu/pages/login/login.wxss`
- Create: `sportdocu/pages/login/login.js`
- Create: `sportdocu/pages/login/login.json`
- Modify: `sportdocu/utils/auth.js`
- Modify: `sportdocu/app.json`（注册 login 页）
- Modify: `sportdocu/pages/register-form/register-form.js`（触发登录）

- [ ] **Step 1: login.json**

```json
{ "usingComponents": {}, "navigationBarTitleText": "登录" }
```

- [ ] **Step 2: login.wxml**

```xml
<view class="login-page">
  <view class="login-card">
    <text class="login-title">参赛人员登录</text>
    <input class="login-input" placeholder="学号/工号" value="{{userCode}}" bindinput="onInputCode" />
    <input class="login-input" placeholder="密码" password value="{{password}}" bindinput="onInputPwd" />
    <button class="login-btn" loading="{{loading}}" bindtap="handleLogin">登录</button>
    <text class="login-hint">默认密码：dlust123456（首次登录请修改）</text>
  </view>
</view>
```

- [ ] **Step 3: login.wxss**

```css
.login-page { padding: 60rpx 40rpx; }
.login-card { background:#fff; border-radius:16rpx; padding:48rpx 40rpx; box-shadow:0 4rpx 16rpx rgba(0,0,0,0.06); display:flex; flex-direction:column; gap:24rpx; }
.login-title { font-size:36rpx; font-weight:600; color:#303133; text-align:center; margin-bottom:16rpx; }
.login-input { border:1rpx solid #dcdfe6; border-radius:8rpx; padding:20rpx 24rpx; font-size:28rpx; }
.login-btn { background:#2196F3; color:#fff; border-radius:8rpx; margin-top:16rpx; }
.login-hint { font-size:22rpx; color:#909399; text-align:center; }
```

- [ ] **Step 4: login.js**

```js
const auth = require('../../utils/auth')

Page({
  data: { userCode: '', password: '', loading: false, redirect: '' },
  onLoad(opts) { if (opts.redirect) this.setData({ redirect: decodeURIComponent(opts.redirect) }) },
  onInputCode(e) { this.setData({ userCode: e.detail.value }) },
  onInputPwd(e) { this.setData({ password: e.detail.value }) },
  async handleLogin() {
    const { userCode, password } = this.data
    if (!userCode || !password) { wx.showToast({ title: '请输入账号密码', icon: 'none' }); return }
    this.setData({ loading: true })
    try {
      await auth.login(userCode, password)
      wx.showToast({ title: '登录成功', icon: 'success' })
      if (this.data.redirect) wx.redirectTo({ url: this.data.redirect })
      else wx.switchTab({ url: '/pages/profile/profile' })
    } catch (e) {
      wx.showToast({ title: e.message || '登录失败', icon: 'none' })
    } finally { this.setData({ loading: false }) }
  }
})
```

- [ ] **Step 5: 改造 auth.js（去微信登录）**

完整重写 `sportdocu/utils/auth.js`：
```js
const BASE_URL = 'http://localhost:8080'

function getToken() { return wx.getStorageSync('token') }
function isLoggedIn() { return !!getToken() }

function login(userCode, password) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + '/api/auth/login',
      method: 'POST',
      data: { userCode, password },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          wx.setStorageSync('token', res.data.data.token)
          wx.setStorageSync('userInfo', {
            userId: res.data.data.userId,
            userCode: res.data.data.userCode,
            name: res.data.data.name
          })
          resolve(res.data.data)
        } else { reject(new Error(res.data?.message || '账号密码错误')) }
      },
      fail: () => reject(new Error('网络错误'))
    })
  })
}

function logout() { wx.removeStorageSync('token'); wx.removeStorageSync('userInfo') }

function request(options) {
  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      url: BASE_URL + options.url,
      header: { ...(options.header || {}), Authorization: 'Bearer ' + (getToken() || '') },
      success: (res) => {
        if (res.statusCode === 401) {
          // token 失效：清登录态，跳登录页
          logout()
          wx.redirectTo({ url: '/pages/login/login?redirect=' + encodeURIComponent('/' + getCurrentPages()[0]?.route || '') })
          reject(new Error('未登录'))
          return
        }
        resolve(res.data)
      },
      fail: reject
    })
  })
}

function requireLogin(redirect) {
  if (!isLoggedIn()) {
    wx.navigateTo({ url: '/pages/login/login?redirect=' + encodeURIComponent(redirect || '') })
    return false
  }
  return true
}

module.exports = { BASE_URL, getToken, isLoggedIn, login, logout, request, requireLogin }
```

- [ ] **Step 6: app.json 注册 login 页**

在 pages 数组加 `"pages/login/login"`。

- [ ] **Step 7: 报名页触发登录**

`register-form.js` onLoad 开头加：
```js
onLoad(options) {
  const redirect = '/pages/register-form/register-form?' + Object.entries(options).map(([k,v])=>`${k}=${v}`).join('&')
  if (!auth.requireLogin(redirect)) return
  // 原有 onLoad 逻辑
}
```

- [ ] **Step 8: 手测 + Commit**

```bash
# 手测：游客打开报名页 → 跳登录页 → 输入学号 dlust123456 → 登录成功回报名页
git add sportdocu/pages/login/ sportdocu/utils/auth.js sportdocu/app.json sportdocu/pages/register-form/register-form.js
git commit -m "feat: 小程序账号密码登录页 + auth.js 重构（去微信登录）+ 报名触发登录"
```

---

## 阶段 I：小程序我的

### Task 14: profile 改造 + 我的参赛项目页 + 后端 /api/my/registrations

**Files:**
- Modify: `sportdocu/pages/profile/profile.wxml`
- Modify: `sportdocu/pages/profile/profile.wxss`
- Modify: `sportdocu/pages/profile/profile.js`
- Create: `sportdocu/pages/my-registrations/my-registrations.wxml`
- Create: `sportdocu/pages/my-registrations/my-registrations.wxss`
- Create: `sportdocu/pages/my-registrations/my-registrations.js`
- Create: `sportdocu/pages/my-registrations/my-registrations.json`
- Modify: `sportdocu/app.json`
- Modify: 后端：`Controller/user/` 新增 MyController.java；`Mapper` 新增查询；`WebMvcConfig` 已含 `/api/my/**`（Task 7 已加）

- [ ] **Step 1: 后端 MyController + 查询**

```java
// Controller/user/MyController.java
package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.RegistrationMapper;
import com.dlust.sportbackend.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/my")
public class MyController {
    @Autowired private ParticipantMapper participantMapper;
    @Autowired private RegistrationMapper registrationMapper;

    @GetMapping("/registrations")
    public Result<List<Map<String, Object>>> myRegistrations(@RequestAttribute("userId") Long userId) {
        // 1. 查该 user 的所有 participant（跨运动会）
        List<Participant> participants = participantMapper.selectByUserId(userId);
        // 2. 按 sports_meeting_id 分组
        Map<Long, List<Participant>> grouped = participants.stream()
                .collect(Collectors.groupingBy(Participant::getSportsMeetingId));
        // 3. 每组组装 {sportsMeetingId, sportsMeetingName, items:[...]}
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Participant>> e : grouped.entrySet()) {
            List<Participant> ps = e.getValue();
            Map<String, Object> group = new HashMap<>();
            group.put("sportsMeetingId", e.getKey());
            group.put("sportsMeetingName", ps.get(0).getSportsMeetingName()); // JOIN 带出
            List<Map<String, Object>> items = new ArrayList<>();
            for (Participant p : ps) {
                // 查该 participant 的所有报名记录（VO 含 eventName/scheduleName/category）
                var regs = registrationMapper.selectByParticipantId(p.getId());
                for (var r : regs) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("eventName", r.getEventName());
                    item.put("scheduleName", r.getScheduleName());
                    item.put("category", r.getCategory());
                    items.add(item);
                }
            }
            group.put("items", items);
            result.add(group);
        }
        return Result.success(result);
    }
}
```

ParticipantMapper 加：
- `List<Participant> selectByUserId(@Param("userId") Long userId)` —— SELECT 时 JOIN sports_meeting 取 `name AS sports_meeting_name`（Participant.java 加 `sportsMeetingName` 扩展字段）。
- RegistrationMapper 的 `selectByParticipantId` 确保 VO 含 `eventName/scheduleName/category`（如已有则复用）。

- [ ] **Step 2: profile.js 改造**

```js
const auth = require('../../utils/auth')

Page({
  data: { userInfo: null },
  onShow() {
    if (!auth.isLoggedIn()) { this.setData({ userInfo: null }); return }
    this.setData({ userInfo: wx.getStorageSync('userInfo') })
  },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }) },
  goMyRegistrations() {
    if (!auth.requireLogin('/pages/my-registrations/my-registrations')) return
    wx.navigateTo({ url: '/pages/my-registrations/my-registrations' })
  },
  changePassword() {
    if (!auth.requireLogin('/pages/profile/profile')) return
    wx.navigateTo({ url: '/pages/change-password/change-password' })
  },
  onLogout() {
    auth.logout()
    this.setData({ userInfo: null })
    wx.showToast({ title: '已退出', icon: 'success' })
  }
})
```

- [ ] **Step 2b: 改密码页（4 文件）**

`pages/change-password/change-password.json`:
```json
{ "usingComponents": {}, "navigationBarTitleText": "修改密码" }
```

`change-password.wxml`:
```xml
<view class="cp-page">
  <view class="cp-card">
    <input class="cp-input" placeholder="原密码" password value="{{oldPassword}}" bindinput="onInputOld" />
    <input class="cp-input" placeholder="新密码" password value="{{newPassword}}" bindinput="onInputNew" />
    <input class="cp-input" placeholder="确认新密码" password value="{{confirm}}" bindinput="onInputConfirm" />
    <button class="cp-btn" loading="{{loading}}" bindtap="handleSubmit">提交</button>
  </view>
</view>
```

`change-password.wxss`（复用 login 页样式风格）:
```css
.cp-page { padding: 60rpx 40rpx; }
.cp-card { background:#fff; border-radius:16rpx; padding:48rpx 40rpx; box-shadow:0 4rpx 16rpx rgba(0,0,0,0.06); display:flex; flex-direction:column; gap:24rpx; }
.cp-input { border:1rpx solid #dcdfe6; border-radius:8rpx; padding:20rpx 24rpx; font-size:28rpx; }
.cp-btn { background:#2196F3; color:#fff; border-radius:8rpx; margin-top:16rpx; }
```

`change-password.js`:
```js
const auth = require('../../utils/auth')
Page({
  data: { oldPassword:'', newPassword:'', confirm:'', loading:false },
  onInputOld(e){this.setData({oldPassword:e.detail.value})},
  onInputNew(e){this.setData({newPassword:e.detail.value})},
  onInputConfirm(e){this.setData({confirm:e.detail.value})},
  async handleSubmit() {
    const { oldPassword, newPassword, confirm } = this.data
    if (!oldPassword || !newPassword) return wx.showToast({title:'请填写完整',icon:'none'})
    if (newPassword !== confirm) return wx.showToast({title:'两次新密码不一致',icon:'none'})
    this.setData({loading:true})
    try {
      const res = await auth.request({ url:'/api/auth/change-password', method:'POST', data:{oldPassword,newPassword} })
      if (res.code === 200) { wx.showToast({title:'修改成功，请重新登录',icon:'success'}); auth.logout(); wx.redirectTo({url:'/pages/login/login'}) }
      else wx.showToast({title:res.message||'修改失败',icon:'none'})
    } catch(e){ wx.showToast({title:'修改失败',icon:'none'}) }
    finally { this.setData({loading:false}) }
  }
})
```

app.json 的 pages 数组加 `"pages/change-password/change-password"`。

- [ ] **Step 3: profile.wxml 改造**

```xml
<view class="profile-page">
  <view wx:if="{{!userInfo}}" class="login-prompt">
    <text>请先登录</text>
    <button bindtap="goLogin" class="login-btn">登录</button>
  </view>
  <view wx:else>
    <view class="user-card">
      <text class="user-name">{{userInfo.name}}</text>
      <text class="user-code">{{userInfo.userCode}}</text>
    </view>
    <view class="menu">
      <view class="menu-item" bindtap="goMyRegistrations">我的参赛项目</view>
      <view class="menu-item" bindtap="changePassword">修改密码</view>
      <view class="menu-item logout" bindtap="onLogout">退出登录</view>
    </view>
  </view>
</view>
```

- [ ] **Step 4: profile.wxss 配套样式**（卡片 + 菜单项）

- [ ] **Step 5: my-registrations 页（wxml/wxss/js/json）**

仿 event-registrations 页结构：按运动会分组，每组列项目卡片。调 `auth.request({url:'/api/my/registrations'})`。

- [ ] **Step 6: app.json 注册 my-registrations**

- [ ] **Step 7: 手测 + Commit**

```bash
# 手测：游客点「我的」→ 显示登录按钮 → 登录后显示学号/姓名 + 菜单 → 点我的参赛项目 → 看到分组列表
git add sportdocu/pages/profile/ sportdocu/pages/my-registrations/ sportdocu/pages/change-password/ sportdocu/app.json SportBackend/src/main/java/com/dlust/sportbackend/Controller/user/MyController.java SportBackend/src/main/resources/mapper/ParticipantMapper.xml
git commit -m "feat: 小程序我的页（登录态判断 + 我的参赛项目 + 改密码入口）"
```

---

## 最终验证

- [ ] **后端**：`mvn -q compile && mvn -q test`（含 PasswordServiceTest）全绿
- [ ] **前端**：`npm run build` TypeScript 通过
- [ ] **端到端手测**：
  - 管理端：Excel 导入 5 人 → 列表显示 → 重置密码 → 小程序用 dlust123456 登录成功 → 改密码 → 新密码登录
  - 小程序：游客浏览首页/详情/赛程/项目正常 → 点报名跳登录 → 登录后报名成功 → 「我的」显示参赛项目 → 退出后状态清空
  - 数据：原 registration 记录仍可通过新账号查到（迁移未破坏关联）

---

## 注意事项

- **迁移顺序约束**：Task 6（迁移）必须在 Task 2-5 + Task 7-10 代码编译通过后才执行（迁移代码依赖新结构）。建议：先写完 Task 1-10 代码（不 commit），整体编译通过 → 执行迁移 Task 6 → 再 commit 里程碑 → 再做前端 Task 11-14。
- **频繁提交 vs 大重构**：Task 2-10 因中间态编译不过，合并为一次里程碑提交。其余 Task 独立提交。
- **DDL 执行前必须向用户确认**（项目 feedback_db_policy）。Task 6 Step 3/5 的 ALTER 语句执行前要明确询问。
- **BCrypt 性能**：默认 10 轮，单次约 100ms，批量导入 100 人约 10s，可接受。
- **YAGNI**：不引入 Spring Security 完整框架，只用 spring-security-crypto 的 BCryptPasswordEncoder；不做权限分级（admin 靠路径前缀，user 靠拦截列表）；不做 token 黑名单（JWT 无状态，退出仅清本地）。
