package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.TeamService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Team;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/team")
public class AdminTeamController {

    @Autowired
    private TeamService teamService;

    @GetMapping("/list")
    public Result<List<Team>> list(@RequestParam(required = false) Long sportsMeetingId,
                                   @RequestParam(required = false) Long groupTypeId) {
        if (groupTypeId != null) {
            return Result.success(teamService.getByGroupTypeId(groupTypeId));
        }
        return Result.success(teamService.getBySportsMeetingId(sportsMeetingId));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Team team) {
        log.info("添加代表队: name={}", team.getName());
        teamService.add(team);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Team team) {
        log.info("更新代表队: id={}", team.getId());
        teamService.update(team);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除代表队: id={}", id);
        teamService.delete(id);
        return Result.success("删除成功");
    }
}
