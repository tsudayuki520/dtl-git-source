package com.dlust.sportbackend.Controller.user;

import com.dlust.sportbackend.Service.EventService;
import com.dlust.sportbackend.Service.ResultService;
import com.dlust.sportbackend.Service.SportsMeetingService;
import com.dlust.sportbackend.entity.Event;
import com.dlust.sportbackend.entity.ResultVO;
import com.dlust.sportbackend.entity.SportsMeeting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.lang.reflect.Proxy;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserResultControllerTest {
    private MockMvc mvc;
    private Event event;
    private SportsMeeting meeting;
    private List<ResultVO> rows = List.of();
    private int resultReads;
    private int meetingReads;

    // 仅替身实现查询接口，不启动 Spring 上下文或连接数据库。
    private static <T> T queryStub(Class<T> type, String method, Function<Object[], Object> query) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, called, args) -> {
                    if (!called.getName().equals(method)) throw new AssertionError("Unexpected call: " + called);
                    return query.apply(args);
                }));
    }

    @BeforeEach
    void setUp() {
        ResultService results = queryStub(ResultService.class, "getByEventId", args -> {
            resultReads++;
            assertEquals(10L, args[0]);
            return rows;
        });
        EventService events = queryStub(EventService.class, "getById", args -> Long.valueOf(10).equals(args[0]) ? event : null);
        SportsMeetingService meetings = queryStub(SportsMeetingService.class, "getById", args -> {
            meetingReads++;
            return meeting;
        });
        mvc = MockMvcBuilders.standaloneSetup(new UserResultController(results, events, meetings)).build();
        event = new Event();
        event.setId(10L);
        event.setSportsMeetingId(1L);
        event.setIsDeleted(0);
        meeting = new SportsMeeting();
        meeting.setStatus(2);
    }

    @Test
    void publicResponseContainsScoresButNoPrivateParticipantFields() throws Exception {
        ResultVO row = new ResultVO();
        row.setId(100L);
        row.setEventId(10L);
        row.setScheduleId(20L);
        row.setParticipantName("测试选手");
        row.setCollege("测试学院");
        row.setScoreValue(12000);
        row.setPoints(8);
        row.setPhone("13800000000");
        row.setUserCode("student001");
        row.setParticipantId(88L);
        row.setMajor("测试专业");
        rows = List.of(row);

        mvc.perform(get("/api/result/listByEvent").param("eventId", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].scoreValue").value(12000))
                .andExpect(jsonPath("$.data[0].participantName").value("测试选手"))
                .andExpect(jsonPath("$.data[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data[0].userCode").doesNotExist())
                .andExpect(jsonPath("$.data[0].participantId").doesNotExist())
                .andExpect(jsonPath("$.data[0].major").doesNotExist());
    }

    @Test
    void finishedMeetingIsReadableAndEmptyResultsAreSuccessful() throws Exception {
        meeting.setStatus(3);
        mvc.perform(get("/api/result/listByEvent").param("eventId", "10"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void preparationRegistrationAndMissingMeetingsDoNotExposeResults() throws Exception {
        for (Integer status : new Integer[]{null, 0, 1}) {
            meeting.setStatus(status);
            mvc.perform(get("/api/result/listByEvent").param("eventId", "10"))
                    .andExpect(jsonPath("$.code").value(404));
        }
        meeting = null;
        mvc.perform(get("/api/result/listByEvent").param("eventId", "10"))
                .andExpect(jsonPath("$.code").value(404));
        assertEquals(0, resultReads);
    }

    @Test
    void missingAndDeletedEventsDoNotQueryResults() throws Exception {
        mvc.perform(get("/api/result/listByEvent").param("eventId", "999"))
                .andExpect(jsonPath("$.code").value(404));
        event.setIsDeleted(1);
        mvc.perform(get("/api/result/listByEvent").param("eventId", "10"))
                .andExpect(jsonPath("$.code").value(404));
        assertEquals(0, resultReads);
        assertEquals(0, meetingReads);
    }
}
