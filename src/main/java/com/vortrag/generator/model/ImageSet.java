package com.vortrag.generator.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "image_sets")
public class ImageSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ImageSet() {}

    public ImageSet(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
