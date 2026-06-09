package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.GroupTypeMapper;
import com.dlust.sportbackend.Service.GroupTypeService;
import com.dlust.sportbackend.entity.GroupType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupTypeServiceImpl implements GroupTypeService {

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
}
