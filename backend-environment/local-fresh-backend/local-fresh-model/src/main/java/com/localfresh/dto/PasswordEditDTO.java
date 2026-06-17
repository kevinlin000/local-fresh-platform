package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PasswordEditDTO implements Serializable {

    //员工id
    private Long empId;

    //旧密碼
    private String oldPassword;

    //新密碼
    private String newPassword;

}
