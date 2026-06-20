package com.vortrag.generator.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class PythonExecutionService {

    public void runScript(List<String> command) throws Exception {
        System.out.println("Executing command: " + String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // Read process output in real-time
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Python Script Output] " + line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Script beendete mit Fehlercode: " + exitCode);
        }
    }

    public void generatePptx(String jsonPath, String pptxPath, String imagesDir) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add("python/generate_pptx.py");
        command.add("--json-path");
        command.add(jsonPath);
        command.add("--output-path");
        command.add(pptxPath);
        command.add("--images-dir");
        command.add(imagesDir);
        
        runScript(command);
    }

    public void exportSlides(String pptxPath, String outputDir) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add("python/export_slides.py");
        command.add("--pptx-path");
        command.add(pptxPath);
        command.add("--output-dir");
        command.add(outputDir);
        
        runScript(command);
    }

    public void renderVideo(String imagesDir, String audioDir, String outputVideoPath, int slideCount) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add("python/render_video.py");
        command.add("--images-dir");
        command.add(imagesDir);
        command.add("--audio-dir");
        command.add(audioDir);
        command.add("--output-video");
        command.add(outputVideoPath);
        command.add("--slide-count");
        command.add(String.valueOf(slideCount));
        
        runScript(command);
    }

    public void importPptx(String pptxPath, String jsonPath) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add("python/import_pptx.py");
        command.add(pptxPath);
        command.add(jsonPath);
        
        runScript(command);
    }
}
