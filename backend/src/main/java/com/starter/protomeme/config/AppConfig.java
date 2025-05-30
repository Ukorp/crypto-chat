package com.starter.protomeme.config;

import com.starter.protomeme.chat.ChatMessage;
import com.starter.protomeme.chat.SessionInfo;
import io.grpc.stub.StreamObserver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
public class AppConfig {

    @Bean
    public ConcurrentMap<String, StreamObserver<ChatMessage>> streamMap() {
        return new ConcurrentHashMap<>();
    }
    @Bean
    public ConcurrentMap<String, SessionInfo> sessionMap() {
        return new ConcurrentHashMap<>();
    }
}
