package com.graduation.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.graduation.his.common.Result;
import com.graduation.his.domain.dto.FeedbackMessageDTO;
import com.graduation.his.domain.dto.DiagnosisDTO;
import com.graduation.his.domain.po.User;
import com.graduation.his.domain.vo.DiagnosisVO;
import com.graduation.his.exception.BusinessException;
import com.graduation.his.service.business.IMedicalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author hua
 * @description 医疗服务模块控制器
 * @create 2025-03-30 16:12
 */
@Slf4j
@RestController
@RequestMapping("/medical")
public class MedicalServiceController {

    @Autowired
    private IMedicalService medicalService;
    
    /**
     * 获取患者的诊断记录列表
     * 
     * @param patientId 患者ID
     * @return 诊断记录列表
     */
    @SaCheckRole("patient")
    @GetMapping("/patient/{patientId}/diagnoses")
    public Result<List<DiagnosisVO>> getPatientDiagnoses(@PathVariable Long patientId) {
        log.info("获取患者的诊断记录列表, patientId: {}", patientId);
        try {
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();
            
            // 验证是否为管理员或当前患者
            if (user.getRole() != 2 && !medicalService.isCurrentPatient(patientId)) {
                return Result.error("无权访问该患者的诊断记录");
            }
            
            List<DiagnosisVO> diagnoses = medicalService.getPatientDiagnoses(patientId);
            return Result.success("获取患者诊断记录成功", diagnoses);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取患者诊断记录业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取患者诊断记录异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }
    
    /**
     * 获取医生的诊断记录列表
     * 
     * @param doctorId 医生ID
     * @return 诊断记录列表
     */
    @SaCheckRole("doctor")
    @GetMapping("/doctor/{doctorId}/diagnoses")
    public Result<List<DiagnosisVO>> getDoctorDiagnoses(@PathVariable Long doctorId) {
        log.info("获取医生的诊断记录列表, doctorId: {}", doctorId);
        try {
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();
            
            // 验证是否为管理员或当前医生
            if (user.getRole() != 2 && !medicalService.isCurrentDoctor(doctorId)) {
                return Result.error("无权访问该医生的诊断记录");
            }
            
            List<DiagnosisVO> diagnoses = medicalService.getDoctorDiagnoses(doctorId);
            return Result.success("获取医生诊断记录成功", diagnoses);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取医生诊断记录业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取医生诊断记录异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }
    
    /**
     * 获取诊断详情
     * 
     * @param diagId 诊断ID
     * @return 诊断详情
     */
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    @GetMapping("/diagnoses/{diagId}")
    public Result<DiagnosisVO> getDiagnosisDetail(@PathVariable Long diagId) {
        log.info("获取诊断详情, diagId: {}", diagId);
        try {
            DiagnosisVO diagnosis = medicalService.getDiagnosisDetail(diagId);
            
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();
            
            // 验证是否为管理员、当前患者或当前医生
            if (user.getRole() != 2 && !medicalService.isCurrentPatient(diagnosis.getPatientId()) && !medicalService.isCurrentDoctor(diagnosis.getDoctorId())) {
                return Result.error("无权访问该诊断记录");
            }
            
            return Result.success("获取诊断详情成功", diagnosis);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取诊断详情业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取诊断详情异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }
    
    /**
     * 获取诊断相关的所有反馈消息
     * 
     * @param diagId 诊断ID
     * @return 反馈消息列表
     */
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    @GetMapping("/diagnoses/{diagId}/feedback")
    public Result<List<FeedbackMessageDTO>> getFeedbackMessages(@PathVariable Long diagId) {
        log.info("获取诊断相关的所有反馈消息, diagId: {}", diagId);
        try {
            // 获取诊断详情
            DiagnosisVO diagnosis = medicalService.getDiagnosisDetail(diagId);
            
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();
            
            // 验证是否为管理员、当前患者或当前医生
            if (user.getRole() != 2 && !medicalService.isCurrentPatient(diagnosis.getPatientId()) && !medicalService.isCurrentDoctor(diagnosis.getDoctorId())) {
                return Result.error("无权访问该诊断记录的反馈消息");
            }
            
            // 如果是患者或医生，标记所有消息为已读
            if (medicalService.isCurrentPatient(diagnosis.getPatientId()) || medicalService.isCurrentDoctor(diagnosis.getDoctorId())) {
                Long entityId = user.getRole() == 0 ? diagnosis.getPatientId() : diagnosis.getDoctorId();
                medicalService.markAllMessagesAsRead(diagId, entityId, user.getRole());
            }
            
            List<FeedbackMessageDTO> messages = medicalService.getFeedbackMessages(diagId);
            return Result.success("获取反馈消息成功", messages);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取反馈消息业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取反馈消息异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }
    
    /**
     * 发送诊后反馈消息
     * 
     * @param diagId 诊断ID
     * @param content 消息内容
     * @return 反馈消息
     */
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    @PostMapping("/diagnoses/{diagId}/feedback")
    public Result<FeedbackMessageDTO> sendFeedbackMessage(@PathVariable Long diagId, @RequestParam String content) {
        log.info("发送诊后反馈消息, diagId: {}, content: {}", diagId, content);
        try {
            // 获取诊断详情
            DiagnosisVO diagnosis = medicalService.getDiagnosisDetail(diagId);
            
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();
            
            // 确定发送者类型和ID
            Integer senderType = null;
            Long senderId = null;
            
            if (user.getRole() == 0 && medicalService.isCurrentPatient(diagnosis.getPatientId())) {
                // 患者发送
                senderType = 0;
                senderId = diagnosis.getPatientId();
                
                // 检查是否在反馈期内(15天)
                if (!medicalService.canFeedback(diagId)) {
                    return Result.error("已超出反馈期限(15天)");
                }
            } else if (user.getRole() == 1 && medicalService.isCurrentDoctor(diagnosis.getDoctorId())) {
                // 医生发送
                senderType = 1;
                senderId = diagnosis.getDoctorId();
            } else {
                return Result.error("无权发送反馈消息");
            }
            
            FeedbackMessageDTO message = medicalService.sendFeedbackMessage(diagId, content, senderType, senderId);
            return Result.success("发送反馈消息成功", message);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("发送反馈消息业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("发送反馈消息异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }
    
    /**
     * 获取未读消息数量映射
     * 
     * @return 诊断ID -> 未读消息数量 的映射
     */
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    @GetMapping("/feedback/unread/counts")
    public Result<Map<String, Integer>> getUnreadMessageCounts() {
        try {
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();
            
            // 根据用户角色获取不同类型的ID
            Long entityId = null;
            if (user.getRole() == 0) {
                // 患者
                entityId = medicalService.getPatientIdByUserId(user.getId());
            } else if (user.getRole() == 1) {
                // 医生
                entityId = medicalService.getDoctorIdByUserId(user.getId());
            } else {
                return Result.error("未知错误，请联系管理员");
            }
            
            // 获取所有诊断的未读消息数量映射
            Map<String, Integer> counts = medicalService.getAllUnreadMessageCounts(entityId, user.getRole());
            return Result.success("获取未读消息数量成功", counts);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取未读消息数量业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取未读消息数量异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }
    
    /**
     * 标记诊断相关的所有消息为已读
     * 
     * @param diagId 诊断ID
     * @return 是否成功
     */
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    @PostMapping("/diagnoses/{diagId}/feedback/read")
    public Result<Boolean> markMessagesAsRead(@PathVariable Long diagId) {
        log.info("标记诊断相关的所有消息为已读, diagId: {}", diagId);
        try {
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();
            
            // 获取诊断详情
            DiagnosisVO diagnosis = medicalService.getDiagnosisDetail(diagId);
            
            // 验证是否为管理员、当前患者或当前医生
            if (user.getRole() != 2 && !medicalService.isCurrentPatient(diagnosis.getPatientId()) && !medicalService.isCurrentDoctor(diagnosis.getDoctorId())) {
                return Result.error("无权访问该诊断记录的消息");
            }
            
            // 根据用户角色获取不同类型的ID
            Long entityId = null;
            if (user.getRole() == 0) {
                // 患者
                entityId = diagnosis.getPatientId();
            } else if (user.getRole() == 1) {
                // 医生
                entityId = diagnosis.getDoctorId();
            }
            
            boolean result = medicalService.markAllMessagesAsRead(diagId, entityId, user.getRole());
            return Result.success(result ? "标记成功" : "标记失败", result);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("标记消息为已读业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("标记消息为已读异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }

    /**
     * 创建诊断记录
     * 
     * @param dto 诊断记录DTO
     * @return 诊断记录详情
     */
    @SaCheckRole("doctor")
    @PostMapping("/diagnoses")
    public Result<DiagnosisVO> createDiagnosis(@RequestBody DiagnosisDTO dto) {
        log.info("接收到创建诊断记录请求");
        try {
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();
            
            // 验证当前医生身份
            if (!medicalService.isCurrentDoctor(dto.getDoctorId())) {
                return Result.error("无权创建该诊断记录");
            }
            
            DiagnosisVO diagnosis = medicalService.createDiagnosis(dto);
            return Result.success("创建诊断记录成功", diagnosis);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("创建诊断记录业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建诊断记录异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }

    /**
     * 根据预约ID获取诊断记录
     * 
     * @param appointmentId 预约ID
     * @return 诊断记录详情，如果不存在则返回null
     */
    @SaCheckRole(value = {"doctor", "patient"},mode = SaMode.OR)
    @GetMapping("/appointment/{appointmentId}/diagnosis")
    public Result<DiagnosisVO> getDiagnosisByAppointmentId(@PathVariable Long appointmentId) {
        log.info("接收到根据预约ID获取诊断记录请求, appointmentId: {}", appointmentId);
        try {
            DiagnosisVO diagnosis = medicalService.getDiagnosisByAppointmentId(appointmentId);
            
            if (diagnosis == null) {
                return Result.success("该预约尚未有诊断记录", null);
            }
            
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();
            
            // 验证是否为管理员、当前患者或当前医生
            if (user.getRole() != 2 && !medicalService.isCurrentPatient(diagnosis.getPatientId()) && !medicalService.isCurrentDoctor(diagnosis.getDoctorId())) {
                return Result.error("无权访问该诊断记录");
            }
            
            return Result.success("获取诊断记录成功", diagnosis);
        } catch (BusinessException e) {
            // 业务异常直接抛出（由全局异常处理器处理）
            log.error("获取诊断记录业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取诊断记录异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }
}
