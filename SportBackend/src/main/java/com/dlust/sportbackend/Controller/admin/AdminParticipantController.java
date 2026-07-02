package com.dlust.sportbackend.Controller.admin;

import com.alibaba.excel.EasyExcel;
import com.dlust.sportbackend.Service.ImportService;
import com.dlust.sportbackend.Service.ParticipantService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.dto.ParticipantImportRow;
import com.dlust.sportbackend.entity.Participant;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/participant")
public class AdminParticipantController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ImportService importService;

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

    /**
     * 新增参赛人员。请求体携带 sportsMeetingId、userCode、name、gender、phone、college、major。
     * 内部 upsert user（不存在则建，默认密码 dlust123456）+ upsert participant 关联（幂等）。
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Participant participant) {
        log.info("添加参赛人员: userCode={}, sportsMeetingId={}",
                participant.getUserCode(), participant.getSportsMeetingId());
        participantService.add(participant);
        return Result.success("添加成功");
    }

    /**
     * 编辑参赛人员资料：实际更新对应 user 的 name/gender/phone/college/major。
     * 请求体携带 id（participant.id）及上述字段。participant 行不变；team_id 不在此管理。
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody Participant participant) {
        log.info("更新参赛人员资料: id={}", participant.getId());
        participantService.update(participant);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除参赛人员（保留用户账号）: id={}", id);
        participantService.delete(id);
        return Result.success("删除成功");
    }

    @PutMapping("/clearTeam")
    public Result<String> clearTeam(@RequestParam Long participantId) {
        log.info("移出代表队: participantId={}", participantId);
        participantService.clearTeamId(participantId);
        return Result.success("移出成功");
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importParticipants(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sportsMeetingId") Long sportsMeetingId) {
        log.info("批量导入参赛人员: sportsMeetingId={}", sportsMeetingId);
        return Result.success(importService.importParticipants(file, sportsMeetingId));
    }

    @GetMapping("/import-template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=participant-template.xlsx");
        EasyExcel.write(response.getOutputStream(), ParticipantImportRow.class)
                .sheet("参赛人员导入").doWrite(new ArrayList<>());
    }
}
