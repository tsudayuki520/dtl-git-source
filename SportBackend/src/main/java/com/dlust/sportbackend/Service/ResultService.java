package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Result;
import com.dlust.sportbackend.entity.ResultVO;
import java.util.List;

public interface ResultService {

    List<ResultVO> getBySportsMeetingId(Long sportsMeetingId);

    List<ResultVO> getByEventId(Long eventId);

    List<ResultVO> getByEventAndSchedule(Long eventId, Long scheduleId);

    void add(Result result);

    void update(Result result);

    void delete(Long id);
}
