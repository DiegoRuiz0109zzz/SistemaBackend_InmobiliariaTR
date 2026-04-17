package com.sistema.base.api.core.Usuario.Clientes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    Boolean existsByNumeroDocumento(String numeroDocumento);
}
