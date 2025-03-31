package com.graduation.his.service.entity.impl;

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

}
