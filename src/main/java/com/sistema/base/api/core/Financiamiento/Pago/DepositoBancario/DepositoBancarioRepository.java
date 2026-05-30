package com.sistema.base.api.core.Financiamiento.Pago.DepositoBancario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepositoBancarioRepository extends JpaRepository<DepositoBancario, Long> {

    // Para listar todos los depósitos que se hicieron con el dinero de un recibo específico
    List<DepositoBancario> findByNumeroReciboCaja(String numeroReciboCaja);
}
