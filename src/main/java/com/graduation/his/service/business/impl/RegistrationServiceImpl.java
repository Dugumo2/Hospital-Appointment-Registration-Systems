package com.graduation.his.service.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.common.exception.BusinessException;
import com.graduation.his.domain.dto.AiConsultConnectionRequest;
import com.graduation.his.domain.dto.AiConsultRequest;
import com.graduation.his.domain.dto.ConsultSession;
import com.graduation.his.domain.po.Appointment;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Schedule;
import com.graduation.his.service.business.IRegistrationService;
import com.graduation.his.service.entity.IAIService;
import com.graduation.his.service.entity.IAppointmentService;
import com.graduation.his.service.entity.IDoctorService;
import com.graduation.his.service.entity.IScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hua
 * @description 预约挂号与问诊服务实现类
 * @create 2025-03-30 17:29
 */
@Slf4j
@Service
public class RegistrationServiceImpl implements IRegistrationService {

    @Autowired
    private IAIService aiService;
    
    @Autowired
    private IDoctorService doctorService;
    
    @Autowired
    private IScheduleService scheduleService;
    
    @Autowired
    private IAppointmentService appointmentService;
    
    @Override
    public SseEmitter createAiConsultConnection(AiConsultConnectionRequest request) {
        log.info("创建AI问诊SSE连接, appointmentId: {}, patientId: {}, sessionId: {}", 
                request.getAppointmentId(), request.getPatientId(), request.getSessionId());
        
        // 验证参数
        if (request.getAppointmentId() == null) {
            throw new IllegalArgumentException("预约ID不能为空");
        }
        if (request.getPatientId() == null) {
            throw new IllegalArgumentException("患者ID不能为空");
        }
        
        // 调用AI服务创建连接
        return aiService.createSseConnection(request);
    }
    
    @Override
    public String sendAiConsultRequest(AiConsultRequest request) {
        log.info("发送AI问诊请求, patientId: {}, sessionId: {}", request.getPatientId(), request.getSessionId());
        
        // 参数验证
        if (request.getPatientId() == null) {
            throw new IllegalArgumentException("患者ID不能为空");
        }
        
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("问题内容不能为空");
        }
        
        // 调用AI服务处理问诊请求
        String sessionId = aiService.processAiConsult(request);
        log.info("AI问诊请求已处理, 返回sessionId: {}", sessionId);
        
