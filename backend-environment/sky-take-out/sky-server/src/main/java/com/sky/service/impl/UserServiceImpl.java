package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登錄
     * @param userLoginDTO
     * @return
     */
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenid(userLoginDTO.getCode());

        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 判斷當前用戶是否為新用戶
        User user = userMapper.selectByOpenid(openid);

        // 如果是新用戶，自動完成註冊
        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        return user;
    }

    /**
     * 調用微信接口服務，獲取微信用戶的openid
     * 【台灣開發環境 Mock 版】
     * 原本會呼叫微信 jscode2session API，但因台灣無法申請微信小程式，此處 mock 處理
     * 後續會將整個微信登錄模組替換為 Google OAuth 2.0 / LINE Login
     * @param code
     * @return
     */
    private String getOpenid(String code) {
        log.info("【Mock 登錄】收到 code: {}", code);
        String mockOpenid = "mock_openid_" + code;
        log.info("【Mock 微信登錄】產生假 openid: {}", mockOpenid);
        return mockOpenid;
    }
}