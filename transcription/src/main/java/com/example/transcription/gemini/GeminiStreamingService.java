package com.example.transcription.gemini;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GeminiStreamingService {

    private final WebClient webClient;

    public GeminiStreamingService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Authorization", "Bearer YOUR_GEMINI_API_KEY")
                .build();
    }

    public GeminiSession openSession() {
        return new GeminiSession(webClient);
    }

    // ================= SESSION =================

    public static class GeminiSession {

        private final WebClient webClient;
        private final Sinks.Many<String> transcriptSink;
        private final Sinks.Many<byte[]> audioSink;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        GeminiSession(WebClient webClient) {
            this.webClient = webClient;
            this.transcriptSink = Sinks.many().multicast().onBackpressureBuffer();
            this.audioSink = Sinks.many().unicast().onBackpressureBuffer();

            startStreaming();
        }

        // 🔁 Send audio immediately
        public void sendAudio(byte[] pcmChunk) {
            if (!closed.get()) {
                audioSink.tryEmitNext(pcmChunk);
            }
        }

        // 📤 Receive partial transcripts
        public Flux<String> transcriptStream() {
            return transcriptSink.asFlux();
        }

        // ❌ Close everything
        public void close() {
            if (closed.compareAndSet(false, true)) {
                audioSink.tryEmitComplete();
                transcriptSink.tryEmitComplete();
            }
        }

        // ================= INTERNAL =================

        private void startStreaming() {

            webClient.post()
                    .uri("/v1beta/models/gemini-1.5-pro:streamGenerateContent")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(audioSink.asFlux(), byte[].class)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(event -> {
                        // 🔍 Parse partial transcript (pseudo)
                        if (event.contains("text")) {
                            transcriptSink.tryEmitNext(event);
                        }
                    })
                    .doOnError(err -> transcriptSink.tryEmitError(err))
                    .doFinally(sig -> close())
                    .subscribe();
        }
    }
}
