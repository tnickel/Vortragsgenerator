package com.vortrag.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vortrag.generator.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class ElevenLabsService {

    private final SettingRepository settingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.elevenlabs.api-key}")
    private String defaultApiKey;

    @Value("${app.elevenlabs.voice-id}")
    private String defaultVoiceId;

    public ElevenLabsService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    private String getApiKey() {
        return settingRepository.findByKeyName("ELEVENLABS_API_KEY")
                .map(setting -> setting.getValValue())
                .filter(val -> !val.trim().isEmpty())
                .orElse(defaultApiKey);
    }

    private String getVoiceId() {
        return settingRepository.findByKeyName("ELEVENLABS_VOICE_ID")
                .map(setting -> setting.getValValue())
                .filter(val -> !val.trim().isEmpty())
                .orElse(defaultVoiceId);
    }

    public void generateAudio(String text, String outputPath) throws Exception {
        String apiKey = getApiKey();
        String voiceId = getVoiceId();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("ElevenLabs API-Key ist nicht konfiguriert! Bitte trage ihn in den Einstellungen ein.");
        }
        if (voiceId == null || voiceId.trim().isEmpty()) {
            throw new IllegalStateException("ElevenLabs Voice-ID ist nicht konfiguriert! Bitte trage sie in den Einstellungen ein.");
        }

        System.out.println("Sende Text an ElevenLabs (Stimme: " + voiceId + ", Länge: " + text.length() + ")...");
        
        String url = "https://api.elevenlabs.io/v1/text-to-speech/" + voiceId;

        // Build Payload
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("text", text);
        payloadMap.put("model_id", "eleven_multilingual_v2");
        
        Map<String, Object> voiceSettings = new HashMap<>();
        voiceSettings.put("stability", 0.5);
        voiceSettings.put("similarity_boost", 0.75);
        payloadMap.put("voice_settings", voiceSettings);

        String payload = objectMapper.writeValueAsString(payloadMap);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("xi-api-key", apiKey)
                .header("accept", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            String errorMsg = new String(response.body(), "UTF-8");
            throw new Exception("ElevenLabs API Fehler (Status " + response.statusCode() + "): " + errorMsg);
        }

        // Save audio bytes to output path
        File outputFile = new File(outputPath);
        // Ensure parent directories exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(response.body());
        }

        System.out.println("Audio erfolgreich gespeichert unter: " + outputPath);
    }
}
