package com.dlust.sportbackend.Service.Impl;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RecordServiceImplTest {

    @Test
    void convertScoreValueToScore_trackSeconds() {
        // 径赛 10250 毫秒 → 10.25 秒
        assertEquals(new BigDecimal("10.25"),
                RecordServiceImpl.convertScoreValueToScore(10250, "径赛"));
    }

    @Test
    void convertScoreValueToScore_fieldMeters() {
        // 田赛 680 厘米 → 6.80 米
        assertEquals(new BigDecimal("6.80"),
                RecordServiceImpl.convertScoreValueToScore(680, "田赛"));
    }

    @Test
    void convertScoreValueToScore_teamEvent() {
        // 团队赛 70000 毫秒 → 70.00 秒（同径赛逻辑）
        assertEquals(new BigDecimal("70.00"),
                RecordServiceImpl.convertScoreValueToScore(70000, "团队赛"));
    }

    @Test
    void convertScoreValueToScore_nullScoreValue() {
        assertNull(RecordServiceImpl.convertScoreValueToScore(null, "径赛"));
    }

    @Test
    void convertScoreValueToScore_nullCategoryDefaultsTrack() {
        // category null 默认按径赛（÷1000）
        assertEquals(new BigDecimal("10.25"),
                RecordServiceImpl.convertScoreValueToScore(10250, null));
    }

    @Test
    void validateReviewAction_nullResultId_throws() {
        assertThrows(RuntimeException.class, () ->
                RecordServiceImpl.validateReviewAction(null, "approve"));
    }

    @Test
    void validateReviewAction_invalidAction_throws() {
        assertThrows(RuntimeException.class, () ->
                RecordServiceImpl.validateReviewAction(1L, "maybe"));
    }

    @Test
    void validateReviewAction_approveOk() {
        assertDoesNotThrow(() ->
                RecordServiceImpl.validateReviewAction(1L, "approve"));
    }

    @Test
    void validateReviewAction_rejectOk() {
        assertDoesNotThrow(() ->
                RecordServiceImpl.validateReviewAction(1L, "reject"));
    }
}
