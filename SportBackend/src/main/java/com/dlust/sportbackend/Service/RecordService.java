package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Record;
import java.util.List;

public interface RecordService {

    List<Record> getAll(Long sportsMeetingId, String eventName, String category);

    void add(Record record);

    void update(Record record);

    void delete(Long id);
}
