package com.graduation.his.service.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.graduation.his.common.exception.BusinessException;
import com.graduation.his.domain.po.Clinic;
import com.graduation.his.domain.po.Department;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Schedule;
import com.graduation.his.service.business.IAdminService;
import com.graduation.his.service.entity.IClinicService;
import com.graduation.his.service.entity.IDepartmentService;
import com.graduation.his.service.entity.IDoctorService;
import com.graduation.his.service.entity.IScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hua
 * @description 管理服务实现类
 * @create 2025-04-04 16:10
 */
@Slf4j
@Service
public class AdminServiceImpl implements IAdminService {

    @Autowired
    private IDoctorService doctorService;
    
    @Autowired
    private IScheduleService scheduleService;
    
    @Autowired
    private IDepartmentService departmentService;
    
    @Autowired
    private IClinicService clinicService;

    // ---------- 医生信息管理 ----------
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Doctor createDoctor(Doctor doctor) {
        log.info("创建医生信息: {}", doctor);
        
        if (doctor == null) {
            throw new BusinessException("医生信息不能为空");
        }
        
        // 数据验证
        if (doctor.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        
        if (StringUtils.isBlank(doctor.getName())) {
            throw new BusinessException("医生姓名不能为空");
        }
        
        // 检查门诊是否存在
        if (doctor.getClinicId() != null) {
            Clinic clinic = clinicService.getById(doctor.getClinicId());
            if (clinic == null) {
                throw new BusinessException("门诊不存在");
            }
            
            // 通过门诊关联获取科室
            Department department = departmentService.getById(clinic.getDeptId());
            if (department == null) {
                throw new BusinessException("门诊关联的科室不存在");
            }
        }
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        doctor.setCreateTime(now);
        doctor.setUpdateTime(now);
        
        // 保存医生信息
        doctorService.save(doctor);
        
        log.info("医生信息创建成功, ID: {}", doctor.getDoctorId());
        
        return doctor;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDoctor(Doctor doctor) {
        log.info("更新医生信息: {}", doctor);
        
        if (doctor == null) {
            throw new BusinessException("医生信息不能为空");
        }
        
        if (doctor.getDoctorId() == null) {
            throw new BusinessException("医生ID不能为空");
        }
        
        // 检查医生是否存在
        Doctor existingDoctor = doctorService.getById(doctor.getDoctorId());
        if (existingDoctor == null) {
            throw new BusinessException("医生不存在");
        }
        
        // 检查门诊是否存在
        if (doctor.getClinicId() != null) {
            Clinic clinic = clinicService.getById(doctor.getClinicId());
            if (clinic == null) {
                throw new BusinessException("门诊不存在");
            }
            
            // 通过门诊关联获取科室
            Department department = departmentService.getById(clinic.getDeptId());
            if (department == null) {
                throw new BusinessException("门诊关联的科室不存在");
            }
        }
        
        // 设置更新时间
        doctor.setUpdateTime(LocalDateTime.now());
        
        // 更新医生信息
        boolean result = doctorService.updateById(doctor);
        
        log.info("医生信息更新 {}, ID: {}", result ? "成功" : "失败", doctor.getDoctorId());
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDoctor(Long doctorId) {
        log.info("删除医生信息, doctorId: {}", doctorId);
        
        if (doctorId == null) {
            throw new BusinessException("医生ID不能为空");
        }
        
        // 检查医生是否存在
        Doctor doctor = doctorService.getById(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        
        // 检查是否有关联的排班
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, doctorId);
        long count = scheduleService.count(queryWrapper);
        
        if (count > 0) {
            throw new BusinessException("该医生有关联的排班，无法删除");
        }
        
        // 删除医生信息
        boolean result = doctorService.removeById(doctorId);
        
        log.info("医生信息删除 {}, ID: {}", result ? "成功" : "失败", doctorId);
        
        return result;
    }
    
    @Override
    public List<Doctor> getDoctorList(Long deptId, String name, Long clinicId) {
        log.info("获取医生列表, deptId: {}, name: {}, clinicId: {}", deptId, name, clinicId);
        
        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加条件
        if (deptId != null) {
            // 通过门诊表关联科室ID查询
            List<Clinic> clinics = clinicService.list(new LambdaQueryWrapper<Clinic>()
                    .eq(Clinic::getDeptId, deptId)
                    .select(Clinic::getClinicId));
            
            List<Long> clinicIds = clinics.stream()
                    .map(Clinic::getClinicId)
                    .collect(Collectors.toList());
            
            if (clinicIds.isEmpty()) {
                // 如果科室下没有门诊，直接返回空列表
                return new ArrayList<>();
            }
            
            queryWrapper.in(Doctor::getClinicId, clinicIds);
        }
        
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(Doctor::getName, name);
        }
        
        if (clinicId != null) {
            queryWrapper.eq(Doctor::getClinicId, clinicId);
        }
        
        // 排序
        queryWrapper.orderByAsc(Doctor::getClinicId)
                .orderByAsc(Doctor::getName);
        
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
    
    // ---------- 科室管理 ----------
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Department createDepartment(Department department) {
        log.info("创建科室: {}", department);
        
        if (department == null) {
            throw new BusinessException("科室信息不能为空");
        }
        
        // 数据验证
        if (StringUtils.isBlank(department.getDeptName())) {
            throw new BusinessException("科室名称不能为空");
        }
        
        // 检查是否存在同名科室
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getDeptName, department.getDeptName());
        long count = departmentService.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException("科室名称已存在");
        }
        
        // 设置默认值
        if (department.getIsActive() == null) {
            department.setIsActive(1); // 默认有效
        }
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        department.setCreateTime(now);
        department.setUpdateTime(now);
        
        // 保存科室信息
        departmentService.save(department);
        
        log.info("科室创建成功, ID: {}", department.getDeptId());
        
        return department;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDepartment(Department department) {
        log.info("更新科室: {}", department);
        
        if (department == null) {
            throw new BusinessException("科室信息不能为空");
        }
        
        if (department.getDeptId() == null) {
            throw new BusinessException("科室ID不能为空");
        }
        
        // 检查科室是否存在
        Department existingDepartment = departmentService.getById(department.getDeptId());
        if (existingDepartment == null) {
            throw new BusinessException("科室不存在");
        }
        
        // 检查科室名称是否已存在（排除当前科室）
        if (StringUtils.isNotBlank(department.getDeptName()) && !department.getDeptName().equals(existingDepartment.getDeptName())) {
            LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Department::getDeptName, department.getDeptName())
                    .ne(Department::getDeptId, department.getDeptId());
            long count = departmentService.count(queryWrapper);
            if (count > 0) {
                throw new BusinessException("科室名称已存在");
            }
        }
        
        // 设置更新时间
        department.setUpdateTime(LocalDateTime.now());
        
        // 更新科室信息
        boolean result = departmentService.updateById(department);
        
        log.info("科室更新 {}, ID: {}", result ? "成功" : "失败", department.getDeptId());
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean logicDeleteDepartment(Long deptId) {
        log.info("逻辑删除科室, deptId: {}", deptId);
        
        if (deptId == null) {
            throw new BusinessException("科室ID不能为空");
        }
        
        // 检查科室是否存在
        Department department = departmentService.getById(deptId);
        if (department == null) {
            throw new BusinessException("科室不存在");
        }
        
        // 检查科室是否已经是无效状态
        if (department.getIsActive() == 0) {
            throw new BusinessException("科室已经是无效状态");
        }
        
        // 检查是否有关联的门诊
        LambdaQueryWrapper<Clinic> clinicQuery = new LambdaQueryWrapper<>();
        clinicQuery.eq(Clinic::getDeptId, deptId);
        long clinicCount = clinicService.count(clinicQuery);
        
        if (clinicCount > 0) {
            // 获取所有关联的门诊
            List<Clinic> clinics = clinicService.list(clinicQuery.select(Clinic::getClinicId));
            List<Long> clinicIds = clinics.stream()
                    .map(Clinic::getClinicId)
                    .collect(Collectors.toList());
                    
            // 检查是否有关联的医生
            if (!clinicIds.isEmpty()) {
                LambdaQueryWrapper<Doctor> doctorQuery = new LambdaQueryWrapper<>();
                doctorQuery.in(Doctor::getClinicId, clinicIds);
                long doctorCount = doctorService.count(doctorQuery);
                
                if (doctorCount > 0) {
                    throw new BusinessException("该科室有关联的医生，无法删除");
                }
            }
            
            throw new BusinessException("该科室有关联的门诊，无法删除");
        }
        
        // 修改为无效状态
        LambdaUpdateWrapper<Department> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Department::getDeptId, deptId)
                .set(Department::getIsActive, 0)
                .set(Department::getUpdateTime, LocalDateTime.now());
        
        boolean result = departmentService.update(updateWrapper);
        
        log.info("科室逻辑删除 {}, ID: {}", result ? "成功" : "失败", deptId);
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreDepartment(Long deptId) {
        log.info("恢复科室, deptId: {}", deptId);
        
        if (deptId == null) {
            throw new BusinessException("科室ID不能为空");
        }
        
        // 检查科室是否存在
        Department department = departmentService.getById(deptId);
        if (department == null) {
            throw new BusinessException("科室不存在");
        }
        
        // 检查科室是否已经是有效状态
        if (department.getIsActive() == 1) {
            throw new BusinessException("科室已经是有效状态");
        }
        
        // 修改为有效状态
        LambdaUpdateWrapper<Department> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Department::getDeptId, deptId)
                .set(Department::getIsActive, 1)
                .set(Department::getUpdateTime, LocalDateTime.now());
        
        boolean result = departmentService.update(updateWrapper);
        
        log.info("科室恢复 {}, ID: {}", result ? "成功" : "失败", deptId);
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean physicalDeleteDepartment(Long deptId) {
        log.info("物理删除科室, deptId: {}", deptId);
        
        if (deptId == null) {
            throw new BusinessException("科室ID不能为空");
        }
        
        // 检查科室是否存在
        Department department = departmentService.getById(deptId);
        if (department == null) {
            throw new BusinessException("科室不存在");
        }
        
        // 检查科室是否为无效状态
        if (department.getIsActive() == 1) {
            throw new BusinessException("有效科室不能物理删除，请先执行逻辑删除");
        }
        
        // 检查是否有关联的门诊
        LambdaQueryWrapper<Clinic> clinicQuery = new LambdaQueryWrapper<>();
        clinicQuery.eq(Clinic::getDeptId, deptId);
        long clinicCount = clinicService.count(clinicQuery);
        
        if (clinicCount > 0) {
            // 获取所有关联的门诊
            List<Clinic> clinics = clinicService.list(clinicQuery.select(Clinic::getClinicId));
            List<Long> clinicIds = clinics.stream()
                    .map(Clinic::getClinicId)
                    .collect(Collectors.toList());
                    
            // 检查是否有关联的医生
            if (!clinicIds.isEmpty()) {
                LambdaQueryWrapper<Doctor> doctorQuery = new LambdaQueryWrapper<>();
                doctorQuery.in(Doctor::getClinicId, clinicIds);
                long doctorCount = doctorService.count(doctorQuery);
                
                if (doctorCount > 0) {
                    throw new BusinessException("该科室有关联的医生，无法删除");
                }
            }
            
            throw new BusinessException("该科室有关联的门诊，无法删除");
        }
        
        // 执行物理删除
        boolean result = departmentService.removeById(deptId);
        
        log.info("科室物理删除 {}, ID: {}", result ? "成功" : "失败", deptId);
        
        return result;
    }
    
    @Override
    public List<Department> getDepartmentList(Boolean isActive) {
        log.info("获取科室列表, isActive: {}", isActive);
        
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加条件
        if (isActive != null) {
            queryWrapper.eq(Department::getIsActive, isActive ? 1 : 0);
        }
        
        // 排序
        queryWrapper.orderByAsc(Department::getDeptId);
        
        return departmentService.list(queryWrapper);
    }
    
    @Override
    public Department getDepartmentDetail(Long deptId) {
        log.debug("获取科室详情: {}", deptId);
        if (deptId == null) {
            throw new BusinessException("科室ID不能为空");
        }
        
        Department department = departmentService.getById(deptId);
        if (department == null) {
            throw new BusinessException("科室不存在");
        }
        
        return department;
    }
    
    // ---------- 门诊管理 ----------
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Clinic createClinic(Clinic clinic) {
        log.info("创建门诊: {}", clinic);
        
        if (clinic == null) {
            throw new BusinessException("门诊信息不能为空");
        }
        
        // 数据验证
        if (StringUtils.isBlank(clinic.getClinicName())) {
            throw new BusinessException("门诊名称不能为空");
        }
        
        if (clinic.getDeptId() == null) {
            throw new BusinessException("科室ID不能为空");
        }
        
        // 检查科室是否存在
        Department department = departmentService.getById(clinic.getDeptId());
        if (department == null) {
            throw new BusinessException("科室不存在");
        }
        
        // 检查科室是否有效
        if (department.getIsActive() == 0) {
            throw new BusinessException("科室已被禁用，无法创建门诊");
        }
        
        // 检查是否存在同名门诊
        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Clinic::getClinicName, clinic.getClinicName())
                .eq(Clinic::getDeptId, clinic.getDeptId());
        long count = clinicService.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException("该科室下已存在同名门诊");
        }
        
        // 设置默认值
        if (clinic.getIsActive() == null) {
            clinic.setIsActive(1); // 默认有效
        }
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        clinic.setCreateTime(now);
        clinic.setUpdateTime(now);
        
        // 保存门诊信息
        clinicService.save(clinic);
        
        log.info("门诊创建成功, ID: {}", clinic.getClinicId());
        
        return clinic;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateClinic(Clinic clinic) {
        log.info("更新门诊: {}", clinic);
        
        if (clinic == null) {
            throw new BusinessException("门诊信息不能为空");
        }
        
        if (clinic.getClinicId() == null) {
            throw new BusinessException("门诊ID不能为空");
        }
        
        // 检查门诊是否存在
        Clinic existingClinic = clinicService.getById(clinic.getClinicId());
        if (existingClinic == null) {
            throw new BusinessException("门诊不存在");
        }
        
        // 如果要更新所属科室
        if (clinic.getDeptId() != null && !clinic.getDeptId().equals(existingClinic.getDeptId())) {
            // 检查科室是否存在
            Department department = departmentService.getById(clinic.getDeptId());
            if (department == null) {
                throw new BusinessException("科室不存在");
            }
            
            // 检查科室是否有效
            if (department.getIsActive() == 0) {
                throw new BusinessException("科室已被禁用，无法关联门诊");
            }
            
            // 检查是否存在同名门诊
            LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Clinic::getClinicName, clinic.getClinicName() != null ? clinic.getClinicName() : existingClinic.getClinicName())
                    .eq(Clinic::getDeptId, clinic.getDeptId())
                    .ne(Clinic::getClinicId, clinic.getClinicId());
            long count = clinicService.count(queryWrapper);
            if (count > 0) {
                throw new BusinessException("该科室下已存在同名门诊");
            }
        } else if (clinic.getClinicName() != null && !clinic.getClinicName().equals(existingClinic.getClinicName())) {
            // 如果只更新门诊名称，检查同一科室下是否有同名门诊
            LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Clinic::getClinicName, clinic.getClinicName())
                    .eq(Clinic::getDeptId, existingClinic.getDeptId())
                    .ne(Clinic::getClinicId, clinic.getClinicId());
            long count = clinicService.count(queryWrapper);
            if (count > 0) {
                throw new BusinessException("该科室下已存在同名门诊");
            }
        }
        
