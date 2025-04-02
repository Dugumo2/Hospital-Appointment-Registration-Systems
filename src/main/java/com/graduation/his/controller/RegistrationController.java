package com.graduation.his.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.domain.dto.AiConsultConnectionRequest;
import com.graduation.his.domain.dto.AiConsultRequest;
import com.graduation.his.domain.dto.ConsultSession;
import com.graduation.his.domain.po.Appointment;
import com.graduation.his.domain.po.Department;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.domain.po.Schedule;
import com.graduation.his.domain.vo.AppointmentVO;
import com.graduation.his.domain.vo.DepartmentVO;
import com.graduation.his.domain.vo.DoctorVO;
import com.graduation.his.domain.vo.Result;
import com.graduation.his.domain.vo.ScheduleVO;
import com.graduation.his.service.business.IRegistrationService;
import com.graduation.his.service.entity.IDepartmentService;
import com.graduation.his.service.entity.IDoctorService;
import com.graduation.his.service.entity.IPatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    
    @Autowired
    private IPatientService patientService;
    
    @Autowired
    private IDoctorService doctorService;
    
    @Autowired
    private IDepartmentService departmentService;
    
    /**
     * 获取科室列表
     * 
     * @param onlyActive 是否只返回有效科室 (可选，默认true)
     * @return 科室列表
     */
    @GetMapping("/departments")
    public Result<List<DepartmentVO>> getDepartmentList(
            @RequestParam(required = false, defaultValue = "true") boolean onlyActive) {
        log.info("接收到获取科室列表请求, onlyActive: {}", onlyActive);
        try {
            List<Department> departments = departmentService.getDepartmentList(onlyActive);
            
            // 查询各科室的医生数量
            Map<Long, Long> deptDoctorCount = new HashMap<>();
            List<Doctor> allDoctors = doctorService.list();
            for (Doctor doctor : allDoctors) {
                Long deptId = doctor.getDeptId().longValue();
                deptDoctorCount.put(deptId, deptDoctorCount.getOrDefault(deptId, 0L) + 1);
            }
            
            List<DepartmentVO> departmentVOs = departments.stream().map(dept -> {
                DepartmentVO vo = new DepartmentVO();
                BeanUtils.copyProperties(dept, vo);
                
                // 设置科室类型名称
                vo.setDeptTypeName(getDeptTypeName(dept.getDeptType()));
                
                // 设置医生数量
                vo.setDoctorCount(deptDoctorCount.getOrDefault(dept.getDeptId(), 0L).intValue());
                
                return vo;
            }).collect(Collectors.toList());
            
            return Result.ok(departmentVOs, "获取科室列表成功");
        } catch (Exception e) {
            log.error("获取科室列表异常", e);
            return Result.fail("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取科室详情
     * 
     * @param deptId 科室ID
     * @return 科室详情
     */
    @GetMapping("/departments/{deptId}")
    public Result<DepartmentVO> getDepartmentDetail(@PathVariable Long deptId) {
        log.info("接收到获取科室详情请求, deptId: {}", deptId);
        try {
            Department department = departmentService.getById(deptId);
            if (department == null) {
                return Result.fail("科室不存在");
            }
            
            DepartmentVO vo = new DepartmentVO();
            BeanUtils.copyProperties(department, vo);
            
            // 设置科室类型名称
            vo.setDeptTypeName(getDeptTypeName(department.getDeptType()));
            
            // 计算医生数量
            LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Doctor::getDeptId, deptId);
            long doctorCount = doctorService.count(queryWrapper);
            vo.setDoctorCount((int) doctorCount);
            
            return Result.ok(vo, "获取科室详情成功");
        } catch (Exception e) {
            log.error("获取科室详情异常", e);
            return Result.fail("服务异常，请稍后重试");
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
            return Result.ok(sessionId, "AI问诊请求已发送");
        } catch (IllegalArgumentException e) {
            log.error("AI问诊请求参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("AI问诊请求异常", e);
            return Result.fail("服务异常，请稍后重试");
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
            return Result.ok(result, result ? "会话已结束" : "结束会话失败");
        } catch (IllegalArgumentException e) {
            log.error("结束AI问诊会话参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("结束AI问诊会话异常", e);
            return Result.fail("服务异常，请稍后重试");
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
                return Result.fail("未找到相关会话记录");
            }
            return Result.ok(session, "获取会话记录成功");
        } catch (IllegalArgumentException e) {
            log.error("获取AI问诊历史会话参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取AI问诊历史会话异常", e);
            return Result.fail("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取医生列表
     * 
     * @param deptId 科室ID (可选)
     * @return 医生列表
     */
    @GetMapping("/doctors")
    public Result<List<DoctorVO>> getDoctorList(@RequestParam(required = false) Long deptId) {
        log.info("接收到获取医生列表请求, deptId: {}", deptId);
        try {
            List<Doctor> doctors = registrationService.getDoctorList(deptId);
            List<DoctorVO> doctorVOs = doctors.stream().map(doctor -> {
                DoctorVO vo = new DoctorVO();
                BeanUtils.copyProperties(doctor, vo);
                
                // 获取科室名称
                vo.setDeptName(departmentService.getDepartmentName(doctor.getDeptId().longValue()));
                
                // TODO: 获取评分等信息
                
                return vo;
            }).collect(Collectors.toList());
            return Result.ok(doctorVOs, "获取医生列表成功");
        } catch (Exception e) {
            log.error("获取医生列表异常", e);
            return Result.fail("服务异常，请稍后重试");
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
            Doctor doctor = registrationService.getDoctorDetail(doctorId);
            DoctorVO vo = new DoctorVO();
            BeanUtils.copyProperties(doctor, vo);
            
            // 获取科室名称
            vo.setDeptName(departmentService.getDepartmentName(doctor.getDeptId().longValue()));
            
            // TODO: 获取评分等信息
            
            return Result.ok(vo, "获取医生详情成功");
        } catch (IllegalArgumentException e) {
            log.error("获取医生详情参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取医生详情异常", e);
            return Result.fail("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取医生排班
     * 
     * @param doctorId 医生ID
     * @param startDate 开始日期 (可选，默认今天)
     * @param endDate 结束日期 (可选，默认开始日期后7天)
     * @return 排班列表
     */
    @GetMapping("/doctors/{doctorId}/schedules")
    public Result<List<ScheduleVO>> getDoctorSchedules(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        log.info("接收到获取医生排班请求, doctorId: {}, startDate: {}, endDate: {}", doctorId, startDate, endDate);
        try {
            Map<LocalDate, List<Schedule>> scheduleMap = registrationService.getDoctorSchedule(doctorId, startDate, endDate);
            
            List<ScheduleVO> scheduleVOs = new ArrayList<>();
            Doctor doctor = doctorService.getById(doctorId);
            
            scheduleMap.forEach((date, schedules) -> {
                for (Schedule schedule : schedules) {
                    ScheduleVO vo = new ScheduleVO();
                    BeanUtils.copyProperties(schedule, vo);
                    vo.setDoctorName(doctor != null ? doctor.getName() : "");
                    
                    // TODO: 计算已预约人数和是否可预约等信息
                    
                    scheduleVOs.add(vo);
                }
            });
            
            return Result.ok(scheduleVOs, "获取医生排班成功");
        } catch (IllegalArgumentException e) {
            log.error("获取医生排班参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取医生排班异常", e);
            return Result.fail("服务异常，请稍后重试");
        }
    }
    
    /**
     * 创建预约挂号
     * 
     * @param patientId 患者ID
     * @param scheduleId 排班ID
     * @return 预约记录
     */
    @PostMapping("/create")
    public Result<AppointmentVO> createAppointment(
            @RequestParam Long patientId,
            @RequestParam Long scheduleId) {
        log.info("接收到创建预约挂号请求, patientId: {}, scheduleId: {}", patientId, scheduleId);
        try {
            Appointment appointment = registrationService.createAppointment(patientId, scheduleId);
            
            // 构建返回VO
            AppointmentVO vo = new AppointmentVO();
            BeanUtils.copyProperties(appointment, vo);
            
            // 获取患者信息
            Patient patient = patientService.getById(patientId);
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }
            
            // 获取医生信息
            Doctor doctor = doctorService.getById(appointment.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setDeptId(doctor.getDeptId());
                // 获取科室名称
                vo.setDeptName(departmentService.getDepartmentName(doctor.getDeptId().longValue()));
            }
            
            // 设置状态描述
            vo.setStatusDesc(getStatusDesc(appointment.getStatus()));
            
            // 设置是否可取消
            LocalDate today = LocalDate.now();
            boolean canCancel = appointment.getStatus() == 0 // 未就诊
                    && appointment.getAppointmentDate().isAfter(today); // 非当天
            vo.setCanCancel(canCancel);
            
            return Result.ok(vo, "预约挂号成功");
        } catch (IllegalArgumentException e) {
            log.error("创建预约挂号参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建预约挂号异常", e);
            return Result.fail("服务异常，请稍后重试");
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
            return Result.ok(result, result ? "取消预约成功" : "取消预约失败");
        } catch (IllegalArgumentException e) {
            log.error("取消预约挂号参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("取消预约挂号异常", e);
            return Result.fail("服务异常，请稍后重试");
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
            List<Appointment> appointments = registrationService.getPatientAppointments(patientId, status);
            List<AppointmentVO> appointmentVOs = convertToAppointmentVOs(appointments);
            return Result.ok(appointmentVOs, "获取预约记录成功");
        } catch (IllegalArgumentException e) {
            log.error("获取患者预约记录参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取患者预约记录异常", e);
            return Result.fail("服务异常，请稍后重试");
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
            List<Appointment> appointments = registrationService.getDoctorAppointments(doctorId, date, status);
            List<AppointmentVO> appointmentVOs = convertToAppointmentVOs(appointments);
            return Result.ok(appointmentVOs, "获取预约记录成功");
        } catch (IllegalArgumentException e) {
            log.error("获取医生预约记录参数错误: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取医生预约记录异常", e);
            return Result.fail("服务异常，请稍后重试");
        }
    }
    
    /**
     * 将 Appointment 列表转换为 AppointmentVO 列表
     */
    private List<AppointmentVO> convertToAppointmentVOs(List<Appointment> appointments) {
        List<AppointmentVO> vos = new ArrayList<>();
        
        for (Appointment appointment : appointments) {
            AppointmentVO vo = new AppointmentVO();
            BeanUtils.copyProperties(appointment, vo);
            
            // 获取患者信息
            Patient patient = patientService.getById(appointment.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }
            
            // 获取医生信息
            Doctor doctor = doctorService.getById(appointment.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setDeptId(doctor.getDeptId());
                // 获取科室名称
                vo.setDeptName(departmentService.getDepartmentName(doctor.getDeptId().longValue()));
            }
            
            // 设置状态描述
            vo.setStatusDesc(getStatusDesc(appointment.getStatus()));
            
            // 设置是否可取消
            LocalDate today = LocalDate.now();
            boolean canCancel = appointment.getStatus() == 0 // 未就诊
                    && appointment.getAppointmentDate().isAfter(today); // 非当天
            vo.setCanCancel(canCancel);
            
            vos.add(vo);
        }
        
        return vos;
    }
    
    /**
     * 获取预约状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        
        switch (status) {
            case 0:
                return "待就诊";
            case 1:
                return "已就诊";
            case 2:
                return "已取消";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 获取科室类型名称
     */
    private String getDeptTypeName(Integer deptType) {
        if (deptType == null) {
            return "其他";
        }
        
        switch (deptType) {
            case 0:
                return "内科";
            case 1:
                return "外科";
            case 2:
                return "妇产科";
            case 3:
                return "儿科";
            case 4:
                return "五官科";
            case 5:
                return "其他";
            default:
                return "其他";
        }
    }
}
