package com.cirf.dashboard.global.config;

import com.cirf.dashboard.domain.collect.dto.ProgressEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<Integer, ProgressEvent> progressEventCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)  // 진행 중인 작업만 캐시 (완료 시 삭제됨)
                .maximumSize(10000)
                .build();
    }
}
