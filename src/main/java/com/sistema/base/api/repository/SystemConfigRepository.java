package com.sistema.base.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.base.api.entity.SystemConfig;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Integer> {
    
    Optional<SystemConfig> findByConfigKey(String configKey);
}
