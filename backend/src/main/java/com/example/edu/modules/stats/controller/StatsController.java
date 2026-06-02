package com.example.edu.modules.stats.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "统计分析")
@RestController @RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/api/stats/semester/{semesterId}/preview")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<StatsService.GradeRow>> preview(@PathVariable Long semesterId) {
        return R.ok(statsService.calculateSemesterGrades(semesterId));
    }

    @GetMapping("/api/stats/semester/{semesterId}/export")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<byte[]> export(@PathVariable Long semesterId) {
        byte[] data = statsService.exportExcel(semesterId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=semester-grades.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
