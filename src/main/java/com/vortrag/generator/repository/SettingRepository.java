package com.vortrag.generator.repository;

import com.vortrag.generator.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findByKeyName(String keyName);
}
