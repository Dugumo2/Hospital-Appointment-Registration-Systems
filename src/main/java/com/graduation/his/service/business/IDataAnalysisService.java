package com.graduation.his.service.business;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author hua
 * @description 数据可视化接口
 * @create 2025-04-12 20:50
 */
public interface IDataAnalysisService {
    
    /**
     * 获取患者就诊频次统计（按时间划分）
     * @param doctorId 医生ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param timeUnit 时间单位(day、week、month、year)
     * @return 统计数据，键为时间点，值为就诊次数
     */
    Map<String, Integer> getPatientVisitFrequency(Long doctorId, LocalDate startDate, LocalDate endDate, String timeUnit);
    
    /**
     * 获取AI问诊使用频率统计
     * @param doctorId 医生ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param timeUnit 时间单位(day、week、month、year)
     * @return 统计数据，键为时间点，值为使用次数
     */
    Map<String, Integer> getAiConsultFrequency(Long doctorId, LocalDate startDate, LocalDate endDate, String timeUnit);
    
    /**
     * 获取患者年龄分布统计
     * @param doctorId 医生ID
     * @return 统计数据，键为年龄段（如"0-18"、"19-30"等），值为人数
     */
    Map<String, Integer> getPatientAgeDistribution(Long doctorId);
    
    /**
     * 获取患者性别比例统计
     * @param doctorId 医生ID
     * @return 统计数据，键为性别（如"男"、"女"），值为人数
     */
    Map<String, Integer> getPatientGenderRatio(Long doctorId);
    
    /**
     * 获取患者地区分布统计
     * @param doctorId 医生ID
     * @return 统计数据，键为地区，值为人数
     */
    Map<String, Integer> getPatientRegionalDistribution(Long doctorId);
    
    /**
     * 获取医生工作量统计（按照医生统计接诊人数）
     * @param doctorId 医生ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据，键为医生姓名，值为接诊人数
     */
    Map<String, Integer> getDoctorWorkloadStatistics(Long doctorId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取科室工作量统计
     * @param doctorId 医生ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据，键为科室名称，值为接诊人数
     */
    Map<String, Integer> getDepartmentWorkloadStatistics(Long doctorId, LocalDate startDate, LocalDate endDate);
}
