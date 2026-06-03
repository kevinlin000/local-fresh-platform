package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class EmployeeDTO implements Serializable {

    private Long id;

    @NotBlank(message = "員工帳號不能為空")
    private String username;

    @NotBlank(message = "員工姓名不能為空")
    private String name;

    @Pattern(regexp = "^$|^09\\d{8}$", message = "手機格式錯誤")
    private String phone;

    private String sex;

    private String idNumber;

}
