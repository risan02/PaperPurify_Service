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
    @SystemMessage(fromResource = "aiSystemPrompt.txt")
    String chat(@MemoryId String memoryId, @UserMessage String message);

    @SystemMessage(fromResource = "aiSystemPrompt.txt")
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String message, String fileContent);
}
