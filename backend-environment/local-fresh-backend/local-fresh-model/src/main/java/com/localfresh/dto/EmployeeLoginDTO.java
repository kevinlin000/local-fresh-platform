package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;

@Data
public class EmployeeLoginDTO implements Serializable {

    @NotBlank(message = "員工帳號不能為空")
    private String username;

    @NotBlank(message = "密碼不能為空")
    private String password;

}
