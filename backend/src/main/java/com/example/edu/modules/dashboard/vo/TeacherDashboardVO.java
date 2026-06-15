package com.example.edu.modules.dashboard.vo;

import com.example.edu.modules.task.vo.SubmissionVO;
import com.example.edu.modules.task.vo.TaskVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeacherDashboardVO {
    private Integer pendingGrading;
    private Integer upcomingDeadlines;
    private Integer recentCount;
    private List<RecentSubmissionVO> pendingSubmissions;
    private List<RecentSubmissionVO> recentSubmissions;
    private List<UpcomingTaskVO> upcomingTasks;

    @Data
    @Builder
    public static class RecentSubmissionVO {
        private SubmissionVO submission;
        private String taskTitle;
        private String semesterName;
    }

    @Data
    @Builder
    public static class UpcomingTaskVO {
        private TaskVO task;
        private String semesterName;
        private String lessonName;
    }
}
