package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.RegistrationService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.RegistrationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/registration")
public class AdminRegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @GetMapping("/list")
    public Result<List<RegistrationVO>> list(@RequestParam Long sportsMeetingId) {
        log.info("查询报名记录: sportsMeetingId={}", sportsMeetingId);
        return Result.success(registrationService.getBySportsMeetingId(sportsMeetingId));
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        log.info("更新报名状态: id={}, status={}", id, status);
        registrationService.update(id, status);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除报名记录: id={}", id);
        registrationService.delete(id);
        return Result.success("删除成功");
    }
}