        // 设置更新时间
        clinic.setUpdateTime(LocalDateTime.now());
        
        // 更新门诊信息
        boolean result = clinicService.updateById(clinic);
        
        log.info("门诊更新 {}, ID: {}", result ? "成功" : "失败", clinic.getClinicId());
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean logicDeleteClinic(Long clinicId) {
        log.info("逻辑删除门诊, clinicId: {}", clinicId);
        
        if (clinicId == null) {
            throw new BusinessException("门诊ID不能为空");
        }
        
        // 检查门诊是否存在
        Clinic clinic = clinicService.getById(clinicId);
        if (clinic == null) {
            throw new BusinessException("门诊不存在");
        }
        
        // 检查门诊是否已经是无效状态
        if (clinic.getIsActive() == 0) {
            throw new BusinessException("门诊已经是无效状态");
        }
        
        // 检查是否有关联的医生
        LambdaQueryWrapper<Doctor> doctorQuery = new LambdaQueryWrapper<>();
        doctorQuery.eq(Doctor::getClinicId, clinicId);
        long doctorCount = doctorService.count(doctorQuery);
        if (doctorCount > 0) {
            throw new BusinessException("该门诊有关联的医生，无法删除");
        }
        
        // 修改为无效状态
        LambdaUpdateWrapper<Clinic> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Clinic::getClinicId, clinicId)
                .set(Clinic::getIsActive, 0)
                .set(Clinic::getUpdateTime, LocalDateTime.now());
        
