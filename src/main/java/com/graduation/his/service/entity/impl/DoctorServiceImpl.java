package com.graduation.his.service.entity.impl;

import com.graduation.his.domain.po.Doctor;
import com.graduation.his.mapper.DoctorMapper;
import com.graduation.his.service.entity.IDoctorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 医生信息表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements IDoctorService {

}
