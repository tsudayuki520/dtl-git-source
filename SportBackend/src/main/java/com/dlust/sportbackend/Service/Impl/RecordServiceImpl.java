package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.RecordMapper;
import com.dlust.sportbackend.Service.RecordService;
import com.dlust.sportbackend.entity.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecordServiceImpl implements RecordService {

    @Autowired
    private RecordMapper recordMapper;

    @Override
    public List<Record> getAll() {
        return recordMapper.selectAll();
    }

    @Override
    public void add(Record record) {
        recordMapper.insert(record);
    }

    @Override
    public void update(Record record) {
        recordMapper.updateById(record);
    }

    @Override
    public void delete(Long id) {
        recordMapper.deleteById(id);
    }
}
