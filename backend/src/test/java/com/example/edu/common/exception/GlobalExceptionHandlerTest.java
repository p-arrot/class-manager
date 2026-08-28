package com.example.edu.common.exception;

import com.example.edu.common.result.ErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest
    @MethodSource("businessErrors")
    void mapsBusinessCodeFamiliesToHttpStatus(ErrorCode errorCode, HttpStatus expectedStatus) {
        ResponseEntity<?> response = handler.handleBizException(new BizException(errorCode));

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    }

    private static Stream<Arguments> businessErrors() {
        return Stream.of(
                Arguments.of(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST),
                Arguments.of(ErrorCode.USERNAME_PASSWORD_ERROR, HttpStatus.UNAUTHORIZED),
                Arguments.of(ErrorCode.COURSE_ACCESS_DENIED, HttpStatus.FORBIDDEN),
                Arguments.of(ErrorCode.SUBMISSION_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(ErrorCode.SUBMISSION_LOCKED, HttpStatus.CONFLICT),
                Arguments.of(ErrorCode.MINIO_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }
}
