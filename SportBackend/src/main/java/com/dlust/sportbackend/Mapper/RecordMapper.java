package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.Record;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordMapper {

    List<Record> selectAll();

    Record selectById(@Param("id") Long id);

    void insert(Record record);

    void updateById(Record record);

    void deleteById(@Param("id") Long id);
}
