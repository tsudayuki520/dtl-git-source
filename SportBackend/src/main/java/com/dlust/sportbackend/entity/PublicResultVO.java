package com.dlust.sportbackend.entity;

/** 小程序成绩展示字段；不暴露手机号、学号等管理端选手资料。 */
public record PublicResultVO(
        Long id, Long eventId, Long scheduleId, String scheduleName,
        String participantName, String college, String category,
        Integer scoreValue, Integer points) {
    public static PublicResultVO from(ResultVO result) {
        return new PublicResultVO(result.getId(), result.getEventId(), result.getScheduleId(),
                result.getScheduleName(), result.getParticipantName(), result.getCollege(),
                result.getCategory(), result.getScoreValue(), result.getPoints());
    }
}
