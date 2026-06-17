package com.localfresh.aspect;

import com.localfresh.annotation.AutoFill;
import com.localfresh.constant.AutoFillConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自定義切面，實現公共字段的自動填充
 */
@Aspect
@Component
@Slf4j

public class AutoFillAspect {

    /**
     *  切入點
     */
    @Pointcut("execution(* com.localfresh.mapper.*.*(..)) && @annotation(com.localfresh.annotation.AutoFill)")
    public void autoFillPointcut(){}

    /**
     *  前置通知,在通知中為公共字段賦值
     */
     @Before("autoFillPointcut()")
    public void autoFill(JoinPoint joinPoint){
         log.info("開始進行公共字段的填充...");

         //獲取當前被攔截的方法上的資料庫操作類型
         MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //方法簽名對象
         AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); //獲取到資料庫的操作類型
         OperationType operationType = autoFill.value();//獲取到資料庫的操作類型


         //獲取到當前被攔截的方法上的參數 - 實體對象
         Object[] args = joinPoint.getArgs();
         if(args == null || args.length == 0){
             return;
         }
         Object entity = args[0];


         //準備賦值的資料
         LocalDateTime now = LocalDateTime.now();
         Long currentId = BaseContext.getCurrentId();

         //根據資料庫的操作類型，為對應的屬性通過反射來賦值
         if(operationType == OperationType.INSERT){
             //為4個公共字段來賦值
             try {
                 Method setCreatTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class);
                 Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                 Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                 Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                 // 通過反射來為對象屬性來賦值
                 setCreatTime.invoke(entity, now);
                 setCreateUser.invoke(entity, currentId);
                 setUpdateTime.invoke(entity, now);
                 setUpdateUser.invoke(entity, currentId);

             } catch (Exception e) {
                 log.error("公共字段自動填充失敗", e);
             }


         }else if(operationType == OperationType.UPDATE){
             // 為2個公共字段來賦值
             try {
                 Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                 Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                 // 通過反射來為對象屬性來賦值
                 setUpdateTime.invoke(entity, now);
                 setUpdateUser.invoke(entity, currentId);

             } catch (Exception e) {
                 log.error("公共字段自動填充失敗", e);
             }

         }

     }
}
