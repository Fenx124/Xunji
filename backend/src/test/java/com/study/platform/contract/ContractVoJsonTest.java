package com.study.platform.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.study.platform.common.config.JacksonConfig;
import com.study.platform.note.dto.NotePinRequest;
import com.study.platform.note.vo.NoteFlagVO;
import com.study.platform.schedule.dto.ScheduleCreateRequest;
import com.study.platform.schedule.vo.ScheduleVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractVoJsonTest {

    private final ObjectMapper mapper = contractMapper();

    @Test
    void shouldKeepBooleanFieldNamesRequiredByTheContract() throws Exception {
        String json = mapper.writeValueAsString(new NoteFlagVO(7L, true, false));

        assertTrue(json.contains("\"isPinned\":true"));
        assertTrue(json.contains("\"isFavorite\":false"));
        assertFalse(json.contains("\"pinned\""));
        assertFalse(json.contains("\"favorite\""));

        NotePinRequest request = mapper.readValue("{\"isPinned\":true}", NotePinRequest.class);
        assertEquals(Boolean.TRUE, request.isPinned());
    }

    @Test
    void shouldSerializeScheduleClockAndRemindedFlag() throws Exception {
        ScheduleVO schedule = new ScheduleVO(
                3L, 9L, "线性代数", "复习", null,
                LocalDateTime.of(2026, 11, 21, 9, 0, 0),
                LocalDateTime.of(2026, 11, 21, 9, 30, 0),
                LocalDateTime.of(2026, 11, 21, 8, 45, 0),
                false, 2, 0, "EBBINGHAUS", null,
                LocalDateTime.of(2026, 11, 20, 12, 0, 0),
                LocalDateTime.of(2026, 11, 20, 12, 0, 0)
        );

        String json = mapper.writeValueAsString(schedule);

        assertTrue(json.contains("\"reminded\":false"));
        assertTrue(json.contains("\"startTime\":\"2026-11-21 09:00:00\""));
        assertTrue(json.contains("\"sourceType\":\"EBBINGHAUS\""));

        ScheduleCreateRequest request = mapper.readValue("""
                {"title":"复习","startTime":"2026-11-21 09:00:00","priority":2}
                """, ScheduleCreateRequest.class);
        assertEquals(LocalDateTime.of(2026, 11, 21, 9, 0, 0), request.startTime());
        assertEquals(2, request.priority());
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
