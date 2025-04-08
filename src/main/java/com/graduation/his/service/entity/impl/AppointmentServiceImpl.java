package com.graduation.his.service.entity.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduation.his.domain.po.Appointment;
import com.graduation.his.mapper.AppointmentMapper;
import com.graduation.his.service.entity.IAppointmentService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 预约挂号表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements IAppointmentService {

}
