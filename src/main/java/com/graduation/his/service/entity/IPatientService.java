package com.graduation.his.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduation.his.domain.po.Patient;

/**
 * <p>
 * 患者信息表 服务类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
public interface IPatientService extends IService<Patient> {

    /**
     * 根据用户ID查询患者
     * @param userId 用户ID
     * @return 患者对象
     */
    Patient getByUserId(Long userId);
}
