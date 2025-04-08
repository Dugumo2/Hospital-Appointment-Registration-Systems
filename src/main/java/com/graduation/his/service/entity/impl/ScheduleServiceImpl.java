package com.graduation.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.graduation.his.common.exception.BusinessException;
import com.graduation.his.domain.po.Schedule;
import com.graduation.his.mapper.ScheduleMapper;
import com.graduation.his.service.entity.IScheduleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 医生排班表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements IScheduleService {

    @Override
    public List<Schedule> getSchedulesByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate) {
        if (doctorId == null || startDate == null || endDate == null) {
            return null;
        }
        
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, doctorId)
                .ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate)
                .eq(Schedule::getStatus, 1)  // 只查询有效排班
                .orderByAsc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getTimeSlot);
        
        return list(queryWrapper);
    }

    @Override
    public List<Schedule> getSchedulesByClinicAndDateRange(Long clinicId, LocalDate startDate, LocalDate endDate) {
        if (clinicId == null || startDate == null || endDate == null) {
            return null;
        }
        
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getClinicId, clinicId)
                .ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate)
                .eq(Schedule::getStatus, 1)  // 只查询有效排班
                .orderByAsc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getTimeSlot)
                .orderByAsc(Schedule::getDoctorId);
        
        return list(queryWrapper);
    }

    @Override
    public List<Schedule> getAvailableSchedules(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        
        // 先获取所有的排班信息
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate)
                .eq(Schedule::getStatus, 1)  // 只查询有效排班
                .orderByAsc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getTimeSlot)
                .orderByAsc(Schedule::getClinicId)
                .orderByAsc(Schedule::getDoctorId);
        
        List<Schedule> schedules = list(queryWrapper);
        
        // 过滤出有可用名额的排班
        return schedules.stream()
                .filter(schedule -> schedule.getCurrentPatients() < schedule.getMaxPatients())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementCurrentPatients(Long scheduleId) {
        if (scheduleId == null) {
            throw new BusinessException("排班ID不能为空");
        }
        
        // 获取排班信息
        Schedule schedule = getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        
        // 检查是否有可用名额
        if (schedule.getCurrentPatients() >= schedule.getMaxPatients()) {
            throw new BusinessException("该排班已无可预约名额");
        }
        
        // 增加已预约人数
        LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Schedule::getScheduleId, scheduleId)
                .setSql("current_patients = current_patients + 1");
        
        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decrementCurrentPatients(Long scheduleId) {
        if (scheduleId == null) {
            throw new BusinessException("排班ID不能为空");
        }
        
        // 获取排班信息
        Schedule schedule = getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        
        // 检查当前预约人数
        if (schedule.getCurrentPatients() <= 0) {
            throw new BusinessException("该排班当前预约人数已为0");
        }
        
        // 减少已预约人数
        LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Schedule::getScheduleId, scheduleId)
                .setSql("current_patients = current_patients - 1");
        
        return update(updateWrapper);
    }
}
