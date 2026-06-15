package com.example.edu.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_teams")
public class ProjectTeam {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private String name;
    @TableField("created_at") private LocalDateTime createdAt;
}
