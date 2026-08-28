package com.example.edu.modules.exam.controller;

import com.example.edu.common.result.R;
import com.example.edu.common.dto.ReturnSubmissionDTO;
import com.example.edu.common.submission.SubmissionStatus;
import com.example.edu.modules.evaluation.service.DimensionScoreService;
import com.example.edu.modules.exam.entity.*;
import com.example.edu.modules.exam.service.ExamService;
import com.example.edu.modules.exam.vo.*;
import com.example.edu.modules.user.entity.User;
import com.example.edu.modules.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        List<Exam> exams = examService.listExams(semesterId);
        Set<Long> paperIds = exams.stream().map(Exam::getPaperId).collect(Collectors.toSet());
        Map<Long, ExamPaper> papers = paperIds.isEmpty() ? Map.of() : examService.listPaperByIds(paperIds).stream()
                .collect(Collectors.toMap(ExamPaper::getId, p -> p));
        Map<Long, ExamSubmission> mySubmissions = examService.listMySubmissions(exams.stream().map(Exam::getId).toList());
        return R.ok(exams.stream().map(e -> toExamVO(e, papers.get(e.getPaperId()), mySubmissions.get(e.getId()))).toList());
    }

    @PostMapping("/api/semesters/{semesterId}/exams")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<ExamVO> createExam(@PathVariable Long semesterId, @RequestBody Exam exam) {
        exam.setSemesterId(semesterId);
        Exam e = examService.createExam(exam);
        return R.ok(toExamVO(e, null, null));
    }

    @GetMapping("/api/exams/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public R<ExamVO> getExam(@PathVariable Long id) {
        Exam exam = examService.getExam(id);
        ExamPaper paper = examService.listPaperByIds(List.of(exam.getPaperId())).stream().findFirst().orElse(null);
        ExamSubmission submission = "student".equals(com.example.edu.common.security.SecurityUtils.getCurrentUserRole())
                ? examService.getMySubmission(id) : null;
        return R.ok(toExamVO(exam, paper, submission));
    }

    @PutMapping("/api/exams/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<ExamVO> updateExam(@PathVariable Long id, @RequestBody Exam exam) {
        Exam e = examService.updateExam(id, exam);
        return R.ok(toExamVO(e, null, null));
    }

    @DeleteMapping("/api/exams/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> deleteExam(@PathVariable Long id) { examService.deleteExam(id); return R.ok(); }

    @PostMapping("/api/exams/{examId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public R<ExamSubmissionVO> startExam(@PathVariable Long examId) {
        return R.ok(toSubmissionVO(examService.startExam(examId)));
    }

    @PutMapping("/api/exams/{examId}/draft")
    @PreAuthorize("hasRole('STUDENT')")
    public R<ExamSubmissionVO> saveDraft(@PathVariable Long examId, @RequestBody Map<String,String> body) {
        return R.ok(toSubmissionVO(examService.saveDraft(examId, body.get("answers"))));
    }

    @GetMapping("/api/exams/{examId}/my-submission")
    @PreAuthorize("hasRole('STUDENT')")
    public R<ExamSubmissionVO> getMySubmission(@PathVariable Long examId) {
        ExamSubmission submission = examService.getMySubmission(examId);
        return R.ok(submission == null ? null : toSubmissionVO(submission));
    }

    @PostMapping("/api/exams/{examId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public R<ExamSubmissionVO> submit(@PathVariable Long examId, @RequestBody Map<String,String> body) {
        ExamSubmission s = examService.submit(examId, body.get("answers"));
        return R.ok(toSubmissionVO(s));
    }

    @GetMapping("/api/exams/{examId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<List<ExamSubmissionVO>> listSubmissions(@PathVariable Long examId) {
        return R.ok(examService.listSubmissionInbox(examId));
    }

    @PutMapping("/api/exam-submissions/{id}/grade")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> grade(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        Object scoreObj = body.get("score");
        Integer score = scoreObj instanceof Number n ? n.intValue() : null;
        examService.gradeSubmission(id, score, Boolean.TRUE.equals(body.get("absent")), parseDimensionScores(body.get("dimensionScores")));
        return R.ok();
    }

    @PutMapping("/api/exam-submissions/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<Void> returnSubmission(@PathVariable Long id, @Valid @RequestBody ReturnSubmissionDTO dto) {
        examService.returnSubmission(id, dto.reason());
        return R.ok();
    }

    private List<DimensionScoreService.ScoreInput> parseDimensionScores(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<DimensionScoreService.ScoreInput> scores = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            Object questionId = map.get("questionId");
            Object dimension = map.get("dimension");
            Object earnedScore = map.get("earnedScore");
            Object maxScore = map.get("maxScore");
            Object autoGraded = map.get("autoGraded");
            if (!(dimension instanceof String dim)) continue;
            scores.add(new DimensionScoreService.ScoreInput(
                    questionId != null ? String.valueOf(questionId) : null,
                    dim,
                    toBigDecimal(earnedScore),
                    toBigDecimal(maxScore),
                    Boolean.TRUE.equals(autoGraded)));
        }
        return scores;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        if (value instanceof String text && !text.isBlank()) return new BigDecimal(text);
        return BigDecimal.ZERO;
    }

    private ExamVO toExamVO(Exam exam, ExamPaper paper, ExamSubmission submission) {
        return ExamVO.builder()
                .id(exam.getId())
                .name(exam.getName())
                .semesterId(exam.getSemesterId())
                .paperId(exam.getPaperId())
                .paperContent(examService.paperContentForCurrentUser(paper))
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .weight(exam.getWeight())
                .createdAt(exam.getCreatedAt())
                .submissionId(submission != null ? submission.getId() : null)
                .submissionStatus(submission != null ? submission.getStatus() : "not_started")
                .submittedAt(submission != null ? submission.getSubmittedAt() : null)
                .score(submission != null ? submission.getScore() : null)
                .returnReason(submission != null ? submission.getReturnReason() : null)
                .canResubmit(submission == null || SubmissionStatus.canResubmit(submission.getStatus()))
                .startedAt(submission != null ? submission.getStartedAt() : null)
                .build();
    }

    private ExamSubmissionVO toSubmissionVO(ExamSubmission submission) {
        User user = userMapper.selectById(submission.getStudentId());
        return ExamSubmissionVO.builder()
                .id(submission.getId()).submissionId(submission.getId()).examId(submission.getExamId())
                .studentId(submission.getStudentId()).studentName(user != null ? user.getName() : null)
                .studentNo(user != null ? user.getStudentNo() : null)
                .answers(submission.getAnswers()).score(submission.getScore()).status(submission.getStatus())
                .canResubmit(SubmissionStatus.canResubmit(submission.getStatus()))
                .returnReason(submission.getReturnReason()).returnedAt(submission.getReturnedAt())
                .startedAt(submission.getStartedAt())
                .revisionCount(Optional.ofNullable(submission.getRevisionCount()).orElse(0))
                .submittedAt(submission.getSubmittedAt()).createdAt(submission.getCreatedAt()).build();
    }
}
