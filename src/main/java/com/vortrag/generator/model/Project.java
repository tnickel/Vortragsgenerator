package com.vortrag.generator.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Column(nullable = false)
    private String status; // CREATED, GENERATING_PPTX, PPTX_READY, GENERATING_AUDIO, COMPLETED, FAILED

    @Column(name = "created_at", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "pptx_file_path")
    private String pptxFilePath;

    @Column(name = "video_file_path")
    private String videoFilePath;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private String version = "v1";

    @Column(name = "version_group_id")
    private Long versionGroupId;

    // Constructors
    public Project() {
        this.status = "CREATED";
        this.version = "v1";
    }

    public Project(String name, String prompt, String modelName) {
        this.name = name;
        this.prompt = prompt;
        this.modelName = modelName;
        this.status = "CREATED";
        this.version = "v1";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getPptxFilePath() { return pptxFilePath; }
    public void setPptxFilePath(String pptxFilePath) { this.pptxFilePath = pptxFilePath; }

    public String getVideoFilePath() { return videoFilePath; }
    public void setVideoFilePath(String videoFilePath) { this.videoFilePath = videoFilePath; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Long getVersionGroupId() { return versionGroupId; }
    public void setVersionGroupId(Long versionGroupId) { this.versionGroupId = versionGroupId; }
}
