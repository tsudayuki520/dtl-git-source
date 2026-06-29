package com.dlust.sportbackend.Service.Impl;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventScheduleServiceImplTest {

    private static Map<Long, Integer> callSyncPlan(Map<Long, Integer> existing,
                                                   List<Long> selectedSorted,
                                                   boolean eventAllowRegister) {
        return EventScheduleServiceImpl.computeSyncPlan(existing, selectedSorted, eventAllowRegister);
    }

    @Test
    void newProject_multipleRounds_totalOn_onlyLowestOpen() {
        Map<Long, Integer> plan = callSyncPlan(new LinkedHashMap<>(),
                List.of(10L, 20L, 30L), true);
        assertEquals(1, plan.get(10L));
        assertEquals(0, plan.get(20L));
        assertEquals(0, plan.get(30L));
    }

    @Test
    void newProject_singleRound_totalOn_thatRoundOpen() {
        Map<Long, Integer> plan = callSyncPlan(new LinkedHashMap<>(), List.of(10L), true);
        assertEquals(1, plan.get(10L));
    }

    @Test
    void newProject_totalOff_allClosed() {
        Map<Long, Integer> plan = callSyncPlan(new LinkedHashMap<>(), List.of(10L, 20L), false);
        assertEquals(0, plan.get(10L));
        assertEquals(0, plan.get(20L));
    }

    @Test
    void editProject_preservesExistingSwitch_newRoundsDefaultClosed() {
        Map<Long, Integer> existing = new LinkedHashMap<>();
        existing.put(10L, 1);
        Map<Long, Integer> plan = callSyncPlan(existing, List.of(10L, 30L), true);
        assertEquals(1, plan.get(10L));
        assertEquals(0, plan.get(30L));
    }

    @Test
    void editProject_droppedRoundAbsentFromPlan() {
        Map<Long, Integer> existing = new LinkedHashMap<>();
        existing.put(10L, 1);
        existing.put(20L, 0);
        Map<Long, Integer> plan = callSyncPlan(existing, List.of(10L), true);
        assertTrue(plan.containsKey(10L));
        assertFalse(plan.containsKey(20L));
    }

    @Test
    void editProject_clearAllRounds_returnsEmptyPlan() {
        // 用户在编辑弹窗清空所有轮次 → plan 为空（调用方据此软删全部现有关联）
        Map<Long, Integer> existing = new LinkedHashMap<>();
        existing.put(10L, 1);
        existing.put(20L, 0);
        Map<Long, Integer> plan = callSyncPlan(existing, List.of(), true);
        assertTrue(plan.isEmpty());
    }
}
