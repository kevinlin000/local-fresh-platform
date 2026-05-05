package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户登录
 */
@Data
public class MemberLoginDTO implements Serializable {

    private String code;

}
