package com.graduation.his.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.his.domain.po.Schedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 医生排班表 Mapper 接口
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

}
