package com.sistema.base.api.core.Lotizacion.Etapa;

import com.sistema.base.api.core.Lotizacion.Urbanizacion.Urbanizacion;
import com.sistema.base.api.core.Lotizacion.Urbanizacion.UrbanizacionRepository;
import lombok.RequiredArgsConstructor;
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
