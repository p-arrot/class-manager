package com.example.edu.modules.evaluation.service;

import com.example.edu.modules.evaluation.dto.EvaluateDTO;
import com.example.edu.modules.evaluation.vo.EvaluationVO;
import com.example.edu.modules.evaluation.vo.RadarVO;

import java.util.List;
import java.util.Map;

public interface EvaluationService {

    void evaluate(Long submissionId, EvaluateDTO dto);

    List<EvaluationVO> getStudentEvaluations(Long studentId, Long semesterId);

    RadarVO getRadar(Long studentId, Long semesterId);

    Map<String, Integer> getGradeScores();

    void autoGradeMissedDeadlines(Long taskId);

    void returnSubmission(Long submissionId, String reason);
}
