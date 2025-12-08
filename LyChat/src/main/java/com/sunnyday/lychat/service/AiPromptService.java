package com.sunnyday.lychat.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * AI提示词服务
 * 根据语言环境加载对应的提示词文件
 */
@Service
public class AiPromptService {

    /**
     * 根据语言环境获取系统提示词
     * 
     * @param locale 语言环境
     * @return 系统提示词内容
     */
    public String getSystemPrompt(Locale locale) {
        String resourceName;
        
        if (Locale.SIMPLIFIED_CHINESE.equals(locale)) {
            resourceName = "aiSystemPrompt_zh_CN.txt";
        } else {
            // 默认日文
            resourceName = "aiSystemPrompt_ja_JP.txt";
        }
        
        try {
            ClassPathResource resource = new ClassPathResource(resourceName);
            InputStream inputStream = resource.getInputStream();
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 如果读取失败，返回默认的日文提示词
            try {
                ClassPathResource defaultResource = new ClassPathResource("aiSystemPrompt.txt");
                InputStream inputStream = defaultResource.getInputStream();
                byte[] bytes = inputStream.readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new RuntimeException("无法加载AI提示词文件", ex);
            }
        }
    }
}

