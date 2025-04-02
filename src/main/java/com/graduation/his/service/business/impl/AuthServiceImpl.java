package com.graduation.his.service.business.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.common.Constants;
import com.graduation.his.common.exception.BusinessException;
import com.graduation.his.domain.dto.ThirdPartyLoginInfoDTO;
import com.graduation.his.domain.dto.UserLoginDTO;
import com.graduation.his.domain.dto.UserRegisterDTO;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.domain.po.User;
import com.graduation.his.domain.vo.UserVO;
import com.graduation.his.service.business.IAuthService;
import com.graduation.his.service.entity.IPatientService;
import com.graduation.his.service.entity.IUserService;
import com.graduation.his.service.entity.MailService;
import com.graduation.his.utils.redis.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final IRedisService redisService;
    private final IUserService userService;
    private final IPatientService patientService;
    private final Map<String, AuthRequest> authRequestMap;
    
    /**
     * 邮箱验证码有效期（分钟）
     */
    private static final int EMAIL_CODE_EXPIRE_MINUTES = 5;
    
    /**
     * 第三方登录信息缓存前缀
     */
    private static final String THIRD_PARTY_LOGIN_KEY = "his:third_party:login:";
    
    /**
     * 第三方登录信息过期时间（小时）
     */
    private static final int THIRD_PARTY_LOGIN_EXPIRE_HOURS = 1;
    
    @Override
    public void sendEmailCode(String email) {
        if (StringUtils.isBlank(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        
        try {
            // 发送邮件验证码，验证码在方法内生成并保存到Redis
            mailService.sendVerificationCode(email);
            log.info("已发送验证码到邮箱：{}", email);
        } catch (Exception e) {
            log.error("发送验证码失败: {}", e.getMessage());
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
        
        // 验证用户名是否存在
        if (checkUserExists(username, null)) {
            throw new BusinessException("用户名已存在");
        }
        
        // 验证邮箱是否存在
        if (checkUserExists(null, email)) {
            throw new BusinessException("邮箱已存在");
        }
        
        // 验证验证码
        String key = Constants.RedisKey.HIS_MAIL_CODE + email;
        String codeInRedis = redisService.getValue(key);
        
        if (StringUtils.isBlank(codeInRedis)) {
            throw new BusinessException("验证码已过期");
        }
        
        if (!codeInRedis.equals(verifyCode)) {
            throw new BusinessException("验证码错误");
        }
        
        // 删除验证码
        redisService.remove(key);
        
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
        
        // 返回用户信息
        return buildUserVO(user, patient);
    }
    
    @Override
    public void logout() {
        StpUtil.logout();
    }
    
    @Override
    public String getThirdPartyLoginUrl(String platform) {
        if (StringUtils.isBlank(platform)) {
            throw new BusinessException("平台不能为空");
        }
        
        AuthRequest authRequest = authRequestMap.get(platform.toLowerCase());
        if (authRequest == null) {
            throw new BusinessException("不支持的平台");
        }
        
        // 生成state参数，用于防止CSRF攻击
        String state = UUID.randomUUID().toString();
        // 保存state到Redis，设置5分钟过期
        String stateKey = "auth:state:" + state;
        redisService.setValue(stateKey, state, TimeUnit.MINUTES.toMillis(5));
        
        return authRequest.authorize(state);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public String thirdPartyCallback(String platform, AuthCallback callback) {
        if (StringUtils.isBlank(platform)) {
            throw new BusinessException("平台不能为空");
        }
        
        AuthRequest authRequest = authRequestMap.get(platform.toLowerCase());
        if (authRequest == null) {
            throw new BusinessException("不支持的平台");
        }
        
        // 验证state参数，防止CSRF攻击
        String state = callback.getState();
        String stateKey = "auth:state:" + state;
        String savedState = redisService.getValue(stateKey);
        if (savedState == null || !savedState.equals(state)) {
            log.error("第三方登录state验证失败");
            return "/login?error=登录验证失败，请重试";
        }
        // 删除state
        redisService.remove(stateKey);
        
        // 获取第三方用户信息
        AuthResponse<AuthUser> response = authRequest.login(callback);
        if (response.getCode() != 2000) {
            log.error("第三方登录失败：{}", response.getMsg());
            return "/login?error=" + response.getMsg();
        }
        
        AuthUser authUser = response.getData();
        
        // 查询是否已有关联用户
        User user = userService.getByThirdParty(authUser.getUuid(), platform);
        
        if (user != null) {
            // 已关联用户，直接登录
            StpUtil.login(user.getId());
            
            // 更新最后登录时间和用户头像(如果有)
            user.setUpdateTime(LocalDateTime.now());
            // 如果第三方有新的头像，更新用户头像
            if (StringUtils.isNotBlank(authUser.getAvatar()) && 
                !authUser.getAvatar().equals(user.getAvatar())) {
                user.setAvatar(authUser.getAvatar());
            }
            userService.updateById(user);
            
            return "/";
        } else {
            // 未关联用户，保存临时信息到Redis，跳转到完善信息页面
            String token = UUID.randomUUID().toString();
            String key = THIRD_PARTY_LOGIN_KEY + token;
            
            // 保存第三方用户信息到Redis
            ThirdPartyLoginInfoDTO infoDTO = ThirdPartyLoginInfoDTO.builder()
                    .openId(authUser.getUuid())
                    .platform(platform)
                    .email(authUser.getEmail())
                    .name(authUser.getNickname())
                    .avatar(authUser.getAvatar())
                    .accessToken(authUser.getToken().getAccessToken())
                    .refreshToken(authUser.getToken().getRefreshToken())
                    .build();
            
            String infoJson = com.alibaba.fastjson2.JSON.toJSONString(infoDTO);
            redisService.setValue(key, infoJson, TimeUnit.HOURS.toMillis(THIRD_PARTY_LOGIN_EXPIRE_HOURS));
            
            // 构建重定向URL
            String redirectUrl = "/third-party-register?token=" + token;
            if (StringUtils.isNotBlank(authUser.getEmail())) {
                redirectUrl += "&email=" + authUser.getEmail();
            }
            if (StringUtils.isNotBlank(authUser.getNickname())) {
                redirectUrl += "&nickname=" + authUser.getNickname();
            }
            
            return redirectUrl;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO completeThirdPartyLoginInfo(ThirdPartyLoginInfoDTO loginInfoDTO) {
        if (loginInfoDTO == null) {
            throw new BusinessException("登录信息不能为空");
        }
        
        // 验证token，获取Redis中的第三方登录信息
        String token = loginInfoDTO.getThirdPartyToken();
        if (StringUtils.isBlank(token)) {
            throw new BusinessException("第三方登录令牌不能为空");
        }
        
        String key = THIRD_PARTY_LOGIN_KEY + token;
        String value = redisService.getValue(key);
        
        if (StringUtils.isBlank(value)) {
            throw new BusinessException("登录信息已过期，请重新授权");
        }
        
        // 解析Redis中的信息
        ThirdPartyLoginInfoDTO storedInfo = com.alibaba.fastjson2.JSON.parseObject(value, ThirdPartyLoginInfoDTO.class);
        String openId = storedInfo.getOpenId();
        String platform = storedInfo.getPlatform();
        
        // 删除Redis中的临时信息
        redisService.remove(key);
        
        // 验证必填信息
        if (StringUtils.isBlank(loginInfoDTO.getName())) {
            throw new BusinessException("姓名不能为空");
        }
        
        // 生成唯一用户名
        String username = generateUniqueUsername(loginInfoDTO.getName(), platform);
        
        // 保存用户信息
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtils.md5Hex(UUID.randomUUID().toString() + Constants.SALT)); // 随机密码
        user.setEmail(loginInfoDTO.getEmail());
        user.setPhone(loginInfoDTO.getPhone());
        user.setRole(0); // 0-患者
        user.setOpenId(openId);
        user.setAuthType(platform);
        // 设置头像，优先使用第三方头像
        if (StringUtils.isNotBlank(storedInfo.getAvatar())) {
            user.setAvatar(storedInfo.getAvatar());
        }
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userService.save(user);
        
        // 保存患者信息
        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patient.setName(loginInfoDTO.getName());
        patient.setGender(loginInfoDTO.getGender());
        patient.setAge(loginInfoDTO.getAge());
        patient.setIdCard(loginInfoDTO.getIdCard());
        patient.setRegion(loginInfoDTO.getRegion());
        patient.setAddress(loginInfoDTO.getAddress());
        patient.setCreateTime(LocalDateTime.now());
        patient.setUpdateTime(LocalDateTime.now());
        patientService.save(patient);
        
        // 自动登录
        StpUtil.login(user.getId());
        
        log.info("第三方用户注册成功：{}，平台：{}", user.getUsername(), platform);
        
        // 返回用户信息
        return buildUserVO(user, patient);
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
            userVO.setPhone(user.getPhone());
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
        userVO.setAvatar(user.getAvatar());
        userVO.setRole(user.getRole());
        userVO.setCreateTime(user.getCreateTime());
        userVO.setUpdateTime(user.getUpdateTime());
        
        return userVO;
    }
    
    /**
     * 生成唯一的用户名
     * 格式：name_平台_6位随机字符
     * @param name 用户姓名或昵称
     * @param platform 平台类型
     * @return 唯一用户名
     */
    private String generateUniqueUsername(String name, String platform) {
        // 移除名称中的特殊字符，只保留字母、数字和下划线
        String safeName = name.replaceAll("[^a-zA-Z0-9_]", "");
        if (StringUtils.isBlank(safeName)) {
            safeName = "user"; // 如果名称清理后为空，使用默认名称
        }
        
        // 生成6位随机字符串
        String randomStr = UUID.randomUUID().toString().substring(0, 6);
        String username = safeName + "_" + platform + "_" + randomStr;
        
        // 确保用户名唯一
        int suffix = 1;
        String tempUsername = username;
        while (userService.getByUsername(tempUsername) != null) {
            tempUsername = username + suffix;
            suffix++;
        }
        
        return tempUsername;
    }
    
    /**
     * 更新第三方登录的accessToken
     * 根据OAuth2.0规范，当access_token过期时可以使用refresh_token获取新的access_token
     * @param platform 平台类型
     * @param userId 用户ID
     * @param refreshToken 刷新令牌
     * @return 是否更新成功
     */
    private boolean refreshAccessToken(String platform, Long userId, String refreshToken) {
        if (StringUtils.isBlank(platform) || userId == null || StringUtils.isBlank(refreshToken)) {
            return false;
        }
        
        AuthRequest authRequest = authRequestMap.get(platform.toLowerCase());
        if (authRequest == null) {
            return false;
        }
        
        try {
            // 创建AuthToken对象
            me.zhyd.oauth.model.AuthToken authToken = new me.zhyd.oauth.model.AuthToken();
            authToken.setRefreshToken(refreshToken);
            
            // 调用第三方平台刷新token接口
            AuthResponse<?> response = authRequest.refresh(authToken);
            if (response.getCode() == 2000) {
                // 刷新成功，更新用户的token信息
                Object data = response.getData();
                if (data != null && data instanceof me.zhyd.oauth.model.AuthToken) {
                    me.zhyd.oauth.model.AuthToken newAuthToken = (me.zhyd.oauth.model.AuthToken) data;
                    
                    // 在实际应用中，可以将新的token信息存储到数据库或Redis中
                    // 这里只是记录日志
                    log.info("用户[{}]的{}平台token已刷新", userId, platform);
                    return true;
                }
            } else {
                log.error("刷新{}平台token失败: {}", platform, response.getMsg());
            }
        } catch (Exception e) {
            log.error("刷新{}平台token异常: {}", platform, e.getMessage());
        }
        
        return false;
    }

    /**
     * 验证第三方登录token是否有效
     * @param platform 平台类型
     * @param accessToken 访问令牌
     * @return 是否有效
     */
    private boolean validateAccessToken(String platform, String accessToken) {
        if (StringUtils.isBlank(platform) || StringUtils.isBlank(accessToken)) {
            return false;
        }
        
        if (platform.equalsIgnoreCase("qq")) {
            // 对于QQ登录，可以通过调用OpenAPI来验证token是否有效
            // 实际项目中可以调用QQ的OpenAPI进行验证
            // 这里简单返回true，假设token有效
            return true;
        }
        
        // 对于其他平台，可以实现对应的验证逻辑
        return true;
    }
}
