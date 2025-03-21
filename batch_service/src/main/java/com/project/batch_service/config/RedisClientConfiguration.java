package com.project.batch_service.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisClientConfiguration {
    @Bean
    public RedisClient redisClient() {
        return RedisClient.create("redis://localhost:6379");
    }
}
