package com.graduation.his.service.business.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.common.Constants;
import com.graduation.his.common.exception.BusinessException;
import com.graduation.his.domain.dto.UserLoginDTO;
import com.graduation.his.domain.dto.UserRegisterDTO;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.domain.po.User;
import com.graduation.his.domain.vo.UserVO;
import com.graduation.his.service.business.IAuthService;
import com.graduation.his.service.entity.IPatientService;
import com.graduation.his.service.entity.IUserService;
import com.graduation.his.service.entity.MailService;
import com.graduation.his.utils.redis.RedissonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author hua
 * @description 登录鉴权，用户信息服务类
 * @create 2025-03-31 20:49
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {
    
    private final MailService mailService;
    private final RedissonService redissonService;
    private final IUserService userService;
    private final IPatientService patientService;
    
    @Override
    public void sendEmailCode(String email) {
        if (StringUtils.isBlank(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        
        try {
            // 使用MailService发送验证码
            mailService.sendVerificationCode(email);
            log.info("已发送验证码到邮箱：{}", email);
        } catch (Exception e) {
            log.error("发送验证码失败: {}", e.getMessage(), e);
            throw new BusinessException("发送验证码失败");
        }
    }
    
    @Override
    public boolean checkUserExists(String username, String email) {
        if (StringUtils.isNotBlank(username)) {
            User user = userService.getByUsername(username);
            return user != null;
        }
        
        if (StringUtils.isNotBlank(email)) {
            User user = userService.getByEmail(email);
            return user != null;
        }
        
        return false;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO registerDTO) {
        // 验证参数
        if (registerDTO == null) {
            throw new BusinessException("注册信息不能为空");
        }
        
        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();
        String email = registerDTO.getEmail();
        String verifyCode = registerDTO.getVerifyCode();
        
        if (StringUtils.isBlank(username)) {
            throw new BusinessException("用户名不能为空");
        }
        
        if (StringUtils.isBlank(password)) {
            throw new BusinessException("密码不能为空");
        }
        
        if (StringUtils.isBlank(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        
        if (StringUtils.isBlank(verifyCode)) {
            throw new BusinessException("验证码不能为空");
        }

        // 验证验证码
        String key = Constants.RedisKey.HIS_MAIL_CODE + email;
        String codeInRedis = redissonService.getValue(key);
        
        if (StringUtils.isBlank(codeInRedis)) {
            throw new BusinessException("验证码已过期");
        }
        
        if (!codeInRedis.equals(verifyCode)) {
            throw new BusinessException("验证码错误");
        }
        
        // 删除验证码
        redissonService.remove(key);
        
        // 保存用户信息
        User user = new User();
        user.setUsername(username);
        // 密码加盐哈希
        user.setPassword(DigestUtils.md5Hex(password + Constants.SALT));
        user.setEmail(email);
        user.setPhone(registerDTO.getPhone());
        user.setRole(0); // 0-患者
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userService.save(user);
        
        // 保存患者信息
        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patient.setName(registerDTO.getName());
        patient.setGender(registerDTO.getGender());
        patient.setAge(registerDTO.getAge());
        patient.setIdCard(registerDTO.getIdCard());
        patient.setRegion(registerDTO.getRegion());
        patient.setAddress(registerDTO.getAddress());
        patient.setCreateTime(LocalDateTime.now());
        patient.setUpdateTime(LocalDateTime.now());
        patientService.save(patient);
        
        log.info("用户注册成功：{}", username);
    }
    
    @Override
    public UserVO login(UserLoginDTO loginDTO) {
        if (loginDTO == null) {
            throw new BusinessException("登录信息不能为空");
        }
        
        String account = loginDTO.getAccount();
        String password = loginDTO.getPassword();
        
        if (StringUtils.isBlank(account)) {
            throw new BusinessException("账号不能为空");
        }
        
        if (StringUtils.isBlank(password)) {
            throw new BusinessException("密码不能为空");
        }
        
        // 查询用户
        User user = null;
        
        // 判断是邮箱还是用户名
        if (account.contains("@")) {
            user = userService.getByEmail(account);
        } else {
            user = userService.getByUsername(account);
        }
        
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 验证密码
        String encryptedPassword = DigestUtils.md5Hex(password + Constants.SALT);
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        // 更新最后登录时间
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        
        // 记录登录状态
        StpUtil.login(user.getId(), loginDTO.getRememberMe());
        
        // 获取患者信息
        Patient patient = patientService.getByUserId(user.getId());
        
        // 构建用户信息视图对象
        UserVO userVO = buildUserVO(user, patient);
        
        // 设置token
        userVO.setToken(StpUtil.getTokenValue());
        
        return userVO;
    }
    
    @Override
    public void logout() {
        StpUtil.logout();
    }
    
    @Override
    public UserVO getCurrentUserInfo() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        Patient patient = patientService.getByUserId(userId);
        
        return buildUserVO(user, patient);
    }
    
    /**
     * 构建用户视图对象
     * @param user 用户对象
     * @param patient 患者对象
     * @return 用户视图对象
     */
    private UserVO buildUserVO(User user, Patient patient) {
        UserVO userVO = new UserVO();
        
        userVO.setUserId(user.getId());
        
        if (patient != null) {
            userVO.setPatientId(patient.getPatientId());
            userVO.setName(patient.getName());
            userVO.setGender(patient.getGender());
            userVO.setAge(patient.getAge());
            userVO.setRegion(patient.getRegion());
            userVO.setAddress(patient.getAddress());
            
            // 身份证号脱敏显示
            if (StringUtils.isNotBlank(patient.getIdCard())) {
                String idCard = patient.getIdCard();
                if (idCard.length() > 10) {
                    userVO.setIdCard(idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4));
                } else {
                    userVO.setIdCard(idCard);
                }
            }
        }
        
        userVO.setUsername(user.getUsername());
        userVO.setEmail(user.getEmail());
        userVO.setPhone(user.getPhone());
        userVO.setAvatar(user.getAvatar());
        userVO.setRole(user.getRole());
        userVO.setCreateTime(user.getCreateTime());
        userVO.setUpdateTime(user.getUpdateTime());
        
        return userVO;
    }
}

