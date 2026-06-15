package com.example.edu.modules.dashboard.controller;

import com.example.edu.common.result.R;
import com.example.edu.modules.dashboard.service.DashboardService;
import com.example.edu.modules.dashboard.vo.StudentDashboardVO;
import com.example.edu.modules.dashboard.vo.TeacherDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/dashboard/student")
    @PreAuthorize("hasRole('STUDENT')")
    public R<StudentDashboardVO> studentDashboard() {
        return R.ok(dashboardService.studentDashboard());
    }

    @GetMapping("/api/dashboard/teacher")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public R<TeacherDashboardVO> teacherDashboard() {
        return R.ok(dashboardService.teacherDashboard());
    }
}
