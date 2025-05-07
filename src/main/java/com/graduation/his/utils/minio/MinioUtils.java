package com.graduation.his.utils.minio;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO工具类，用于处理用户头像的上传和管理
 */
@Component
@Slf4j
public class MinioUtils {
    @Autowired
    private MinioClient minioClient;

    /**
     * 检查桶是否存在
     * @param bucketName 桶名称
     * @return 是否存在
     */
    public boolean isBucketExists(String bucketName) throws Exception {
        boolean isBucketExists = minioClient.bucketExists(BucketExistsArgs
                .builder()
                .bucket(bucketName)
                .build());
        return isBucketExists;
    }

    /**
     * 创建一个新桶，并设置为公开只读
     * @param bucketName 桶名称
     */
    public void createBucket(String bucketName) throws Exception {
        // 检查桶是否存在，不存在则创建
        if (!isBucketExists(bucketName)) {
            // 创建bucket
            minioClient.makeBucket(MakeBucketArgs
                    .builder()
                    .bucket(bucketName)
                    .build());

            // 设置bucket策略为公开只读
            String policyJsonString = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"PublicRead\",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"*\"},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}";

            minioClient.setBucketPolicy(SetBucketPolicyArgs
                    .builder()
                    .bucket(bucketName)
                    .config(policyJsonString)
                    .build());
            
            log.info("桶 {} 创建成功并设置为公开只读", bucketName);
        } else {
            log.info("桶 {} 已存在", bucketName);
        }
    }

    /**
     * 上传用户头像
     * @param bucketName 桶名称
     * @param file 用户上传的头像文件
     * @return 文件的访问URL
     */
    public String uploadAvatar(String bucketName, MultipartFile file) throws Exception {
        // 检查桶是否存在，不存在则创建
        if (!isBucketExists(bucketName)) {
            createBucket(bucketName);
        }
        
        // 生成唯一的文件名（使用UUID）
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = "avatar/" + UUID.randomUUID().toString() + extension;
        
        // 上传文件到MinIO
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        
        // 返回文件的URL
        return getFileUrl(bucketName, objectName, 7, TimeUnit.DAYS);
    }
    
    /**
     * 更新用户头像
     * @param bucketName 桶名称
     * @param file 新的头像文件
     * @param oldObjectUrl 旧头像的URL（可能为空）
     * @return 新头像的URL
     */
    public String updateAvatar(String bucketName, MultipartFile file, String oldObjectUrl) throws Exception {
        // 如果存在旧头像，则删除
        if (oldObjectUrl != null && !oldObjectUrl.isEmpty()) {
            String oldObjectName = extractObjectNameFromUrl(oldObjectUrl);
            if (oldObjectName != null) {
                try {
                    removeFile(bucketName, oldObjectName);
                } catch (Exception e) {
                    log.warn("删除旧头像失败: {}", oldObjectName, e);
                    // 继续执行，不阻止上传新头像
                }
            }
        }
        
        // 上传新头像
        return uploadAvatar(bucketName, file);
    }

    /**
     * 从URL中提取对象名称
     * @param url MinIO对象URL
     * @return 对象名称
     */
    public String extractObjectNameFromUrl(String url) {
        // 示例URL: http://minio-server:9000/avatars/avatar/xxx.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256...
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // 找到桶名后的路径部分
            int bucketEndIndex = url.indexOf("/", url.indexOf("//") + 2);
            if (bucketEndIndex > 0) {
                int nextSlash = url.indexOf("/", bucketEndIndex + 1);
                if (nextSlash > 0) {
                    // 查找查询参数开始的位置
                    int queryIndex = url.indexOf("?", nextSlash);
                    if (queryIndex > 0) {
                        // 返回不包含查询参数的对象名称
                        return url.substring(nextSlash + 1, queryIndex);
                    } else {
                        // 没有查询参数
                        return url.substring(nextSlash + 1);
                    }
                }
            }
        } catch (Exception e) {
            log.error("从URL提取对象名称失败: {}", url, e);
        }
        return null;
    }

    /**
     * 获取文件URL
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param duration 有效期时长
     * @param unit 时间单位
     * @return 文件URL
     */
    public String getFileUrl(String bucketName, String objectName, int duration, TimeUnit unit) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(duration, unit)
                        .build());
    }

    /**
     * 获取文件URL（默认7天有效期）
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 文件URL
     */
    public String getFileUrl(String bucketName, String objectName) throws Exception {
        return getFileUrl(bucketName, objectName, 7, TimeUnit.DAYS);
    }

    /**
     * 删除文件
     * @param bucketName 桶名称
     * @param objectName 对象名称
     */
    public void removeFile(String bucketName, String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs
                .builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
        log.info("文件删除成功: {}/{}", bucketName, objectName);
    }

    /**
     * 检查文件是否存在
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 是否存在
     */
    public boolean isFileExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
