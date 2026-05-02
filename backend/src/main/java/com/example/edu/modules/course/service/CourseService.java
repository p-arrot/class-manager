package com.example.edu.modules.course.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.edu.modules.course.dto.CourseCreateDTO;
import com.example.edu.modules.course.dto.CoursePageDTO;
import com.example.edu.modules.course.dto.CourseUpdateDTO;
import com.example.edu.modules.course.vo.CourseDetailVO;
import com.example.edu.modules.course.vo.CourseVO;

public interface CourseService {
    CourseVO create(CourseCreateDTO dto);
    void delete(Long id);
    CourseVO update(Long id, CourseUpdateDTO dto);
    CourseDetailVO getById(Long id);
    IPage<CourseVO> page(CoursePageDTO dto);
}
