package com.sistema.base.api.core.Lotizacion.Urbanizacion;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrbanizacionService {

    private final UrbanizacionRepository urbanizacionRepository;


    @Transactional(readOnly = true)
    public List<Urbanizacion> listarTodosActivos() {
        return urbanizacionRepository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public Page<Urbanizacion> listarPaginado(int page, int size, String search) {
        // "Sort.by("id").ascending()" asegura que Primavera 1 salga antes que Primavera 2
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        if (search != null && !search.trim().isEmpty()) {
            return urbanizacionRepository.findByEnabledTrueAndNombreContainingIgnoreCase(search, pageable);
        }
        return urbanizacionRepository.findByEnabledTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Urbanizacion obtenerPorId(Long id) {
        return urbanizacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Urbanización no encontrada"));
    }

    @Transactional
    public Urbanizacion guardar(Urbanizacion urbanizacion) {
        return urbanizacionRepository.save(urbanizacion);
    }

    @Transactional
    public Urbanizacion actualizar(Long id, Urbanizacion request) {
        Urbanizacion urbanizacion = obtenerPorId(id);
        urbanizacion.setNombre(request.getNombre());
        urbanizacion.setUbicacion(request.getUbicacion());
        urbanizacion.setEnabled(request.isEnabled());
        return urbanizacionRepository.save(urbanizacion);
    }

    @Transactional
    public void eliminar(Long id) {
        Urbanizacion urbanizacion = obtenerPorId(id);
        urbanizacion.setEnabled(false);
        urbanizacionRepository.save(urbanizacion);
    }
}
