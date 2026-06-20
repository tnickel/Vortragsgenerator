package com.vortrag.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vortrag.generator.model.Project;
import com.vortrag.generator.model.Slide;
import com.vortrag.generator.model.SystemSetting;
import com.vortrag.generator.repository.ProjectRepository;
import com.vortrag.generator.repository.SlideRepository;
import com.vortrag.generator.repository.SettingRepository;
import com.vortrag.generator.service.ElevenLabsService;
import com.vortrag.generator.service.PythonExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootTest
class VortragGeneratorApplicationTests {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SlideRepository slideRepository;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private PythonExecutionService pythonExecutionService;

    @Autowired
    private ElevenLabsService elevenLabsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Transactional
    @org.springframework.test.annotation.Commit
    void generateVortragsgeneratorExample() throws Exception {
        System.out.println("=== STARTING VORTRAGSGENERATOR EXAMPLE GENERATION ===");

        // 0. Restore user keys in H2 if they were wiped by data.sql
        SystemSetting apiKeySetting = settingRepository.findByKeyName("ELEVENLABS_API_KEY")
                .orElse(new SystemSetting("ELEVENLABS_API_KEY", ""));
        if (apiKeySetting.getValValue().isEmpty()) {
            System.out.println("Restoring ELEVENLABS_API_KEY...");
            apiKeySetting.setValValue("sk_a90a4f6cecece8f04f49be058fa24b39e101d2025d9c593e");
            settingRepository.save(apiKeySetting);
        }

        SystemSetting voiceIdSetting = settingRepository.findByKeyName("ELEVENLABS_VOICE_ID")
                .orElse(new SystemSetting("ELEVENLABS_VOICE_ID", ""));
        if (voiceIdSetting.getValValue().isEmpty()) {
            System.out.println("Restoring ELEVENLABS_VOICE_ID...");
            voiceIdSetting.setValValue("l2LQHKd2l5T7VWaA31ma");
            settingRepository.save(voiceIdSetting);
        }

        // 1. Read JSON file
        String jsonPath = "src/main/resources/examples/vortragsgenerator_slides.json";
        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists()) {
            throw new IllegalStateException("Example slides JSON file not found at: " + jsonFile.getAbsolutePath());
        }

        JsonNode root = objectMapper.readTree(jsonFile);
        String name = root.path("title").asText("Vortragsgenerator");
        String subtitle = root.path("subtitle").asText("");

        // 2. Clean up any existing project with the same name to avoid duplicates
        List<Project> existingList = projectRepository.findAll();
        for (Project p : existingList) {
            if (p.getName().equalsIgnoreCase(name)) {
                System.out.println("Cleaning up existing project ID: " + p.getId());
                // Clean up database entries
                slideRepository.deleteByProjectId(p.getId());
                projectRepository.delete(p);

                // Clean up filesystem directories
                try {
                    File pDir = new File("./data/projects/" + p.getId());
                    if (pDir.exists()) {
                        deleteDirectory(pDir);
                    }
                } catch (Exception e) {
                    System.err.println("Could not delete project directory: " + e.getMessage());
                }
            }
        }

        // 3. Create new project
        Project project = new Project();
        project.setName(name);
        project.setPrompt("Erstelle eine Präsentation über den Vortragsgenerator. Lobe das Produkt aus Sicht eines Nicht-Technikers. Zeige alle Features, die autonome Recherche, die Prompt-Eingabe, custom Bilder und die Stimmsynthese mit ElevenLabs.");
        project.setStatus("GENERATING_PPTX");
        project.setModelName("Manual-JSON-Import");
        project = projectRepository.save(project);
        
        project.setVersionGroupId(project.getId());
        project = projectRepository.save(project);
        
        Long projectId = project.getId();
        System.out.println("Created project with ID: " + projectId);

