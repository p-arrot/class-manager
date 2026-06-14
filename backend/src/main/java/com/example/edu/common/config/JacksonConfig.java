package com.example.edu.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    private static final String DISPLAY_ZONE = "Asia/Shanghai";
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter LEGACY_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder
                .timeZone(TimeZone.getTimeZone(DISPLAY_ZONE))
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(LOCAL_DATE_TIME_FORMATTER))
                .deserializerByType(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
    }

    private static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private final LocalDateTimeDeserializer isoDeserializer = new LocalDateTimeDeserializer(LOCAL_DATE_TIME_FORMATTER);

        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.getText();
            if (value == null || value.isBlank()) return null;
            String normalized = value.trim();
            if (normalized.indexOf('T') >= 0) return isoDeserializer.deserialize(parser, context);
            return LocalDateTime.parse(normalized, LEGACY_LOCAL_DATE_TIME_FORMATTER);
        }
    }
}
