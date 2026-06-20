package com.vortrag.generator.repository;

import com.vortrag.generator.model.ImageSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageSetRepository extends JpaRepository<ImageSet, Long> {
}
