package com.graduation.his.service.business;

import com.graduation.his.domain.po.Clinic;
import com.graduation.his.domain.po.Department;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author hua
 * @description 管理服务接口
 * @create 2025-04-04 16:10
 */
public interface IAdminService {
    
    // ---------- 医生信息管理 ----------
    
    /**
     * 创建医生信息
     * @param doctor 医生信息
     * @return 创建后的医生信息
     */
    Doctor createDoctor(Doctor doctor);
    
    /**
     * 更新医生信息
     * @param doctor 医生信息
     * @return 是否更新成功
     */
    boolean updateDoctor(Doctor doctor);
    
    /**
     * 删除医生信息
     * @param doctorId 医生ID
     * @return 是否删除成功
     */
    boolean deleteDoctor(Long doctorId);
    
    /**
     * 获取医生列表
     * @param deptId 科室ID（可选）
     * @param name 医生姓名（可选，模糊匹配）
     * @param clinicId 门诊ID（可选）
     * @return 医生列表
     */
    List<Doctor> getDoctorList(Long deptId, String name, Long clinicId);
    
    /**
     * 获取医生详情
     * @param doctorId 医生ID
     * @return 医生详情
     */
    Doctor getDoctorDetail(Long doctorId);
    
    // ---------- 坐诊排班管理 ----------
    
    /**
     * 创建排班
     * @param schedule 排班信息
     * @return 创建后的排班信息
     */
    Schedule createSchedule(Schedule schedule);
    
    /**
     * 更新排班
     * @param schedule 排班信息
     * @return 是否更新成功
     */
    boolean updateSchedule(Schedule schedule);
    
    /**
     * 逻辑删除排班
     * @param scheduleId 排班ID
     * @return 是否删除成功
     */
    boolean logicDeleteSchedule(Long scheduleId);
    
    /**
     * 获取排班列表
     * @param doctorId 医生ID（可选）
     * @param clinicId 门诊ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 排班列表
     */
    List<Schedule> getScheduleList(Long doctorId, Long clinicId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取排班详情
     * @param scheduleId 排班ID
     * @return 排班详情
     */
    Schedule getScheduleDetail(Long scheduleId);
    
    /**
     * 手动执行自动排班（用于测试和手动排班）
     * @param startDate 开始日期
     * @param endDate 结束日期（最多两周）
     * @param clinicId 门诊ID（可选，不指定则为所有门诊）
     * @return 是否排班成功
     */
    boolean executeAutoSchedule(LocalDate startDate, LocalDate endDate, Long clinicId);
    
    /**
     * 获取排班状态统计
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按天统计的排班状态，key为日期，value为该日期的排班状态（true表示已排班，false表示未排班）
     */
    Map<LocalDate, Boolean> getScheduleStatus(LocalDate startDate, LocalDate endDate);
    
    // ---------- 科室管理 ----------
    
    /**
     * 创建科室
     * @param department 科室信息
     * @return 创建后的科室信息
     */
    Department createDepartment(Department department);
    
    /**
     * 更新科室
     * @param department 科室信息
     * @return 是否更新成功
     */
    boolean updateDepartment(Department department);
    
    /**
     * 逻辑删除科室
     * @param deptId 科室ID
     * @return 是否删除成功
     */
    boolean logicDeleteDepartment(Long deptId);
    
    /**
     * 恢复逻辑删除的科室
     * @param deptId 科室ID
     * @return 是否恢复成功
     */
    boolean restoreDepartment(Long deptId);
    
    /**
     * 物理删除科室（彻底删除）
     * @param deptId 科室ID
     * @return 是否删除成功
     */
    boolean physicalDeleteDepartment(Long deptId);
    
    /**
     * 获取科室列表
     * @param isActive 是否有效（可选）
     * @return 科室列表
     */
    List<Department> getDepartmentList(Boolean isActive);
    
    /**
     * 获取科室详情
     * @param deptId 科室ID
     * @return 科室详情
     */
    Department getDepartmentDetail(Long deptId);
    
    // ---------- 门诊管理 ----------
    
    /**
     * 创建门诊
     * @param clinic 门诊信息
     * @return 创建后的门诊信息
     */
    Clinic createClinic(Clinic clinic);
    
    /**
     * 更新门诊
     * @param clinic 门诊信息
     * @return 是否更新成功
     */
    boolean updateClinic(Clinic clinic);
    
    /**
     * 逻辑删除门诊
     * @param clinicId 门诊ID
     * @return 是否删除成功
     */
    boolean logicDeleteClinic(Long clinicId);
    
    /**
     * 恢复逻辑删除的门诊
     * @param clinicId 门诊ID
     * @return 是否恢复成功
     */
    boolean restoreClinic(Long clinicId);
    
    /**
     * 物理删除门诊（彻底删除）
     * @param clinicId 门诊ID
     * @return 是否删除成功
     */
    boolean physicalDeleteClinic(Long clinicId);
    
    /**
     * 获取门诊列表
     * @param deptId 科室ID（可选）
     * @param isActive 是否有效（可选）
     * @return 门诊列表
     */
    List<Clinic> getClinicList(Long deptId, Boolean isActive);
    
    /**
     * 获取门诊详情
     * @param clinicId 门诊ID
     * @return 门诊详情
     */
    Clinic getClinicDetail(Long clinicId);
}
