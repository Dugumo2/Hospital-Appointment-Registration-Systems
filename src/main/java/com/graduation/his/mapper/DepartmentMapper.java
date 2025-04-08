package com.graduation.his.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.his.domain.po.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 医院科室表 Mapper 接口
 * </p>
 *
 * @author hua
 * @since 2025-04-02
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

}
