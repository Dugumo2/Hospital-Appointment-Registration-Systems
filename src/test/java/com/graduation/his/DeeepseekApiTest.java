package com.graduation.his;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.graduation.his.domain.dto.AIRequest;
import com.graduation.his.domain.dto.Message;
import com.graduation.his.domain.dto.ResponseFormat;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DeeepseekApiTest {

    public static void main(String[] args) {
//        testMultiRoundConversation();
        testMultiRoundConversationByFlux();
    }

    public static void testMultiRoundConversation() {
        // 创建HttpClient实例，设置超时时间为3分钟
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 180000) // 连接超时3分钟
                .responseTimeout(Duration.ofMinutes(3)) // 响应超时3分钟
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(180, TimeUnit.SECONDS)) // 读取超时3分钟
                                .addHandlerLast(new WriteTimeoutHandler(180, TimeUnit.SECONDS))); // 写入超时3分钟

        // 创建WebClient实例
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://api.deepseek.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer sk-9487d59d27ce471091b8a7ab03d5d5ac") // 替换为实际的API密钥
                .build();

        // 维护完整的对话历史
        List<Message> messages = new ArrayList<>();

        // 添加系统角色消息（仅在对话开始时添加一次）
        messages.add(createSystemMessage());

        // 创建响应格式
        ResponseFormat responseFormat = new ResponseFormat();
        responseFormat.setType("text");

        Scanner scanner = new Scanner(System.in);
        String userInput;

        log.info("欢迎使用DeepSeek AI对话系统，输入'退出'可结束对话");

        // 开始多轮对话循环
        while (true) {
            log.info("请输入您的问题: ");
            userInput = scanner.nextLine().trim();

            // 检查是否退出
            if ("退出".equals(userInput) || "exit".equalsIgnoreCase(userInput)) {
                log.info("对话结束，谢谢使用！");
                break;
            }

            // 添加用户消息到历史记录
            Message userMessage = com.graduation.his.domain.dto.Message.builder()
                    .role("user")
                    .content(userInput)
                    .build();
            messages.add(userMessage);

            // 构建完整请求体
            AIRequest requestBody = AIRequest.builder()
                    .messages(messages)
                    .model("deepseek-chat")
                    .maxTokens(2048)
                    .temperature(0.3)
                    .frequencyPenalty(0.5)
                    .presencePenalty(0.8)
                    .topP(0.9)
                    .stop(new String[]{"请注意："})
                    .stream(false) // 多轮对话中使用非流式响应
                    .responseFormat(responseFormat)
                    .build();

            try {
                // 发送请求并获取响应
                String responseJson = webClient.post()
                        .uri("/chat/completions")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                log.info("原始响应: {}", responseJson);

                // 解析响应JSON
                JSONObject jsonResponse = JSON.parseObject(responseJson);

                // 提取AI助手的回复内容 (根据DeepSeek API的响应格式)
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    String aiResponse = message.getString("content");

                    log.info("AI响应: {}", aiResponse);

                    // 创建AI助手的回复消息并添加到历史记录
                    Message assistantMessage = com.graduation.his.domain.dto.Message.builder()
                            .role("assistant")
                            .content(aiResponse)
                            .build();
                    messages.add(assistantMessage);
                } else {
                    log.error("无法从响应中提取AI回复");
                }

            } catch (Exception e) {
                log.error("请求异常: ", e);
                log.error("异常详情: {}", e.getMessage());
            }
        }

        scanner.close();
    }

    private static Message createSystemMessage() {
        String system_content = """
角色设定：
- 您是一位拥有执业资格的 AI 全科医生
- 必须遵循《人工智能医疗助手伦理准则》

行为规范：
1. 问诊流程：症状确认 → 初步建议 → 就医指引
2. 必须包含风险提示语句
3. 禁用药物剂量建议

输出格式：
【症状分析】...
【初步判断】...
【就医建议】...
""";
        // 创建系统消息
        return Message.builder()
                .role("system")
                .content(system_content)
                .build();
    }

    private static AIRequest createRequestBody() {
        // 创建系统消息
        Message systemMessage = createSystemMessage();

        // 创建用户消息
        Message userMessage = Message.builder()
                .role("user")
                .content("Hi")
                .build();

        // 创建响应格式
        ResponseFormat responseFormat = new ResponseFormat();
        responseFormat.setType("text");

        // 创建请求对象
        return AIRequest.builder()
                .messages(Arrays.asList(systemMessage, userMessage))
                .model("deepseek-chat")
                .maxTokens(2048)
                .temperature(0.3)
                .frequencyPenalty(0.5)
                .presencePenalty(0.8)
                .topP(0.9)
                .stop(new String[]{"请注意："})
                .stream(false) // 设置为false以获取完整响应
                .responseFormat(responseFormat)
                .build();
    }

    public static void testMultiRoundConversationByFlux() {
        // HttpClient配置保持不变
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 180000)
                .responseTimeout(Duration.ofMinutes(3))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(180, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(180, TimeUnit.SECONDS)));

        // WebClient配置保持不变
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://api.deepseek.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer sk-9487d59d27ce471091b8a7ab03d5d5ac")
                .build();

        // 维护完整的对话历史
        List<Message> messages = new ArrayList<>();
        messages.add(createSystemMessage());

        // 创建响应格式
        ResponseFormat responseFormat = new ResponseFormat();
        responseFormat.setType("text");

        Scanner scanner = new Scanner(System.in);
        String userInput;

        log.info("欢迎使用DeepSeek AI对话系统（流式响应版），输入'退出'可结束对话");

        // 开始多轮对话循环
        while (true) {
            log.info("请输入您的问题: ");
            userInput = scanner.nextLine().trim();

            // 检查是否退出
            if ("退出".equals(userInput) || "exit".equalsIgnoreCase(userInput)) {
                log.info("对话结束，谢谢使用！");
                break;
            }

            // 添加用户消息到历史记录
            Message userMessage = Message.builder()
                    .role("user")
                    .content(userInput)
                    .build();
            messages.add(userMessage);

            // 构建请求体 - 修改stream参数为true以启用流式响应
            AIRequest requestBody = AIRequest.builder()
                    .messages(messages)
                    .model("deepseek-chat")
                    .maxTokens(2048)
                    .temperature(0.3)
                    .frequencyPenalty(0.5)
                    .presencePenalty(0.8)
                    .topP(0.9)
                    .stop(new String[]{"请注意："})
                    .stream(true) // 关键修改：启用流式响应
                    .responseFormat(responseFormat)
                    .build();

            try {
                // 使用StringBuilder收集完整的响应内容
                StringBuilder fullResponseContent = new StringBuilder();

                // 使用两个CountDownLatch分别控制数据接收完成和处理完成
                CountDownLatch receiveCompleteLatch = new CountDownLatch(1);
                CountDownLatch processingCompleteLatch = new CountDownLatch(1);

                // 跟踪是否发生错误
                AtomicBoolean hasError = new AtomicBoolean(false);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();

                // 使用一个标志表示是否正在接收数据
                AtomicBoolean isReceiving = new AtomicBoolean(true);
                // 上次接收到数据的时间
                AtomicLong lastDataReceivedTime = new AtomicLong(System.currentTimeMillis());

                // 使用exchangeToFlux访问原始响应并处理流式数据
                webClient.post()
                        .uri("/chat/completions")
                        .bodyValue(requestBody)
                        .retrieve()
                        // 使用bodyToFlux处理流式响应，每个数据块都是一个字符串
                        .bodyToFlux(String.class)
                        // 订阅流式响应
                        .subscribe(
                                // 处理每个数据块
                                chunk -> {
                                    // 更新最后数据接收时间
                                    lastDataReceivedTime.set(System.currentTimeMillis());

                                    // 处理数据块（通常以"data: "开头）
                                    String dataContent = chunk;

                                    // 如果是流式SSE格式，需要去掉前缀
                                    if (chunk.startsWith("data: ")) {
                                        dataContent = chunk.substring(6);
                                    }

                                    // 忽略心跳消息（通常是空行或[DONE]）
                                    if (dataContent.trim().isEmpty() || "[DONE]".equals(dataContent.trim())) {
                                        return;
                                    }

                                    try {
                                        // 解析JSON响应块
                                        JSONObject jsonChunk = JSON.parseObject(dataContent);
                                        JSONArray choices = jsonChunk.getJSONArray("choices");

                                        if (choices != null && !choices.isEmpty()) {
                                            JSONObject choice = choices.getJSONObject(0);
                                            JSONObject delta = choice.getJSONObject("delta");

                                            // 提取内容增量
                                            if (delta != null && delta.containsKey("content")) {
                                                String contentDelta = delta.getString("content");
                                                // 实时输出内容增量（模拟打字效果）
                                                System.out.print(contentDelta);
                                                // 添加到完整响应
                                                fullResponseContent.append(contentDelta);
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.error("解析流式数据块错误: {}", dataContent, e);
                                    }
                                },
                                // 处理错误
                                error -> {
                                    log.error("流式响应出错: ", error);
                                    hasError.set(true);
                                    errorRef.set(error);
                                    isReceiving.set(false);
                                    receiveCompleteLatch.countDown(); // 出错时减少接收计数
                                    processingCompleteLatch.countDown(); // 出错时也减少处理计数
                                },
                                // 完成处理（流结束信号）
                                () -> {
                                    System.out.println(); // 换行
                                    log.info("流式响应接收完成");
                                    isReceiving.set(false);
                                    receiveCompleteLatch.countDown(); // 流结束，减少接收计数

                                    // 获取完整的响应内容
                                    String aiResponse = fullResponseContent.toString();

                                    // 创建AI助手的回复消息并添加到历史记录
                                    Message assistantMessage = Message.builder()
                                            .role("assistant")
                                            .content(aiResponse)
                                            .build();
                                    messages.add(assistantMessage);

                                    // 在这里再等待一些时间，确保所有控制台输出已完成
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }

                                    // 最后再减少处理计数，表示全部处理完成
                                    processingCompleteLatch.countDown();
                                }
                        );

                // 等待流式响应数据接收完成
                try {
                    // 首先等待流结束信号
                    boolean receiveCompleted = receiveCompleteLatch.await(2, TimeUnit.MINUTES);

                    if (!receiveCompleted) {
                        log.warn("等待流式数据接收超时");
                        isReceiving.set(false);
                    }

                    // 如果流已结束，但还有剩余数据处理，再等待一会确保处理完成
                    if (receiveCompleted) {
                        // 添加额外的等待时间，确保控制台输出完全展示
                        // 等待处理完成
                        boolean processingCompleted = processingCompleteLatch.await(10, TimeUnit.SECONDS);

                        if (!processingCompleted) {
                            log.warn("等待处理完成超时");
                        }
                    }

                    // 如果发生了错误，记录错误
                    if (hasError.get()) {
                        log.error("流式响应处理过程中发生错误", errorRef.get());
                    }

                    // 在继续之前确保响应都已经打印完毕
                    System.out.println(); // 额外打印一个换行，确保分隔

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("等待响应时被中断", e);
                }

            } catch (Exception e) {
                log.error("请求异常: ", e);
            }
        }

        scanner.close();
    }
} 