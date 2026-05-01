package com.example.edu.modules.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.edu.modules.user.dto.*;
import com.example.edu.modules.user.vo.TeacherClassVO;
import com.example.edu.modules.user.vo.TeacherVO;

import java.util.List;

public interface TeacherService {

    TeacherVO create(TeacherCreateDTO dto);

    TeacherVO update(Long id, TeacherUpdateDTO dto);

    IPage<TeacherVO> page(TeacherPageDTO dto);

    TeacherVO getById(Long id);

    List<TeacherClassVO> getTeacherClasses(Long teacherId);

    int batchBind(Long teacherId, BatchBindDTO dto);

    int batchUnbind(Long teacherId, BatchUnbindDTO dto);
}
