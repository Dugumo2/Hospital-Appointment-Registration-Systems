package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 科室信息VO，用于接口返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentVO {
    
    /**
     * 科室ID
     */
    private Long deptId;
    
    /**
     * 科室名称
     */
    private String deptName;
    
    /**
     * 科室编码
     */
    private String deptCode;
    
    /**
     * 科室类型(0-内科,1-外科,2-妇产科,3-儿科,4-五官科,5-其他)
     */
    private Integer deptType;
    
    /**
     * 科室类型名称
     */
    private String deptTypeName;
    
    /**
     * 科室描述
     */
    private String description;
    
    /**
     * 是否有效(0-无效,1-有效)
     */
    private Integer isActive;
    
    /**
     * 医生数量
     */
    private Integer doctorCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 