package com.sistema.base.api.core.Lotizacion.Manzana;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManzanaRepository extends JpaRepository<Manzana, Long> {
    List<Manzana> findByEnabledTrue();
    List<Manzana> findByEtapaIdAndEnabledTrueOrderByNombreAsc(Long etapaId);// Para el combo en cascada

    // ✅ PARA LA TABLA PAGINADA: Las 4 combinaciones posibles
    Page<Manzana> findByEnabledTrue(Pageable pageable);

    Page<Manzana> findByEnabledTrueAndNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Manzana> findByEnabledTrueAndEtapaId(Long etapaId, Pageable pageable);

    Page<Manzana> findByEnabledTrueAndEtapaIdAndNombreContainingIgnoreCase(Long etapaId, String nombre, Pageable pageable);
}
