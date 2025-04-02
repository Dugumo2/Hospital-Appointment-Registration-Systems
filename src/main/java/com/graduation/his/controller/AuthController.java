package com.graduation.his.controller;

import com.graduation.his.common.Result;
import com.graduation.his.domain.dto.UserLoginDTO;
import com.graduation.his.domain.dto.UserRegisterDTO;
import com.graduation.his.domain.vo.UserVO;
import com.graduation.his.service.business.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author hua
 * @description 登录鉴权、用户信息相关接口
 * @create 2025-03-31 21:30
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final IAuthService authService;
    
    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @return 处理结果
     */
    @PostMapping("/sendEmailCode")
    public Result<Void> sendEmailCode(@RequestParam String email) {
        authService.sendEmailCode(email);
        return Result.success();
    }
    
    /**
     * 检查用户名或邮箱是否已存在
     * @param username 用户名
     * @param email 邮箱
     * @return 是否存在
     */
    @GetMapping("/checkUserExists")
    public Result<Boolean> checkUserExists(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        boolean exists = authService.checkUserExists(username, email);
        return Result.success(exists);
    }
    
    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 处理结果
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success();
    }
    
    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录成功的用户信息
     */
    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody UserLoginDTO loginDTO) {
        UserVO userVO = authService.login(loginDTO);
        return Result.success(userVO);
    }
    
    /**
     * 退出登录
     * @return 处理结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }
    
    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    @GetMapping("/currentUser")
    public Result<UserVO> getCurrentUserInfo() {
        UserVO userVO = authService.getCurrentUserInfo();
        return Result.success(userVO);
    }
}