        boolean result = clinicService.update(updateWrapper);
        
        log.info("门诊逻辑删除 {}, ID: {}", result ? "成功" : "失败", clinicId);
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreClinic(Long clinicId) {
        log.info("恢复门诊, clinicId: {}", clinicId);
        
        if (clinicId == null) {
            throw new BusinessException("门诊ID不能为空");
        }
        
        // 检查门诊是否存在
        Clinic clinic = clinicService.getById(clinicId);
        if (clinic == null) {
            throw new BusinessException("门诊不存在");
        }
        
        // 检查门诊是否已经是有效状态
        if (clinic.getIsActive() == 1) {
            throw new BusinessException("门诊已经是有效状态");
        }
        
        // 检查所属科室是否有效
        Department department = departmentService.getById(clinic.getDeptId());
        if (department == null) {
            throw new BusinessException("科室不存在");
        }
        if (department.getIsActive() == 0) {
            throw new BusinessException("门诊所属科室已被禁用，无法恢复门诊");
        }
        
        // 修改为有效状态
        LambdaUpdateWrapper<Clinic> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Clinic::getClinicId, clinicId)
                .set(Clinic::getIsActive, 1)
                .set(Clinic::getUpdateTime, LocalDateTime.now());
        
        boolean result = clinicService.update(updateWrapper);
        
