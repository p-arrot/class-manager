package com.example.edu.modules.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.edu.modules.user.dto.PasswordResetDTO;
import com.example.edu.modules.user.dto.StudentBatchIdsDTO;
import com.example.edu.modules.user.dto.StudentCreateDTO;
import com.example.edu.modules.user.dto.StudentPageDTO;
import com.example.edu.modules.user.dto.StudentUpdateDTO;
import com.example.edu.modules.user.vo.StudentImportResultVO;
import com.example.edu.modules.user.vo.StudentVO;
import org.springframework.web.multipart.MultipartFile;

public interface StudentService {

    StudentImportResultVO importStudents(MultipartFile file);

    IPage<StudentVO> listStudents(StudentPageDTO dto);

    StudentVO create(StudentCreateDTO dto);

    StudentVO update(Long id, StudentUpdateDTO dto);

    void delete(Long id);

    void resetPassword(Long id, PasswordResetDTO dto);

    void batchDelete(StudentBatchIdsDTO dto);

    void batchResetPassword(StudentBatchIdsDTO dto);
}
