package com.study.platform.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.study.platform.common.config.JacksonConfig;
import com.study.platform.dashboard.vo.DashboardOverviewVO;
import com.study.platform.focus.dto.FocusRecordCreateRequest;
import com.study.platform.focus.vo.FocusRecordVO;
import com.study.platform.focus.vo.FocusTodayStatsVO;
import com.study.platform.system.vo.AdminAiStatsVO;
import com.study.platform.system.vo.AdminUserVO;
import com.study.platform.system.vo.AuditLogVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAndFocusVoJsonTest {

    private final ObjectMapper mapper = contractMapper();

    @Test
    void shouldSerializeFocusDashboardAndAdminContracts() throws Exception {
        FocusRecordVO focus = new FocusRecordVO(
                4L, 8L, "线性代数", 3L, "复习",
                25, 20, "COMPLETED",
                LocalDateTime.of(2026, 11, 20, 21, 0, 0),
                LocalDateTime.of(2026, 11, 20, 21, 20, 0),
                "例题",
                LocalDateTime.of(2026, 11, 20, 21, 20, 1)
        );
        String focusJson = mapper.writeValueAsString(focus);
        assertTrue(focusJson.contains("\"noteTitle\":\"线性代数\""));
        assertTrue(focusJson.contains("\"scheduleTitle\":\"复习\""));
        assertTrue(focusJson.contains("\"startedAt\":\"2026-11-20 21:00:00\""));
        assertTrue(focusJson.contains("\"status\":\"COMPLETED\""));

        FocusRecordCreateRequest request = mapper.readValue("""
                {"plannedMinutes":25,"actualMinutes":20,"status":"ABORTED","startedAt":"2026-11-20 21:00:00","endedAt":"2026-11-20 21:10:00"}
                """, FocusRecordCreateRequest.class);
        assertEquals(25, request.plannedMinutes());
        assertEquals("ABORTED", request.status());

        DashboardOverviewVO overview = new DashboardOverviewVO(
                new DashboardOverviewVO.Summary(12, 3, 80.5, 175, 50),
                List.of(new DashboardOverviewVO.HeatCell("2026-11-20", 5)),
                List.of(new DashboardOverviewVO.TrendPoint("2026-11-14", 2, 3, 50)),
                List.of(new DashboardOverviewVO.NameCount("未归类", 1)),
                List.of(new DashboardOverviewVO.TagCount("MySQL", "#409EFF", 4))
        );
        String overviewJson = mapper.writeValueAsString(overview);
        assertTrue(overviewJson.contains("\"monthCompletionRate\":80.5"));
        assertTrue(overviewJson.contains("\"weekFocusMinutes\":175"));
        assertTrue(overviewJson.contains("\"date\":\"2026-11-20\""));
        assertTrue(overviewJson.contains("\"notesUpdated\":2"));
        assertTrue(overviewJson.contains("\"name\":\"未归类\""));
        assertTrue(overviewJson.contains("\"color\":\"#409EFF\""));

        AdminUserVO adminUser = new AdminUserVO(
                1L, "admin", "管理员", "admin@example.com", 1,
                List.of("ADMIN", "USER"), 50,
                LocalDateTime.of(2026, 11, 20, 9, 0, 0)
        );
        String userJson = mapper.writeValueAsString(adminUser);
        assertTrue(userJson.contains("\"roles\":[\"ADMIN\",\"USER\"]"));
        assertTrue(userJson.contains("\"aiDailyLimit\":50"));
        assertTrue(userJson.contains("\"createdAt\":\"2026-11-20 09:00:00\""));

        AuditLogVO audit = new AuditLogVO(
                6L, 1L, "admin", "ADMIN", "强制下线", "POST",
                "/api/v1/admin/users/2/kick", "127.0.0.1", 12, 1, null,
                LocalDateTime.of(2026, 11, 20, 22, 0, 0)
        );
        String auditJson = mapper.writeValueAsString(audit);
        assertTrue(auditJson.contains("\"httpMethod\":\"POST\""));
        assertTrue(auditJson.contains("\"requestUri\":\"/api/v1/admin/users/2/kick\""));
        assertTrue(auditJson.contains("\"costMs\":12"));
        assertTrue(auditJson.contains("\"errorMsg\":null"));

        AdminAiStatsVO empty = new AdminAiStatsVO(
                0, 0, 0.0, 0, 0,
                List.of(),
                List.of(),
                List.of(new AdminAiStatsVO.DailyStat("2026-11-20", 0, 0))
        );
        String statsJson = mapper.writeValueAsString(empty);
        assertTrue(statsJson.contains("\"totalCalls\":0"));
        assertTrue(statsJson.contains("\"successRate\":0.0"));
        assertTrue(statsJson.contains("\"avgLatencyMs\":0"));
        assertTrue(statsJson.contains("\"byFeature\":[]"));
        assertTrue(statsJson.contains("\"byUser\":[]"));
        assertTrue(statsJson.contains("\"dailyTrend\":[{\"date\":\"2026-11-20\",\"calls\":0,\"successCalls\":0}]"));

        String todayJson = mapper.writeValueAsString(new FocusTodayStatsVO(2, 50, 3));
        assertTrue(todayJson.contains("\"completedCount\":2"));
        assertTrue(todayJson.contains("\"totalMinutes\":50"));
        assertTrue(todayJson.contains("\"totalSessions\":3"));
    }

    private static ObjectMapper contractMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(JacksonConfig.DATE_TIME_FORMATTER));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(JacksonConfig.DATE_TIME_FORMATTER));
        mapper.registerModule(module);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
