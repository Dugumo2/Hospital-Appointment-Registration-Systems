package com.graduation.his.domain.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRequest {
    @JSONField(name = "messages")
    private List<Message> messages;
    
    @JSONField(name = "model")
    private String model = "deepseek-chat";
    
    @JSONField(name = "max_tokens")
    private Integer maxTokens = 2048;
    
    @JSONField(name = "response_format")
    private ResponseFormat responseFormat = new ResponseFormat();
    
    @JSONField(name = "temperature")
    private Double temperature = 0.3;
    
    @JSONField(name = "frequency_penalty")
    private Double frequencyPenalty = 0.5;
    
    @JSONField(name = "presence_penalty")
    private Double presencePenalty = 0.8;
    
    @JSONField(name = "top_p")
    private Double topP = 0.9;
    
    @JSONField(name = "stop")
    private String[] stop = new String[]{"请注意："};
    
    @JSONField(name = "stream")
    private Boolean stream = true;
} 