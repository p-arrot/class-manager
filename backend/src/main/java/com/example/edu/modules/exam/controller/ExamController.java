package com.example.edu.modules.exam.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.exam.entity.*;
import com.example.edu.modules.exam.service.ExamService;
import com.example.edu.modules.exam.vo.*;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "考试管理")
@RestController @RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;
    private final UserMapper userMapper;

    @GetMapping("/api/exam-papers")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<ExamPaperVO>> listPapers() {
        return R.ok(examService.listPapers().stream().map(p -> ExamPaperVO.builder()
                .id(p.getId()).title(p.getTitle()).content(p.getContent())
                .totalScore(p.getTotalScore()).teacherId(p.getTeacherId())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build()).toList());
    }

    @PostMapping("/api/exam-papers")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<ExamPaperVO> createPaper(@RequestBody ExamPaper paper) {
        ExamPaper p = examService.createPaper(paper);
        return R.ok(ExamPaperVO.builder().id(p.getId()).title(p.getTitle()).content(p.getContent())
                .totalScore(p.getTotalScore()).teacherId(p.getTeacherId())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build());
    }

    @GetMapping("/api/semesters/{semesterId}/exams")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<List<ExamVO>> listExams(@PathVariable Long semesterId) {
        return R.ok(examService.listExams(semesterId).stream().map(e -> ExamVO.builder()
                .id(e.getId()).name(e.getName()).semesterId(e.getSemesterId()).paperId(e.getPaperId())
                .startTime(e.getStartTime()).endTime(e.getEndTime()).weight(e.getWeight())
                .createdAt(e.getCreatedAt()).build()).toList());
    }

    @PostMapping("/api/semesters/{semesterId}/exams")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<ExamVO> createExam(@PathVariable Long semesterId, @RequestBody Exam exam) {
        exam.setSemesterId(semesterId);
        Exam e = examService.createExam(exam);
        return R.ok(ExamVO.builder().id(e.getId()).name(e.getName()).semesterId(e.getSemesterId())
                .paperId(e.getPaperId()).startTime(e.getStartTime()).endTime(e.getEndTime())
                .weight(e.getWeight()).createdAt(e.getCreatedAt()).build());
    }

    @DeleteMapping("/api/exams/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> deleteExam(@PathVariable Long id) { examService.deleteExam(id); return R.ok(); }

    @PostMapping("/api/exams/{examId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public R<Map<String,String>> startExam(@PathVariable Long examId) {
        return R.ok(Map.of("status", "started", "examId", String.valueOf(examId)));
    }

    @PostMapping("/api/exams/{examId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public R<ExamSubmissionVO> submit(@PathVariable Long examId, @RequestBody Map<String,String> body) {
        ExamSubmission s = examService.submit(examId, body.get("answers"));
        User u = userMapper.selectById(s.getStudentId());
        return R.ok(ExamSubmissionVO.builder().id(s.getId()).examId(s.getExamId())
                .studentId(s.getStudentId()).studentName(u != null ? u.getName() : null)
                .studentNo(u != null ? u.getStudentNo() : null)
                .answers(s.getAnswers()).score(s.getScore()).status(s.getStatus())
                .submittedAt(s.getSubmittedAt()).createdAt(s.getCreatedAt()).build());
    }

    @GetMapping("/api/exams/{examId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<ExamSubmissionVO>> listSubmissions(@PathVariable Long examId) {
        List<ExamSubmission> subs = examService.listSubmissions(examId);
        java.util.Set<Long> sids = subs.stream().map(ExamSubmission::getStudentId).collect(Collectors.toSet());
        Map<Long,User> um = sids.isEmpty() ? Map.of() : userMapper.selectBatchIds(sids).stream().collect(Collectors.toMap(User::getId, u->u));
        return R.ok(subs.stream().map(s -> {
            User u = um.get(s.getStudentId());
            return ExamSubmissionVO.builder().id(s.getId()).examId(s.getExamId())
                    .studentId(s.getStudentId()).studentName(u != null ? u.getName() : null)
                    .studentNo(u != null ? u.getStudentNo() : null)
                    .answers(s.getAnswers()).score(s.getScore()).status(s.getStatus())
                    .submittedAt(s.getSubmittedAt()).createdAt(s.getCreatedAt()).build();
        }).toList());
    }

    @PutMapping("/api/exam-submissions/{id}/grade")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> grade(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        Object scoreObj = body.get("score");
        Integer score = scoreObj instanceof Number n ? n.intValue() : null;
        examService.gradeSubmission(id, score, Boolean.TRUE.equals(body.get("absent")));
        return R.ok();
    }
}
