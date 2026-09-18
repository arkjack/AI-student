package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.LoginDTO;
import com.edu.aienlighten.dto.RegisterDTO;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.vo.LoginVO;

public interface UserService {

    LoginVO login(LoginDTO dto);

    LoginVO register(RegisterDTO dto);

    User getById(Long id);
}
