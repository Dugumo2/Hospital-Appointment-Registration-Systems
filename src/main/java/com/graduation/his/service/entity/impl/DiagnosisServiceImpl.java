package com.graduation.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.domain.po.Diagnosis;
import com.graduation.his.mapper.DiagnosisMapper;
import com.graduation.his.service.entity.IDiagnosisService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * <p>
 * 医生诊断记录表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Service
public class DiagnosisServiceImpl extends ServiceImpl<DiagnosisMapper, Diagnosis> implements IDiagnosisService {

    @Override
    public List<Diagnosis> getDiagnosesByPatientId(Long patientId) {
        if (patientId == null) {
            return null;
        }
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getPatientId, patientId);
        queryWrapper.orderByDesc(Diagnosis::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public List<Diagnosis> getDiagnosesByDoctorId(Long doctorId) {
        if (doctorId == null) {
            return null;
        }
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getDoctorId, doctorId);
        queryWrapper.orderByDesc(Diagnosis::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public boolean isWithinFeedbackPeriod(Long diagId) {
        if (diagId == null) {
            return false;
        }
        
        Diagnosis diagnosis = getById(diagId);
        if (diagnosis == null) {
            return false;
        }
        
        // 获取诊断时间
        LocalDateTime diagTime = diagnosis.getCreateTime();
        if (diagTime == null) {
            return false;
        }
        
        // 计算诊断时间距离现在的天数
        long daysBetween = ChronoUnit.DAYS.between(diagTime, LocalDateTime.now());
        
        // 如果在15天内，则允许反馈
        return daysBetween <= 15;
    }

    @Override
    public List<Diagnosis> getDiagnosesByPatientAndDoctor(Long patientId, Long doctorId) {
        if (patientId == null || doctorId == null) {
            return null;
        }
        
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getPatientId, patientId)
                .eq(Diagnosis::getDoctorId, doctorId)
                .orderByDesc(Diagnosis::getCreateTime);
        
        return list(queryWrapper);
    }

    @Override
    public Diagnosis getDiagnosisDetail(Long diagId) {
        if (diagId == null) {
            return null;
        }
        return getById(diagId);
    }
}
