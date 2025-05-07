package com.graduation.his.service.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.his.domain.po.AiConsultRecord;
import com.graduation.his.domain.po.Appointment;
import com.graduation.his.domain.po.Clinic;
import com.graduation.his.domain.po.Department;
import com.graduation.his.domain.po.Diagnosis;
import com.graduation.his.domain.po.Doctor;
import com.graduation.his.domain.po.Patient;
import com.graduation.his.service.business.IDataAnalysisService;
import com.graduation.his.service.entity.IAIService;
import com.graduation.his.service.entity.IClinicService;
import com.graduation.his.service.entity.IDepartmentService;
import com.graduation.his.service.entity.IDiagnosisService;
import com.graduation.his.service.entity.IDoctorService;
import com.graduation.his.service.entity.IPatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author hua
 * @description 可视化界面服务类
 * @create 2025-04-12 20:50
 */
@Slf4j
@Service
public class DataAnalysisServiceImpl implements IDataAnalysisService {

    @Autowired
    private IPatientService patientService;
    
    @Autowired
    private IDoctorService doctorService;
    
    @Autowired
    private IDiagnosisService diagnosisService;
    
    @Autowired
    private IAIService aiService;
    
    @Autowired
    private IDepartmentService departmentService;
    
    @Autowired
    private IClinicService clinicService;
    
    /**
     * 获取指定医生的患者ID列表
     * @param doctorId 医生ID
     * @return 患者ID列表
     */
    private List<Long> getPatientIdsByDoctorId(Long doctorId) {
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getDoctorId, doctorId);
        List<Diagnosis> diagnosisList = diagnosisService.list(queryWrapper);
        
        return diagnosisList.stream()
                .map(Diagnosis::getPatientId)
                .distinct()
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Integer> getPatientVisitFrequency(Long doctorId, LocalDate startDate, LocalDate endDate, String timeUnit) {
        log.info("获取患者就诊频次统计, doctorId: {}, startDate: {}, endDate: {}, timeUnit: {}", doctorId, startDate, endDate, timeUnit);
        
        // 参数校验
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(6);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (timeUnit == null || timeUnit.isEmpty()) {
            timeUnit = "month";
        }
        
        // 查询时间范围内的指定医生的所有就诊记录
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getDoctorId, doctorId)
                   .between(Diagnosis::getCreateTime, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        List<Diagnosis> diagnosisList = diagnosisService.list(queryWrapper);
        
        // 使用指定的时间单位对数据进行分组统计
        Map<String, Integer> result;
        DateTimeFormatter formatter;
        Function<Diagnosis, String> groupingFunction;
        
        switch (timeUnit.toLowerCase()) {
            case "day":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                groupingFunction = diag -> diag.getCreateTime().toLocalDate().format(formatter);
                break;
            case "week":
                groupingFunction = diag -> {
                    LocalDate date = diag.getCreateTime().toLocalDate();
                    int weekOfYear = date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
                    return date.getYear() + "-W" + weekOfYear;
                };
                break;
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                groupingFunction = diag -> diag.getCreateTime().toLocalDate().format(formatter);
                break;
            case "month":
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                groupingFunction = diag -> diag.getCreateTime().toLocalDate().format(formatter);
                break;
        }
        
        // 进行分组统计
        result = diagnosisList.stream()
                .collect(Collectors.groupingBy(groupingFunction, Collectors.summingInt(diag -> 1)));
        
        // 填充空缺的时间点
        result = fillMissingTimePoints(result, startDate, endDate, timeUnit);
        
        return result;
    }

