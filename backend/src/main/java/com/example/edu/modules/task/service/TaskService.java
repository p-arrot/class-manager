package com.example.edu.modules.task.service;

import com.example.edu.modules.task.dto.SubmissionDTO;
import com.example.edu.modules.task.dto.TaskCreateDTO;
import com.example.edu.modules.task.dto.TaskUpdateDTO;
import com.example.edu.modules.task.vo.SubmissionVO;
import com.example.edu.modules.task.vo.TaskAnalyticsVO;
import com.example.edu.modules.task.vo.TaskDetailVO;
import com.example.edu.modules.task.vo.TaskVO;

import java.util.List;
import java.util.Map;

public interface TaskService {

    TaskVO create(Long lessonId, TaskCreateDTO dto);

    TaskVO update(Long id, TaskUpdateDTO dto);

    void delete(Long id);

    TaskDetailVO getById(Long id);

    List<TaskVO> listByLessonId(Long lessonId);

    SubmissionVO submit(Long taskId, SubmissionDTO dto);

    List<SubmissionVO> listSubmissions(Long taskId, Long classId);

    SubmissionVO getSubmission(Long id);

    List<SubmissionVO> getStudentSubmissions(Long studentId, Long semesterId);

    Map<String, Object> getSubmissionStats(Long taskId);

    TaskAnalyticsVO getTaskAnalytics(Long taskId, Long classId);

    SubmissionVO getMySubmission(Long taskId, Long studentId);
}
