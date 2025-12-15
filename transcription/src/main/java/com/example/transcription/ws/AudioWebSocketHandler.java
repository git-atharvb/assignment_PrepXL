package com.example.transcription.ws;

import com.example.transcription.gemini.GeminiStreamingService;
import com.example.transcription.gemini.GeminiStreamingService.GeminiSession;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

@Component
public class AudioWebSocketHandler implements WebSocketHandler {

    private final GeminiStreamingService geminiService;

    public AudioWebSocketHandler(GeminiStreamingService geminiService) {
        this.geminiService = geminiService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        System.out.println("WebSocket connected");

        // 🔑 One Gemini session per WebSocket
        GeminiSession gemini = geminiService.openSession();

        // 1️⃣ Receive audio chunks
        Flux<Void> audioIn =
                session.receive()
                        .filter(msg -> msg.getType() == WebSocketMessage.Type.BINARY)
                        .map(WebSocketMessage::getPayload)
                        .map(dataBuffer -> {
                            ByteBuffer buffer = dataBuffer.asByteBuffer();
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            return bytes;
                        })
                        .doOnNext(bytes -> {
                            System.out.println("Forwarding audio: " + bytes.length);
                            gemini.sendAudio(bytes);
                        })
                        .thenMany(Flux.empty());

        // 2️⃣ Send partial transcripts back
        Flux<WebSocketMessage> transcriptOut =
                gemini.transcriptStream()
                        .map(text -> {
                            System.out.println("Sending transcript: " + text);
                            return session.textMessage(text);
                        });

        // 3️⃣ Cleanup on disconnect
        return session.send(transcriptOut)
                .and(audioIn)
                .doFinally(signal -> {
                    System.out.println("Client disconnected: " + signal);
                    gemini.close();
                });
    }
}
