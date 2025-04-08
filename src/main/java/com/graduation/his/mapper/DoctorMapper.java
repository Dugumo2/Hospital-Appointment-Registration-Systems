package com.graduation.his.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.his.domain.po.Doctor;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 医生信息表 Mapper 接口
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

}
