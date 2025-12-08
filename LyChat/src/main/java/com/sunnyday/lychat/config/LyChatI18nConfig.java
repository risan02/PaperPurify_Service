package com.sunnyday.lychat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * LyChat模块国际化配置类
 * 用于支持中、日双语切换
 * 注意：设置为@Primary，确保优先使用我们的MessageSource，覆盖Spring Boot自动配置的MessageSource
 */
@Configuration
public class LyChatI18nConfig {
    
    /**
     * 配置国际化资源文件
     * 设置为@Primary，当存在多个MessageSource时优先使用此Bean
     * 这会覆盖Spring Boot自动配置的MessageSource
     */
    @Bean(name = "messageSource")
    @Primary
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true); // 如果找不到key，使用key作为默认消息
        messageSource.setCacheSeconds(3600); // 缓存1小时
        return messageSource;
    }
}

