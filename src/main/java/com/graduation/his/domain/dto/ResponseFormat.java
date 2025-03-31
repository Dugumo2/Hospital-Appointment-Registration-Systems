package com.graduation.his.domain.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class ResponseFormat {
    @JSONField(name = "type")
    private String type = "text";
} 