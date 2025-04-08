package com.graduation.his.service.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.graduation.his.common.exception.BusinessException;
import com.graduation.his.domain.dto.AiConsultConnectionRequest;
import com.graduation.his.domain.dto.AiConsultRequest;
import com.graduation.his.domain.dto.ConsultSession;
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
import com.graduation.his.service.business.IRegistrationService;
import com.graduation.his.service.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    
    @Autowired
    private IClinicService clinicService;
    
    @Autowired
    private IDepartmentService departmentService;
    
    @Autowired
    private IPatientService patientService;
    
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
    public List<Schedule> getScheduleList(Long deptId, Long clinicId, Long doctorId, LocalDate startDate, LocalDate endDate) {
        log.debug("获取排班列表, deptId: {}, clinicId: {}, doctorId: {}, startDate: {}, endDate: {}", 
                deptId, clinicId, doctorId, startDate, endDate);
        
        // 设置默认日期范围：如果未指定起始日期，则使用今天；如果未指定结束日期，则使用起始日期后7天
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate effectiveEndDate = endDate != null ? endDate : effectiveStartDate.plusDays(7);
        
        // 创建查询条件
        QueryWrapper<Schedule> queryWrapper = new QueryWrapper<>();
        
        // 只查询有效的排班
        queryWrapper.eq("status", 1);
        
        // 日期范围条件
        queryWrapper.ge("schedule_date", effectiveStartDate);
        queryWrapper.le("schedule_date", effectiveEndDate);
        
        // 如果指定了医生ID，直接按医生ID筛选
        if (doctorId != null) {
            queryWrapper.eq("doctor_id", doctorId);
        } 
        // 如果指定了门诊ID，查询该门诊下的所有医生，然后按医生ID筛选
        else if (clinicId != null) {
            List<Doctor> doctors = doctorService.getDoctorsByClinicId(clinicId);
            if (doctors.isEmpty()) {
                return new ArrayList<>();
            }
            List<Long> doctorIds = doctors.stream().map(Doctor::getDoctorId).collect(Collectors.toList());
            queryWrapper.in("doctor_id", doctorIds);
        } 
        // 如果指定了科室ID，查询该科室下所有门诊的所有医生，然后按医生ID筛选
        else if (deptId != null) {
            List<Clinic> clinics = clinicService.getClinicsByDeptId(deptId, true);
            if (clinics.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Long> doctorIds = new ArrayList<>();
            for (Clinic clinic : clinics) {
                List<Doctor> doctors = doctorService.getDoctorsByClinicId(clinic.getClinicId());
                doctorIds.addAll(doctors.stream().map(Doctor::getDoctorId).collect(Collectors.toList()));
            }
            
            if (doctorIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            queryWrapper.in("doctor_id", doctorIds);
        }
        
        // 按日期升序、时段升序排序
        queryWrapper.orderByAsc("schedule_date", "time_slot");
        
        return scheduleService.list(queryWrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appointment createAppointment(Long patientId, Long scheduleId, Integer isRevisit) {
        log.info("创建预约挂号, patientId: {}, scheduleId: {}, isRevisit: {}", patientId, scheduleId, isRevisit);
        
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
        
        // 检查排班日期是否在有效范围内（今天未结束的时间段到未来7天）
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(6);
        if (schedule.getScheduleDate().isBefore(today) || schedule.getScheduleDate().isAfter(maxDate)) {
            throw new BusinessException("该时段已不可预约");
        }
        
        // 如果是当天的排班，检查时间段是否已经结束
        if (schedule.getScheduleDate().equals(today)) {
            String timeSlot = schedule.getTimeSlot();
            if (timeSlot != null && timeSlot.contains("-")) {
                String endTimeStr = timeSlot.split("-")[1];
                try {
                    LocalTime endTime = LocalTime.parse(endTimeStr);
                    if (LocalTime.now().isAfter(endTime)) {
                        throw new BusinessException("该时段已结束，无法预约");
                    }
                } catch (Exception e) {
                    log.warn("解析时间段出错: {}", timeSlot);
                    throw new BusinessException("时间段格式错误");
                }
            }
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
        if (schedule.getCurrentPatients() >= schedule.getMaxPatients()) {
            throw new BusinessException("该时段预约已满");
        }
        
        // 创建预约记录
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setScheduleId(scheduleId);
        appointment.setAppointmentDate(schedule.getScheduleDate());
        appointment.setTimeSlot(schedule.getTimeSlot());
        appointment.setIsRevisit(isRevisit != null ? isRevisit : 0); // 默认为初诊
        appointment.setStatus(0); // 待就诊
        appointment.setCreateTime(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());
        
        // 保存预约记录
        appointmentService.save(appointment);
        
        // 更新排班的已预约人数
        scheduleService.incrementCurrentPatients(scheduleId);
        
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
        
        if (result) {
            // 减少排班的已预约人数
            scheduleService.decrementCurrentPatients(appointment.getScheduleId());
        }
        
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

    @Override
    public List<Department> getDepartmentList(boolean onlyActive) {
        return departmentService.getDepartmentList(onlyActive);
    }

    @Override
    public List<Clinic> getClinicList(Long deptId, boolean onlyActive) {
        if (deptId == null) {
            throw new BusinessException("科室ID不能为空");
        }
        return clinicService.getClinicsByDeptId(deptId, onlyActive);
    }

    @Override
    public Patient getPatientByUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return patientService.getByUserId(userId);
    }

    @Override
    public List<DoctorVO> getDoctorListVO(Long deptId, String name, Long clinicId) {
        List<Doctor> doctors;
        
        // 按照指定条件查询医生
        if (clinicId != null) {
            // 根据门诊ID查询医生
            doctors = doctorService.getDoctorsByClinicId(clinicId);
        } else if (name != null && !name.trim().isEmpty()) {
            // 根据姓名查询医生
            doctors = doctorService.getDoctorsByName(name);
        } else if (deptId != null) {
            // 根据科室ID查询门诊，再查询医生
            List<Clinic> clinics = clinicService.getClinicsByDeptId(deptId, true);
            if (clinics == null || clinics.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 汇总所有门诊下的医生
            doctors = new ArrayList<>();
            for (Clinic clinic : clinics) {
                List<Doctor> doctorsInClinic = doctorService.getDoctorsByClinicId(clinic.getClinicId());
                if (doctorsInClinic != null && !doctorsInClinic.isEmpty()) {
                    doctors.addAll(doctorsInClinic);
                }
            }
        } else {
            // 没有提供查询条件，返回空列表
            return new ArrayList<>();
        }
        
        if (doctors == null || doctors.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 转换为VO对象
        return convertToDoctorVOs(doctors);
    }

    @Override
    public DoctorVO getDoctorDetailVO(Long doctorId) {
        if (doctorId == null) {
            throw new BusinessException("医生ID不能为空");
        }
        
        Doctor doctor = doctorService.getById(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        
        // 转换为VO对象
        return convertToDoctorVO(doctor);
    }
    
    @Override
    public ScheduleDetailVO getScheduleDetail(Long scheduleId, Long patientId) {
        log.info("获取排班详情, scheduleId: {}, patientId: {}", scheduleId, patientId);
        
        if (scheduleId == null) {
            throw new BusinessException("排班ID不能为空");
        }
        
        // 查询排班信息
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        
        // 创建详情VO
        ScheduleDetailVO detailVO = new ScheduleDetailVO();
        BeanUtils.copyProperties(schedule, detailVO);
        
        // 设置剩余可预约数量
        detailVO.setRemainingQuota(schedule.getMaxPatients() - schedule.getCurrentPatients());
        
        // 设置是否可预约
        boolean canBook = schedule.getStatus() == 1 && 
                schedule.getCurrentPatients() < schedule.getMaxPatients() &&
                schedule.getScheduleDate().isAfter(LocalDate.now().minusDays(1));
        detailVO.setCanBook(canBook);
        
        // 查询医生信息
        if (schedule.getDoctorId() != null) {
            Doctor doctor = doctorService.getById(schedule.getDoctorId());
            if (doctor != null) {
                detailVO.setDoctorId(doctor.getDoctorId());
                detailVO.setDoctorName(doctor.getName());
                detailVO.setDoctorTitle(doctor.getTitle());
                detailVO.setDoctorAvatar(null); // 如果有头像字段，这里可以设置
                
                // 查询门诊和科室信息
                if (doctor.getClinicId() != null) {
                    Clinic clinic = clinicService.getById(doctor.getClinicId());
                    if (clinic != null) {
                        detailVO.setClinicId(clinic.getClinicId());
                        
                        // 获取门诊名称
                        try {
                            String clinicName = clinic.getClinicName();
                            detailVO.setClinicName(clinicName);
                        } catch (Exception e) {
                            log.warn("获取门诊名称失败", e);
                        }
                        
                        // 设置科室信息
                        if (clinic.getDeptId() != null) {
                            Department dept = departmentService.getById(clinic.getDeptId());
                            if (dept != null) {
                                detailVO.setDeptId(dept.getDeptId());
                                
                                // 获取科室名称
                                try {
                                    String deptName = dept.getDeptName();
                                    detailVO.setDeptName(deptName);
                                } catch (Exception e) {
                                    log.warn("获取科室名称失败", e);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 如果提供了患者ID，设置患者信息
        if (patientId != null) {
            Patient patient = patientService.getById(patientId);
            if (patient != null) {
                detailVO.setPatientId(patient.getPatientId());
                detailVO.setPatientName(patient.getName());
            }
        }
        
        return detailVO;
    }
    
    @Override
    public List<ScheduleListVO> getScheduleListVO(ScheduleQuery query) {
        if (query == null) {
            throw new BusinessException("查询参数不能为空");
        }
        
        if (query.getStartDate() == null) {
            query.setStartDate(LocalDate.now());
        }
        
        if (query.getEndDate() == null) {
            query.setEndDate(query.getStartDate().plusDays(6)); // 默认查询一周的数据
        }
        
        // 查询排班记录
        List<Schedule> schedules;
        
        if (query.getDoctorId() != null) {
            // 如果指定了医生ID，直接查询该医生的排班
            schedules = scheduleService.getSchedulesByDoctorAndDateRange(
                    query.getDoctorId(), query.getStartDate(), query.getEndDate());
        } else {
            // 否则查询科室或门诊的排班
            schedules = getScheduleList(
                    query.getDeptId(), query.getClinicId(), null, 
                    query.getStartDate(), query.getEndDate());
        }
        
        if (schedules == null || schedules.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 转换为VO
        List<ScheduleListVO> result = new ArrayList<>();
        for (Schedule schedule : schedules) {
            ScheduleListVO vo = new ScheduleListVO();
            BeanUtils.copyProperties(schedule, vo);
            
            // 设置剩余可预约数量
            vo.setRemainingQuota(schedule.getMaxPatients() - schedule.getCurrentPatients());
            
            // 设置是否可预约
            boolean canBook = schedule.getStatus() == 1 && 
                    schedule.getCurrentPatients() < schedule.getMaxPatients() &&
                    schedule.getScheduleDate().isAfter(LocalDate.now().minusDays(1));
            vo.setCanBook(canBook);
            
            // 设置医生信息
            if (schedule.getDoctorId() != null) {
                Doctor doctor = doctorService.getById(schedule.getDoctorId());
                if (doctor != null) {
                    vo.setDoctorName(doctor.getName());
                    vo.setDoctorTitle(doctor.getTitle());
                    vo.setDoctorIntroduction(doctor.getIntroduction());
                    // 假设医生信息中有头像字段，这里设置头像
                    vo.setDoctorAvatar(null);
                }
            }
            
            result.add(vo);
        }
        
        return result;
    }
    
    @Override
    public AppointmentVO createAppointmentVO(Long patientId, Long scheduleId, Integer isRevisit) {
        if (patientId == null) {
            throw new BusinessException("患者ID不能为空");
        }
        if (scheduleId == null) {
            throw new BusinessException("排班ID不能为空");
        }
        
        // 先创建预约记录
        Appointment appointment = createAppointment(patientId, scheduleId, isRevisit);
        
        // 获取患者信息
        Patient patient = patientService.getById(patientId);
        
        // 查询医生信息
        Doctor doctor = doctorService.getById(appointment.getDoctorId());
        
        // 转换为VO并返回
        AppointmentVO vo = new AppointmentVO();
        BeanUtils.copyProperties(appointment, vo);
        
        if (patient != null) {
            vo.setPatientName(patient.getName());
        }
        
        if (doctor != null) {
            vo.setDoctorName(doctor.getName());
            
            // 获取医生所属的门诊和科室信息
            if (doctor.getClinicId() != null) {
                Clinic clinic = clinicService.getById(doctor.getClinicId());
                if (clinic != null) {
                    // 设置门诊名称
                    vo.setClinicName(clinic.getClinicName());
                    
                    // 设置科室名称
                    if (clinic.getDeptId() != null) {
                        Department dept = departmentService.getById(clinic.getDeptId());
                        if (dept != null) {
                            vo.setDeptName(dept.getDeptName());
                        }
                    }
                }
            }
        }
        
        // 设置状态描述和是否可取消
        vo.setStatusDesc(getStatusDesc(vo.getStatus()));
        boolean canCancel = vo.getStatus() == 0 && 
                            appointment.getAppointmentDate() != null && 
                            appointment.getAppointmentDate().isAfter(LocalDate.now());
        vo.setCanCancel(canCancel);
        
        return vo;
    }
    
    @Override
    public List<AppointmentVO> getPatientAppointmentVOs(Long patientId, Integer status) {
        // 先获取预约记录
        List<Appointment> appointments = getPatientAppointments(patientId, status);
        
        // 转换为VO
        return convertToAppointmentVOs(appointments);
    }
    
    @Override
    public List<AppointmentVO> getDoctorAppointmentVOs(Long doctorId, LocalDate date, Integer status) {
        // 先获取预约记录
        List<Appointment> appointments = getDoctorAppointments(doctorId, date, status);
        
        // 转换为VO
        return convertToAppointmentVOs(appointments);
    }

    /**
     * 将Doctor对象转换为DoctorVO对象
     */
    private List<DoctorVO> convertToDoctorVOs(List<Doctor> doctors) {
        if (CollectionUtils.isEmpty(doctors)) {
            return new ArrayList<>();
        }
        
        return doctors.stream().map(this::convertToDoctorVO).collect(Collectors.toList());
    }
    
    /**
     * 将单个Doctor对象转换为DoctorVO对象
     */
    private DoctorVO convertToDoctorVO(Doctor doctor) {
        DoctorVO vo = new DoctorVO();
        BeanUtils.copyProperties(doctor, vo);
        
        // 设置科室名称
        if (doctor.getClinicId() != null) {
            Clinic clinic = clinicService.getById(doctor.getClinicId());
            if (clinic != null && clinic.getDeptId() != null) {
                String deptName = departmentService.getDepartmentName(clinic.getDeptId());
                vo.setDeptName(deptName);
            }
        }
        
        return vo;
    }
    
    /**
     * 将Appointment对象转换为AppointmentVO对象
     */
    private List<AppointmentVO> convertToAppointmentVOs(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取相关患者信息
        Set<Long> patientIds = appointments.stream()
            .map(Appointment::getPatientId)
            .collect(Collectors.toSet());
        Map<Long, Patient> patientMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            List<Patient> patients = patientService.listByIds(patientIds);
            if (patients != null && !patients.isEmpty()) {
                patientMap = patients.stream()
                    .collect(Collectors.toMap(Patient::getPatientId, p -> p));
            }
        }
        
        // 获取相关医生信息
        Set<Long> doctorIds = appointments.stream()
            .map(Appointment::getDoctorId)
            .collect(Collectors.toSet());
        Map<Long, Doctor> doctorMap = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            List<Doctor> doctors = doctorService.listByIds(doctorIds);
            if (doctors != null && !doctors.isEmpty()) {
                doctorMap = doctors.stream()
                    .collect(Collectors.toMap(Doctor::getDoctorId, d -> d));
            }
        }
        
        // 获取相关排班信息
        Set<Long> scheduleIds = appointments.stream()
            .map(Appointment::getScheduleId)
            .collect(Collectors.toSet());
        Map<Long, Schedule> scheduleMap = new HashMap<>();
        if (!scheduleIds.isEmpty()) {
            List<Schedule> schedules = scheduleService.listByIds(scheduleIds);
            if (schedules != null && !schedules.isEmpty()) {
                scheduleMap = schedules.stream()
                    .collect(Collectors.toMap(Schedule::getScheduleId, s -> s));
            }
        }
        
        // 转换为VO
        List<AppointmentVO> result = new ArrayList<>();
        for (Appointment appointment : appointments) {
            AppointmentVO vo = new AppointmentVO();
            BeanUtils.copyProperties(appointment, vo);
            
            // 设置患者名称
            Patient patient = patientMap.get(appointment.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }
            
            // 设置医生名称
            Doctor doctor = doctorMap.get(appointment.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                
                // 设置门诊和科室信息
                if (doctor.getClinicId() != null) {
                    Clinic clinic = clinicService.getById(doctor.getClinicId());
                    if (clinic != null) {
                        vo.setClinicName(clinic.getClinicName());
                        
                        // 设置科室名称
                        if (clinic.getDeptId() != null) {
                            Department dept = departmentService.getById(clinic.getDeptId());
                            if (dept != null) {
                                vo.setDeptName(dept.getDeptName());
                            }
                        }
                    }
                }
            }
            
            // 设置状态描述和是否可取消
            setAppointmentStatusInfo(vo, appointment.getAppointmentDate());
            
            result.add(vo);
        }
        
        return result;
    }
    
    /**
     * 设置预约状态描述和是否可取消
     */
    private void setAppointmentStatusInfo(AppointmentVO vo, LocalDate appointmentDate) {
        // 设置状态描述
        vo.setStatusDesc(getStatusDesc(vo.getStatus()));
        
        // 设置是否可取消（只有待就诊状态且预约日期在今天之后的才可取消）
        boolean canCancel = vo.getStatus() == 0 && 
                            appointmentDate != null && 
                            appointmentDate.isAfter(LocalDate.now());
        vo.setCanCancel(canCancel);
    }
    
    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        
        switch (status) {
            case 0:
                return "待就诊";
            case 1:
                return "已完成";
            case 2:
                return "已取消";
            case 3:
                return "已爽约";
            default:
                return "未知";
        }
    }

    @Override
    public List<Clinic> getClinicsByName(String name, boolean onlyActive) {
        log.info("通过名称查询门诊列表, name: {}, onlyActive: {}", name, onlyActive);
        
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("门诊名称不能为空");
        }
        
        // 调用实体服务查询门诊
        List<Clinic> clinics = clinicService.getClinicsByName(name);
        
        // 如果需要过滤有效门诊
        if (onlyActive && clinics != null) {
            clinics = clinics.stream()
                    .filter(clinic -> clinic.getIsActive() == 1)
                    .collect(Collectors.toList());
        }
        
        return clinics;
    }
}
