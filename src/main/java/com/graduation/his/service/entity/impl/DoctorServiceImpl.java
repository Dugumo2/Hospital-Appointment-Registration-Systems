package com.graduation.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.mapper.DoctorMapper;
import com.graduation.his.service.entity.IDoctorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 医生信息表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements IDoctorService {

    @Override
    public List<Doctor> getDoctorsByClinicId(Long clinicId) {
        if (clinicId == null) {
            return null;
        }
        
        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Doctor::getClinicId, clinicId);
        queryWrapper.orderByAsc(Doctor::getName);
        return list(queryWrapper);
    }

    @Override
    public List<Doctor> getDoctorsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Doctor::getName, name);
        queryWrapper.orderByAsc(Doctor::getClinicId)
                .orderByAsc(Doctor::getName);
        return list(queryWrapper);
    }

    @Override
    public Doctor getDoctorByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        
        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Doctor::getUserId, userId);
        return getOne(queryWrapper);
    }
}
