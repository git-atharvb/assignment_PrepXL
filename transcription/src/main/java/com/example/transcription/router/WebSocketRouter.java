package com.example.transcription.router;

import com.example.transcription.ws.AudioWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration
public class WebSocketRouter {

    @Bean
    public SimpleUrlHandlerMapping webSocketMapping(
            AudioWebSocketHandler audioWebSocketHandler) {

        return new SimpleUrlHandlerMapping(
                Map.of("/ws/transcribe", audioWebSocketHandler),
                1
        );
    }
}
