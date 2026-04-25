package com.example.edu.modules.auth.service;

import com.example.edu.modules.auth.dto.LoginDTO;
import com.example.edu.modules.auth.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);
}
