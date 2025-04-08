package com.graduation.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.domain.po.Clinic;
import com.graduation.his.mapper.ClinicMapper;
import com.graduation.his.service.entity.IClinicService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 门诊表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-04-04
 */
@Service
public class ClinicServiceImpl extends ServiceImpl<ClinicMapper, Clinic> implements IClinicService {

    @Override
    public List<Clinic> getClinicsByDeptId(Long deptId, boolean onlyActive) {
        if (deptId == null) {
            return null;
        }
        
        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Clinic::getDeptId, deptId);
        
        // 如果只查询有效门诊，则添加条件
        if (onlyActive) {
            queryWrapper.eq(Clinic::getIsActive, 1);
        }
        
        // 排序
        queryWrapper.orderByAsc(Clinic::getClinicName);
        
        return list(queryWrapper);
    }

    @Override
    public List<Clinic> getClinicsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Clinic::getClinicName, name);
        
        // 默认只查询有效门诊
        queryWrapper.eq(Clinic::getIsActive, 1);
        
        // 排序
        queryWrapper.orderByAsc(Clinic::getDeptId)
                .orderByAsc(Clinic::getClinicName);
        
        return list(queryWrapper);
    }

    @Override
    public String getClinicName(Long clinicId) {
        if (clinicId == null) {
            return null;
        }
        
        Clinic clinic = getById(clinicId);
        return clinic != null ? clinic.getClinicName() : null;
    }
}
