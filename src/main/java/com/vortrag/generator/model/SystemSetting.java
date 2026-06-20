package com.vortrag.generator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_name", unique = true, nullable = false)
    private String keyName;

    @Column(name = "val_value", nullable = false)
    private String valValue;

    // Constructors
    public SystemSetting() {}

    public SystemSetting(String keyName, String valValue) {
        this.keyName = keyName;
        this.valValue = valValue;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }

    public String getValValue() { return valValue; }
    public void setValValue(String valValue) { this.valValue = valValue; }
}
