package com.graduation.his.service.business.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.graduation.his.common.Constants;
import com.graduation.his.exception.BusinessException;
import com.graduation.his.domain.dto.UserLoginDTO;
import com.graduation.his.domain.dto.UserRegisterDTO;
import com.graduation.his.domain.dto.UserUpdateDTO;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.domain.po.User;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.vo.UserVO;
import com.graduation.his.service.business.IAuthService;
import com.graduation.his.service.entity.IPatientService;
import com.graduation.his.service.entity.IUserService;
import com.graduation.his.service.entity.MailService;
import com.graduation.his.service.entity.IDoctorService;
import com.graduation.his.utils.minio.MinioUtils;
import com.graduation.his.utils.redis.RedissonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final IDoctorService doctorService;
    private final MinioUtils minioUtils;
    
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
        // 设置默认头像
        user.setAvatar(Constants.MinioConstants.DEFAULT_AVATAR_URL);
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
        Patient patient = null;
        if (user.getRole() == 0) {
            patient = patientService.getByUserId(user.getId());
        }
        
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
        
        Patient patient = null;
        if (user.getRole() == 0) {
            patient = patientService.getByUserId(userId);
        }
        
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
        
        // 如果是医生角色，则获取医生信息
        if (user.getRole() == 1) {
            Doctor doctor = doctorService.getDoctorByUserId(user.getId());
            if (doctor != null) {
                userVO.setDoctorId(doctor.getDoctorId());
                // 如果患者信息为空，则使用医生的姓名等基本信息
                if (patient == null) {
                    userVO.setName(doctor.getName());
                    // 其他医生信息根据需要设置
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
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(String oldPassword, String newPassword) {
        // 验证参数
        if (StringUtils.isBlank(oldPassword)) {
            throw new BusinessException("原密码不能为空");
        }
        
        if (StringUtils.isBlank(newPassword)) {
            throw new BusinessException("新密码不能为空");
        }
        
        // 校验是否登录
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        
        // 获取当前登录用户
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 验证原密码是否正确
        String encryptedOldPassword = DigestUtils.md5Hex(oldPassword + Constants.SALT);
        if (!encryptedOldPassword.equals(user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        
        // 更新密码
        String encryptedNewPassword = DigestUtils.md5Hex(newPassword + Constants.SALT);
        user.setPassword(encryptedNewPassword);
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        
        log.info("用户[{}]密码修改成功", user.getUsername());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateAvatar(MultipartFile file) {
        // 校验参数
        if (file == null || file.isEmpty()) {
            throw new BusinessException("头像文件不能为空");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("上传的文件不是图片类型");
        }
        
        // 校验是否登录
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        
        // 获取当前登录用户
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        try {
            // 获取旧头像URL
            String oldAvatarUrl = user.getAvatar();
            // 如果是默认头像，则设置为null，不删除默认头像
            if (Constants.MinioConstants.DEFAULT_AVATAR_URL.equals(oldAvatarUrl)) {
                oldAvatarUrl = null;
            }
            
            // 上传新头像到MinIO
            String avatarUrl = minioUtils.updateAvatar(
                    Constants.MinioConstants.USER_AVATAR_BUCKET, 
                    file, 
                    oldAvatarUrl
            );
            
            // 更新用户头像
            user.setAvatar(avatarUrl);
            user.setUpdateTime(LocalDateTime.now());
            userService.updateById(user);
            
            log.info("用户[{}]头像更新成功", user.getUsername());
            return avatarUrl;
        } catch (Exception e) {
            log.error("更新头像失败: {}", e.getMessage(), e);
            throw new BusinessException("更新头像失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserInfo(UserUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new BusinessException("更新信息不能为空");
        }
        
        // 校验是否登录
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 获取用户信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 更新用户信息
        if (StringUtils.isNotBlank(updateDTO.getPhone())) {
            user.setPhone(updateDTO.getPhone());
        }
        
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        
        // 根据用户角色更新对应信息
        if (user.getRole() == 0) {
            // 患者角色
            updatePatientInfo(userId, updateDTO);
        } else if (user.getRole() == 1) {
            // 医生角色
            updateDoctorInfo(userId, updateDTO);
        }
        
        log.info("用户[{}]信息更新成功", user.getUsername());
        
        // 返回更新后的用户信息
        return getCurrentUserInfo();
    }
    
    /**
     * 更新患者信息
     * @param userId 用户ID
     * @param updateDTO 更新信息
     */
    private void updatePatientInfo(Long userId, UserUpdateDTO updateDTO) {
        // 获取患者信息
        Patient patient = patientService.getByUserId(userId);
        if (patient == null) {
            throw new BusinessException("患者信息不存在");
        }
        
        // 更新患者信息
        if (StringUtils.isNotBlank(updateDTO.getName())) {
            patient.setName(updateDTO.getName());
        }
        
        if (updateDTO.getGender() != null) {
            patient.setGender(updateDTO.getGender());
        }
        
        if (updateDTO.getAge() != null) {
            patient.setAge(updateDTO.getAge());
        }
        
        if (StringUtils.isNotBlank(updateDTO.getIdCard())) {
            patient.setIdCard(updateDTO.getIdCard());
        }
        
        if (StringUtils.isNotBlank(updateDTO.getRegion())) {
            patient.setRegion(updateDTO.getRegion());
        }
        
        if (StringUtils.isNotBlank(updateDTO.getAddress())) {
            patient.setAddress(updateDTO.getAddress());
        }
        
        patient.setUpdateTime(LocalDateTime.now());
        patientService.updateById(patient);
    }
    
    /**
     * 更新医生信息
     * @param userId 用户ID
     * @param updateDTO 更新信息
     */
    private void updateDoctorInfo(Long userId, UserUpdateDTO updateDTO) {
        // 获取医生信息
        Doctor doctor = doctorService.getDoctorByUserId(userId);
        if (doctor == null) {
            throw new BusinessException("医生信息不存在");
        }
        
        // 更新医生信息
        if (StringUtils.isNotBlank(updateDTO.getName())) {
            doctor.setName(updateDTO.getName());
        }
        
        // 其他医生特有信息需要在Doctor类中定义或者扩展UserUpdateDTO
        
        doctor.setUpdateTime(LocalDateTime.now());
        doctorService.updateById(doctor);
    }
}

