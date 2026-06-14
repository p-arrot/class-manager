package com.example.edu.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private final ObjectMapper objectMapper = objectMapper();

    @Test
    void serializesLocalDateTimeAsShanghaiLocalIsoString() throws Exception {
        TimePayload payload = new TimePayload(LocalDateTime.of(2026, 6, 14, 1, 23, 45));

        assertThat(objectMapper.writeValueAsString(payload)).contains("\"at\":\"2026-06-14T01:23:45\"");
    }

    @Test
    void deserializesIsoAndLegacyLocalDateTimeStrings() throws Exception {
        TimePayload iso = objectMapper.readValue("{\"at\":\"2026-06-14T01:23:45\"}", TimePayload.class);
        TimePayload legacy = objectMapper.readValue("{\"at\":\"2026-06-14 01:23:45\"}", TimePayload.class);

        assertThat(iso.at()).isEqualTo(LocalDateTime.of(2026, 6, 14, 1, 23, 45));
        assertThat(legacy.at()).isEqualTo(LocalDateTime.of(2026, 6, 14, 1, 23, 45));
    }

    private static ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().localDateTimeCustomizer().customize(builder);
        return builder.build();
    }

    private record TimePayload(LocalDateTime at) {
    }
}
