package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.UserCreateDTO;
import com.edu.aienlighten.dto.UserUpdateDTO;
import com.edu.aienlighten.vo.PageVO;
import com.edu.aienlighten.vo.UserVO;

public interface AdminUserService {

    PageVO<UserVO> page(Integer role, String keyword, Integer page, Integer size);

    UserVO create(UserCreateDTO dto);

    void update(Long id, UserUpdateDTO dto);

    void updateStatus(Long id, Integer status);

    void delete(Long id);
}
