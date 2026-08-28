package com.example.edu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@MapperScan("com.example.edu.modules.**.mapper")
public class EduApplication {

    private static final String DEFAULT_TIME_ZONE = "Asia/Shanghai";

    public static void main(String[] args) {
        configureDefaultTimeZone(System.getenv("APP_TIME_ZONE"));
        SpringApplication.run(EduApplication.class, args);
    }

    static void configureDefaultTimeZone(String configuredTimeZone) {
        String zoneId = configuredTimeZone == null || configuredTimeZone.isBlank()
                ? DEFAULT_TIME_ZONE : configuredTimeZone.trim();
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zoneId)));
    }
}
