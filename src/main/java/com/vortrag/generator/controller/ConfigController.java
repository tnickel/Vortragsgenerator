package com.vortrag.generator.controller;

import com.vortrag.generator.model.SystemSetting;
import com.vortrag.generator.repository.SettingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ConfigController {

    private final SettingRepository settingRepository;

    public ConfigController(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getSettings() {
        Map<String, String> settingsMap = new HashMap<>();
        // Set default placeholders
        settingsMap.put("ELEVENLABS_API_KEY", "");
        settingsMap.put("ELEVENLABS_VOICE_ID", "");
        settingsMap.put("OPENROUTER_API_KEY", "");
        settingsMap.put("OPENROUTER_MODEL", "anthropic/claude-opus-4.8");

        List<SystemSetting> settingsList = settingRepository.findAll();
        for (SystemSetting s : settingsList) {
            settingsMap.put(s.getKeyName(), s.getValValue());
        }

        // Hide sensitive keys for UI presentation (partially masking)
        Map<String, String> masked = new HashMap<>();
        for (Map.Entry<String, String> entry : settingsMap.entrySet()) {
            String val = entry.getValue();
            if ((entry.getKey().contains("KEY") || entry.getKey().contains("PASSWORD")) && val != null && val.length() > 8) {
                masked.put(entry.getKey(), val.substring(0, 6) + "..." + val.substring(val.length() - 4));
            } else {
                masked.put(entry.getKey(), val);
            }
        }

        return ResponseEntity.ok(masked);
    }

    @PostMapping("/settings")
    public ResponseEntity<Void> saveSettings(@RequestBody Map<String, String> newSettings) {
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Skip updating if it is masked value (i.e. starts with masked layout and ends, meaning user did not change it)
            if (value != null && value.contains("...")) {
                continue;
            }

            Optional<SystemSetting> opt = settingRepository.findByKeyName(key);
            if (opt.isPresent()) {
                SystemSetting setting = opt.get();
                setting.setValValue(value);
                settingRepository.save(setting);
            } else {
                settingRepository.save(new SystemSetting(key, value));
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/models")
    public List<Map<String, String>> getAvailableModels() {
        List<Map<String, String>> models = new ArrayList<>();
        
        models.add(Map.of("id", "anthropic/claude-opus-4.8", "name", "Anthropic: Claude Opus 4.8"));
        models.add(Map.of("id", "anthropic/claude-opus-4.8-fast", "name", "Anthropic: Claude Opus 4.8 (Fast)"));
        models.add(Map.of("id", "moonshotai/kimi-k2.7-code", "name", "MoonshotAI: Kimi K2.7 Code"));
        models.add(Map.of("id", "~moonshotai/kimi-latest", "name", "MoonshotAI: Kimi Latest"));
        models.add(Map.of("id", "~anthropic/claude-sonnet-latest", "name", "Anthropic: Claude Sonnet Latest"));
        models.add(Map.of("id", "deepseek/deepseek-r1", "name", "DeepSeek: R1"));
        models.add(Map.of("id", "google/gemini-2.5-pro", "name", "Google: Gemini 2.5 Pro"));

        return models;
    }

    @GetMapping("/images")
    public List<String> getAvailableImages() {
        // Predefined list of premium mascot/cartoon images copied from the references
        return Arrays.asList(
            "mascot_hello.png",
            "mascot_think.png",
            "slow_pc.png",
            "pc_doctor.png",
            "paper_chaos.png",
            "research_you.png",
            "conductor.png",
            "toolbox.png",
            "results_trophy.png",
            "speed_boost.png"
        );
    }
}
