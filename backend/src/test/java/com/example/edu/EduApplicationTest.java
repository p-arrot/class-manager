package com.example.edu;

import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class EduApplicationTest {

    @Test
    void defaultsRuntimeClockToShanghaiTime() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            EduApplication.configureDefaultTimeZone(null);

            assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Shanghai");
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