        log.info("门诊恢复 {}, ID: {}", result ? "成功" : "失败", clinicId);
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean physicalDeleteClinic(Long clinicId) {
        log.info("物理删除门诊, clinicId: {}", clinicId);
        
        if (clinicId == null) {
            throw new BusinessException("门诊ID不能为空");
        }
        
        // 检查门诊是否存在
        Clinic clinic = clinicService.getById(clinicId);
        if (clinic == null) {
            throw new BusinessException("门诊不存在");
        }
        
        // 检查门诊是否为无效状态
        if (clinic.getIsActive() == 1) {
            throw new BusinessException("有效门诊不能物理删除，请先执行逻辑删除");
        }
        
        // 检查是否有关联的医生
        LambdaQueryWrapper<Doctor> doctorQuery = new LambdaQueryWrapper<>();
        doctorQuery.eq(Doctor::getClinicId, clinicId);
        long doctorCount = doctorService.count(doctorQuery);
        if (doctorCount > 0) {
            throw new BusinessException("该门诊有关联的医生，无法删除");
        }
        
        // 执行物理删除
        boolean result = clinicService.removeById(clinicId);
        
        log.info("门诊物理删除 {}, ID: {}", result ? "成功" : "失败", clinicId);
        
        return result;
    }
    
    @Override
    public List<Clinic> getClinicList(Long deptId, Boolean isActive) {
        log.info("获取门诊列表, deptId: {}, isActive: {}", deptId, isActive);
        
        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加条件
        if (deptId != null) {
            queryWrapper.eq(Clinic::getDeptId, deptId);
        }
        
        if (isActive != null) {
            queryWrapper.eq(Clinic::getIsActive, isActive ? 1 : 0);
        }
        
        // 排序
        queryWrapper.orderByAsc(Clinic::getDeptId)
                .orderByAsc(Clinic::getClinicId);
        
        return clinicService.list(queryWrapper);
    }
    
