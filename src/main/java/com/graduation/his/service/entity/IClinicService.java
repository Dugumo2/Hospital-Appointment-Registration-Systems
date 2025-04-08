package com.graduation.his.service.entity;

import com.graduation.his.domain.po.Clinic;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 门诊表 服务类
 * </p>
 *
 * @author hua
 * @since 2025-04-04
 */
public interface IClinicService extends IService<Clinic> {

    /**
     * 根据科室ID获取门诊列表
     *
     * @param deptId 科室ID
     * @param onlyActive 是否只返回有效门诊
     * @return 门诊列表
     */
    List<Clinic> getClinicsByDeptId(Long deptId, boolean onlyActive);

    /**
     * 根据门诊名称模糊查询门诊列表
     *
     * @param name 门诊名称
     * @return 门诊列表
     */
    List<Clinic> getClinicsByName(String name);

    /**
     * 获取门诊名称
     *
     * @param clinicId 门诊ID
     * @return 门诊名称，如果门诊不存在则返回null
     */
    String getClinicName(Long clinicId);
}
