package com.example.edu.modules.dashboard.vo;

import com.example.edu.modules.course.vo.CourseVO;
import com.example.edu.modules.task.vo.SubmissionVO;
import com.example.edu.modules.task.vo.TaskVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentDashboardVO {
    private List<CourseVO> courses;
    private Long totalCourses;
    private List<DueTaskVO> dueTasks;
    private List<RecentGradeVO> recentGrades;

    @Data
    @Builder
    public static class DueTaskVO {
        private TaskVO task;
        private String courseName;
        private String lessonName;
        private Long courseId;
    }

    @Data
    @Builder
    public static class RecentGradeVO {
        private SubmissionVO submission;
        private String taskTitle;
        private String courseName;
    }
}