    @Override
    public Clinic getClinicDetail(Long clinicId) {
        log.info("获取门诊详情, clinicId: {}", clinicId);
        
        if (clinicId == null) {
            throw new BusinessException("门诊ID不能为空");
        }
        
        Clinic clinic = clinicService.getById(clinicId);
        if (clinic == null) {
            throw new BusinessException("门诊不存在");
        }
        
        return clinic;
    }
    
    // ---------- 排班管理 ----------
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Schedule createSchedule(Schedule schedule) {
        log.info("创建排班: {}", schedule);
        
        if (schedule == null) {
            throw new BusinessException("排班信息不能为空");
        }
        
        validateSchedule(schedule);
        
        // 检查同一医生同一时间是否已有排班
        checkDoctorScheduleConflict(schedule);
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        schedule.setCreateTime(now);
        schedule.setUpdateTime(now);
        
        // 保存排班信息
        scheduleService.save(schedule);
        
        log.info("排班创建成功, ID: {}", schedule.getScheduleId());
        
        return schedule;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSchedule(Schedule schedule) {
        log.info("更新排班: {}", schedule);
        
        if (schedule == null) {
            throw new BusinessException("排班信息不能为空");
        }
        
        if (schedule.getScheduleId() == null) {
            throw new BusinessException("排班ID不能为空");
        }
        
        // 检查排班是否存在
        Schedule existingSchedule = scheduleService.getById(schedule.getScheduleId());
        if (existingSchedule == null) {
            throw new BusinessException("排班不存在");
        }
        
        // 只允许修改未过期且未被预约的排班
        LocalDate today = LocalDate.now();
        if (existingSchedule.getScheduleDate().isBefore(today)) {
            throw new BusinessException("不能修改过期的排班");
        }
        
        // TODO: 检查排班是否已被预约，如果已被预约则不允许修改
        if (existingSchedule.getCurrentPatients() != null && existingSchedule.getCurrentPatients() > 0) {
            throw new BusinessException("该排班已有预约，无法修改");
        }
        
        // 如果修改了医生、日期或时段，需要检查冲突
        if ((schedule.getDoctorId() != null && !schedule.getDoctorId().equals(existingSchedule.getDoctorId())) || 
            (schedule.getScheduleDate() != null && !schedule.getScheduleDate().equals(existingSchedule.getScheduleDate())) || 
            (schedule.getTimeSlot() != null && !schedule.getTimeSlot().equals(existingSchedule.getTimeSlot()))) {
            
            // 构建完整的排班对象用于检查冲突
            Schedule checkSchedule = new Schedule();
            checkSchedule.setScheduleId(schedule.getScheduleId());
            checkSchedule.setDoctorId(schedule.getDoctorId() != null ? schedule.getDoctorId() : existingSchedule.getDoctorId());
            checkSchedule.setScheduleDate(schedule.getScheduleDate() != null ? schedule.getScheduleDate() : existingSchedule.getScheduleDate());
            checkSchedule.setTimeSlot(schedule.getTimeSlot() != null ? schedule.getTimeSlot() : existingSchedule.getTimeSlot());
            
            checkDoctorScheduleConflict(checkSchedule);
        }
        
        // 设置更新时间
        schedule.setUpdateTime(LocalDateTime.now());
        
        // 更新排班信息
        boolean result = scheduleService.updateById(schedule);
        
        log.info("排班更新 {}, ID: {}", result ? "成功" : "失败", schedule.getScheduleId());
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean logicDeleteSchedule(Long scheduleId) {
        log.info("逻辑删除排班, scheduleId: {}", scheduleId);
        
        if (scheduleId == null) {
            throw new BusinessException("排班ID不能为空");
        }
        
        // 检查排班是否存在
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        
        // 只允许删除未过期且未被预约的排班
        LocalDate today = LocalDate.now();
        if (schedule.getScheduleDate().isBefore(today)) {
            throw new BusinessException("不能删除过期的排班");
        }
        
        // 检查排班是否已被预约
        if (schedule.getCurrentPatients() != null && schedule.getCurrentPatients() > 0) {
            throw new BusinessException("该排班已有预约，无法删除");
        }
        
        // 逻辑删除（将状态设置为已取消）
        LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Schedule::getScheduleId, scheduleId)
                .set(Schedule::getStatus, 0) // 0表示已取消
                .set(Schedule::getUpdateTime, LocalDateTime.now());
        
        boolean result = scheduleService.update(updateWrapper);
        
        log.info("排班逻辑删除 {}, ID: {}", result ? "成功" : "失败", scheduleId);
        
        return result;
    }
    
    @Override
    public List<Schedule> getScheduleList(Long doctorId, Long clinicId, LocalDate startDate, LocalDate endDate) {
        log.info("获取排班列表, doctorId: {}, clinicId: {}, startDate: {}, endDate: {}", doctorId, clinicId, startDate, endDate);
        
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        
        // 构建查询条件
        if (doctorId != null) {
            queryWrapper.eq(Schedule::getDoctorId, doctorId);
        }
        
        if (clinicId != null) {
            queryWrapper.eq(Schedule::getClinicId, clinicId);
        }
        
        if (startDate != null) {
            queryWrapper.ge(Schedule::getScheduleDate, startDate);
        }
        
        if (endDate != null) {
            queryWrapper.le(Schedule::getScheduleDate, endDate);
        }
        
        // 默认只查询有效排班
        queryWrapper.eq(Schedule::getStatus, 1); // 1表示有效
        
        // 排序
        queryWrapper.orderByAsc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getTimeSlot)
                .orderByAsc(Schedule::getDoctorId);
        
        return scheduleService.list(queryWrapper);
    }
    
