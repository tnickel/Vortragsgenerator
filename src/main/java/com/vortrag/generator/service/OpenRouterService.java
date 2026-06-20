package com.vortrag.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vortrag.generator.model.ProjectImage;
import com.vortrag.generator.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class OpenRouterService {

    private final SettingRepository settingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.openrouter.url}")
    private String openRouterUrl;

    @Value("${app.openrouter.api-key}")
    private String defaultApiKey;

    public OpenRouterService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    private String getApiKey() {
        return settingRepository.findByKeyName("OPENROUTER_API_KEY")
                .map(setting -> setting.getValValue())
                .filter(val -> !val.trim().isEmpty())
                .orElse(defaultApiKey);
    }

    public String generatePresentationJson(String prompt, String model, List<ProjectImage> customImages) throws Exception {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenRouter API-Key ist nicht konfiguriert! Bitte trage ihn in den Einstellungen ein.");
        }

        StringBuilder customImagesPrompt = new StringBuilder();
        if (customImages != null && !customImages.isEmpty()) {
            customImagesPrompt.append("\nZUSÄTZLICHE REGEL FÜR BILDER:\n");
            customImagesPrompt.append("Für diese Präsentation wurden vom User eigene Bilder hochgeladen. Du SOLLTEST diese Bilder bevorzugt in den Folien verwenden, wenn sie thematisch passen. Verwende als Wert für das Feld 'image_name' exakt den angegebenen Dateinamen (inklusive Endung wie .png/.jpg/.jpeg).\n");
            customImagesPrompt.append("Hier ist die Liste der verfügbaren eigenen Bilder und deren Beschreibung:\n");
            for (ProjectImage img : customImages) {
                customImagesPrompt.append("- '").append(img.getFilename()).append("': ").append(img.getDescription()).append("\n");
            }
            customImagesPrompt.append("Nutze diese hochgeladenen Bilder anstelle der Standard-Mascots, wann immer sie thematisch zu einer Folie passen.\n");
        }

        String systemPrompt = """
            Du bist ein professioneller Präsentations-Struktur-Generator.
            Deine Aufgabe ist es, einen strukturierten Entwurf für eine PowerPoint-Präsentation basierend auf dem Prompt des Users zu erstellen.
            
            Du musst ausschließlich gültigen JSON-Code zurückgeben, der der folgenden Struktur entspricht:
            {
              "title": "Titel des Vortrags",
              "subtitle": "Untertitel des Vortrags (z.B. Für Designer, eine kreative Einführung)",
              "slides": [
                {
                  "slide_number": 1,
                  "type": "title",
                  "title": "Titel der ersten Folie",
                  "subtitle": "Untertitel für die erste Folie",
                  "bullets": [],
                  "notes": "Spannender, ausformulierter Sprechtext (ca. 2-3 Sätze), der später als Voiceover vertont wird.",
                  "image_name": "mascot_hello"
                },
                {
                  "slide_number": 2,
                  "type": "standard",
                  "title": "Titel der Folie",
                  "bullets": [
                    "Erster Bulletpoint",
                    "Zweiter Bulletpoint",
                    "Dritter Bulletpoint"
                  ],
                  "notes": "Ausformulierter Sprechtext für diese Folie (ca. 3-4 Sätze).",
                  "image_name": "mascot_think"
                }
              ]
            }
            
            REGELN:
            1. Gib NUR valides JSON zurück. Keine Erklärungen vor oder nach dem JSON, keine Markdown-Codeblocks (wie ```json ... ```), sondern direkt den JSON-String.
            2. Jede Folie muss einen Typ 'type' haben: 'title' für die Titelfolie (meist die 1. Folie), sonst 'standard'.
            3. Die Folien müssen eine fortlaufende slide_number (1, 2, 3...) haben.
            4. Für das Feld 'image_name' musst du aus folgender Liste von verfügbaren Cartoon-Bildern das am besten passende Bild auswählen (Gib nur den Basename ohne Dateiendung an, z.B. 'pc_doctor'):
               - 'mascot_hello': Eignet sich super für Begrüßung/Titelfolie.
               - 'mascot_think': Eignet sich für Erklärungen, Fragen, theoretische Grundlagen.
               - 'slow_pc': Eignet sich, um Probleme, Ruckler, volle Festplatten oder langsame Systeme darzustellen.
               - 'pc_doctor': Eignet sich für Diagnose, Reparatur, Bereinigung, Checks.
               - 'paper_chaos': Eignet sich, um Chaos, viel administrative Arbeit, repetitive Aufgaben darzustellen.
               - 'research_you': Eignet sich, um Recherchen, Analysen, Lernen, Erfassen von Informationen darzustellen.
               - 'conductor': Eignet sich für Prozesse, Ablaufdiagramme, Regie, Masterprompts.
               - 'toolbox': Eignet sich für Werkzeuge, Skripte, Code-Helpers, Automatisierungstools.
               - 'results_trophy': Eignet sich für Erfolge, fertige Ergebnisse, Auszeichnungen.
               - 'speed_boost': Eignet sich für Fazit, Raketen-Boost, Zusammenfassung, Performancegewinn.
            """
            + customImagesPrompt.toString() +
            """
            5. Der Sprechtext ('notes') wird für ElevenLabs benötigt. Er muss ansprechend, flüssig zu lesen und gut formuliert sein. Schreibe ihn in Deutsch.
            """;

        // Construct request payload
        String payload = objectMapper.createObjectNode()
                .put("model", model)
                .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt))
                        .add(objectMapper.createObjectNode().put("role", "user").put("content", prompt))
                ).toString();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(openRouterUrl))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "Antigravity Presentation Generator")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("OpenRouter API Fehler (Status " + response.statusCode() + "): " + response.body());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String content = responseJson.path("choices").get(0).path("message").path("content").asText();

        // Clean markdown code blocks if the LLM ignored the prompt rule
        content = content.trim();
        if (content.startsWith("```")) {
            // strip starting markdown fencing
            content = content.replaceAll("^```(?:json)?", "");
            // strip ending markdown fencing
            content = content.replaceAll("```$", "");
            content = content.trim();
        }

        // Validate that it is indeed valid JSON before returning
        try {
            objectMapper.readTree(content);
        } catch (Exception e) {
            System.err.println("Ungültiges JSON vom LLM zurückgegeben:\n" + content);
            throw new Exception("Das Modell hat kein gültiges JSON zurückgegeben. Bitte versuche es erneut.");
        }

        return content;
    }
}
