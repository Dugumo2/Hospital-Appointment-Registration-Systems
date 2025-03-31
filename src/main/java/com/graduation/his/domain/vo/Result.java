package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果VO
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 返回码
     */
    private Integer code;
    
    /**
     * 返回消息
     */
    private String message;
    
    /**
     * 返回数据
     */
    private T data;
    
    /**
     * 成功结果，带数据和消息
     */
    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(true, 200, message, data);
    }
    
    /**
     * 成功结果，只带数据
     */
    public static <T> Result<T> ok(T data) {
        return ok(data, "操作成功");
    }
    
    /**
     * 成功结果，不带数据
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }
    
    /**
     * 失败结果，带消息
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(false, 500, message, null);
    }
    
    /**
     * 失败结果，带消息和错误码
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(false, code, message, null);
    }
} 