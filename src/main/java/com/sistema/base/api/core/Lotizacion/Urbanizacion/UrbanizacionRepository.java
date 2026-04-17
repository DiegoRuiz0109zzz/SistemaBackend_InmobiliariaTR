package com.sistema.base.api.core.Lotizacion.Urbanizacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// 1. UrbanizacionRepository.java
@Repository
public interface UrbanizacionRepository extends JpaRepository<Urbanizacion, Long> {
    List<Urbanizacion> findByEnabledTrue();

}