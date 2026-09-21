package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Mapper.ParticipantMapper;
import com.dlust.sportbackend.Mapper.SportsMeetingMapper;
import com.dlust.sportbackend.Service.RegistrationService;
import com.dlust.sportbackend.common.Result;
import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.SportsMeeting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/register")
public class UserRegisterController {

    @Autowired
    private ParticipantMapper participantMapper;

    @Autowired
    private SportsMeetingMapper sportsMeetingMapper;

    @Autowired
    private RegistrationService registrationService;

    /**
     * 报名接口
     * 身份来自 JWT 注入的 userId；不再接受前端传入的 userCode/name/phone 等身份字段。
     *
     * 校验顺序（便宜→贵、静态→动态、无副作用→有副作用），Controller 只负责前两步，
     * 其余在 RegistrationService.add(Participant,...) 内按序执行：
     *   ① 运动会报名窗口（本方法）
     *   ② 项目静态校验：存在且未软删、属于本运动会、未结束（Service）
     *   ③ 资格：必须有代表队(participant→team→group_type)（Service）
     *   ④ 组别匹配：项目必须面向报名人所在组别（Service）
     *   ⑤ 轮次：显式传入的轮次也必须确实开放（Service）
     *   ⑥ 是否已报名（Service）
     *   ⑦⑧ 规则A/B/C 限报（Service）
     *   ⑨ 写库：participant（若不存在）+ registration（Service）
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestAttribute("userId") Long userId,
                                 @RequestBody Map<String, Object> body) {
        Long sportsMeetingId = readLong(body, "sportsMeetingId");
        Long eventId = readLong(body, "eventId");
        log.info("报名参赛: userId={}, sportsMeetingId={}, eventId={}", userId, sportsMeetingId, eventId);
        if (sportsMeetingId == null || eventId == null) {
            return Result.error(400, "参数缺失或格式错误：sportsMeetingId / eventId");
        }

        // ① 校验报名时间（必须排在项目/轮次校验之前，否则截止后可能先报出「未开放报名」）
        SportsMeeting meeting = sportsMeetingMapper.selectById(sportsMeetingId);
        if (meeting == null) {
            return Result.error(404, "运动会不存在");
        }
        if (meeting.getStatus() != 1) {
            return Result.error(400, "当前不在报名时间内");
        }
        LocalDateTime now = LocalDateTime.now();
        if (meeting.getRegistrationStart() != null && now.isBefore(meeting.getRegistrationStart())) {
            return Result.error(400, "报名尚未开始");
        }
        if (meeting.getRegistrationEnd() != null && now.isAfter(meeting.getRegistrationEnd())) {
            return Result.error(400, "报名已截止");
        }

        // ② 参赛人员：已存在则复用；不存在则先构造，待校验全部通过后由 Service 落库，
        //    避免报名失败时留下一批 team_id 为空的孤儿 participant
        Participant participant = participantMapper.selectByUserIdAndSportsMeetingId(userId, sportsMeetingId);
        if (participant == null) {
            participant = new Participant();
            participant.setSportsMeetingId(sportsMeetingId);
            participant.setUserId(userId);
        }

        // ③ 项目校验 + 已报名 + 限报规则 + 写库，全部在 Service 内按序执行
        try {
            registrationService.add(participant, eventId, readLong(body, "scheduleId"));
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }

        return Result.success("报名成功");
    }

    /**
     * 获取某运动会下各项目的已报名人数
     */
    @GetMapping("/count")
    public Result<Map<Long, Integer>> getCount(@RequestParam Long sportsMeetingId) {
        log.info("获取报名人数统计: sportsMeetingId={}", sportsMeetingId);
        return Result.success(registrationService.countBySportsMeetingId(sportsMeetingId));
    }

    /**
     * 读 body 里的整数字段。两种来源都要认：
     * Apifox/手写 JSON 里是不带引号的数字（Jackson → Integer），
     * 小程序从 URL query 拿到的是字符串（"43"），直接强转 Number 会 ClassCastException。
     * 缺失或非法返回 null，由调用方给出明确的 400 提示（而不是抛 NPE 把 Java 异常文案漏给前端）。
     */
    private static Long readLong(Map<String, Object> body, String field) {
        Object v = body.get(field);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
