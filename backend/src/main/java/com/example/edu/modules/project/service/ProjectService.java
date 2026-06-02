package com.example.edu.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.edu.common.exception.BizException;
import com.example.edu.common.result.ErrorCode;
import com.example.edu.modules.audit.service.AuditLogService;
import com.example.edu.modules.project.entity.*;
import com.example.edu.modules.project.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectScoreMapper scoreMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public Project create(Project project) {
        projectMapper.insert(project);
        auditLogService.record("创建项目", "project", project.getId(), project.getName());
        return project;
    }

    public List<Project> listBySemester(Long semesterId) {
        return projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getSemesterId, semesterId)
                .orderByDesc(Project::getCreatedAt));
    }

    public void delete(Long id) {
        Project p = projectMapper.selectById(id);
        if (p == null) throw new BizException(ErrorCode.NOT_FOUND);
        projectMapper.deleteById(id);
        auditLogService.record("删除项目", "project", id, p.getName());
    }

    @Transactional
    public void score(Long projectId, List<ProjectScore> scores) {
        // Delete old scores
        scoreMapper.delete(new LambdaQueryWrapper<ProjectScore>().eq(ProjectScore::getProjectId, projectId));
        for (ProjectScore s : scores) {
            s.setProjectId(projectId);
            if (s.getIsSpecial() == null) s.setIsSpecial(0);
            scoreMapper.insert(s);
        }
        auditLogService.record("项目评分", "project", projectId, "评分" + scores.size() + "人");
    }

    public List<ProjectScore> listScores(Long projectId) {
        return scoreMapper.selectList(new LambdaQueryWrapper<ProjectScore>()
                .eq(ProjectScore::getProjectId, projectId));
    }
}
