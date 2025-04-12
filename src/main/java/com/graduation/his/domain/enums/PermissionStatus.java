package com.graduation.his.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PermissionStatus {
        PATIENT(0, "patient"),
        DOCTOR(1, "doctor"),
        ADMIN(2, "admin");

        @EnumValue
        private int value;
        private String desc;

        PermissionStatus(Integer value, String desc) {
            this.value = value;
            this.desc = desc;
        }


        // 静态方法：根据 value 获取枚举实例
        public static PermissionStatus of(int value) {
            for (PermissionStatus status : PermissionStatus.values()) {
                if (status.getValue() == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("账户状态错误: " + value);
        }
    }