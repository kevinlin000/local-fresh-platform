package com.localfresh.exception;

/**
 * 登入失敗
 */
public class LoginFailedException extends BaseException{
    public LoginFailedException(String msg){
        super(msg);
    }
}
