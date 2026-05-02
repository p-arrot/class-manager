package com.example.edu.modules.course.service;

import com.example.edu.modules.course.dto.SemesterCreateDTO;
import com.example.edu.modules.course.dto.SemesterUpdateDTO;
import com.example.edu.modules.course.vo.SemesterVO;

import java.util.List;

public interface SemesterService {
    SemesterVO create(Long courseId, SemesterCreateDTO dto);
    void delete(Long id);
    SemesterVO update(Long id, SemesterUpdateDTO dto);
    SemesterVO getById(Long id);
    List<SemesterVO> listByCourseId(Long courseId);
}
