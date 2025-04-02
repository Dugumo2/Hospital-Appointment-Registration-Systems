package com.graduation.his.service.business;

import com.graduation.his.domain.dto.AiConsultConnectionRequest;
import com.graduation.his.domain.dto.AiConsultRequest;
import com.graduation.his.domain.dto.ConsultSession;
import com.graduation.his.domain.po.Appointment;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Schedule;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author hua
 * @description 预约挂号与问诊服务接口
 * @create 2025-03-30 17:28
 */
public interface IRegistrationService {
    
    /**
     * 创建AI问诊SSE连接
     * @param request 连接请求(包含会话ID、预约ID和患者ID)
     * @return SSE连接对象
     */
    SseEmitter createAiConsultConnection(AiConsultConnectionRequest request);
    
    /**
     * 发送AI问诊请求
     * @param request 问诊请求
     * @return 会话ID
     */
    String sendAiConsultRequest(AiConsultRequest request);
    
    /**
     * 结束AI问诊会话（将会话保存到数据库并标记为已结束）
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean endAiConsultSession(String sessionId);
    
    /**
     * 获取AI问诊历史会话（优先从Redis获取，Redis不存在则从数据库获取）
     * @param sessionId 会话ID
     * @return 会话详情
     */
    ConsultSession getAiConsultHistory(String sessionId);
    
    /**
     * 获取医生列表，可按科室ID筛选
     * @param deptId 科室ID，可为null表示获取所有医生
     * @return 医生列表
     */
    List<Doctor> getDoctorList(Long deptId);
    
    /**
     * 获取医生详情
     * @param doctorId 医生ID
     * @return 医生详情
     */
    Doctor getDoctorDetail(Long doctorId);
    
    /**
     * 获取指定日期范围内医生的排班信息
     * @param doctorId 医生ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日期到排班列表的映射
     */
    Map<LocalDate, List<Schedule>> getDoctorSchedule(Long doctorId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 创建预约挂号
     * @param patientId 患者ID
     * @param scheduleId 排班ID
     * @return 预约记录
     */
    Appointment createAppointment(Long patientId, Long scheduleId);
    
    /**
     * 取消预约挂号
     * @param appointmentId 预约ID
     * @param patientId 患者ID，用于验证操作权限
     * @return 是否取消成功
     */
    boolean cancelAppointment(Long appointmentId, Long patientId);
    
    /**
     * 获取患者的预约挂号记录
     * @param patientId 患者ID
     * @param status 预约状态，可为null表示获取所有状态
     * @return 预约记录列表
     */
    List<Appointment> getPatientAppointments(Long patientId, Integer status);
    
    /**
     * 获取医生的预约挂号记录
     * @param doctorId 医生ID
     * @param date 指定日期，可为null表示获取所有日期
     * @param status 预约状态，可为null表示获取所有状态
     * @return 预约记录列表
     */
    List<Appointment> getDoctorAppointments(Long doctorId, LocalDate date, Integer status);
}
