package com.graduation.his.controller;

import com.graduation.his.common.Result;
import com.graduation.his.domain.dto.AiConsultConnectionRequest;
import com.graduation.his.domain.dto.AiConsultRequest;
import com.graduation.his.domain.dto.ConsultSession;
import com.graduation.his.domain.po.*;
import com.graduation.his.domain.query.ScheduleQuery;
import com.graduation.his.domain.vo.AppointmentVO;
import com.graduation.his.domain.vo.DoctorVO;
import com.graduation.his.domain.vo.ScheduleDetailVO;
import com.graduation.his.domain.vo.ScheduleListVO;
import com.graduation.his.service.business.IRegistrationService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 预约挂号表 前端控制器
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Slf4j
@RestController
@RequestMapping("/appointment")
public class RegistrationController {
    
    @Autowired
    private IRegistrationService registrationService;
    
    /**
     * 获取科室列表
     * 
     * @param onlyActive 是否只返回有效科室 (可选，默认true)
     * @return 科室列表
     */
    @GetMapping("/departments")
    public Result<List<Department>> getDepartmentList(
            @RequestParam(required = false, defaultValue = "true") boolean onlyActive) {
        log.info("接收到获取科室列表请求, onlyActive: {}", onlyActive);
        try {
            List<Department> departments = registrationService.getDepartmentList(onlyActive);
            return Result.success("获取科室列表成功", departments);
        } catch (Exception e) {
            log.error("获取科室列表异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取门诊列表
     * 
     * @param deptId 科室ID (可选)
     * @param onlyActive 是否只返回有效门诊 (可选，默认true)
     * @return 门诊列表
     */
    @GetMapping("/clinics")
    public Result<List<Clinic>> getClinicList(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false, defaultValue = "true") boolean onlyActive) {
        log.info("接收到获取门诊列表请求, deptId: {}, onlyActive: {}", deptId, onlyActive);
        try {
            List<Clinic> clinics = registrationService.getClinicList(deptId, onlyActive);
            return Result.success("获取门诊列表成功", clinics);
        } catch (Exception e) {
            log.error("获取门诊列表异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 通过名称搜索门诊列表
     * 
     * @param name 门诊名称 (模糊匹配)
     * @param onlyActive 是否只返回有效门诊 (可选，默认true)
     * @return 门诊列表
     */
    @GetMapping("/clinics/search")
    public Result<List<Clinic>> searchClinicByName(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "true") boolean onlyActive) {
        log.info("接收到通过名称搜索门诊列表请求, name: {}, onlyActive: {}", name, onlyActive);
        try {
            List<Clinic> clinics = registrationService.getClinicsByName(name, onlyActive);
            return Result.success("搜索门诊列表成功", clinics);
        } catch (IllegalArgumentException e) {
            log.error("搜索门诊列表参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("搜索门诊列表异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 创建AI问诊SSE连接
     * 
     * 创建Server-Sent Events连接，用于实时接收AI问诊响应
     * 会话状态会存储在Redis中，有效期为6小时
     * 
     * @param request 连接请求(包含会话ID、预约ID和患者ID)
     * @return SSE连接
     */
    @PostMapping(value = "/ai-consult/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createAiConsultConnection(@RequestBody AiConsultConnectionRequest request) {
        log.info("接收到创建AI问诊SSE连接请求, appointmentId: {}, patientId: {}, sessionId: {}", 
                request.getAppointmentId(), request.getPatientId(), request.getSessionId());
        
        try {
            return registrationService.createAiConsultConnection(request);
        } catch (IllegalArgumentException e) {
            log.error("创建AI问诊SSE连接参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建AI问诊SSE连接异常", e);
            throw new RuntimeException("服务异常，请稍后重试");
        }
    }
    
    /**
     * 发送AI问诊请求
     * 
     * 向AI发送问诊请求，会话进行中时会话状态存储在Redis中
     * 每条用户消息和AI回复都会实时记录到Redis，有效期为6小时
     * 
     * @param request 问诊请求
     * @return 结果(包含会话ID)
     */
    @PostMapping("/ai-consult/send")
    public Result<String> sendAiConsultRequest(@RequestBody AiConsultRequest request) {
        log.info("接收到AI问诊请求, patientId: {}, sessionId: {}", request.getPatientId(), request.getSessionId());
        try {
            String sessionId = registrationService.sendAiConsultRequest(request);
            return Result.success("AI问诊请求已发送", sessionId);
        } catch (IllegalArgumentException e) {
            log.error("AI问诊请求参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("AI问诊请求异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 结束AI问诊会话
     * 
     * 结束会话并将完整对话历史保存到数据库中
     * 此操作会将会话状态标记为已结束，并永久保留对话历史
     * 
     * @param sessionId 会话ID
     * @return 结果
     */
    @PostMapping("/ai-consult/end")
    public Result<Boolean> endAiConsultSession(
            @RequestParam String sessionId) {
        log.info("接收到结束AI问诊会话请求, sessionId: {}", sessionId);
        try {
            boolean result = registrationService.endAiConsultSession(sessionId);
            return Result.success(result ? "会话已结束" : "结束会话失败", result);
        } catch (IllegalArgumentException e) {
            log.error("结束AI问诊会话参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("结束AI问诊会话异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取AI问诊历史会话
     * 
     * 获取历史会话详情，包括所有对话内容
     * 优先从Redis获取活跃会话，若Redis中不存在则从数据库获取已结束会话
     * 
     * @param sessionId 会话ID
     * @return 会话详情
     */
    @GetMapping("/ai-consult/history")
    public Result<ConsultSession> getAiConsultHistory(
            @RequestParam String sessionId) {
        log.info("接收到获取AI问诊历史会话请求, sessionId: {}", sessionId);
        try {
            ConsultSession session = registrationService.getAiConsultHistory(sessionId);
            if (session == null) {
                return Result.error("未找到相关会话记录");
            }
            return Result.success("获取会话记录成功", session);
        } catch (IllegalArgumentException e) {
            log.error("获取AI问诊历史会话参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取AI问诊历史会话异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取医生列表
     * 
     * @param deptId 科室ID (可选)
     * @param name 医生姓名 (可选，模糊匹配)
     * @param clinicId 门诊ID (可选)
     * @return 医生列表
     */
    @GetMapping("/doctors")
    public Result<List<DoctorVO>> getDoctorList(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long clinicId) {
        log.info("接收到获取医生列表请求, deptId: {}, name: {}, clinicId: {}", deptId, name, clinicId);
        try {
            List<DoctorVO> doctorVOs = registrationService.getDoctorListVO(deptId, name, clinicId);
            return Result.success("获取医生列表成功", doctorVOs);
        } catch (Exception e) {
            log.error("获取医生列表异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取医生详情
     * 
     * @param doctorId 医生ID
     * @return 医生详情
     */
    @GetMapping("/doctors/{doctorId}")
    public Result<DoctorVO> getDoctorDetail(@PathVariable Long doctorId) {
        log.info("接收到获取医生详情请求, doctorId: {}", doctorId);
        try {
            DoctorVO vo = registrationService.getDoctorDetailVO(doctorId);
            return Result.success("获取医生详情成功", vo);
        } catch (IllegalArgumentException e) {
            log.error("获取医生详情参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取医生详情异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取可用排班列表 - 简化版本，用于列表展示
     * 
     * @param query 查询参数，包含以下可选条件：
     *              - deptId（科室ID，可选）
     *              - clinicId（门诊ID，可选）  
     *              - doctorId（医生ID，可选）
     *              - title（医生职称，可选，用于筛选特定职称的医生）
     *              - startDate（开始日期，可选，默认为当天）
     *              - endDate（结束日期，可选，默认为开始日期后7天）
     * @return 排班列表，包含医生基本信息和可预约状态
     */
    @PostMapping("/schedules")
    public Result<List<ScheduleListVO>> getScheduleList(ScheduleQuery query) {
        log.info("接收到获取排班列表请求, 查询条件: {}", query);
        try {
            List<ScheduleListVO> scheduleVOs = registrationService.getScheduleListVO(query);
            return Result.success("获取排班列表成功", scheduleVOs);
        } catch (Exception e) {
            log.error("获取排班列表异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取排班详情 - 详细版本，用于预约页面
     * 
     * @param scheduleId 排班ID
     * @return 排班详情，包含医生、科室、门诊和预约相关的完整信息
     */
    @GetMapping("/schedules/{scheduleId}")
    public Result<ScheduleDetailVO> getScheduleDetail(@PathVariable Long scheduleId) {
        log.info("接收到获取排班详情请求, scheduleId: {}", scheduleId);
        try {
            // 获取当前登录用户ID，如果有的话
            Long patientId = null;
            // 使用Sa-Token获取用户ID
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                // 根据用户ID查询患者信息
                Patient patient = registrationService.getPatientByUserId(userId);
                if (patient != null) {
                    patientId = patient.getPatientId();
                }
            }
            
            ScheduleDetailVO detailVO = registrationService.getScheduleDetail(scheduleId, patientId);
            return Result.success("获取排班详情成功", detailVO);
        } catch (IllegalArgumentException e) {
            log.error("获取排班详情参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取排班详情异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取医生排班 - 简化版本，用于列表展示
     * 
     * @param query 查询参数，包含以下可选条件：
     *              - doctorId（医生ID，必填）
     *              - title（医生职称，可选，用于筛选特定职称的医生）
     *              - startDate（开始日期，可选，默认为当天）
     *              - endDate（结束日期，可选，默认为开始日期后7天）
     * @return 排班列表，包含医生基本信息和可预约状态
     */
    @PostMapping("/doctor-schedules")
    public Result<List<ScheduleListVO>> getDoctorSchedules(ScheduleQuery query) {
        if (query == null || query.getDoctorId() == null) {
            return Result.error("医生ID不能为空");
        }
        
        log.info("接收到获取医生排班请求, 查询条件: {}", query);
        try {
            // 设置默认值：开始日期默认今天，结束日期默认为开始日期后7天
            if (query.getStartDate() == null) {
                query.setStartDate(LocalDate.now());
            }
            
            if (query.getEndDate() == null) {
                query.setEndDate(query.getStartDate().plusDays(6)); // 一周
            }
            
            List<ScheduleListVO> scheduleVOs = registrationService.getScheduleListVO(query);
            return Result.success("获取医生排班成功", scheduleVOs);
        } catch (IllegalArgumentException e) {
            log.error("获取医生排班参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取医生排班异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 创建预约挂号
     * 
     * @param patientId 患者ID
     * @param scheduleId 排班ID
     * @param isRevisit 是否为复诊(0-初诊,1-复诊)
     * @return 预约记录
     */
    @PostMapping("/create")
    public Result<AppointmentVO> createAppointment(
            @RequestParam Long patientId,
            @RequestParam Long scheduleId,
            @RequestParam(required = false, defaultValue = "0") Integer isRevisit) {
        log.info("接收到创建预约挂号请求, patientId: {}, scheduleId: {}, isRevisit: {}", patientId, scheduleId, isRevisit);
        try {
            AppointmentVO vo = registrationService.createAppointmentVO(patientId, scheduleId, isRevisit);
            return Result.success("预约挂号成功", vo);
        } catch (IllegalArgumentException e) {
            log.error("创建预约挂号参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建预约挂号异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 取消预约挂号
     * 
     * @param appointmentId 预约ID
     * @param patientId 患者ID
     * @return 取消结果
     */
    @PostMapping("/cancel")
    public Result<Boolean> cancelAppointment(
            @RequestParam Long appointmentId,
            @RequestParam Long patientId) {
        log.info("接收到取消预约挂号请求, appointmentId: {}, patientId: {}", appointmentId, patientId);
        try {
            boolean result = registrationService.cancelAppointment(appointmentId, patientId);
            return Result.success(result ? "取消预约成功" : "取消预约失败", result);
        } catch (IllegalArgumentException e) {
            log.error("取消预约挂号参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("取消预约挂号异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取患者的预约记录
     * 
     * @param patientId 患者ID
     * @param status 预约状态 (可选)
     * @return 预约记录列表
     */
    @GetMapping("/patient/{patientId}")
    public Result<List<AppointmentVO>> getPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(required = false) Integer status) {
        log.info("接收到获取患者预约记录请求, patientId: {}, status: {}", patientId, status);
        try {
            List<AppointmentVO> appointmentVOs = registrationService.getPatientAppointmentVOs(patientId, status);
            return Result.success("获取预约记录成功", appointmentVOs);
        } catch (IllegalArgumentException e) {
            log.error("获取患者预约记录参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取患者预约记录异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取医生的预约记录
     * 
     * @param doctorId 医生ID
     * @param date 指定日期 (可选)
     * @param status 预约状态 (可选)
     * @return 预约记录列表
     */
    @GetMapping("/doctor/{doctorId}")
    public Result<List<AppointmentVO>> getDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) Integer status) {
        log.info("接收到获取医生预约记录请求, doctorId: {}, date: {}, status: {}", doctorId, date, status);
        try {
            List<AppointmentVO> appointmentVOs = registrationService.getDoctorAppointmentVOs(doctorId, date, status);
            return Result.success("获取预约记录成功", appointmentVOs);
        } catch (IllegalArgumentException e) {
            log.error("获取医生预约记录参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取医生预约记录异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
}
