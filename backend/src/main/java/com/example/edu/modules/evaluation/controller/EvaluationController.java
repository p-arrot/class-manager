package com.example.edu.modules.evaluation.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.evaluation.dto.EvaluateDTO;
import com.example.edu.modules.evaluation.service.EvaluationService;
import com.example.edu.modules.evaluation.vo.EvaluationVO;
import com.example.edu.modules.evaluation.vo.RadarVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "评价管理")
@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "教师评分")
    @PostMapping("/api/submissions/{submissionId}/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> evaluate(@PathVariable Long submissionId,
                            @Valid @RequestBody EvaluateDTO dto) {
        evaluationService.evaluate(submissionId, dto);
        return R.ok();
    }

    @Operation(summary = "学生评价汇总")
    @GetMapping("/api/students/{studentId}/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<List<EvaluationVO>> getStudentEvaluations(
            @PathVariable Long studentId,
            @RequestParam Long semesterId) {
        return R.ok(evaluationService.getStudentEvaluations(studentId, semesterId));
    }

    @Operation(summary = "学生雷达图数据")
    @GetMapping("/api/students/{studentId}/radar")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<RadarVO> getRadar(@PathVariable Long studentId,
                               @RequestParam Long semesterId) {
        return R.ok(evaluationService.getRadar(studentId, semesterId));
    }

    @Operation(summary = "自动评分（截止后未提交 → F）")
    @PostMapping("/api/tasks/{taskId}/auto-grade")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> autoGrade(@PathVariable Long taskId) {
        evaluationService.autoGradeMissedDeadlines(taskId);
        return R.ok();
    }
}
