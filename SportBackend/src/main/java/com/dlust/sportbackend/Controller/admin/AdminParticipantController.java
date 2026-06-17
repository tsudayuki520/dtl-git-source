package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.ParticipantService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Participant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/participant")
public class AdminParticipantController {

    @Autowired
    private ParticipantService participantService;

    @GetMapping("/list")
    public Result<List<Participant>> list(@RequestParam Long sportsMeetingId) {
        log.info("查询参赛人员: sportsMeetingId={}", sportsMeetingId);
        return Result.success(participantService.getBySportsMeetingId(sportsMeetingId));
    }

    @GetMapping("/listByTeam")
    public Result<List<Participant>> listByTeam(@RequestParam Long teamId) {
        log.info("查询代表队参赛人员: teamId={}", teamId);
        return Result.success(participantService.getByTeamId(teamId));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody Participant participant) {
        log.info("添加参赛人员: name={}", participant.getName());
        participantService.add(participant);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Participant participant) {
        log.info("更新参赛人员: id={}", participant.getId());
        participantService.update(participant);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除参赛人员: id={}", id);
        participantService.delete(id);
        return Result.success("删除成功");
    }

    @PutMapping("/clearTeam")
    public Result<String> clearTeam(@RequestParam Long participantId) {
        log.info("移出代表队: participantId={}", participantId);
        participantService.clearTeam(participantId);
        return Result.success("移出成功");
    }
}
