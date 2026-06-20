package com.vortrag.generator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "slides")
public class Slide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "slide_number", nullable = false)
    private Integer slideNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String bullets; // JSON representation of array of strings

    @Column(columnDefinition = "TEXT", nullable = false)
    private String notes; // Spoken script

    @Column(name = "image_name", nullable = false)
    private String imageName; // e.g. mascot_hello.png

    @Column(name = "audio_file_path")
    private String audioFilePath;

    @Column(name = "video_file_path")
    private String videoFilePath;

    // Constructors
    public Slide() {}

    public Slide(Long projectId, Integer slideNumber, String title, String bullets, String notes, String imageName) {
        this.projectId = projectId;
        this.slideNumber = slideNumber;
        this.title = title;
        this.bullets = bullets;
        this.notes = notes;
        this.imageName = imageName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Integer getSlideNumber() { return slideNumber; }
    public void setSlideNumber(Integer slideNumber) { this.slideNumber = slideNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBullets() { return bullets; }
    public void setBullets(String bullets) { this.bullets = bullets; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public String getAudioFilePath() { return audioFilePath; }
    public void setAudioFilePath(String audioFilePath) { this.audioFilePath = audioFilePath; }

    public String getVideoFilePath() { return videoFilePath; }
    public void setVideoFilePath(String videoFilePath) { this.videoFilePath = videoFilePath; }
}
