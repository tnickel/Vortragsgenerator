package com.vortrag.generator.repository;

import com.vortrag.generator.model.ImageSetEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ImageSetEntryRepository extends JpaRepository<ImageSetEntry, Long> {
    List<ImageSetEntry> findBySetId(Long setId);
}
