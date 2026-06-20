package com.vortrag.generator.repository;

import com.vortrag.generator.model.Slide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SlideRepository extends JpaRepository<Slide, Long> {
    List<Slide> findByProjectIdOrderBySlideNumberAsc(Long projectId);
    void deleteByProjectId(Long projectId);
}
