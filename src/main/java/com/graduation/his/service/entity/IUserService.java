package com.graduation.his.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduation.his.domain.po.User;

/**
 * <p>
 * 用户基本信息表 服务类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
public interface IUserService extends IService<User> {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象
     */
    User getByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户对象
     */
    User getByEmail(String email);

    /**
     * 根据第三方平台ID和类型查询用户
     * @param platformId 平台ID
     * @param platformType 平台类型
     * @return 用户对象
     */
    User getByThirdParty(String platformId, String platformType);
}
