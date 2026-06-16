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
