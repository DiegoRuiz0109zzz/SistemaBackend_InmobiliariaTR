package com.sistema.base.api.core.Financiamiento.Cuota;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CuotaRepository extends JpaRepository<Cuota, Long> {
    List<Cuota> findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(Long contratoId);
}