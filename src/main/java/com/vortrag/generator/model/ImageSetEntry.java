package com.vortrag.generator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "image_set_entries")
public class ImageSetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "set_id", nullable = false)
    private Long setId;

    @Column(nullable = false)
    private String filename;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Constructors
    public ImageSetEntry() {}

    public ImageSetEntry(Long setId, String filename, String description) {
        this.setId = setId;
        this.filename = filename;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSetId() { return setId; }
    public void setSetId(Long setId) { this.setId = setId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
