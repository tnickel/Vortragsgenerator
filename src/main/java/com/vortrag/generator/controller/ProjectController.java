package com.vortrag.generator.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vortrag.generator.model.Project;
import com.vortrag.generator.model.Slide;
import com.vortrag.generator.model.PromptHistory;
import com.vortrag.generator.model.ProjectImage;
import com.vortrag.generator.repository.ProjectRepository;
import com.vortrag.generator.repository.SlideRepository;
import com.vortrag.generator.repository.PromptHistoryRepository;
import com.vortrag.generator.repository.ProjectImageRepository;
import com.vortrag.generator.service.OpenRouterService;
import com.vortrag.generator.service.PythonExecutionService;
import com.vortrag.generator.service.ElevenLabsService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final SlideRepository slideRepository;
    private final PromptHistoryRepository promptHistoryRepository;
    private final ProjectImageRepository projectImageRepository;
    private final OpenRouterService openRouterService;
    private final PythonExecutionService pythonExecutionService;
    private final ElevenLabsService elevenLabsService;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProjectController(ProjectRepository projectRepository,
                             SlideRepository slideRepository,
                             PromptHistoryRepository promptHistoryRepository,
                             ProjectImageRepository projectImageRepository,
                             OpenRouterService openRouterService,
                             PythonExecutionService pythonExecutionService,
                             ElevenLabsService elevenLabsService) {
        this.projectRepository = projectRepository;
        this.slideRepository = slideRepository;
        this.promptHistoryRepository = promptHistoryRepository;
        this.projectImageRepository = projectImageRepository;
        this.openRouterService = openRouterService;
        this.pythonExecutionService = pythonExecutionService;
        this.elevenLabsService = elevenLabsService;
    }

    // Initialize example project files on first access
    private void checkAndInitExampleProject(Project project) {
        if (project.getId() == 1L) {
            String projectDir = "./data/projects/1";
            File dir = new File(projectDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Copy PowerPoint from reference
            File pptxDest = new File(projectDir + "/Antigravity_Vortrag.pptx");
            File pptxSrc = new File("./src/main/resources/examples/designer_vortrag/Antigravity_Vortrag.pptx");
            if (pptxSrc.exists() && !pptxDest.exists()) {
                try {
                    Files.copy(pptxSrc.toPath(), pptxDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    project.setPptxFilePath(pptxDest.getPath());
                } catch (Exception e) {
                    System.err.println("Could not copy example PPTX: " + e.getMessage());
                }
            }

            // Copy Video from reference
            File videoDest = new File(projectDir + "/Antigravity_Vortrag_Video.mp4");
            File videoSrc = new File("./src/main/resources/examples/designer_vortrag/Antigravity_Vortrag_Video.mp4");
            if (videoSrc.exists() && !videoDest.exists()) {
                try {
                    Files.copy(videoSrc.toPath(), videoDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    project.setVideoFilePath(videoDest.getPath());
                } catch (Exception e) {
                    System.err.println("Could not copy example Video: " + e.getMessage());
                }
            }

            // Copy Slide Images from reference
            File imagesDestDir = new File(projectDir + "/slides");
            File imagesSrcDir = new File("./src/main/resources/examples/designer_vortrag/slides");
            if (imagesSrcDir.exists() && !imagesDestDir.exists()) {
                imagesDestDir.mkdirs();
                try {
                    File[] images = imagesSrcDir.listFiles();
                    if (images != null) {
                        for (File img : images) {
                            Files.copy(img.toPath(), new File(imagesDestDir, img.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not copy example slides: " + e.getMessage());
                }
            }

            // Copy Slide Audio from reference
            File audioDestDir = new File(projectDir + "/temp_audio");
            File audioSrcDir = new File("./src/main/resources/examples/designer_vortrag/temp_audio");
            if (audioSrcDir.exists() && !audioDestDir.exists()) {
                audioDestDir.mkdirs();
                try {
                    File[] audios = audioSrcDir.listFiles();
                    if (audios != null) {
                        for (File aud : audios) {
                            Files.copy(aud.toPath(), new File(audioDestDir, aud.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not copy example audios: " + e.getMessage());
                }
            }

            projectRepository.save(project);
        } else if (project.getId() == 3L) {
            String projectDir = "./data/projects/3";
            File dir = new File(projectDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Copy PowerPoint from reference
            File pptxDest = new File(projectDir + "/presentation.pptx");
            File pptxSrc = new File("./src/main/resources/examples/vortragsgenerator/presentation.pptx");
            if (pptxSrc.exists() && !pptxDest.exists()) {
                try {
                    Files.copy(pptxSrc.toPath(), pptxDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    project.setPptxFilePath(pptxDest.getPath());
                } catch (Exception e) {
                    System.err.println("Could not copy example PPTX: " + e.getMessage());
                }
            }

            // Copy Video from reference
            File videoDest = new File(projectDir + "/presentation_video.mp4");
            File videoSrc = new File("./src/main/resources/examples/vortragsgenerator/presentation_video.mp4");
            if (videoSrc.exists() && !videoDest.exists()) {
                try {
                    Files.copy(videoSrc.toPath(), videoDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    project.setVideoFilePath(videoDest.getPath());
                } catch (Exception e) {
                    System.err.println("Could not copy example Video: " + e.getMessage());
                }
            }

            // Copy Slides JSON from reference
            File jsonDest = new File(projectDir + "/slides.json");
            File jsonSrc = new File("./src/main/resources/examples/vortragsgenerator/slides.json");
            if (jsonSrc.exists() && !jsonDest.exists()) {
                try {
                    Files.copy(jsonSrc.toPath(), jsonDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    System.err.println("Could not copy example slides.json: " + e.getMessage());
                }
            }

            // Copy Slide Images from reference
            File imagesDestDir = new File(projectDir + "/slides");
            File imagesSrcDir = new File("./src/main/resources/examples/vortragsgenerator/slides");
            if (imagesSrcDir.exists() && !imagesDestDir.exists()) {
                imagesDestDir.mkdirs();
                try {
                    File[] images = imagesSrcDir.listFiles();
                    if (images != null) {
                        for (File img : images) {
                            Files.copy(img.toPath(), new File(imagesDestDir, img.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not copy example slides: " + e.getMessage());
                }
            }

            // Copy Slide Audio from reference
            File audioDestDir = new File(projectDir + "/audio");
            File audioSrcDir = new File("./src/main/resources/examples/vortragsgenerator/audio");
            if (audioSrcDir.exists() && !audioDestDir.exists()) {
                audioDestDir.mkdirs();
                try {
                    File[] audios = audioSrcDir.listFiles();
                    if (audios != null) {
                        for (File aud : audios) {
                            Files.copy(aud.toPath(), new File(audioDestDir, aud.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not copy example audios: " + e.getMessage());
                }
            }

            projectRepository.save(project);
        }
    }

    @GetMapping("/projects")
    public List<Project> getProjects() {
        List<Project> list = projectRepository.findAll();
        java.util.Map<Long, Project> latestByGroup = new java.util.HashMap<>();
        for (Project p : list) {
            checkAndInitExampleProject(p);
            if (p.getVersionGroupId() == null) {
                p.setVersionGroupId(p.getId());
                projectRepository.save(p);
            }
            Project existing = latestByGroup.get(p.getVersionGroupId());
            if (existing == null || p.getId() > existing.getId()) {
                latestByGroup.put(p.getVersionGroupId(), p);
            }
        }
        List<Project> result = new java.util.ArrayList<>(latestByGroup.values());
        result.sort((a, b) -> b.getVersionGroupId().compareTo(a.getVersionGroupId()));
        return result;
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(p -> {
                    checkAndInitExampleProject(p);
                    if (p.getVersionGroupId() == null) {
                        p.setVersionGroupId(p.getId());
                        projectRepository.save(p);
                    }
                    return ResponseEntity.ok(p);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/projects")
    public Project createProject(@RequestBody Project project) {
        Project saved = projectRepository.save(project);
        saved.setVersionGroupId(saved.getId());
        saved = projectRepository.save(saved);
        // Save prompt in prompt history as well
        if (project.getPrompt() != null && !project.getPrompt().trim().isEmpty()) {
            promptHistoryRepository.save(new PromptHistory(project.getName(), project.getPrompt()));
        }
        return saved;
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        if (!projectRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // Clean up filesystem directories
        try {
            File projectDir = new File("./data/projects/" + id);
            if (projectDir.exists()) {
                deleteDirectory(projectDir);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Löschen des Projektordners: " + e.getMessage());
        }

        projectRepository.deleteById(id);
        return ResponseEntity.ok().build();
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

    @GetMapping("/projects/{id}/slides")
    public List<Slide> getSlides(@PathVariable Long id) {
        return slideRepository.findByProjectIdOrderBySlideNumberAsc(id);
    }

    @PutMapping("/projects/{id}/slides/{slideId}")
    public ResponseEntity<Slide> updateSlide(@PathVariable Long id, @PathVariable Long slideId, @RequestBody Slide slideDetails) {
        return slideRepository.findById(slideId)
                .map(slide -> {
                    slide.setTitle(slideDetails.getTitle());
                    slide.setBullets(slideDetails.getBullets());
                    
                    // If notes changed, invalidate the synthesized audio file
                    if (slideDetails.getNotes() != null && !slideDetails.getNotes().equals(slide.getNotes())) {
                        slide.setNotes(slideDetails.getNotes());
                        if (slide.getAudioFilePath() != null) {
                            File f = new File(slide.getAudioFilePath());
                            if (f.exists()) {
                                f.delete();
                            }
                            slide.setAudioFilePath(null);
                            slide.setVideoFilePath(null);
                        }
                    }
                    
                    slide.setImageName(slideDetails.getImageName());
                    Slide saved = slideRepository.save(slide);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/projects/{id}/generate-pptx")
    public ResponseEntity<Project> generatePptx(@PathVariable Long id) {
        Optional<Project> opt = projectRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Project project = opt.get();
        project.setStatus("GENERATING_PPTX");
        projectRepository.save(project);

        // Run slide design logic in background thread
        executorService.submit(() -> {
            try {
                String projectDir = "./data/projects/" + id;
                File dir = new File(projectDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                List<Slide> existingSlides = slideRepository.findByProjectIdOrderBySlideNumberAsc(id);
                
                // Call OpenRouter LLM if no slides exist yet
                if (existingSlides.isEmpty()) {
                    System.out.println("Keine Folien vorhanden. Rufe OpenRouter auf...");
                    String model = project.getModelName();
                    if (model == null || model.isEmpty()) {
                        model = "anthropic/claude-opus-4.8";
                    }
                    List<ProjectImage> customImages = projectImageRepository.findByProjectId(id);
                    String jsonResponse = openRouterService.generatePresentationJson(project.getPrompt(), model, customImages);
                    
                    // Parse slides structure
                    JsonNode root = objectMapper.readTree(jsonResponse);
                    project.setName(root.path("title").asText(project.getName()));
                    projectRepository.save(project);
                    
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
                            
                            Slide slide = new Slide(id, num, title, bulletsJson, notes, imageName);
                            slideRepository.save(slide);
                        }
                    }
                    existingSlides = slideRepository.findByProjectIdOrderBySlideNumberAsc(id);
                }

                // Write slides state to JSON file for Python script
                ObjectNode projectJsonNode = objectMapper.createObjectNode();
                projectJsonNode.put("title", project.getName());
                projectJsonNode.put("subtitle", "Generierte Präsentation");
                ArrayNode slidesArrayNode = projectJsonNode.putArray("slides");
                
                for (Slide s : existingSlides) {
                    ObjectNode sn = slidesArrayNode.addObject();
                    sn.put("slide_number", s.getSlideNumber());
                    sn.put("title", s.getTitle());
                    sn.set("bullets", objectMapper.readTree(s.getBullets()));
                    sn.put("notes", s.getNotes());
                    sn.put("image_name", s.getImageName());
                }

                String jsonPath = projectDir + "/slides.json";
                objectMapper.writeValue(new File(jsonPath), projectJsonNode);

                // Run python script: Generate PPTX
                String pptxPath = projectDir + "/presentation.pptx";
                String imagesAssetsDir = "./src/main/resources/static/images";
                pythonExecutionService.generatePptx(jsonPath, pptxPath, imagesAssetsDir);

                // Run python script: Export PNG slides
                String outputSlidesDir = projectDir + "/slides";
                pythonExecutionService.exportSlides(pptxPath, outputSlidesDir);

                // Update Project State
                project.setPptxFilePath(pptxPath);
                project.setStatus("PPTX_READY");
                projectRepository.save(project);
                System.out.println("Projekt " + id + ": PPTX und Bilder erfolgreich erstellt.");

            } catch (Exception e) {
                System.err.println("Fehler in Stufe 1: " + e.getMessage());
                e.printStackTrace();
                project.setStatus("FAILED");
                projectRepository.save(project);
            }
        });

        return ResponseEntity.ok(project);
    }

    @PostMapping("/projects/{id}/generate-audio")
    public ResponseEntity<Project> generateAudio(@PathVariable Long id) {
        Optional<Project> opt = projectRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Project project = opt.get();
        project.setStatus("GENERATING_AUDIO");
        projectRepository.save(project);

        // Run ElevenLabs voiceover and video synthesis in background thread
        executorService.submit(() -> {
            try {
                String projectDir = "./data/projects/" + id;
                List<Slide> slides = slideRepository.findByProjectIdOrderBySlideNumberAsc(id);
                
                if (slides.isEmpty()) {
                    throw new Exception("Keine Folien zum Vertonen vorhanden! Bitte führe erst Stufe 1 aus.");
                }

                String audioDir = projectDir + "/audio";
                File audioDirFile = new File(audioDir);
                if (!audioDirFile.exists()) {
                    audioDirFile.mkdirs();
                }

                // 1. Generate Voiceover MP3s via ElevenLabs for each slide
                for (Slide s : slides) {
                    String audioPath = audioDir + "/slide_" + s.getSlideNumber() + ".mp3";
                    File audioFile = new File(audioPath);
                    
                    // Skip ElevenLabs call if audio already cached and valid
                    if (!audioFile.exists() || s.getAudioFilePath() == null) {
                        elevenLabsService.generateAudio(s.getNotes(), audioPath);
                        s.setAudioFilePath(audioPath);
                        slideRepository.save(s);
                    } else {
                        System.out.println("Nutze gecachte Audiodatei für Folie " + s.getSlideNumber());
                    }
                }

                // 2. Combine Slides and Audios into Presentation Video
                String slidesDir = projectDir + "/slides";
                String outputVideoPath = projectDir + "/presentation_video.mp4";
                
                pythonExecutionService.renderVideo(slidesDir, audioDir, outputVideoPath, slides.size());

                // 3. Mark completed
                project.setVideoFilePath(outputVideoPath);
                project.setStatus("COMPLETED");
                projectRepository.save(project);
                System.out.println("Projekt " + id + ": Vertonung und Video abgeschlossen!");

            } catch (Exception e) {
                System.err.println("Fehler in Stufe 2: " + e.getMessage());
                e.printStackTrace();
                project.setStatus("FAILED");
                projectRepository.save(project);
            }
        });

        return ResponseEntity.ok(project);
    }

    // Serve slide image PNGs
    @GetMapping("/projects/{id}/slides/{number}/image")
    public ResponseEntity<Resource> getSlideImage(@PathVariable Long id, @PathVariable int number) {
        String path = "./data/projects/" + id + "/slides/slide_" + number + ".png";
        
        // Example project fallback path
        if (id == 1L) {
            File f = new File(path);
            if (!f.exists()) {
                path = "D:/AntiGravitySoftware/GitWorkspace/VortragAntigravity/vortrag_Claude/temp_slides/Folie" + number + ".PNG";
            }
        }

        File file = new File(path);
        if (!file.exists()) {
            // Fallback default image
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }

    // Serve slide voiceover MP3s
    @GetMapping("/projects/{id}/slides/{number}/audio")
    public ResponseEntity<Resource> getSlideAudio(@PathVariable Long id, @PathVariable int number) {
        String path = "./data/projects/" + id + "/audio/slide_" + number + ".mp3";
        
        // Example project fallback path
        if (id == 1L) {
            File f = new File(path);
            if (!f.exists()) {
                path = "D:/AntiGravitySoftware/GitWorkspace/VortragAntigravity/vortrag_Claude/temp_audio/slide_" + number + ".mp3";
            }
        }

        File file = new File(path);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(resource);
    }

    // Serve generated presentation video (streams streamable MP4)
    @GetMapping("/projects/{id}/video")
    public ResponseEntity<Resource> getProjectVideo(@PathVariable Long id) {
        String path = "./data/projects/" + id + "/presentation_video.mp4";
        
        if (id == 1L) {
            File f = new File(path);
            if (!f.exists()) {
                path = "D:/AntiGravitySoftware/GitWorkspace/VortragAntigravity/vortrag_Claude/Antigravity_Vortrag_Video.mp4";
            }
        }

        File file = new File(path);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }

    // Serve PowerPoint download file
    @GetMapping("/projects/{id}/pptx")
    public ResponseEntity<Resource> getProjectPptx(@PathVariable Long id) {
        String path = "./data/projects/" + id + "/presentation.pptx";
        String downloadName = "vortrag.pptx";
        
        if (id == 1L) {
            File f = new File(path);
            if (!f.exists()) {
                path = "D:/AntiGravitySoftware/GitWorkspace/VortragAntigravity/vortrag_Claude/Antigravity_Vortrag.pptx";
            }
            downloadName = "Antigravity_Einfuehrung_Designer.pptx";
        }

        File file = new File(path);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .body(resource);
    }

    // Fetch prompt history
    @GetMapping("/prompt-history")
    public List<PromptHistory> getPromptHistory() {
        return promptHistoryRepository.findAllByOrderByUsedAtDesc();
    }

    // Clone project version
    @PostMapping("/projects/{id}/clone")
    public ResponseEntity<Project> cloneProject(@PathVariable Long id, @RequestParam(required = false) String versionName) {
        Optional<Project> optParent = projectRepository.findById(id);
        if (optParent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Project parent = optParent.get();
        
        List<Project> groupVersions = projectRepository.findByVersionGroupId(parent.getVersionGroupId());
        String nextVersion = versionName;
        if (nextVersion == null || nextVersion.trim().isEmpty()) {
            nextVersion = "v" + (groupVersions.size() + 1);
        }
        
        Project child = new Project();
        child.setName(parent.getName());
        child.setPrompt(parent.getPrompt());
        child.setModelName(parent.getModelName());
        child.setStatus(parent.getStatus());
        child.setParentId(parent.getId());
        child.setVersion(nextVersion);
        child.setVersionGroupId(parent.getVersionGroupId());
        
        Project savedChild = projectRepository.save(child);
        Long childId = savedChild.getId();
        
        // Clone slides
        List<Slide> parentSlides = slideRepository.findByProjectIdOrderBySlideNumberAsc(parent.getId());
        for (Slide ps : parentSlides) {
            Slide cs = new Slide();
            cs.setProjectId(childId);
            cs.setSlideNumber(ps.getSlideNumber());
            cs.setTitle(ps.getTitle());
            cs.setBullets(ps.getBullets());
            cs.setNotes(ps.getNotes());
            cs.setImageName(ps.getImageName());
            
            if (ps.getAudioFilePath() != null) {
                cs.setAudioFilePath("./data/projects/" + childId + "/audio/slide_" + ps.getSlideNumber() + ".mp3");
            }
            if (ps.getVideoFilePath() != null) {
                cs.setVideoFilePath("./data/projects/" + childId + "/temp_video/slide_" + ps.getSlideNumber() + ".mp4");
            }
            slideRepository.save(cs);
        }
        
        // Clone project images metadata in DB
        List<ProjectImage> parentImages = projectImageRepository.findByProjectId(parent.getId());
        for (ProjectImage pi : parentImages) {
            ProjectImage ci = new ProjectImage(childId, pi.getFilename(), pi.getDescription());
            projectImageRepository.save(ci);
        }
        
        // Clone files physically
        try {
            File parentDir = new File("./data/projects/" + parent.getId());
            File childDir = new File("./data/projects/" + childId);
            if (parentDir.exists()) {
                copyDirectory(parentDir, childDir);
                
                // Adjust paths in child entity
                if (parent.getPptxFilePath() != null) {
                    savedChild.setPptxFilePath("./data/projects/" + childId + "/presentation.pptx");
                }
                if (parent.getVideoFilePath() != null) {
                    savedChild.setVideoFilePath("./data/projects/" + childId + "/presentation_video.mp4");
                }
                savedChild = projectRepository.save(savedChild);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Kopieren der Dateien für Projekt-Klon: " + e.getMessage());
        }
        
        return ResponseEntity.ok(savedChild);
    }
    
    private void copyDirectory(File source, File destination) throws Exception {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    copyDirectory(new File(source, file), new File(destination, file));
                }
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // PowerPoint file upload and parse
    @PostMapping("/projects/upload-pptx")
    public ResponseEntity<Project> uploadPptx(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "name", required = false) String customName) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String originalFilename = file.getOriginalFilename();
            String name = (customName != null && !customName.trim().isEmpty()) 
                    ? customName.trim() 
                    : (originalFilename != null ? originalFilename.replace(".pptx", "") : "Importierter Vortrag");
            
            // Create Project
            Project project = new Project();
            project.setName(name);
            project.setPrompt("Importiert aus PowerPoint-Datei: " + originalFilename);
            project.setStatus("GENERATING_PPTX");
            project.setModelName("PowerPoint-Import");
            
            Project saved = projectRepository.save(project);
            Long id = saved.getId();
            
            // Set version group ID
            saved.setVersionGroupId(id);
            saved = projectRepository.save(saved);
            
            String projectDir = "./data/projects/" + id;
            File dir = new File(projectDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Save uploaded PPTX
            File pptxDest = new File(projectDir + "/presentation.pptx");
            file.transferTo(pptxDest);
            
            // Execute import_pptx.py script
            String jsonPath = projectDir + "/slides.json";
            pythonExecutionService.importPptx(pptxDest.getPath(), jsonPath);
            
            // Read slides.json and save Slide entries
            File jsonFile = new File(jsonPath);
            if (jsonFile.exists()) {
                JsonNode root = objectMapper.readTree(jsonFile);
                JsonNode slidesNode = root.path("slides");
                if (slidesNode.isArray()) {
                    for (JsonNode sn : slidesNode) {
                        int num = sn.path("slide_number").asInt();
                        String title = sn.path("title").asText("Folie");
                        
                        ArrayNode bulletsArray = objectMapper.createArrayNode();
                        JsonNode bulletsNode = sn.path("bullets");
                        if (bulletsNode.isArray()) {
                            for (JsonNode b : bulletsNode) {
                                bulletsArray.add(b.asText());
                            }
                        }
                        
                        String bulletsJson = bulletsArray.toString();
                        String notes = sn.path("notes").asText("");
                        String imageName = sn.path("image_name").asText("mascot_think.png");
                        
                        Slide slide = new Slide(id, num, title, bulletsJson, notes, imageName);
                        slideRepository.save(slide);
                    }
                }
            }
            
            // Export slides as images
            String slidesDir = projectDir + "/slides";
            pythonExecutionService.exportSlides(pptxDest.getPath(), slidesDir);
            
            // Update Project details
            saved.setPptxFilePath(pptxDest.getPath());
            saved.setStatus("PPTX_READY");
            projectRepository.save(saved);
            
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            System.err.println("Error importing uploaded PowerPoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update project prompt
    @PutMapping("/projects/{id}/prompt")
    public ResponseEntity<Project> updateProjectPrompt(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return projectRepository.findById(id)
                .map(project -> {
                    String prompt = body.get("prompt");
                    if (prompt != null) {
                        if (!prompt.equals(project.getPrompt())) {
                            project.setPrompt(prompt);
                            // Clear existing slides for this project
                            List<Slide> slides = slideRepository.findByProjectIdOrderBySlideNumberAsc(id);
                            for (Slide s : slides) {
                                if (s.getAudioFilePath() != null) {
                                    try {
                                        new File(s.getAudioFilePath()).delete();
                                    } catch (Exception e) {
                                        System.err.println("Could not delete audio file: " + e.getMessage());
                                    }
                                }
                                if (s.getVideoFilePath() != null) {
                                    try {
                                        new File(s.getVideoFilePath()).delete();
                                    } catch (Exception e) {
                                        System.err.println("Could not delete video file: " + e.getMessage());
                                    }
                                }
                                slideRepository.delete(s);
                            }
                            // Reset file paths and status
                            project.setPptxFilePath(null);
                            project.setVideoFilePath(null);
                            project.setStatus("CREATED");
                        }
                        Project saved = projectRepository.save(project);
                        return ResponseEntity.ok(saved);
                    }
                    return ResponseEntity.badRequest().<Project>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Get versions of a project group
    @GetMapping("/projects/{id}/versions")
    public ResponseEntity<List<Project>> getProjectVersions(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(project -> {
                    List<Project> versions = projectRepository.findByVersionGroupId(project.getVersionGroupId());
                    versions.sort((a, b) -> a.getId().compareTo(b.getId()));
                    return ResponseEntity.ok(versions);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ----------------------------------------------------
    // CUSTOM PROJECT IMAGES ENDPOINTS
    // ----------------------------------------------------

    // Get uploaded images for a project
    @GetMapping("/projects/{id}/custom-images")
    public List<ProjectImage> getCustomImages(@PathVariable Long id) {
        return projectImageRepository.findByProjectId(id);
    }

    // Upload custom image
    @PostMapping("/projects/{id}/custom-images")
    public ResponseEntity<ProjectImage> uploadCustomImage(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) {
                filename = "image_" + System.currentTimeMillis() + ".png";
            }
            
            // Clean filename to prevent path traversal
            filename = new File(filename).getName();

            String customImagesDir = "./data/projects/" + id + "/custom_images";
            File dir = new File(customImagesDir).getAbsoluteFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(dir, filename);
            file.transferTo(dest);

            ProjectImage img = new ProjectImage(id, filename, description);
            ProjectImage saved = projectImageRepository.save(img);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            System.err.println("Fehler beim Hochladen des Bildes: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update custom image description
    @PutMapping("/projects/{id}/custom-images/{imageId}")
    public ResponseEntity<ProjectImage> updateCustomImageDescription(
            @PathVariable Long id,
            @PathVariable Long imageId,
            @RequestBody java.util.Map<String, String> body) {
        
        return projectImageRepository.findById(imageId)
                .map(img -> {
                    String desc = body.get("description");
                    img.setDescription(desc);
                    ProjectImage saved = projectImageRepository.save(img);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete custom image
    @DeleteMapping("/projects/{id}/custom-images/{imageId}")
    public ResponseEntity<Void> deleteCustomImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        
        return projectImageRepository.findById(imageId)
                .map(img -> {
                    try {
                        String filePath = "./data/projects/" + id + "/custom_images/" + img.getFilename();
                        new File(filePath).delete();
                    } catch (Exception e) {
                        System.err.println("Could not delete physical image: " + e.getMessage());
                    }
                    projectImageRepository.delete(img);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Serve raw uploaded custom image file
    @GetMapping("/projects/{id}/custom-images/file/{filename}")
    public ResponseEntity<Resource> getCustomImageFile(@PathVariable Long id, @PathVariable String filename) {
        // Prevent path traversal
        filename = new File(filename).getName();
        String path = "./data/projects/" + id + "/custom_images/" + filename;
        File file = new File(path);
        
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentType = "image/png";
        if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (filename.toLowerCase().endsWith(".gif")) {
            contentType = "image/gif";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
