package com.graduation.his.utils.minio;

import com.graduation.his.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * MinioUtils工具类测试
 */
@Slf4j
@SpringBootTest
public class MinioUtilsTest {

    @Autowired
    private MinioUtils minioUtils;

    /**
     * 初始化用户头像存储桶
     * 使用方法：在MinIO配置完成后，运行此测试方法创建用户头像存储桶
     */
    @Test
    @DisplayName("初始化用户头像存储桶")
    public void initUserAvatarBucket() {
        try {
            String bucketName = Constants.MinioConstants.USER_AVATAR_BUCKET;
            
            // 检查桶是否存在
            boolean exists = minioUtils.isBucketExists(bucketName);
            
            if (exists) {
                log.info("用户头像存储桶 [{}] 已存在，无需创建", bucketName);
            } else {
                // 创建桶并设置为公开只读
                minioUtils.createBucket(bucketName);
                log.info("用户头像存储桶 [{}] 创建成功", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化用户头像存储桶失败", e);
            throw new RuntimeException("初始化用户头像存储桶失败", e);
        }
    }
} 