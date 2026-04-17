package com.sistema.base.api.core.Financiamiento.Contrato;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    List<Contrato> findByEnabledTrue();
    List<Contrato> findByClienteIdAndEnabledTrue(Long clienteId);
}
