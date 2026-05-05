package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GoogleOAuthLoginDTO implements Serializable {

    private String code;
    private String redirectUri;
}
