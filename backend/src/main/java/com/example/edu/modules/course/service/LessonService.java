package com.example.edu.modules.course.service;

import com.example.edu.modules.course.dto.LessonCreateDTO;
import com.example.edu.modules.course.dto.LessonSortDTO;
import com.example.edu.modules.course.dto.LessonUpdateDTO;
import com.example.edu.modules.course.vo.LessonVO;

import java.util.List;

public interface LessonService {
    LessonVO create(Long semesterId, LessonCreateDTO dto);
    void delete(Long id);
    LessonVO update(Long id, LessonUpdateDTO dto);
    LessonVO getById(Long id);
    List<LessonVO> listBySemesterId(Long semesterId);
    void reorder(Long id, LessonSortDTO dto);
}
