package com.graduation.his.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.graduation.his.common.Result;
import com.graduation.his.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理 BusinessException
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置为 400
    public Result handleBusinessException(BusinessException e) {
        log.info("test");
        return Result.error(e.getCode(), e.getMessage());
    }

    //处理异常
    @ExceptionHandler(Exception.class) //指定能够处理的异常类型
    public Result ex(Exception e){
        e.printStackTrace();//打印堆栈中的异常信息

        //捕获到异常之后，响应一个标准的Result
        return Result.error("对不起,操作失败,请联系管理员");
    }

    // 全局异常拦截（拦截项目中的NotLoginException异常）
    @ExceptionHandler(NotLoginException.class)
    public Result handlerNotLoginException(NotLoginException nle)
            throws Exception {

        // 打印堆栈，以供调试
        nle.printStackTrace();

        // 判断场景值，定制化异常信息
        String message = "";
        switch (nle.getType()) {
            case NotLoginException.NOT_TOKEN:
                message = "未能读取到有效 token";
                break;
            case NotLoginException.INVALID_TOKEN:
                message = "token 无效";
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                message = "token 已过期";
                break;
            case NotLoginException.BE_REPLACED:
                message = "token 已被顶下线";
                break;
            case NotLoginException.KICK_OUT:
                message = "token 已被踢下线";
                break;
            default:
                message = "当前会话未登录";
                break;
        }

        // 返回给前端
        return Result.error(message);
    }

}