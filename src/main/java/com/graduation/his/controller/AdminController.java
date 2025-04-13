package com.graduation.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.graduation.his.common.Result;
import com.graduation.his.domain.dto.DoctorDTO;
import com.graduation.his.domain.po.Clinic;
import com.graduation.his.domain.po.Department;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Schedule;
import com.graduation.his.service.business.IAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author hua
 * @description 管理模块控制器
 * @create 2025-04-04 16:10
 */
@SaCheckRole("admin")
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private IAdminService adminService;
    
    // ---------- 医生管理接口 ----------
    
    /**
     * 创建医生
     * @param doctorInfo 医生基本信息（JSON格式）
     * @param avatarFile 医生头像文件（可选）
     * @return 创建后的医生信息
     */
    @PostMapping(value = "/doctor", consumes = "multipart/form-data")
    public Result<Doctor> createDoctor(
            @RequestPart("doctorInfo") DoctorDTO doctorInfo,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        
        if (avatarFile != null) {
            doctorInfo.setAvatarFile(avatarFile);
        }
        
        return Result.success(adminService.createDoctor(doctorInfo));
    }
    
    /**
     * 更新医生
     * @param doctorInfo 医生基本信息（JSON格式）
     * @param avatarFile 医生头像文件（可选）
     * @return 更新结果
     */
    @PutMapping(value = "/doctor", consumes = "multipart/form-data")
    public Result<Boolean> updateDoctor(
            @RequestPart("doctorInfo") DoctorDTO doctorInfo,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        
        if (avatarFile != null) {
            doctorInfo.setAvatarFile(avatarFile);
        }
        
        return Result.success(adminService.updateDoctor(doctorInfo));
    }
    
    /**
     * 删除医生
     * @param doctorId 医生ID
     * @return 删除结果
     */
    @DeleteMapping("/doctor/{doctorId}")
    public Result<Boolean> deleteDoctor(@PathVariable Long doctorId) {
        return Result.success(adminService.deleteDoctor(doctorId));
    }
    
    /**
     * 获取医生详情
     * @param doctorId 医生ID
     * @return 医生详情
     */
    @GetMapping("/doctor/{doctorId}")
    public Result<Doctor> getDoctorDetail(@PathVariable Long doctorId) {
        return Result.success(adminService.getDoctorDetail(doctorId));
    }
    
    // ---------- 排班管理接口 ----------
    
    /**
     * 创建排班
     * @param schedule 排班信息
     * @return 创建后的排班信息
     */
    @PostMapping("/schedule")
    public Result<Schedule> createSchedule(@RequestBody Schedule schedule) {
        return Result.success(adminService.createSchedule(schedule));
    }
    
    /**
     * 更新排班
     * @param schedule 排班信息
     * @return 更新结果
     */
    @PutMapping("/schedule")
    public Result<Boolean> updateSchedule(@RequestBody Schedule schedule) {
        return Result.success(adminService.updateSchedule(schedule));
    }
    
    /**
     * 逻辑删除排班
     * @param scheduleId 排班ID
     * @return 删除结果
     */
    @DeleteMapping("/schedule/{scheduleId}")
    public Result<Boolean> logicDeleteSchedule(@PathVariable Long scheduleId) {
        return Result.success(adminService.logicDeleteSchedule(scheduleId));
    }
    
    /**
     * 获取排班详情
     * @param scheduleId 排班ID
     * @return 排班详情
     */
    @GetMapping("/schedule/{scheduleId}")
    public Result<Schedule> getScheduleDetail(@PathVariable Long scheduleId) {
        return Result.success(adminService.getScheduleDetail(scheduleId));
    }
    
    /**
     * 执行自动排班
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param clinicId 门诊ID（为null表示给所有门诊排班）
     * @return 排班结果
     */
    @PostMapping("/schedule/auto")
    public Result<Boolean> executeAutoSchedule(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Long clinicId) {
        return Result.success(adminService.executeAutoSchedule(startDate, endDate, clinicId));
    }
    
    /**
     * 获取排班状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班状态
     */
    @GetMapping("/schedule/status")
    public Result<Map<LocalDate, Boolean>> getScheduleStatus(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(adminService.getScheduleStatus(startDate, endDate));
    }
    
    // ---------- 科室管理接口 ----------
    
    /**
     * 创建科室
     * @param department 科室信息
     * @return 创建后的科室信息
     */
    @PostMapping("/department")
    public Result<Department> createDepartment(@RequestBody Department department) {
        return Result.success(adminService.createDepartment(department));
    }
    
    /**
     * 更新科室
     * @param department 科室信息
     * @return 更新结果
     */
    @PutMapping("/department")
    public Result<Boolean> updateDepartment(@RequestBody Department department) {
        return Result.success(adminService.updateDepartment(department));
    }
    
    /**
     * 逻辑删除科室
     * @param deptId 科室ID
     * @return 删除结果
     */
    @DeleteMapping("/department/{deptId}")
    public Result<Boolean> logicDeleteDepartment(@PathVariable Long deptId) {
        return Result.success(adminService.logicDeleteDepartment(deptId));
    }
    
    /**
     * 恢复科室
     * @param deptId 科室ID
     * @return 恢复结果
     */
    @PutMapping("/department/restore/{deptId}")
    public Result<Boolean> restoreDepartment(@PathVariable Long deptId) {
        return Result.success(adminService.restoreDepartment(deptId));
    }
    
    /**
     * 物理删除科室
     * @param deptId 科室ID
     * @return 删除结果
     */
    @DeleteMapping("/department/physical/{deptId}")
    public Result<Boolean> physicalDeleteDepartment(@PathVariable Long deptId) {
        return Result.success(adminService.physicalDeleteDepartment(deptId));
    }
    
    /**
     * 获取科室详情
     * @param deptId 科室ID
     * @return 科室详情
     */
    @GetMapping("/department/{deptId}")
    public Result<Department> getDepartmentDetail(@PathVariable Long deptId) {
        return Result.success(adminService.getDepartmentDetail(deptId));
    }
    
    // ---------- 门诊管理接口 ----------
    
    /**
     * 创建门诊
     * @param clinic 门诊信息
     * @return 创建后的门诊信息
     */
    @PostMapping("/clinic")
    public Result<Clinic> createClinic(@RequestBody Clinic clinic) {
        return Result.success(adminService.createClinic(clinic));
    }
    
    /**
     * 更新门诊
     * @param clinic 门诊信息
     * @return 更新结果
     */
    @PutMapping("/clinic")
    public Result<Boolean> updateClinic(@RequestBody Clinic clinic) {
        return Result.success(adminService.updateClinic(clinic));
    }
    
    /**
     * 逻辑删除门诊
     * @param clinicId 门诊ID
     * @return 删除结果
     */
    @DeleteMapping("/clinic/{clinicId}")
    public Result<Boolean> logicDeleteClinic(@PathVariable Long clinicId) {
        return Result.success(adminService.logicDeleteClinic(clinicId));
    }
    
    /**
     * 恢复门诊
     * @param clinicId 门诊ID
     * @return 恢复结果
     */
    @PutMapping("/clinic/restore/{clinicId}")
    public Result<Boolean> restoreClinic(@PathVariable Long clinicId) {
        return Result.success(adminService.restoreClinic(clinicId));
    }
    
    /**
     * 物理删除门诊
     * @param clinicId 门诊ID
     * @return 删除结果
     */
    @DeleteMapping("/clinic/physical/{clinicId}")
    public Result<Boolean> physicalDeleteClinic(@PathVariable Long clinicId) {
        return Result.success(adminService.physicalDeleteClinic(clinicId));
    }
    
    /**
     * 获取门诊详情
     * @param clinicId 门诊ID
     * @return 门诊详情
     */
    @GetMapping("/clinic/{clinicId}")
    public Result<Clinic> getClinicDetail(@PathVariable Long clinicId) {
        return Result.success(adminService.getClinicDetail(clinicId));
    }
}
