package com.localfresh.exception;

/**
 * 密碼修改失敗例外
 */
public class PasswordEditFailedException extends BaseException{

    public PasswordEditFailedException(String msg){
        super(msg);
    }

}
