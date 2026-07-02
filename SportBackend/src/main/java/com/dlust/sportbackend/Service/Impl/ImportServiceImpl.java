package com.dlust.sportbackend.Service.Impl;

import com.alibaba.excel.EasyExcel;
import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.UserMapper;
import com.dlust.sportbackend.Service.ImportService;
import com.dlust.sportbackend.dto.ParticipantImportRow;
import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.User;
import com.dlust.sportbackend.util.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class ImportServiceImpl implements ImportService {
    @Autowired private UserMapper userMapper;
    @Autowired private ParticipantMapper participantMapper;
    @Autowired private PasswordService passwordService;

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
                    fillUserFromRowIfPresent(u, row);
                    userMapper.updateProfile(u);
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
        if (row.getName() != null && !row.getName().trim().isEmpty()) u.setName(row.getName().trim());
        if (row.getGender() != null && !row.getGender().trim().isEmpty()) u.setGender(row.getGender().trim());
        if (row.getPhone() != null && !row.getPhone().trim().isEmpty()) u.setPhone(row.getPhone());
        if (row.getCollege() != null && !row.getCollege().trim().isEmpty()) u.setCollege(row.getCollege());
        if (row.getMajor() != null && !row.getMajor().trim().isEmpty()) u.setMajor(row.getMajor());
    }
}
