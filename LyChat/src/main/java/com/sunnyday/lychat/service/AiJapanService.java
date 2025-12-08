package com.sunnyday.lychat.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
//        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider"//配置会话记忆提供者对象
)
public interface AiJapanService {
    // 注意：此方法使用固定的系统消息，主要用于向后兼容
    @SystemMessage(fromResource = "aiSystemPrompt.txt")
    String chat(@MemoryId String memoryId, @UserMessage String message);

    // 新增方法：不使用@SystemMessage，允许动态传递完整的提示词
    // 这样可以在用户消息中包含系统提示词和语言要求
    String chatWithoutSystemMessage(@MemoryId String memoryId, @UserMessage String fullPrompt);

    @SystemMessage(fromResource = "aiSystemPrompt.txt")
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String message, String fileContent);
}
