package com.graduation.his.config;

import com.graduation.his.service.business.IAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

/**
 * 排班定时任务配置
 */
@Slf4j
@Configuration
@EnableScheduling
public class ScheduleTaskConfig {

    @Autowired
    private IAdminService adminService;

    /**
     * 每两周执行一次排班（每两周的周日凌晨2点执行）
     * cron表达式 "0 0 2 ? * SUN/14" 表示从现在开始每两周的周日凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 ? * SUN/14")
    public void scheduleBiweekly() {
        log.info("开始执行两周排班定时任务");
        
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 计算未来两周的开始日期和结束日期
        LocalDate startDate = today;
        LocalDate endDate = today.plusDays(13); // 两周是14天
        
        try {
            // 检查是否已经有排班数据
            Map<LocalDate, Boolean> scheduleStatus = adminService.getScheduleStatus(startDate, endDate);
            
            // 找出还没有排班的日期范围
            LocalDate actualStartDate = null;
            LocalDate actualEndDate = null;
            
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                boolean hasSchedule = scheduleStatus.getOrDefault(date, false);
                
                if (!hasSchedule) {
                    if (actualStartDate == null) {
                        actualStartDate = date;
                    }
                    actualEndDate = date;
                }
            }
            
            // 如果有需要排班的日期范围，则执行排班
            if (actualStartDate != null && actualEndDate != null) {
                log.info("需要排班的日期范围: {} 至 {}", actualStartDate, actualEndDate);
                boolean success = adminService.executeAutoSchedule(actualStartDate, actualEndDate, null);
                log.info("两周排班定时任务执行 {}", success ? "成功" : "失败");
            } else {
                log.info("无需排班，日期范围内已有排班数据");
            }
        } catch (Exception e) {
            log.error("两周排班定时任务执行异常", e);
        }
    }
    
    /**
     * 每天凌晨1点检查未来第7天是否已排班
     * cron表达式 "0 0 1 * * ?" 表示每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkNextSeventhDay() {
        log.info("开始执行检查第7天排班定时任务");
        
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 计算第7天的日期
        LocalDate seventhDay = today.plusDays(6);
        
        try {
            // 检查第7天是否已有排班
            Map<LocalDate, Boolean> scheduleStatus = adminService.getScheduleStatus(seventhDay, seventhDay);
            boolean hasSchedule = scheduleStatus.getOrDefault(seventhDay, false);
            
            if (!hasSchedule) {
                log.info("第7天 {} 尚未排班，执行单日排班", seventhDay);
                boolean success = adminService.executeAutoSchedule(seventhDay, seventhDay, null);
                log.info("第7天排班定时任务执行 {}", success ? "成功" : "失败");
            } else {
                log.info("第7天 {} 已有排班数据，无需执行排班", seventhDay);
            }
        } catch (Exception e) {
            log.error("检查第7天排班定时任务执行异常", e);
        }
    }
} 