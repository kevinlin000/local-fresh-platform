package com.localfresh.handler;

import com.localfresh.constant.MessageConstant;
import com.localfresh.exception.BaseException;
import com.localfresh.exception.ForbiddenOperationException;
import com.localfresh.exception.ShoppingCartBusinessException;
import com.localfresh.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全域例外處理器，處理專案中拋出的業務例外
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler
    public Result exceptionHandler(ForbiddenOperationException ex){
        log.error("例外訊息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler
    public Result exceptionHandler(ShoppingCartBusinessException ex){
        log.error("例外訊息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕捉業務例外
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("例外訊息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result exceptionHandler(MethodArgumentNotValidException ex) {
        return Result.error(firstFieldErrorMessage(ex.getBindingResult().getFieldError()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Result exceptionHandler(BindException ex) {
        return Result.error(firstFieldErrorMessage(ex.getBindingResult().getFieldError()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result exceptionHandler(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(MessageConstant.UNKNOWN_ERROR);
        return Result.error(message);
    }

    /**
     * 捕獲SQL異常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        // Duplicate entry 'zhangsan' for key 'employee.idx_username'
        String message = ex.getMessage();
        if(message.contains("Duplicate entry")){
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }else {
            return Result.error(MessageConstant.UNKNOWN_ERROR);

        }
    }

    private String firstFieldErrorMessage(FieldError fieldError) {
        if (fieldError == null || fieldError.getDefaultMessage() == null) {
            return MessageConstant.UNKNOWN_ERROR;
        }
        return fieldError.getDefaultMessage();
    }

}
