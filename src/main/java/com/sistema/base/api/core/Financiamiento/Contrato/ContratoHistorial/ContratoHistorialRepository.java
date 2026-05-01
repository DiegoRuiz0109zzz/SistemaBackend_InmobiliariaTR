package com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContratoHistorialRepository extends JpaRepository<ContratoHistorial, Long> {

    // Trae el historial de un contrato ordenado por fecha de registro (del más reciente al más antiguo)
    List<ContratoHistorial> findByContratoIdOrderByFechaRegistroDesc(Long contratoId);
}
