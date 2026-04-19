package com.sistema.base.api.core.Vendedores;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendedorService {

    private final VendedorRepository vendedorRepository;

    @Transactional(readOnly = true)
    public List<Vendedor> listarTodosActivos() {
        return vendedorRepository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public Vendedor obtenerPorId(Long id) {
        return vendedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));
    }

    @Transactional
    public Vendedor guardar(Vendedor vendedor) {
        if (vendedorRepository.existsByNumeroDocumento(vendedor.getNumeroDocumento())) {
            throw new RuntimeException("El documento ya está registrado para otro vendedor.");
        }
        return vendedorRepository.save(vendedor);
    }

    @Transactional
    public Vendedor actualizar(Long id, Vendedor vendedorRequest) {
        Vendedor vendedor = obtenerPorId(id);

        vendedor.setTipoDocumento(vendedorRequest.getTipoDocumento());
        vendedor.setNombres(vendedorRequest.getNombres());
        vendedor.setApellidos(vendedorRequest.getApellidos());
        vendedor.setTelefono(vendedorRequest.getTelefono());
        vendedor.setEmail(vendedorRequest.getEmail());
        vendedor.setEnabled(vendedorRequest.isEnabled());
        return vendedorRepository.save(vendedor);
    }

    @Transactional
    public void eliminar(Long id) {
        Vendedor vendedor = obtenerPorId(id);
        vendedor.setEnabled(false);
        vendedorRepository.save(vendedor);
    }
}
