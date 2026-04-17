package com.sistema.base.api.core.Vendedores;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendedorRepository extends JpaRepository<Vendedor, Long> {
    Boolean existsByNumeroDocumento(String numeroDocumento);
    java.util.List<Vendedor> findByEnabledTrue();
}