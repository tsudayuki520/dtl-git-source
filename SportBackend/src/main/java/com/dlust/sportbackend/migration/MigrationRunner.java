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
        log.warn("=== 迁移完成。请手动执行 DDL 收尾（见 plan Task 6 Step 5）后，删除本 Runner ===");
    }
}