        return sessionId;
    }
    
    @Override
    public boolean endAiConsultSession(String sessionId) {
        log.info("结束AI问诊会话, sessionId: {}", sessionId);
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        
        // 调用AI服务结束会话（会将会话保存到数据库中）
        boolean result = aiService.endConsultSession(sessionId);
        log.info("AI问诊会话结束 {}, sessionId: {}", result ? "成功" : "失败", sessionId);
        
        return result;
    }
    
    @Override
    public ConsultSession getAiConsultHistory(String sessionId) {
        log.info("获取AI问诊历史会话, sessionId: {}", sessionId);
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        
        // 调用AI服务获取会话历史（优先从Redis获取，如Redis不存在则从数据库获取）
        ConsultSession session = aiService.getConsultSession(sessionId);
        
        if (session == null) {
            log.warn("未找到AI问诊历史会话, sessionId: {}", sessionId);
        } else {
            log.info("获取AI问诊历史会话成功, sessionId: {}, 消息数量: {}", 
                    sessionId, session.getMessageHistory().size());
        }
        
        return session;
    }
    
    @Override
    public List<Doctor> getDoctorList(Long deptId) {
        log.info("获取医生列表, deptId: {}", deptId);
        
        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            queryWrapper.eq(Doctor::getDeptId, deptId);
        }
        
        return doctorService.list(queryWrapper);
    }
    
    @Override
    public Doctor getDoctorDetail(Long doctorId) {
        log.info("获取医生详情, doctorId: {}", doctorId);
        
        if (doctorId == null) {
            throw new BusinessException("医生ID不能为空");
        }
        
        Doctor doctor = doctorService.getById(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        
        return doctor;
    }
    
    @Override
    public Map<LocalDate, List<Schedule>> getDoctorSchedule(Long doctorId, LocalDate startDate, LocalDate endDate) {
        log.info("获取医生排班信息, doctorId: {}, startDate: {}, endDate: {}", doctorId, startDate, endDate);
        
        if (doctorId == null) {
            throw new BusinessException("医生ID不能为空");
        }
        
        // 默认查询从今天开始7天内的排班
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        
        if (endDate == null) {
            endDate = startDate.plusDays(6); // 一周内
        }
        
        // 验证日期范围是否合法
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        
        // 只允许查询7天内的排班，从今天算起
        LocalDate maxDate = LocalDate.now().plusDays(6);
        if (endDate.isAfter(maxDate)) {
            endDate = maxDate;
        }
        
        // 查询医生在指定日期范围内的排班
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, doctorId)
                .ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate)
                .eq(Schedule::getStatus, 1) // 有效的排班
                .orderByAsc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getTimeSlot);
        
        List<Schedule> schedules = scheduleService.list(queryWrapper);
        
        // 按日期分组
        return schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getScheduleDate));
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appointment createAppointment(Long patientId, Long scheduleId) {
        log.info("创建预约挂号, patientId: {}, scheduleId: {}", patientId, scheduleId);
        
        if (patientId == null) {
            throw new BusinessException("患者ID不能为空");
        }
        
        if (scheduleId == null) {
            throw new BusinessException("排班ID不能为空");
        }
        
        // 获取排班信息
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在或已被取消");
        }
        
        // 检查排班是否有效
        if (schedule.getStatus() != 1) {
            throw new BusinessException("该时段已停诊");
        }
        
        // 检查排班日期是否在有效范围内（今天到未来7天）
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(6);
        if (schedule.getScheduleDate().isBefore(today) || schedule.getScheduleDate().isAfter(maxDate)) {
            throw new BusinessException("该时段已不可预约");
        }
        
        // 检查是否已预约过该医生的该时段
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getPatientId, patientId)
                .eq(Appointment::getDoctorId, schedule.getDoctorId())
                .eq(Appointment::getScheduleId, scheduleId)
                .ne(Appointment::getStatus, 2); // 非取消状态
        
        long count = appointmentService.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException("您已预约过该时段");
        }
        
        // 检查该排班时段是否已满
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getScheduleId, scheduleId)
                .ne(Appointment::getStatus, 2); // 非取消状态
        
        count = appointmentService.count(queryWrapper);
        if (count >= schedule.getMaxPatients()) {
            throw new BusinessException("该时段预约已满");
        }
        
        // 创建预约记录
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setScheduleId(scheduleId);
        appointment.setAppointmentDate(schedule.getScheduleDate());
        appointment.setTimeSlot(schedule.getTimeSlot());
        appointment.setStatus(0); // 待就诊
        appointment.setCreateTime(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());
        
        appointmentService.save(appointment);
        
        log.info("预约挂号创建成功, appointmentId: {}", appointment.getAppointmentId());
        
        return appointment;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelAppointment(Long appointmentId, Long patientId) {
        log.info("取消预约挂号, appointmentId: {}, patientId: {}", appointmentId, patientId);
        
        if (appointmentId == null) {
            throw new BusinessException("预约ID不能为空");
        }
        
        if (patientId == null) {
            throw new BusinessException("患者ID不能为空");
        }
        
        // 获取预约记录
        Appointment appointment = appointmentService.getById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约记录不存在");
        }
        
        // 验证患者身份
        if (!appointment.getPatientId().equals(patientId)) {
            throw new BusinessException("无权操作此预约记录");
        }
        
        // 检查预约状态
        if (appointment.getStatus() == 2) {
            throw new BusinessException("该预约已取消");
        }
        
        if (appointment.getStatus() == 1) {
            throw new BusinessException("已就诊的预约不能取消");
        }
        
        // 检查是否可以取消（预约当天前一天及之前可取消）
        LocalDate today = LocalDate.now();
        if (appointment.getAppointmentDate().equals(today)) {
            throw new BusinessException("当天的预约无法取消");
        }
        
        // 修改预约状态为已取消
        appointment.setStatus(2); // 已取消
        appointment.setUpdateTime(LocalDateTime.now());
        
        boolean result = appointmentService.updateById(appointment);
        
        log.info("预约挂号取消 {}, appointmentId: {}", result ? "成功" : "失败", appointmentId);
        
        return result;
    }
    
    @Override
    public List<Appointment> getPatientAppointments(Long patientId, Integer status) {
        log.info("获取患者预约记录, patientId: {}, status: {}", patientId, status);
        
        if (patientId == null) {
            throw new BusinessException("患者ID不能为空");
        }
        
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getPatientId, patientId);
        
        if (status != null) {
            queryWrapper.eq(Appointment::getStatus, status);
        }
        
        queryWrapper.orderByDesc(Appointment::getAppointmentDate)
                .orderByDesc(Appointment::getTimeSlot);
        
        return appointmentService.list(queryWrapper);
    }
    
    @Override
    public List<Appointment> getDoctorAppointments(Long doctorId, LocalDate date, Integer status) {
        log.info("获取医生预约记录, doctorId: {}, date: {}, status: {}", doctorId, date, status);
        
        if (doctorId == null) {
            throw new BusinessException("医生ID不能为空");
        }
        
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getDoctorId, doctorId);
        
        if (date != null) {
            queryWrapper.eq(Appointment::getAppointmentDate, date);
        }
        
        if (status != null) {
            queryWrapper.eq(Appointment::getStatus, status);
        }
        
        queryWrapper.orderByAsc(Appointment::getAppointmentDate)
                .orderByAsc(Appointment::getTimeSlot);
        
        return appointmentService.list(queryWrapper);
    }
}
