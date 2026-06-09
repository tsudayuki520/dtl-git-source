package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.GroupType;
import java.util.List;

public interface GroupTypeService {
    List<GroupType> getBySportsMeetingId(Long sportsMeetingId);
    GroupType getById(Long id);
    void add(GroupType groupType);
    void update(GroupType groupType);
    void delete(Long id);
}
