package com.graduation.his.service.business;

import com.graduation.his.domain.dto.ThirdPartyLoginInfoDTO;
import com.graduation.his.domain.dto.UserLoginDTO;
import com.graduation.his.domain.dto.UserRegisterDTO;
import com.graduation.his.domain.vo.UserVO;
import me.zhyd.oauth.model.AuthCallback;

/**
 * @author hua
 * @description 登录鉴权，用户信息接口
 * @create 2025-03-31 20:49
 */
public interface IAuthService {
    
    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     */
    void sendEmailCode(String email);
    
    /**
     * 检查用户名或邮箱是否已存在
     * @param username 用户名
     * @param email 邮箱
     * @return 是否存在
     */
    boolean checkUserExists(String username, String email);
    
    /**
     * 用户注册
     * @param registerDTO 注册信息
     */
    void register(UserRegisterDTO registerDTO);
    
    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录成功的用户信息
     */
    UserVO login(UserLoginDTO loginDTO);
    
    /**
     * 退出登录
     */
    void logout();
    
    /**
     * 获取第三方登录认证链接
     * @param platform 平台类型（qq、weixin等）
     * @return 认证链接
     */
    String getThirdPartyLoginUrl(String platform);
    
    /**
     * 处理第三方登录回调
     * @param platform 平台类型
     * @param callback 回调参数
     * @return 重定向URL
     */
    String thirdPartyCallback(String platform, AuthCallback callback);
    
    /**
     * 第三方登录补充用户信息
     * @param loginInfoDTO 补充的用户信息
     * @return 登录成功的用户信息
     */
    UserVO completeThirdPartyLoginInfo(ThirdPartyLoginInfoDTO loginInfoDTO);
    
    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    UserVO getCurrentUserInfo();
}
