package com.graduation.his.service.entity;

import com.graduation.his.domain.po.Schedule;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 医生排班表 服务类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
public interface IScheduleService extends IService<Schedule> {

    /**
     * 根据医生ID和日期范围获取排班列表
     *
     * @param doctorId  医生ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 排班列表
     */
    List<Schedule> getSchedulesByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据门诊ID和日期范围获取排班列表
     *
     * @param clinicId  门诊ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 排班列表
     */
    List<Schedule> getSchedulesByClinicAndDateRange(Long clinicId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定日期范围内有可预约名额的排班
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 排班列表
     */
    List<Schedule> getAvailableSchedules(LocalDate startDate, LocalDate endDate);

    /**
     * 更新排班的已预约人数(增加)
     *
     * @param scheduleId 排班ID
     * @return 更新是否成功
     */
    boolean incrementCurrentPatients(Long scheduleId);

    /**
     * 更新排班的已预约人数(减少)
     *
     * @param scheduleId 排班ID
     * @return 更新是否成功
     */
    boolean decrementCurrentPatients(Long scheduleId);

}
