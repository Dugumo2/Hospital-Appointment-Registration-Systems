package com.graduation.his.service.business;

import com.graduation.his.domain.dto.UserLoginDTO;
import com.graduation.his.domain.dto.UserRegisterDTO;
import com.graduation.his.domain.dto.UserUpdateDTO;
import com.graduation.his.domain.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

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
     * 获取当前登录用户信息
     * @return 用户信息
     */
    UserVO getCurrentUserInfo();
    
    /**
     * 修改用户密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void updatePassword(String oldPassword, String newPassword);
    
    /**
     * 更新用户头像
     * @param file 头像文件
     * @return 新的头像URL
     */
    String updateAvatar(MultipartFile file);
    
    /**
     * 更新用户个人信息
     * @param updateDTO 用户个人信息
     * @return 更新后的用户信息
     */
    UserVO updateUserInfo(UserUpdateDTO updateDTO);
}
