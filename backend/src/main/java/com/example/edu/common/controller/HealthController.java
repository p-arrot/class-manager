package com.example.edu.common.controller;

import com.example.edu.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    @Operation(summary = "健康检查")
    @GetMapping("/api/health")
    public R<Map<String, String>> health() {
        String db = "unknown";
        if (dataSource != null) {
            try (var conn = dataSource.getConnection()) {
                db = conn.isValid(2) ? "ok" : "error";
            } catch (SQLException e) {
                db = "error:" + e.getMessage();
            }
        }
        return R.ok(Map.of("db", db, "status", "UP"));
    }
}
