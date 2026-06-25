package com.localfresh.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class MemberRegisterDTO implements Serializable {

    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    @Size(max = 128, message = "Email 長度不能超過 128 個字")
    private String email;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, max = 64, message = "密碼長度需為 8 到 64 個字")
    private String password;

    @NotBlank(message = "姓名不能為空")
    @Size(max = 32, message = "姓名長度不能超過 32 個字")
    private String name;

    @Pattern(regexp = "^$|^09\\d{8}$", message = "手機格式需為 09 開頭的 10 碼號碼")
    private String phone;
}
