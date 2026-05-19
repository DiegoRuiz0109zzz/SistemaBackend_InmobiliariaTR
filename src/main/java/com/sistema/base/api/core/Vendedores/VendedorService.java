package com.sistema.base.api.core.Vendedores;

import com.sistema.base.api.entity.User;
import com.sistema.base.api.repository.UserRepository; // <- NECESITAMOS ESTO
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendedorService {

    private final VendedorRepository vendedorRepository;
    private final UserRepository userRepository; // <- INYECTADO

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

        // NUEVO: Validar y asignar el Jefe de Ventas si viene en la petición
        if (vendedor.getJefeVentas() != null && vendedor.getJefeVentas().getId() != null) {
            User jefe = userRepository.findById(vendedor.getJefeVentas().getId())
                    .orElseThrow(() -> new RuntimeException("Jefe de ventas no encontrado en el sistema"));
            vendedor.setJefeVentas(jefe);
        } else {
            vendedor.setJefeVentas(null);
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

        // NUEVO: Actualizar el Jefe de Ventas
        if (vendedorRequest.getJefeVentas() != null && vendedorRequest.getJefeVentas().getId() != null) {
            User jefe = userRepository.findById(vendedorRequest.getJefeVentas().getId())
                    .orElseThrow(() -> new RuntimeException("Jefe de ventas no encontrado en el sistema"));
            vendedor.setJefeVentas(jefe);
        } else {
            vendedor.setJefeVentas(null); // Si desde el frontend mandan null, se le quita el jefe
        }

        return vendedorRepository.save(vendedor);
    }

    @Transactional
    public void eliminar(Long id) {
        Vendedor vendedor = obtenerPorId(id);
        vendedor.setEnabled(false); // Eliminación lógica
        vendedorRepository.save(vendedor);
    }
}