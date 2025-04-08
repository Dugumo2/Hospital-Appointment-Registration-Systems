package com.graduation.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.domain.po.Department;
import com.graduation.his.mapper.DepartmentMapper;
import com.graduation.his.service.entity.IDepartmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 医院科室表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-04-02
 */
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements IDepartmentService {

    @Override
    public List<Department> getDepartmentList(boolean onlyActive) {
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        
        // 如果只查询有效科室，则添加条件
        if (onlyActive) {
            queryWrapper.eq(Department::getIsActive, 1);
        }
        
        // 按科室ID和科室名称排序
        queryWrapper.orderByAsc(Department::getDeptId)
                .orderByAsc(Department::getDeptName);
        
        return list(queryWrapper);
    }

    @Override
    public String getDepartmentName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        
        Department department = getById(deptId);
        return department != null ? department.getDeptName() : null;
    }
}
