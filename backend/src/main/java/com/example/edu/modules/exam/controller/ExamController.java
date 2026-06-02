package com.example.edu.modules.exam.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.exam.entity.*;
import com.example.edu.modules.exam.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "考试管理")
@RestController @RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;

    @GetMapping("/api/exam-papers")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<ExamPaper>> listPapers() { return R.ok(examService.listPapers()); }

    @PostMapping("/api/exam-papers")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<ExamPaper> createPaper(@RequestBody ExamPaper paper) { return R.ok(examService.createPaper(paper)); }

    @GetMapping("/api/semesters/{semesterId}/exams")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<List<Exam>> listExams(@PathVariable Long semesterId) { return R.ok(examService.listExams(semesterId)); }

    @PostMapping("/api/semesters/{semesterId}/exams")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Exam> createExam(@PathVariable Long semesterId, @RequestBody Exam exam) {
        exam.setSemesterId(semesterId); return R.ok(examService.createExam(exam));
    }

    @DeleteMapping("/api/exams/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> deleteExam(@PathVariable Long id) { examService.deleteExam(id); return R.ok(); }

    @PostMapping("/api/exams/{examId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public R<ExamSubmission> submit(@PathVariable Long examId, @RequestBody Map<String,String> body) {
        return R.ok(examService.submit(examId, body.get("answers")));
    }

    @GetMapping("/api/exams/{examId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<ExamSubmission>> listSubmissions(@PathVariable Long examId) {
        return R.ok(examService.listSubmissions(examId));
    }

    @PutMapping("/api/exam-submissions/{id}/grade")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> grade(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        examService.gradeSubmission(id, (Integer) body.get("score"), Boolean.TRUE.equals(body.get("absent")));
        return R.ok();
    }
}