    @Override
    public Map<String, Integer> getAiConsultFrequency(Long doctorId, LocalDate startDate, LocalDate endDate, String timeUnit) {
        log.info("获取AI问诊使用频率统计, doctorId: {}, startDate: {}, endDate: {}, timeUnit: {}", doctorId, startDate, endDate, timeUnit);
        
        // 参数校验
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(6);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (timeUnit == null || timeUnit.isEmpty()) {
            timeUnit = "month";
        }
        
        // 获取该医生的患者ID列表
        List<Long> patientIds = getPatientIdsByDoctorId(doctorId);
        
        // 初始化结果为空Map
        Map<String, Integer> result = new TreeMap<>();
        
        // 如果患者列表为空，直接返回初始化的结果
        if (patientIds == null || patientIds.isEmpty()) {
            log.info("医生 {} 没有相关的患者记录", doctorId);
            // 填充空缺的时间点
            return fillMissingTimePoints(result, startDate, endDate, timeUnit);
        }
        
        // 查询时间范围内的所有AI问诊记录
        LambdaQueryWrapper<AiConsultRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(AiConsultRecord::getPatientId, patientIds)
                   .between(AiConsultRecord::getCreateTime, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        List<AiConsultRecord> consultRecords = aiService.list(queryWrapper);
        
        // 使用指定的时间单位对数据进行分组统计
        DateTimeFormatter formatter;
        Function<AiConsultRecord, String> groupingFunction;
        
        switch (timeUnit.toLowerCase()) {
            case "day":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                groupingFunction = record -> record.getCreateTime().toLocalDate().format(formatter);
                break;
            case "week":
                groupingFunction = record -> {
                    LocalDate date = record.getCreateTime().toLocalDate();
                    int weekOfYear = date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
                    return date.getYear() + "-W" + weekOfYear;
                };
                break;
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                groupingFunction = record -> record.getCreateTime().toLocalDate().format(formatter);
                break;
            case "month":
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                groupingFunction = record -> record.getCreateTime().toLocalDate().format(formatter);
                break;
        }
        
        // 进行分组统计
        result = consultRecords.stream()
                .collect(Collectors.groupingBy(groupingFunction, Collectors.summingInt(record -> 1)));
        
        // 填充空缺的时间点
        result = fillMissingTimePoints(result, startDate, endDate, timeUnit);
        
        return result;
    }

    @Override
    public Map<String, Integer> getPatientAgeDistribution(Long doctorId) {
        log.info("获取患者年龄分布统计, doctorId: {}", doctorId);
        
        // 定义年龄段
        String[] ageRanges = {"0-18", "19-30", "31-45", "46-60", "61-75", "76+"};
        Map<String, Integer> result = new LinkedHashMap<>();
        
        // 初始化结果
        Arrays.stream(ageRanges).forEach(range -> result.put(range, 0));
        
        // 获取该医生的患者ID列表
        List<Long> patientIds = getPatientIdsByDoctorId(doctorId);
        
        // 如果患者列表为空，直接返回初始化的结果
        if (patientIds == null || patientIds.isEmpty()) {
            log.info("医生 {} 没有相关的患者记录", doctorId);
            return result;
        }
        
        // 查询该医生的所有患者信息
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Patient::getPatientId, patientIds);
        List<Patient> patients = patientService.list(queryWrapper);
        
        // 根据患者年龄进行分组
        for (Patient patient : patients) {
            if (patient.getAge() == null) {
                continue;
            }
            
            int age = patient.getAge();
            
            if (age <= 18) {
                result.put("0-18", result.get("0-18") + 1);
            } else if (age <= 30) {
                result.put("19-30", result.get("19-30") + 1);
            } else if (age <= 45) {
                result.put("31-45", result.get("31-45") + 1);
            } else if (age <= 60) {
                result.put("46-60", result.get("46-60") + 1);
            } else if (age <= 75) {
                result.put("61-75", result.get("61-75") + 1);
            } else {
                result.put("76+", result.get("76+") + 1);
            }
        }
        
        return result;
    }

    @Override
    public Map<String, Integer> getPatientGenderRatio(Long doctorId) {
        log.info("获取患者性别比例统计, doctorId: {}", doctorId);
        
        // 初始化结果
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("男", 0);
        result.put("女", 0);
        result.put("未知", 0);
        
        // 获取该医生的患者ID列表
        List<Long> patientIds = getPatientIdsByDoctorId(doctorId);
        
        // 如果患者列表为空，直接返回初始化的结果
        if (patientIds == null || patientIds.isEmpty()) {
            log.info("医生 {} 没有相关的患者记录", doctorId);
            return result;
        }
        
        // 查询该医生的所有患者信息
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Patient::getPatientId, patientIds);
        List<Patient> patients = patientService.list(queryWrapper);
        
        // 统计性别分布
        for (Patient patient : patients) {
            Integer genderCode = patient.getGender();
            
            if (genderCode == null) {
                result.put("未知", result.get("未知") + 1);
            } else if (genderCode == 1) {
                result.put("男", result.get("男") + 1);
            } else if (genderCode == 2) {
                result.put("女", result.get("女") + 1);
            } else {
                result.put("未知", result.get("未知") + 1);
            }
        }
        
