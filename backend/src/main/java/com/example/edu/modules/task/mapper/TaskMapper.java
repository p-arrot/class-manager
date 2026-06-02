package com.example.edu.modules.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.edu.modules.task.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
