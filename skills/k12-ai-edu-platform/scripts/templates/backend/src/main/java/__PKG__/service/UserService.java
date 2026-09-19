package __PKG__.service;

import __PKG__.dto.LoginDTO;
import __PKG__.dto.RegisterDTO;
import __PKG__.entity.User;
import __PKG__.vo.LoginVO;

public interface UserService {

    LoginVO login(LoginDTO dto);

    LoginVO register(RegisterDTO dto);

    User getById(Long id);

    // TODO（阶段 4）：changePassword —— 需校验原密码、写 password_changed_at 作废旧 token、
    //  并对连续失败做锁定；实现要点见 REFERENCE-backend.md §2.2。
}
