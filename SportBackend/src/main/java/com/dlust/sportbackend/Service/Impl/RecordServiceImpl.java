package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Mapper.*;
import com.dlust.sportbackend.Service.RecordService;
import com.dlust.sportbackend.entity.Event;
import com.dlust.sportbackend.entity.GroupType;
import com.dlust.sportbackend.entity.Participant;
import com.dlust.sportbackend.entity.Record;
import com.dlust.sportbackend.entity.Result;
import com.dlust.sportbackend.entity.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RecordServiceImpl implements RecordService {

    @Autowired
    private RecordMapper recordMapper;
    @Autowired
    private ResultMapper resultMapper;
    @Autowired
    private EventMapper eventMapper;
    @Autowired
    private ParticipantMapper participantMapper;
    @Autowired
    private TeamMapper teamMapper;
    @Autowired
    private GroupTypeMapper groupTypeMapper;

    @Override
    public List<Record> getAll(Long sportsMeetingId, String eventName, String category) {
        return recordMapper.selectAll(sportsMeetingId, eventName, category);
    }

    @Override
    public void add(Record record) {
        recordMapper.insert(record);
    }

    @Override
    public void update(Record record) {
        recordMapper.updateById(record);
    }

    @Override
    public void delete(Long id) {
        recordMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void reviewRecord(Long resultId, String action) {
        validateReviewAction(resultId, action);
        Result result = resultMapper.selectById(resultId);
        if (result == null) {
            throw new RuntimeException("成绩不存在");
        }
        if (result.getRecordStatus() != null && result.getRecordStatus() != 0) {
            throw new RuntimeException("该成绩已审核");
        }
        int status = "approve".equals(action) ? 1 : 2;
        resultMapper.updateRecordStatus(resultId, status);
        if (status == 1) {
            // 拼装 record 入册
            Event event = eventMapper.selectById(result.getEventId());
            Participant participant = participantMapper.selectByIdWithUser(result.getParticipantId());
            Team team = participant != null && participant.getTeamId() != null
                    ? teamMapper.selectById(participant.getTeamId()) : null;
            GroupType groupType = event != null && event.getGroupTypeId() != null
                    ? groupTypeMapper.selectById(event.getGroupTypeId()) : null;

            Record record = new Record();
            record.setSportsMeetingId(result.getSportsMeetingId());
            record.setCategory(event != null ? event.getCategory() : null);
            record.setScoreValue(result.getScoreValue());
            record.setScore(convertScoreValueToScore(result.getScoreValue(), event != null ? event.getCategory() : null));
            record.setResultId(result.getId());
            record.setEventName(event != null ? event.getName() : null);
            record.setGroupType(groupType != null ? groupType.getName() : null);
            record.setUnit(team != null ? team.getName() : null);
            record.setName(participant != null ? participant.getName() : null);
            record.setRecordTime(result.getCreateTime());
            recordMapper.insert(record);
        }
    }

    /**
     * 将 result.score_value（INT 内部单位）换算为 record.score（DECIMAL 人类可读）。
     * 径赛/团队赛: 毫秒 → 秒（÷1000）
     * 田赛: 厘米 → 米（÷100）
     * category 为 null 时默认按径赛算。
     */
    static BigDecimal convertScoreValueToScore(Integer scoreValue, String category) {
        if (scoreValue == null) return null;
        if ("田赛".equals(category)) {
            return BigDecimal.valueOf(scoreValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(scoreValue).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
    }

    /**
     * 审核参数校验（抽静态以便单测）。
     */
    static void validateReviewAction(Long resultId, String action) {
        if (resultId == null) {
            throw new RuntimeException("成绩 ID 不能为空");
        }
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new RuntimeException("审核动作非法（必须是 approve 或 reject）");
        }
    }
}
