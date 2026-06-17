package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;

/**
 * C端用户登录
 */
@Data
public class MemberLoginDTO implements Serializable {

    @NotBlank(message = "登入 code 不能為空")
    private String code;

}
