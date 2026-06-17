package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;

@Data
public class GoogleOAuthLoginDTO implements Serializable {

    @NotBlank(message = "Google 授權 code 不能為空")
    private String code;
    private String redirectUri;
}
