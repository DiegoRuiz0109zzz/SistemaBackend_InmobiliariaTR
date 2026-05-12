package com.sistema.base.api.core.Lotizacion.Etapa;

import com.sistema.base.api.core.Lotizacion.Urbanizacion.Urbanizacion;
import com.sistema.base.api.core.Lotizacion.Urbanizacion.UrbanizacionRepository;
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
public class EtapaService {

    private final EtapaRepository etapaRepository;
    private final UrbanizacionRepository urbanizacionRepository;

    @Transactional(readOnly = true)
    public List<Etapa> listarTodosActivos() {
        return etapaRepository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public List<Etapa> listarPorUrbanizacion(Long urbanizacionId) {
        return etapaRepository.findByUrbanizacionIdAndEnabledTrue(urbanizacionId);
    }

    // ✅ NUEVO: Método paginado con orden Ascendente y doble filtro
    @Transactional(readOnly = true)
    public Page<Etapa> listarPaginado(int page, int size, String search, Long urbanizacionId) {
        // Orden ASCENDENTE: Primero en entrar, primero en mostrarse
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasUrbanizacion = urbanizacionId != null;

        // Combinación 1: Tiene ID de Urbanización Y Texto de Búsqueda
        if (hasUrbanizacion && hasSearch) {
            return etapaRepository.findByEnabledTrueAndUrbanizacionIdAndNombreContainingIgnoreCase(urbanizacionId, search, pageable);
        }
        // Combinación 2: Solo tiene ID de Urbanización
        else if (hasUrbanizacion) {
            return etapaRepository.findByEnabledTrueAndUrbanizacionId(urbanizacionId, pageable);
        }
        // Combinación 3: Solo tiene Texto de Búsqueda
        else if (hasSearch) {
            return etapaRepository.findByEnabledTrueAndNombreContainingIgnoreCase(search, pageable);
        }
        // Combinación 4: Sin filtros, trae todo paginado
        else {
            return etapaRepository.findByEnabledTrue(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Etapa obtenerPorId(Long id) {
        return etapaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etapa no encontrada"));
    }

    @Transactional
    public Etapa guardar(Etapa etapa) {
        Urbanizacion urbanizacion = urbanizacionRepository.findById(etapa.getUrbanizacion().getId())
                .orElseThrow(() -> new RuntimeException("La urbanización asignada no existe"));
        etapa.setUrbanizacion(urbanizacion);
        return etapaRepository.save(etapa);
    }

    @Transactional
    public Etapa actualizar(Long id, Etapa request) {
        Etapa etapa = obtenerPorId(id);
        Urbanizacion urbanizacion = urbanizacionRepository.findById(request.getUrbanizacion().getId())
                .orElseThrow(() -> new RuntimeException("La urbanización asignada no existe"));

        etapa.setNombre(request.getNombre());
        etapa.setUrbanizacion(urbanizacion);
        etapa.setEnabled(request.isEnabled());
        return etapaRepository.save(etapa);
    }

    @Transactional
    public void eliminar(Long id) {
        Etapa etapa = obtenerPorId(id);
        etapa.setEnabled(false);
        etapaRepository.save(etapa);
    }
}