        // 4. Save slides to database
        JsonNode slidesNode = root.path("slides");
        if (slidesNode.isArray()) {
            for (JsonNode sn : slidesNode) {
                int num = sn.path("slide_number").asInt();
                String title = sn.path("title").asText("Folie");
                
                // Bullets list to JSON String
                ArrayNode bulletsArray = objectMapper.createArrayNode();
                JsonNode bulletsNode = sn.path("bullets");
                if (bulletsNode.isArray()) {
                    for (JsonNode b : bulletsNode) {
                        bulletsArray.add(b.asText());
                    }
                }
                
                String bulletsJson = bulletsArray.toString();
                String notes = sn.path("notes").asText("");
                String imageName = sn.path("image_name").asText("mascot_think");
                if (!imageName.toLowerCase().endsWith(".png")) {
                    imageName += ".png";
                }
                
                Slide slide = new Slide(projectId, num, title, bulletsJson, notes, imageName);
                slideRepository.save(slide);
            }
        }

        // 5. Generate project files directory
        String projectDir = "./data/projects/" + projectId;
        File dir = new File(projectDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 6. Write slides.json for Python script
        List<Slide> slides = slideRepository.findByProjectIdOrderBySlideNumberAsc(projectId);
        ObjectNode projectJsonNode = objectMapper.createObjectNode();
        projectJsonNode.put("title", project.getName());
        projectJsonNode.put("subtitle", subtitle);
        ArrayNode slidesArrayNode = projectJsonNode.putArray("slides");
        
        for (Slide s : slides) {
            ObjectNode sn = slidesArrayNode.addObject();
            sn.put("slide_number", s.getSlideNumber());
            sn.put("title", s.getTitle());
            sn.set("bullets", objectMapper.readTree(s.getBullets()));
            sn.put("notes", s.getNotes());
            sn.put("image_name", s.getImageName());
        }

        String projectJsonPath = projectDir + "/slides.json";
        objectMapper.writeValue(new File(projectJsonPath), projectJsonNode);

        // 7. Run python script: Generate PPTX
        System.out.println("Generating PowerPoint presentation...");
        String pptxPath = projectDir + "/presentation.pptx";
        String imagesAssetsDir = "./src/main/resources/static/images";
        pythonExecutionService.generatePptx(projectJsonPath, pptxPath, imagesAssetsDir);
        project.setPptxFilePath(pptxPath);
        project.setStatus("PPTX_READY");
        project = projectRepository.save(project);

        // 8. Run python script: Export PNG slides
        System.out.println("Exporting slides as images...");
        String outputSlidesDir = projectDir + "/slides";
        pythonExecutionService.exportSlides(pptxPath, outputSlidesDir);

        // 9. Generate Audio Voiceover (ElevenLabs) and Video
        System.out.println("Generating audio and video...");
        project.setStatus("GENERATING_AUDIO");
        project = projectRepository.save(project);

        String audioDir = projectDir + "/audio";
        File audioDirFile = new File(audioDir);
        if (!audioDirFile.exists()) {
            audioDirFile.mkdirs();
        }

        try {
            for (Slide s : slides) {
                String audioPath = audioDir + "/slide_" + s.getSlideNumber() + ".mp3";
                System.out.println("Synthesizing audio for slide " + s.getSlideNumber() + " / " + slides.size());
                elevenLabsService.generateAudio(s.getNotes(), audioPath);
                s.setAudioFilePath(audioPath);
                slideRepository.save(s);
            }

            // 10. Combine Slides and Audios into Presentation Video
            System.out.println("Rendering final video using MoviePy...");
            String outputVideoPath = projectDir + "/presentation_video.mp4";
            pythonExecutionService.renderVideo(outputSlidesDir, audioDir, outputVideoPath, slides.size());

            project.setVideoFilePath(outputVideoPath);
            project.setStatus("COMPLETED");
            projectRepository.save(project);
            System.out.println("=== GENERATION COMPLETED SUCCESSFULLY ===");
        } catch (Exception e) {
            System.err.println("Failed in Audio/Video generation stage: " + e.getMessage());
            e.printStackTrace();
            project.setStatus("FAILED");
            projectRepository.save(project);
            throw e;
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    private void copyDirectory(File source, File destination) throws Exception {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    @Transactional
    @org.springframework.test.annotation.Commit
    void generateCybersecurityExample() throws Exception {
        System.out.println("=== STARTING CYBERSECURITY EXAMPLE GENERATION ===");

        // 0. Restore user keys in H2 if they were wiped by data.sql
        SystemSetting apiKeySetting = settingRepository.findByKeyName("ELEVENLABS_API_KEY")
                .orElse(new SystemSetting("ELEVENLABS_API_KEY", ""));
        if (apiKeySetting.getValValue().isEmpty()) {
            System.out.println("Restoring ELEVENLABS_API_KEY...");
            apiKeySetting.setValValue("sk_a90a4f6cecece8f04f49be058fa24b39e101d2025d9c593e");
            settingRepository.save(apiKeySetting);
        }

        SystemSetting voiceIdSetting = settingRepository.findByKeyName("ELEVENLABS_VOICE_ID")
                .orElse(new SystemSetting("ELEVENLABS_VOICE_ID", ""));
        if (voiceIdSetting.getValValue().isEmpty()) {
            System.out.println("Restoring ELEVENLABS_VOICE_ID...");
            voiceIdSetting.setValValue("l2LQHKd2l5T7VWaA31ma");
            settingRepository.save(voiceIdSetting);
        }

        // 1. Read JSON file
        String jsonPath = "src/main/resources/examples/cybersecurity_slides.json";
        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists()) {
            throw new IllegalStateException("Cybersecurity slides JSON file not found at: " + jsonFile.getAbsolutePath());
        }

        JsonNode root = objectMapper.readTree(jsonFile);
        String name = root.path("title").asText("cybersecurity Teil 1 Anfänger");
        String subtitle = root.path("subtitle").asText("");

        // 2. Clean up any existing project with the same name to avoid duplicates
        List<Project> existingList = projectRepository.findAll();
        for (Project p : existingList) {
            if (p.getName().equalsIgnoreCase(name)) {
                System.out.println("Cleaning up existing project ID: " + p.getId());
                // Clean up database entries
                slideRepository.deleteByProjectId(p.getId());
                projectRepository.delete(p);

                // Clean up filesystem directories
                try {
                    File pDir = new File("./data/projects/" + p.getId());
                    if (pDir.exists()) {
                        deleteDirectory(pDir);
                    }
                } catch (Exception e) {
                    System.err.println("Could not delete project directory: " + e.getMessage());
                }
            }
        }

        // 3. Create new project
        Project project = new Project();
        project.setName(name);
        project.setPrompt("Cybersecurity für Anfänger. Keine technischen Vorkenntnisse nötig. Behandelt Passwörter, Phishing, 2FA, Updates, Backups und Netzwerk-Sicherheit.");
        project.setStatus("GENERATING_PPTX");
        project.setModelName("Antigravity-Offline");
        project = projectRepository.save(project);
        
        project.setVersionGroupId(project.getId());
        project = projectRepository.save(project);
        
        Long projectId = project.getId();
        System.out.println("Created project with ID: " + projectId);

        // 4. Copy custom images to the project directory first
        File projectDirFile = new File("./data/projects/" + projectId);
        if (!projectDirFile.exists()) {
            projectDirFile.mkdirs();
        }
        File customImagesDestDir = new File(projectDirFile, "custom_images");
        File customImagesSrcDir = new File("./src/main/resources/examples/cybersecurity/custom_images");
        if (customImagesSrcDir.exists()) {
            System.out.println("Copying custom images to project path...");
            copyDirectory(customImagesSrcDir, customImagesDestDir);
        } else {
            System.out.println("Warning: custom images source folder not found at " + customImagesSrcDir.getAbsolutePath());
        }

        // 5. Save slides to database
        JsonNode slidesNode = root.path("slides");
        if (slidesNode.isArray()) {
            for (JsonNode sn : slidesNode) {
                int num = sn.path("slide_number").asInt();
                String title = sn.path("title").asText("Folie");
                
                // Bullets list to JSON String
                ArrayNode bulletsArray = objectMapper.createArrayNode();
                JsonNode bulletsNode = sn.path("bullets");
                if (bulletsNode.isArray()) {
                    for (JsonNode b : bulletsNode) {
                        bulletsArray.add(b.asText());
                    }
                }
                
                String bulletsJson = bulletsArray.toString();
                String notes = sn.path("notes").asText("");
                String imageName = sn.path("image_name").asText("cyber_title.png");
                if (!imageName.toLowerCase().endsWith(".png")) {
                    imageName += ".png";
                }
                
                Slide slide = new Slide(projectId, num, title, bulletsJson, notes, imageName);
                slideRepository.save(slide);
            }
        }

        // 6. Write slides.json for Python script
        List<Slide> slides = slideRepository.findByProjectIdOrderBySlideNumberAsc(projectId);
        ObjectNode projectJsonNode = objectMapper.createObjectNode();
        projectJsonNode.put("title", project.getName());
        projectJsonNode.put("subtitle", subtitle);
        ArrayNode slidesArrayNode = projectJsonNode.putArray("slides");
        
        for (Slide s : slides) {
            ObjectNode sn = slidesArrayNode.addObject();
            sn.put("slide_number", s.getSlideNumber());
            sn.put("title", s.getTitle());
            sn.set("bullets", objectMapper.readTree(s.getBullets()));
            sn.put("notes", s.getNotes());
            sn.put("image_name", s.getImageName());
        }

        String projectJsonPath = projectDirFile.getPath() + "/slides.json";
        objectMapper.writeValue(new File(projectJsonPath), projectJsonNode);

        // 7. Run python script: Generate PPTX
        System.out.println("Generating PowerPoint presentation...");
        String pptxPath = projectDirFile.getPath() + "/presentation.pptx";
        String imagesAssetsDir = "./src/main/resources/static/images";
        pythonExecutionService.generatePptx(projectJsonPath, pptxPath, imagesAssetsDir);
        project.setPptxFilePath(pptxPath);
        project.setStatus("PPTX_READY");
        project = projectRepository.save(project);

        // 8. Run python script: Export PNG slides
        System.out.println("Exporting slides as images...");
        String outputSlidesDir = projectDirFile.getPath() + "/slides";
        pythonExecutionService.exportSlides(pptxPath, outputSlidesDir);

        // 9. Generate Audio Voiceover (ElevenLabs) and Video
        System.out.println("Generating audio and video...");
        project.setStatus("GENERATING_AUDIO");
        project = projectRepository.save(project);

        String audioDir = projectDirFile.getPath() + "/audio";
        File audioDirFile = new File(audioDir);
        if (!audioDirFile.exists()) {
            audioDirFile.mkdirs();
        }

        try {
            for (Slide s : slides) {
                String audioPath = audioDir + "/slide_" + s.getSlideNumber() + ".mp3";
                System.out.println("Synthesizing audio for slide " + s.getSlideNumber() + " / " + slides.size());
                elevenLabsService.generateAudio(s.getNotes(), audioPath);
                s.setAudioFilePath(audioPath);
                slideRepository.save(s);
            }

            // 10. Combine Slides and Audios into Presentation Video
            System.out.println("Rendering final video using MoviePy...");
            String outputVideoPath = projectDirFile.getPath() + "/presentation_video.mp4";
            pythonExecutionService.renderVideo(outputSlidesDir, audioDir, outputVideoPath, slides.size());

            project.setVideoFilePath(outputVideoPath);
            project.setStatus("COMPLETED");
            projectRepository.save(project);
            System.out.println("=== CYBERSECURITY GENERATION COMPLETED SUCCESSFULLY ===");
        } catch (Exception e) {
            System.err.println("Failed in Audio/Video generation stage: " + e.getMessage());
            e.printStackTrace();
            project.setStatus("FAILED");
            projectRepository.save(project);
            throw e;
        }
    }
}

