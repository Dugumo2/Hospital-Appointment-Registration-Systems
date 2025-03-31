package com.graduation.his.utils.security;

/**
 * 安全工具类，用于获取当前登录用户信息
 * 注意：这是一个临时的模拟实现，实际项目中需要替换为真实的安全认证实现
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户ID
     * 模拟实现：返回固定值1L，实际项目中应从认证上下文获取
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        // 模拟返回固定用户ID，实际项目中应从认证上下文获取
        return 1L;
    }
    
    /**
     * 获取当前登录患者ID
     * 模拟实现：返回固定值1L，实际项目中应根据用户ID查询患者信息
     * @return 患者ID
     */
    public static Long getCurrentPatientId() {
        // 模拟返回固定患者ID，实际项目中应查询患者信息
        return 1L;
    }
    
    /**
     * 判断当前用户是否拥有指定角色
     * 模拟实现：始终返回true，实际项目中应检查用户角色
     * @param role 角色名称
     * @return 是否拥有该角色
     */
    public static boolean hasRole(String role) {
        // 模拟始终拥有角色，实际项目中应从认证上下文检查角色
        return true;
    }
} 