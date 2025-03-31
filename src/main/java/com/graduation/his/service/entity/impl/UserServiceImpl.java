package com.graduation.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.domain.po.User;
import com.graduation.his.mapper.UserMapper;
import com.graduation.his.service.entity.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户基本信息表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    @Override
    public User getByEmail(String email) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("LIMIT 1"));
    }

    @Override
    public User getByThirdParty(String platformId, String platformType) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenId, platformId)
                .eq(User::getAuthType, platformType)
                .last("LIMIT 1"));
    }
}
