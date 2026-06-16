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
