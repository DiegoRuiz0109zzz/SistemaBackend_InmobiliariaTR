package com.sistema.base.api.core.Financiamiento.Comision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComisionRepository extends JpaRepository<Comision, Long> {

    // Para saber si un contrato ya generó comisiones y no duplicarlas
    boolean existsByContratoId(Long contratoId);

    // Para listar comisiones de un contrato específico
    List<Comision> findByContratoId(Long contratoId);
}