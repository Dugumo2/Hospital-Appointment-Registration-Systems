package com.graduation.his.domain.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 排班查询条件封装类
 */
@Data
public class ScheduleQuery {
    /**
     * 科室ID
     */
    private Long deptId;
    
    /**
     * 门诊ID
     */
    private Long clinicId;
    
    /**
     * 医生ID
     */
    private Long doctorId;
    
    /**
     * 医生职称
     */
    private String title;
    
    /**
     * 开始日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
} 