    @Override
    public Schedule getScheduleDetail(Long scheduleId) {
        log.info("获取排班详情, scheduleId: {}", scheduleId);
        
        if (scheduleId == null) {
            throw new BusinessException("排班ID不能为空");
        }
        
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        
        return schedule;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeAutoSchedule(LocalDate startDate, LocalDate endDate, Long clinicId) {
        log.info("执行自动排班, startDate: {}, endDate: {}, clinicId: {}", startDate, endDate, clinicId);
        
        if (startDate == null || endDate == null) {
            throw new BusinessException("开始日期和结束日期不能为空");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        
        // 获取需要排班的医生列表和分组
        List<Doctor> allDoctors;
        Map<Long, List<Doctor>> clinicDoctorsMap = new HashMap<>();
        
        if (clinicId != null) {
            // 如果指定了门诊，只获取该门诊下的医生
            allDoctors = doctorService.list(new LambdaQueryWrapper<Doctor>()
                    .eq(Doctor::getClinicId, clinicId));
            
            if (allDoctors.isEmpty()) {
                throw new BusinessException("该门诊下没有医生");
            }
            
            clinicDoctorsMap.put(clinicId, allDoctors);
        } else {
            // 如果没有指定门诊，则为所有门诊排班
            allDoctors = doctorService.list();
            
            if (allDoctors.isEmpty()) {
                throw new BusinessException("没有可排班的医生");
            }
            
            // 按门诊分组医生
            clinicDoctorsMap = allDoctors.stream()
                    .filter(doctor -> doctor.getClinicId() != null)
                    .collect(Collectors.groupingBy(Doctor::getClinicId));
        }
        
        // 获取所有需要排班的门诊信息
        List<Clinic> clinics;
        if (clinicId != null) {
            Clinic clinic = clinicService.getById(clinicId);
            if (clinic == null) {
                throw new BusinessException("门诊不存在");
            }
            clinics = Collections.singletonList(clinic);
        } else {
            // 获取所有门诊
            Set<Long> clinicIds = clinicDoctorsMap.keySet();
            clinics = clinicService.listByIds(clinicIds);
        }
        
        // 获取所有医生的疲劳度统计
        List<Long> doctorIds = allDoctors.stream()
                .map(Doctor::getDoctorId)
                .collect(Collectors.toList());
                
        Map<Long, Integer> fatigueStats = getDoctorFatigueStats(doctorIds);
        
        // 创建排班列表
        List<Schedule> schedules = new ArrayList<>();
        
        // 定义两个时段: 上午和下午
        String[] timeSlots = {"08:00-12:00", "14:00-18:00"};
        
        // 遍历日期
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // 遍历每个时段
            for (String timeSlot : timeSlots) {
                // 为每个门诊安排医生
                for (Clinic clinic : clinics) {
                    Long cId = clinic.getClinicId();
                    List<Doctor> clinicDoctors = clinicDoctorsMap.get(cId);
                    
                    if (clinicDoctors == null || clinicDoctors.isEmpty()) {
                        continue; // 跳过没有医生的门诊
                    }
                    
                    // 检查当前日期时段是否已有排班
                    List<Schedule> existingSchedules = scheduleService.list(new LambdaQueryWrapper<Schedule>()
                            .eq(Schedule::getScheduleDate, currentDate)
                            .eq(Schedule::getTimeSlot, timeSlot)
                            .eq(Schedule::getClinicId, cId)
                            .eq(Schedule::getStatus, 1));
                    
                    if (!existingSchedules.isEmpty()) {
                        log.info("门诊 {} 在日期 {} 时段 {} 已有排班，跳过", cId, currentDate, timeSlot);
                        continue;
                    }
                    
                    // 根据门诊的医生数量决定需要安排的医生数量
                    // 门诊医生数量的一半，至少1名，最多3名
                    int doctorsNeeded = Math.min(3, Math.max(1, clinicDoctors.size() / 2));
                    
                    // 按疲劳度排序医生（疲劳度低的优先排班）
                    List<Doctor> sortedDoctors = new ArrayList<>(clinicDoctors);
                    sortedDoctors.sort(Comparator.comparing(doctor -> 
                            fatigueStats.getOrDefault(doctor.getDoctorId(), 0)));
                    
                    // 为当前门诊分配多名医生
                    int assignedCount = 0;
                    for (Doctor doctor : sortedDoctors) {
                        if (assignedCount >= doctorsNeeded) {
                            break; // 已分配足够数量的医生
                        }
                        
                        // 检查医生在该日期时段是否已有排班
                        boolean hasSchedule = scheduleService.count(new LambdaQueryWrapper<Schedule>()
                                .eq(Schedule::getDoctorId, doctor.getDoctorId())
                                .eq(Schedule::getScheduleDate, currentDate)
                                .eq(Schedule::getTimeSlot, timeSlot)
                                .eq(Schedule::getStatus, 1)) > 0;
                        
                        if (hasSchedule) {
                            continue; // 医生已有排班，跳过
                        }
                        
                        // 创建排班
                        Schedule schedule = new Schedule();
                        schedule.setDoctorId(doctor.getDoctorId());
                        schedule.setClinicId(cId);
                        schedule.setScheduleDate(currentDate);
                        schedule.setTimeSlot(timeSlot);
                        schedule.setStatus(1); // 1表示有效
                        schedule.setMaxPatients(20); // 默认每个时段20个名额
                        schedule.setCurrentPatients(0); // 初始化当前预约人数为0
                        
                        schedules.add(schedule);
                        assignedCount++;
                        
                        // 更新医生疲劳度
                        int currentFatigue = fatigueStats.getOrDefault(doctor.getDoctorId(), 0);
                        fatigueStats.put(doctor.getDoctorId(), currentFatigue + 1);
                    }
                }
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        // 保存排班
        if (!schedules.isEmpty()) {
            // 设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            for (Schedule schedule : schedules) {
                schedule.setCreateTime(now);
                schedule.setUpdateTime(now);
            }
            
            boolean result = scheduleService.saveBatch(schedules);
            log.info("自动排班 {}, 共创建 {} 条排班记录", result ? "成功" : "失败", schedules.size());
            return result;
        } else {
            log.info("没有需要创建的排班");
            return true; // 没有需要创建的排班，视为成功
        }
    }
    
    @Override
    public Map<LocalDate, Boolean> getScheduleStatus(LocalDate startDate, LocalDate endDate) {
        log.info("获取排班状态, startDate: {}, endDate: {}", startDate, endDate);
        
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        
        if (endDate == null) {
            endDate = startDate.plusDays(14); // 默认查询两周
        }
        
        // 查询日期范围内的所有排班
        List<Schedule> schedules = scheduleService.list(new LambdaQueryWrapper<Schedule>()
                .ge(Schedule::getScheduleDate, startDate)
                .le(Schedule::getScheduleDate, endDate)
                .eq(Schedule::getStatus, 1) // 1表示有效
                .select(Schedule::getScheduleDate));
        
        // 按日期分组，检查每天是否有排班
        Set<LocalDate> scheduledDates = schedules.stream()
                .map(Schedule::getScheduleDate)
                .collect(Collectors.toSet());
        
        // 构建结果映射
        Map<LocalDate, Boolean> result = new HashMap<>();
        
        // 填充所有日期的排班状态
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            result.put(currentDate, scheduledDates.contains(currentDate));
            currentDate = currentDate.plusDays(1);
        }
        
        return result;
    }
    
    // 将公开方法改为私有方法，内部使用
    private Map<Long, Integer> getDoctorFatigueStats(List<Long> doctorIds) {
        log.info("获取医生疲劳度统计, doctorIds: {}", doctorIds);
        
        if (doctorIds == null || doctorIds.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<Long, Integer> result = new HashMap<>();
        
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 获取当前月的第一天和最后一天
        LocalDate firstDayOfMonth = YearMonth.from(today).atDay(1);
        LocalDate lastDayOfMonth = YearMonth.from(today).atEndOfMonth();
        
        // 获取最近7天的开始日期
        LocalDate sevenDaysAgo = today.minusDays(7);
        
        // 查询每个医生的排班情况
        for (Long doctorId : doctorIds) {
            // 查询最近7天的排班次数
            long recentCount = scheduleService.count(new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getDoctorId, doctorId)
                    .ge(Schedule::getScheduleDate, sevenDaysAgo)
                    .le(Schedule::getScheduleDate, today)
                    .eq(Schedule::getStatus, 1)); // 1表示有效
            
            // 查询本月的累计排班次数
            long monthlyCount = scheduleService.count(new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getDoctorId, doctorId)
                    .ge(Schedule::getScheduleDate, firstDayOfMonth)
                    .le(Schedule::getScheduleDate, lastDayOfMonth)
                    .eq(Schedule::getStatus, 1)); // 1表示有效
            
            // 计算疲劳度：最近7天的排班次数 × 2 + 本月累计排班次数
            int fatigue = (int) (recentCount * 2 + monthlyCount);
            
            result.put(doctorId, fatigue);
        }
        
        return result;
    }
    
