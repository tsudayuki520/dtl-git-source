package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.TeamScoreAdjustmentService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.TeamScoreAdjustment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/teamScoreAdjustment")
public class AdminTeamScoreAdjustmentController {

    @Autowired
    private TeamScoreAdjustmentService teamScoreAdjustmentService;

    @GetMapping("/list")
    public Result<List<TeamScoreAdjustment>> list(@RequestParam Long teamId) {
        return Result.success(teamScoreAdjustmentService.getByTeamId(teamId));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, Object> body) {
        Long teamId = body.get("teamId") == null ? null : Long.valueOf(body.get("teamId").toString());
        BigDecimal deltaAmount = body.get("deltaAmount") == null ? null
                : new BigDecimal(body.get("deltaAmount").toString());
        String note = body.get("note") == null ? null : body.get("note").toString();
        log.info("添加代表队调整分: teamId={}, delta={}, note={}", teamId, deltaAmount, note);
        teamScoreAdjustmentService.add(teamId, deltaAmount, note);
        return Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除代表队调整分: id={}", id);
        teamScoreAdjustmentService.delete(id);
        return Result.success("删除成功");
    }
}
