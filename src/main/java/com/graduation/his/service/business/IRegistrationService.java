package com.graduation.his.service.business;

import com.graduation.his.domain.dto.AiConsultConnectionRequest;
import com.graduation.his.domain.dto.AiConsultRequest;
import com.graduation.his.domain.dto.ConsultSession;
import com.graduation.his.domain.dto.MessageRecord;
import com.graduation.his.domain.po.Appointment;
import com.graduation.his.domain.po.Clinic;
import com.graduation.his.domain.po.Department;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.domain.po.Schedule;
import com.graduation.his.domain.query.ScheduleQuery;
import com.graduation.his.domain.vo.AppointmentVO;
import com.graduation.his.domain.vo.DoctorVO;
import com.graduation.his.domain.vo.ScheduleDetailVO;
import com.graduation.his.domain.vo.ScheduleListVO;
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
     * 获取AI问诊历史会话（从Redis获取）
     * @param sessionId 会话ID
     * @return 会话详情
     */
    ConsultSession getAiConsultHistory(String sessionId);
    
    /**
     * 获取排班列表视图对象
     * @param query 排班查询条件，包含deptId、clinicId、doctorId、title、startDate和endDate等参数
     * @return 排班列表VO
     */
    List<ScheduleListVO> getScheduleListVO(ScheduleQuery query);
    
    /**
     * 获取排班详情
     * @param scheduleId 排班ID
     * @param patientId 患者ID (可选，传入则会填充患者信息)
     * @return 排班详情VO
     */
    ScheduleDetailVO getScheduleDetail(Long scheduleId, Long patientId);
    
    /**
     * 获取排班列表
     * @param deptId 科室ID (可选)
     * @param clinicId 门诊ID (可选)
     * @param doctorId 医生ID (可选)
     * @param startDate 开始日期 (可选，默认今天)
     * @param endDate 结束日期 (可选，默认开始日期后7天)
     * @return 排班列表
     */
    List<Schedule> getScheduleList(Long deptId, Long clinicId, Long doctorId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 创建预约挂号
     * @param patientId 患者ID
     * @param scheduleId 排班ID
     * @param isRevisit 是否为复诊(0-初诊,1-复诊)
     * @return 预约记录
     */
    Appointment createAppointment(Long patientId, Long scheduleId, Integer isRevisit);
    
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
    
    /**
     * 获取科室列表
     * @param onlyActive 是否只返回有效科室
     * @return 科室列表
     */
    List<Department> getDepartmentList(boolean onlyActive);
    
    /**
     * 获取门诊列表
     * @param deptId 科室ID (可选)
     * @param onlyActive 是否只返回有效门诊
     * @return 门诊列表
     */
    List<Clinic> getClinicList(Long deptId, boolean onlyActive);
    
    /**
     * 通过名称查询门诊列表
     * @param name 门诊名称 (模糊匹配)
     * @param onlyActive 是否只返回有效门诊
     * @return 门诊列表
     */
    List<Clinic> getClinicsByName(String name, boolean onlyActive);
    
    /**
     * 根据用户ID获取患者信息
     * @param userId 用户ID
     * @return 患者信息
     */
    Patient getPatientByUserId(Long userId);
    
    /**
     * 获取医生列表VO
     * @param deptId 科室ID (可选)
     * @param name 医生姓名 (可选，模糊匹配)
     * @param clinicId 门诊ID (可选)
     * @return 医生VO列表
     */
    List<DoctorVO> getDoctorListVO(Long deptId, String name, Long clinicId);
    
    /**
     * 获取医生详情VO
     * @param doctorId 医生ID
     * @return 医生VO
     */
    DoctorVO getDoctorDetailVO(Long doctorId);
    
    /**
     * 创建预约挂号并返回VO
     * @param patientId 患者ID
     * @param scheduleId 排班ID
     * @param isRevisit 是否为复诊(0-初诊,1-复诊)
     * @return 预约记录VO
     */
    AppointmentVO createAppointmentVO(Long patientId, Long scheduleId, Integer isRevisit);
    
    /**
     * 获取患者的预约挂号记录VO
     * @param patientId 患者ID
     * @param status 预约状态，可为null表示获取所有状态
     * @return 预约记录VO列表
     */
    List<AppointmentVO> getPatientAppointmentVOs(Long patientId, Integer status);
    
    /**
     * 获取医生的预约挂号记录VO
     * @param doctorId 医生ID
     * @param date 指定日期，可为null表示获取所有日期
     * @param status 预约状态，可为null表示获取所有状态
     * @return 预约记录VO列表
     */
    List<AppointmentVO> getDoctorAppointmentVOs(Long doctorId, LocalDate date, Integer status);
    
    /**
     * 根据挂号记录ID查询该挂号绑定的AI问诊记录
     * @param appointmentId 挂号记录ID
     * @param doctorId 医生ID，用于验证操作权限
     * @return AI问诊会话信息，如果没有AI问诊则返回null
     */
    ConsultSession getAiConsultByAppointmentId(Long appointmentId, Long doctorId);
    
    /**
     * 获取挂号记录详情
     * @param appointmentId 挂号记录ID
     * @param doctorId 医生ID，用于验证操作权限
     * @return 挂号记录详情
     */
    AppointmentVO getAppointmentDetail(Long appointmentId, Long doctorId);
    
    /**
     * 检查指定预约是否已存在AI问诊记录
     * 
     * @param appointmentId 预约ID
     * @return 是否存在
     */
    boolean isAiConsultExistsByAppointmentId(Long appointmentId);
    
    /**
     * 获取预约相关的消息记录
     * @param appointmentId 预约ID
     * @param userId 当前登录用户ID，用于权限验证
     * @return 消息记录列表
     */
    List<MessageRecord> getAppointmentMessageHistory(Long appointmentId, Long userId);
}
