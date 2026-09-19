package __PKG__.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    // TODO（阶段 4）：加入班级需要 classId，注册后生成入班申请待教师审批。
    //  完整流程见 REFERENCE-architecture.md §3.2「入班审批流」。
}
