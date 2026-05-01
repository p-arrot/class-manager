package com.example.edu.modules.classes.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.edu.modules.classes.dto.ClassCreateDTO;
import com.example.edu.modules.classes.dto.ClassUpdateDTO;
import com.example.edu.modules.classes.dto.ClassPageDTO;
import com.example.edu.modules.classes.vo.ClassVO;

import java.util.List;

public interface ClassService {

    ClassVO create(ClassCreateDTO dto);

    void delete(Long id);

    ClassVO update(Long id, ClassUpdateDTO dto);

    ClassVO getById(Long id);

    IPage<ClassVO> page(ClassPageDTO dto);

    List<ClassVO> listAll();
}
