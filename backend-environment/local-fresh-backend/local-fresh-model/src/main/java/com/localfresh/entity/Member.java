package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 開發模式登入識別值
    private String openid;

    // Google OAuth subject
    private String googleSub;

    // 電子郵件
    private String email;

    // 姓名
    private String name;

    // 手機號碼
    private String phone;

    // 性別 0 女 1 男
    private String sex;

    // 身分證字號
    private String idNumber;

    // 頭像
    private String avatar;

    // Google 頭像
    private String avatarUrl;

    // 登入方式
    private String loginProvider;

    // 註冊時間
    private LocalDateTime createTime;
}
