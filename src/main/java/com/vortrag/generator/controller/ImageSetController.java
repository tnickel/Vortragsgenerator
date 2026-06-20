package com.vortrag.generator.controller;

import com.vortrag.generator.model.ImageSet;
import com.vortrag.generator.model.ImageSetEntry;
import com.vortrag.generator.model.ProjectImage;
import com.vortrag.generator.repository.ImageSetEntryRepository;
import com.vortrag.generator.repository.ImageSetRepository;
import com.vortrag.generator.repository.ProjectImageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ImageSetController {

    private final ImageSetRepository imageSetRepository;
    private final ImageSetEntryRepository imageSetEntryRepository;
    private final ProjectImageRepository projectImageRepository;

    public ImageSetController(ImageSetRepository imageSetRepository,
                              ImageSetEntryRepository imageSetEntryRepository,
                              ProjectImageRepository projectImageRepository) {
        this.imageSetRepository = imageSetRepository;
        this.imageSetEntryRepository = imageSetEntryRepository;
        this.projectImageRepository = projectImageRepository;
    }

    // List all sets
    @GetMapping("/image-sets")
    public List<ImageSet> listImageSets() {
        return imageSetRepository.findAll();
    }

    // Save project images as a named set
    @PostMapping("/image-sets")
    public ResponseEntity<ImageSet> saveImageSet(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        Long projectId = null;
        if (payload.get("projectId") != null) {
            projectId = Long.valueOf(payload.get("projectId").toString());
        }

        if (name == null || name.trim().isEmpty() || projectId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ProjectImage> projectImages = projectImageRepository.findByProjectId(projectId);
        if (projectImages.isEmpty()) {
            return ResponseEntity.badRequest().build(); // Cannot save an empty set
        }

        // Save Set Metadata
        ImageSet set = new ImageSet(name.trim());
        ImageSet savedSet = imageSetRepository.save(set);
        Long setId = savedSet.getId();

        // Create directory for the set images
        File setDir = new File("./data/image_sets/" + setId).getAbsoluteFile();
        if (!setDir.exists()) {
            setDir.mkdirs();
        }

        File projectImagesDir = new File("./data/projects/" + projectId + "/custom_images").getAbsoluteFile();

        // Save Entries and copy files
        for (ProjectImage pImg : projectImages) {
            ImageSetEntry entry = new ImageSetEntry(setId, pImg.getFilename(), pImg.getDescription());
            imageSetEntryRepository.save(entry);

            // Copy physical file to the set directory
            File srcFile = new File(projectImagesDir, pImg.getFilename());
            File destFile = new File(setDir, pImg.getFilename());
            if (srcFile.exists()) {
                try {
                    Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.err.println("Fehler beim Kopieren von " + srcFile + " nach " + destFile + ": " + e.getMessage());
                }
            }
        }

        return ResponseEntity.ok(savedSet);
    }

    // Import a set into a project
    @PostMapping("/image-sets/{setId}/import")
    public ResponseEntity<Void> importImageSet(
            @PathVariable Long setId,
            @RequestParam("projectId") Long projectId) {

        Optional<ImageSet> optSet = imageSetRepository.findById(setId);
        if (optSet.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<ImageSetEntry> entries = imageSetEntryRepository.findBySetId(setId);
        if (entries.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Target project directory
        File projectImagesDir = new File("./data/projects/" + projectId + "/custom_images").getAbsoluteFile();
        if (!projectImagesDir.exists()) {
            projectImagesDir.mkdirs();
        }

        File setDir = new File("./data/image_sets/" + setId).getAbsoluteFile();

        // Process entries
        for (ImageSetEntry entry : entries) {
            // Check if already mapped in project_images table to avoid duplicates
            List<ProjectImage> existing = projectImageRepository.findByProjectId(projectId);
            boolean alreadyExists = existing.stream()
                    .anyMatch(img -> img.getFilename().equals(entry.getFilename()));

            if (!alreadyExists) {
                ProjectImage pImg = new ProjectImage(projectId, entry.getFilename(), entry.getDescription());
                projectImageRepository.save(pImg);
            }

            // Copy file from set directory to project directory
            File srcFile = new File(setDir, entry.getFilename());
            File destFile = new File(projectImagesDir, entry.getFilename());
            if (srcFile.exists()) {
                try {
                    Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.err.println("Fehler beim Kopieren von " + srcFile + " nach " + destFile + ": " + e.getMessage());
                }
            }
        }

        return ResponseEntity.ok().build();
    }

    // Delete a set
    @DeleteMapping("/image-sets/{setId}")
    public ResponseEntity<Void> deleteImageSet(@PathVariable Long setId) {
        if (!imageSetRepository.existsById(setId)) {
            return ResponseEntity.notFound().build();
        }

        // Delete from DB
        imageSetRepository.deleteById(setId); // Cascades to image_set_entries

        // Delete files from disk
        File setDir = new File("./data/image_sets/" + setId).getAbsoluteFile();
        if (setDir.exists()) {
            deleteFolderRecursive(setDir);
        }

        return ResponseEntity.ok().build();
    }

    private void deleteFolderRecursive(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolderRecursive(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}
