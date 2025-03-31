package com.graduation.his.controller;

import com.graduation.his.common.Result;
import com.graduation.his.domain.dto.ThirdPartyLoginInfoDTO;
import com.graduation.his.domain.dto.UserLoginDTO;
import com.graduation.his.domain.dto.UserRegisterDTO;
import com.graduation.his.domain.vo.UserVO;
import com.graduation.his.service.business.IAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * <p>
 * 用户基本信息表 前端控制器
 * </p>
 *
 * @author hua
 * @since 2025-03-30
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
     * @return 发送结果
     */
    @GetMapping("/sendEmailCode")
    public Result<Void> sendEmailCode(@RequestParam String email) {
        authService.sendEmailCode(email);
        return Result.success();
    }

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 检查结果
     */
    @GetMapping("/checkUsername")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = authService.checkUserExists(username, null);
        return Result.success(exists);
    }

    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 检查结果
     */
    @GetMapping("/checkEmail")
    public Result<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = authService.checkUserExists(null, email);
        return Result.success(exists);
    }

    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success();
    }

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody UserLoginDTO loginDTO) {
        UserVO userVO = authService.login(loginDTO);
        return Result.success(userVO);
    }

    /**
     * 用户登出
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 获取第三方登录链接
     * @param platform 平台
     * @return 登录链接
     */
    @GetMapping("/thirdParty/url")
    public Result<String> getThirdPartyLoginUrl(@RequestParam String platform) {
        String url = authService.getThirdPartyLoginUrl(platform);
        return Result.success(url);
    }

    /**
     * 第三方登录回调接口
     * @param platform 平台
     * @param callback 回调参数
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    @GetMapping("/thirdParty/callback/{platform}")
    public void thirdPartyCallback(
            @PathVariable String platform,
            AuthCallback callback,
            HttpServletResponse response) throws IOException {
        String redirectUrl = authService.thirdPartyCallback(platform, callback);
        response.sendRedirect(redirectUrl);
    }

    /**
     * 完善第三方登录信息
     * @param loginInfoDTO 第三方登录信息
     * @return 用户信息
     */
    @PostMapping("/thirdParty/complete")
    public Result<UserVO> completeThirdPartyLoginInfo(@RequestBody ThirdPartyLoginInfoDTO loginInfoDTO) {
        UserVO userVO = authService.completeThirdPartyLoginInfo(loginInfoDTO);
        return Result.success(userVO);
    }

    /**
     * 获取当前用户信息
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> getCurrentUserInfo() {
        UserVO userVO = authService.getCurrentUserInfo();
        return Result.success(userVO);
    }
}
