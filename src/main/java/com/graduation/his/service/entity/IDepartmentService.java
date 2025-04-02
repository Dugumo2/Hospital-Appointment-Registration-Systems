package com.graduation.his.service.entity;

import com.graduation.his.domain.po.Department;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 医院科室表 服务类
 * </p>
 *
 * @author hua
 * @since 2025-04-02
 */
public interface IDepartmentService extends IService<Department> {

    /**
     * 获取所有科室列表
     * @param onlyActive 是否只返回有效科室
     * @return 科室列表
     */
    List<Department> getDepartmentList(boolean onlyActive);
    
    /**
     * 获取科室名称
     * @param deptId 科室ID
     * @return 科室名称，如果科室不存在则返回null
     */
    String getDepartmentName(Long deptId);
}
