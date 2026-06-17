package com.dlust.sportbackend.Controller.admin;

import com.dlust.sportbackend.Service.GroupTypeService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.GroupType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/group-type")
public class AdminGroupTypeController {

    @Autowired
    private GroupTypeService groupTypeService;

    @GetMapping("/list")
    public Result<List<GroupType>> list(@RequestParam Long sportsMeetingId) {
        return Result.success(groupTypeService.getBySportsMeetingId(sportsMeetingId));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody GroupType groupType) {
        log.info("添加组别: name={}", groupType.getName());
        groupTypeService.add(groupType);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody GroupType groupType) {
        log.info("更新组别: id={}", groupType.getId());
        groupTypeService.update(groupType);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除组别: id={}", id);
        groupTypeService.delete(id);
        return Result.success("删除成功");
    }

    // ===== 限报配置 =====

    @GetMapping("/limitConfig")
    public Result<GroupType> limitConfig(@RequestParam Long groupTypeId) {
        return Result.success(groupTypeService.getLimitConfig(groupTypeId));
    }

    @PostMapping("/saveLimitConfig")
    public Result<String> saveLimitConfig(@RequestBody Map<String, Object> params) {
        Long groupTypeId = Long.valueOf(params.get("groupTypeId").toString());
        Integer perTeamLimit = params.get("perTeamLimit") != null
                ? Integer.valueOf(params.get("perTeamLimit").toString()) : 0;
        List<Long> eventIds = ((List<?>) params.get("eventIds")).stream()
                .map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
        Integer perPersonLimit = params.get("perPersonLimit") != null
                ? Integer.valueOf(params.get("perPersonLimit").toString()) : 0;
        Object personEventIdsObj = params.get("personEventIds");
        List<Long> personEventIds = personEventIdsObj != null
                ? ((List<?>) personEventIdsObj).stream()
                    .map(o -> Long.valueOf(o.toString())).collect(Collectors.toList())
                : List.of();
        log.info("保存限报配置: groupTypeId={}, perTeamLimit={}, eventIds={}, perPersonLimit={}, personEventIds={}",
                groupTypeId, perTeamLimit, eventIds, perPersonLimit, personEventIds);
        groupTypeService.saveLimitConfig(groupTypeId, perTeamLimit, eventIds, perPersonLimit, personEventIds);
        return Result.success("保存成功");
    }
}