    // 辅助方法：验证排班信息
    private void validateSchedule(Schedule schedule) {
        if (schedule.getDoctorId() == null) {
            throw new BusinessException("医生ID不能为空");
        }
        
        if (schedule.getScheduleDate() == null) {
            throw new BusinessException("排班日期不能为空");
        }
        
        if (schedule.getTimeSlot() == null) {
            throw new BusinessException("排班时段不能为空");
        }
        
        // 检查排班日期是否在今天之后
        LocalDate today = LocalDate.now();
        if (schedule.getScheduleDate().isBefore(today)) {
            throw new BusinessException("排班日期不能早于今天");
        }
        
        // 检查医生是否存在
        Doctor doctor = doctorService.getById(schedule.getDoctorId());
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        
        // 检查门诊是否存在
        if (schedule.getClinicId() == null) {
            // 如果未指定门诊，则使用医生所属门诊
            schedule.setClinicId(doctor.getClinicId());
        } else {
            Clinic clinic = clinicService.getById(schedule.getClinicId());
            if (clinic == null) {
                throw new BusinessException("门诊不存在");
            }
        }
        
        // 设置默认值
        if (schedule.getStatus() == null) {
            schedule.setStatus(1); // 1表示有效
        }
        
        if (schedule.getMaxPatients() == null) {
            schedule.setMaxPatients(20); // 默认每个时段20个名额
        }
        
        if (schedule.getCurrentPatients() == null) {
            schedule.setCurrentPatients(0); // 初始化当前预约人数为0
        }
    }
    
    // 辅助方法：检查医生排班冲突
    private void checkDoctorScheduleConflict(Schedule schedule) {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, schedule.getDoctorId())
                .eq(Schedule::getScheduleDate, schedule.getScheduleDate())
                .eq(Schedule::getTimeSlot, schedule.getTimeSlot())
                .eq(Schedule::getStatus, 1); // 1表示有效
                
        // 如果是更新操作，需要排除自身
        if (schedule.getScheduleId() != null) {
            queryWrapper.ne(Schedule::getScheduleId, schedule.getScheduleId());
        }
        
        long count = scheduleService.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException("该医生在同一时间已有排班");
        }
    }
}

