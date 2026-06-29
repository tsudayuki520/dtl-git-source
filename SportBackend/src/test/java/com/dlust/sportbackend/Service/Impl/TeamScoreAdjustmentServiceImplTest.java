package com.dlust.sportbackend.Service.Impl;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamScoreAdjustmentServiceImplTest {

    @Test
    void validateAdd_nullTeamId_throws() {
        assertThrows(RuntimeException.class, () ->
            TeamScoreAdjustmentServiceImpl.validateAdd(null, new BigDecimal("5"), "作弊扣分"));
    }

    @Test
    void validateAdd_nullDelta_throws() {
        assertThrows(RuntimeException.class, () ->
            TeamScoreAdjustmentServiceImpl.validateAdd(1L, null, "作弊扣分"));
    }

    @Test
    void validateAdd_blankNote_throws() {
        assertThrows(RuntimeException.class, () ->
            TeamScoreAdjustmentServiceImpl.validateAdd(1L, new BigDecimal("5"), "  "));
    }

    @Test
    void validateAdd_negativeDelta_ok() {
        // 负数 delta（扣分）合法
        assertDoesNotThrow(() ->
            TeamScoreAdjustmentServiceImpl.validateAdd(1L, new BigDecimal("-5"), "作弊扣5分"));
    }

    @Test
    void validateAdd_validInput_ok() {
        assertDoesNotThrow(() ->
            TeamScoreAdjustmentServiceImpl.validateAdd(1L, new BigDecimal("3"), "精神文明+3"));
    }
}
