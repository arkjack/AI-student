package __PKG__.vo;

import lombok.Builder;
import lombok.Data;

/** 登录/注册返回体：token + 前端渲染所需的最小用户信息（绝不含密码散列） */
@Data
@Builder
public class LoginVO {
    private String token;
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;
}
