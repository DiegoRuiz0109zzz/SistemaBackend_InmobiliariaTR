package com.sistema.base.api.core.Lotizacion.Urbanizacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// 1. UrbanizacionRepository.java
@Repository
public interface UrbanizacionRepository extends JpaRepository<Urbanizacion, Long> {
    List<Urbanizacion> findByEnabledTrue();

    // ✅ NUEVO: Paginación normal sin filtros
    Page<Urbanizacion> findByEnabledTrue(Pageable pageable);

    // ✅ NUEVO: Paginación + Búsqueda (Filtra por nombre ignorando mayúsculas/minúsculas)
    Page<Urbanizacion> findByEnabledTrueAndNombreContainingIgnoreCase(String nombre, Pageable pageable);

}