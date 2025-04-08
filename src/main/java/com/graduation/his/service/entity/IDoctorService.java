package com.graduation.his.service.entity;

import com.graduation.his.domain.po.Doctor;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 医生信息表 服务类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
public interface IDoctorService extends IService<Doctor> {

    /**
     * 根据门诊ID获取医生列表
     *
     * @param clinicId 门诊ID
     * @return 医生列表
     */
    List<Doctor> getDoctorsByClinicId(Long clinicId);

    /**
     * 根据医生姓名模糊查询医生列表
     *
     * @param name 医生姓名
     * @return 医生列表
     */
    List<Doctor> getDoctorsByName(String name);

    /**
     * 根据用户ID查询医生信息
     *
     * @param userId 用户ID
     * @return 医生信息
     */
    Doctor getDoctorByUserId(Long userId);

}
