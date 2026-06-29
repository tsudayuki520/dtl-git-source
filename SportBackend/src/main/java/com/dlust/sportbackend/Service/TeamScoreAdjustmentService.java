package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.TeamScoreAdjustment;
import java.math.BigDecimal;
import java.util.List;

public interface TeamScoreAdjustmentService {

    List<TeamScoreAdjustment> getByTeamId(Long teamId);

    void add(Long teamId, BigDecimal deltaAmount, String note);

    void delete(Long id);
}
