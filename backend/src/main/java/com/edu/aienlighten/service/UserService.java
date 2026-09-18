package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.ChangePasswordDTO;
import com.edu.aienlighten.dto.LoginDTO;
import com.edu.aienlighten.dto.RegisterDTO;
import com.edu.aienlighten.dto.UserPreferenceDTO;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.vo.LoginVO;
import com.edu.aienlighten.vo.UserPreferenceVO;

public interface UserService {

    LoginVO login(LoginDTO dto);

    LoginVO register(RegisterDTO dto);

    User getById(Long id);

    /**
     * 修改当前登录用户的密码。
     * 必须先校验原密码；成功后写入 password_changed_at，使改密之前签发的 token 立即失效。
     */
    void changePassword(ChangePasswordDTO dto);

    /** 读取当前登录用户的偏好设置 */
    UserPreferenceVO getPreferences();

    /** 更新当前登录用户的偏好设置（null 字段不修改） */
    UserPreferenceVO updatePreferences(UserPreferenceDTO dto);
}
