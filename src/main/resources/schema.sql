-- Database schema for Presentation Generator

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    prompt TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    model_name VARCHAR(100),
    pptx_file_path VARCHAR(500),
    video_file_path VARCHAR(500),
    parent_id BIGINT,
    version VARCHAR(50) DEFAULT 'v1',
    version_group_id BIGINT
);

CREATE TABLE IF NOT EXISTS slides (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    slide_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    bullets TEXT NOT NULL, -- Stored as JSON string
    notes TEXT NOT NULL,
    image_name VARCHAR(100) NOT NULL,
    audio_file_path VARCHAR(500),
    video_file_path VARCHAR(500),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS prompt_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    prompt TEXT NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_name VARCHAR(100) UNIQUE NOT NULL,
    val_value VARCHAR(500) NOT NULL
);

CREATE TABLE IF NOT EXISTS project_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    description TEXT,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS image_sets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS image_set_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    set_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    description TEXT,
    FOREIGN KEY (set_id) REFERENCES image_sets(id) ON DELETE CASCADE
);

