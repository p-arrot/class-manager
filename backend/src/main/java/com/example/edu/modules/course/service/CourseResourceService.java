package com.example.edu.modules.course.service;

import com.example.edu.modules.course.dto.CourseResourceCreateDTO;
import com.example.edu.modules.course.dto.CourseResourceMoveDTO;
import com.example.edu.modules.course.dto.CourseResourceUpdateDTO;
import com.example.edu.modules.course.vo.CourseResourceVO;

import java.util.List;

public interface CourseResourceService {
    CourseResourceVO createFolder(Long courseId, CourseResourceCreateDTO dto);
    void rename(Long id, CourseResourceUpdateDTO dto);
    void delete(Long id);
    void move(Long id, CourseResourceMoveDTO dto);
    List<CourseResourceVO> getTree(Long courseId);
    List<CourseResourceVO> getChildren(Long courseId, Long parentId);
}
