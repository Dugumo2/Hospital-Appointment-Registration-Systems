package com.graduation.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.graduation.his.common.Result;
import com.graduation.his.exception.BusinessException;
import com.graduation.his.service.business.IDataAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * @author hua
 * @description 数据分析视图控制器
 * @create 2025-03-30 16:13
 */
@SaCheckRole("doctor")
@Slf4j
@RestController
@RequestMapping("/data-analysis")
public class DataAnalysisController {
    
    @Autowired
    private IDataAnalysisService dataAnalysisService;
    
    /**
     * 获取患者就诊频次统计（按时间划分）
     * 
     * @param startDate 开始日期（可选，默认为6个月前）
     * @param endDate 结束日期（可选，默认为当前日期）
     * @param timeUnit 时间单位(day、week、month、year)（可选，默认为month）
     * @return 统计数据，键为时间点，值为就诊次数
     */
    @GetMapping("/patient-visit-frequency")
    public Result<Map<String, Integer>> getPatientVisitFrequency(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String timeUnit) {
        log.info("接收到获取患者就诊频次统计请求, startDate: {}, endDate: {}, timeUnit: {}", startDate, endDate, timeUnit);
        try {
            Map<String, Integer> data = dataAnalysisService.getPatientVisitFrequency(startDate, endDate, timeUnit);
            return Result.success("获取患者就诊频次统计成功", data);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取患者就诊频次统计业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取患者就诊频次统计异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取AI问诊使用频率统计
     * 
     * @param startDate 开始日期（可选，默认为6个月前）
     * @param endDate 结束日期（可选，默认为当前日期）
     * @param timeUnit 时间单位(day、week、month、year)（可选，默认为month）
     * @return 统计数据，键为时间点，值为使用次数
     */
    @GetMapping("/ai-consult-frequency")
    public Result<Map<String, Integer>> getAiConsultFrequency(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String timeUnit) {
        log.info("接收到获取AI问诊使用频率统计请求, startDate: {}, endDate: {}, timeUnit: {}", startDate, endDate, timeUnit);
        try {
            Map<String, Integer> data = dataAnalysisService.getAiConsultFrequency(startDate, endDate, timeUnit);
            return Result.success("获取AI问诊使用频率统计成功", data);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取AI问诊使用频率统计业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取AI问诊使用频率统计异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取患者年龄分布统计
     * 
     * @return 统计数据，键为年龄段（如"0-18"、"19-30"等），值为人数
     */
    @GetMapping("/patient-age-distribution")
    public Result<Map<String, Integer>> getPatientAgeDistribution() {
        log.info("接收到获取患者年龄分布统计请求");
        try {
            Map<String, Integer> data = dataAnalysisService.getPatientAgeDistribution();
            return Result.success("获取患者年龄分布统计成功", data);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取患者年龄分布统计业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取患者年龄分布统计异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取患者性别比例统计
     * 
     * @return 统计数据，键为性别（如"男"、"女"），值为人数
     */
    @GetMapping("/patient-gender-ratio")
    public Result<Map<String, Integer>> getPatientGenderRatio() {
        log.info("接收到获取患者性别比例统计请求");
        try {
            Map<String, Integer> data = dataAnalysisService.getPatientGenderRatio();
            return Result.success("获取患者性别比例统计成功", data);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取患者性别比例统计业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取患者性别比例统计异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取患者地区分布统计
     * 
     * @return 统计数据，键为地区，值为人数
     */
    @GetMapping("/patient-regional-distribution")
    public Result<Map<String, Integer>> getPatientRegionalDistribution() {
        log.info("接收到获取患者地区分布统计请求");
        try {
            Map<String, Integer> data = dataAnalysisService.getPatientRegionalDistribution();
            return Result.success("获取患者地区分布统计成功", data);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取患者地区分布统计业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取患者地区分布统计异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取医生工作量统计
     * 
     * @param startDate 开始日期（可选，默认为1个月前）
     * @param endDate 结束日期（可选，默认为当前日期）
     * @return 统计数据，键为医生姓名，值为接诊人数
     */
    @GetMapping("/doctor-workload")
    public Result<Map<String, Integer>> getDoctorWorkloadStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        log.info("接收到获取医生工作量统计请求, startDate: {}, endDate: {}", startDate, endDate);
        try {
            Map<String, Integer> data = dataAnalysisService.getDoctorWorkloadStatistics(startDate, endDate);
            return Result.success("获取医生工作量统计成功", data);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取医生工作量统计业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取医生工作量统计异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
    
    /**
     * 获取科室工作量统计
     * 
     * @param startDate 开始日期（可选，默认为1个月前）
     * @param endDate 结束日期（可选，默认为当前日期）
     * @return 统计数据，键为科室名称，值为接诊人数
     */
    @GetMapping("/department-workload")
    public Result<Map<String, Integer>> getDepartmentWorkloadStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        log.info("接收到获取科室工作量统计请求, startDate: {}, endDate: {}", startDate, endDate);
        try {
            Map<String, Integer> data = dataAnalysisService.getDepartmentWorkloadStatistics(startDate, endDate);
            return Result.success("获取科室工作量统计成功", data);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取科室工作量统计业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取科室工作量统计异常", e);
            return Result.error("服务异常，请稍后重试");
        }
    }
}