        return result;
    }

    @Override
    public Map<String, Integer> getPatientRegionalDistribution(Long doctorId) {
        log.info("获取患者地区分布统计, doctorId: {}", doctorId);
        
        // 使用地区字段进行分组统计
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("未知", 0);
        
        // 获取该医生的患者ID列表
        List<Long> patientIds = getPatientIdsByDoctorId(doctorId);
        
        // 如果患者列表为空，直接返回初始化的结果
        if (patientIds == null || patientIds.isEmpty()) {
            log.info("医生 {} 没有相关的患者记录", doctorId);
            return result;
        }
        
        // 查询该医生的所有患者信息
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Patient::getPatientId, patientIds);
        List<Patient> patients = patientService.list(queryWrapper);
        
        // 按地区分组统计
        for (Patient patient : patients) {
            String region = patient.getRegion();
            
            if (region == null || region.isEmpty()) {
                // 如果region为空，尝试从address提取
                if (patient.getAddress() != null && !patient.getAddress().isEmpty()) {
                    region = extractRegion(patient.getAddress());
                } else {
                    region = "未知";
                }
            }
            
            // 更新计数
            result.put(region, result.getOrDefault(region, 0) + 1);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Integer> getDoctorWorkloadStatistics(Long doctorId, LocalDate startDate, LocalDate endDate) {
        log.info("获取医生工作量统计, doctorId: {}, startDate: {}, endDate: {}", doctorId, startDate, endDate);
        
        // 参数校验
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        // 查询时间范围内的该医生的所有诊断记录
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getDoctorId, doctorId)
                   .between(Diagnosis::getCreateTime, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        List<Diagnosis> diagnosisList = diagnosisService.list(queryWrapper);
        
        // 按日期分组统计
        Map<String, Integer> result = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // 分组统计每天的接诊次数
        diagnosisList.stream()
                .collect(Collectors.groupingBy(
                        diagnosis -> diagnosis.getCreateTime().toLocalDate().format(formatter),
                        Collectors.summingInt(diagnosis -> 1)
                ))
                .forEach(result::put);
        
        return result;
    }
    
    @Override
    public Map<String, Integer> getDepartmentWorkloadStatistics(Long doctorId, LocalDate startDate, LocalDate endDate) {
        log.info("获取科室工作量统计, doctorId: {}, startDate: {}, endDate: {}", doctorId, startDate, endDate);
        
        // 参数校验
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        // 获取医生所在的科室信息
        Doctor doctor = doctorService.getById(doctorId);
        if (doctor == null || doctor.getClinicId() == null) {
            return new HashMap<>();
        }
        
        Clinic clinic = clinicService.getById(doctor.getClinicId());
        if (clinic == null || clinic.getDeptId() == null) {
            return new HashMap<>();
        }
        
        Department dept = departmentService.getById(clinic.getDeptId());
        if (dept == null) {
            return new HashMap<>();
        }
        
        // 查询时间范围内的该医生的所有诊断记录
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getDoctorId, doctorId)
                   .between(Diagnosis::getCreateTime, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        List<Diagnosis> diagnosisList = diagnosisService.list(queryWrapper);
        
        // 按日期分组统计
        Map<String, Integer> result = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // 分组统计每天的接诊次数
        diagnosisList.stream()
                .collect(Collectors.groupingBy(
                        diagnosis -> diagnosis.getCreateTime().toLocalDate().format(formatter),
                        Collectors.summingInt(diagnosis -> 1)
                ))
                .forEach((date, count) -> {
                    result.put(date + " (" + dept.getDeptName() + ")", count);
                });
        
        return result;
    }
    
    /**
     * 从地址中提取地区信息（省/市）
     */
    private String extractRegion(String address) {
        if (address == null || address.isEmpty()) {
            return "未知";
        }
        
        // 简单地提取省份或城市
        String[] parts = address.split("[省市区县]");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            // 返回第一个匹配的省/市名称
            return parts[0] + (address.contains("省") ? "省" : address.contains("市") ? "市" : "");
        }
        
        // 如果无法提取，返回原始地址的前几个字符
        return address.length() > 5 ? address.substring(0, 5) + "..." : address;
    }
    
    /**
     * 填充缺失的时间点，确保返回的数据连续
     */
    private Map<String, Integer> fillMissingTimePoints(Map<String, Integer> data, LocalDate startDate, LocalDate endDate, String timeUnit) {
        Map<String, Integer> result = new TreeMap<>(); // 使用TreeMap确保按日期排序
        
        LocalDate current = startDate;
        DateTimeFormatter formatter;
        
        switch (timeUnit.toLowerCase()) {
            case "day":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                while (!current.isAfter(endDate)) {
                    String key = current.format(formatter);
                    result.put(key, data.getOrDefault(key, 0));
                    current = current.plusDays(1);
                }
                break;
            case "week":
                while (!current.isAfter(endDate)) {
                    int weekOfYear = current.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
                    String key = current.getYear() + "-W" + weekOfYear;
                    result.put(key, data.getOrDefault(key, 0));
                    current = current.plusWeeks(1);
                }
                break;
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                while (!current.isAfter(endDate)) {
                    String key = current.format(formatter);
                    result.put(key, data.getOrDefault(key, 0));
                    current = current.plusYears(1);
                }
                break;
            case "month":
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                while (!current.isAfter(endDate)) {
                    String key = current.format(formatter);
                    result.put(key, data.getOrDefault(key, 0));
                    current = current.plusMonths(1);
                }
                break;
        }
        
        return result;
    }
}
