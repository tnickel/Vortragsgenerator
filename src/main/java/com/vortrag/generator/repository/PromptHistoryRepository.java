package com.vortrag.generator.repository;

import com.vortrag.generator.model.PromptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PromptHistoryRepository extends JpaRepository<PromptHistory, Long> {
    List<PromptHistory> findAllByOrderByUsedAtDesc();
}
