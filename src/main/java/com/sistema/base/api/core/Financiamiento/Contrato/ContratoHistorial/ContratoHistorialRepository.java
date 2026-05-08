package com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContratoHistorialRepository extends JpaRepository<ContratoHistorial, Long> {

    List<ContratoHistorial> findByContratoIdOrderByFechaRegistroDesc(Long contratoId);

    // ✅ CAMBIO: Ahora validamos duplicados por el tipo de registro en lugar del estado
    boolean existsByContratoIdAndTipoRegistro(Long contratoId, String tipoRegistro);
}