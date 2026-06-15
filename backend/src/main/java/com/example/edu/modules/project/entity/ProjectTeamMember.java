package com.example.edu.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("project_team_members")
public class ProjectTeamMember {
    @TableId(type = IdType.AUTO) private Long id;
    private Long teamId;
    private Long studentId;
}
