package com.vortrag.generator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "project_images")
public class ProjectImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String filename;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Constructors
    public ProjectImage() {}

    public ProjectImage(Long projectId, String filename, String description) {
        this.projectId = projectId;
        this.filename = filename;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
