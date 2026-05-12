package com.sistema.base.api.core.Lotizacion.Manzana;

import com.sistema.base.api.core.Lotizacion.Etapa.Etapa;
import com.sistema.base.api.core.Lotizacion.Etapa.EtapaRepository;
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
public class ManzanaService {

    private final ManzanaRepository manzanaRepository;
    private final EtapaRepository etapaRepository;

    @Transactional(readOnly = true)
    public List<Manzana> listarTodosActivos() {
        return manzanaRepository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public List<Manzana> listarPorEtapa(Long etapaId) {
        return manzanaRepository.findByEtapaIdAndEnabledTrueOrderByNombreAsc(etapaId);
    }

    @Transactional(readOnly = true)
    public Page<Manzana> listarPaginado(int page, int size, String search, Long etapaId, Long urbanizacionId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());

        // Limpiamos la búsqueda: Si mandan un texto vacío o puros espacios, lo volvemos null
        String nombreBusqueda = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Mandamos todo al repositorio, la BD se encarga de ignorar los nulos
        return manzanaRepository.findByFiltrosPaginado(urbanizacionId, etapaId, nombreBusqueda, pageable);
    }

    @Transactional(readOnly = true)
    public Manzana obtenerPorId(Long id) {
        return manzanaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manzana no encontrada"));
    }

    @Transactional
    public Manzana guardar(Manzana manzana) {
        Etapa etapa = etapaRepository.findById(manzana.getEtapa().getId())
                .orElseThrow(() -> new RuntimeException("La etapa asignada no existe"));
        manzana.setEtapa(etapa);
        return manzanaRepository.save(manzana);
    }

    @Transactional
    public Manzana actualizar(Long id, Manzana request) {
        Manzana manzana = obtenerPorId(id);
        Etapa etapa = etapaRepository.findById(request.getEtapa().getId())
                .orElseThrow(() -> new RuntimeException("La etapa asignada no existe"));

        manzana.setNombre(request.getNombre());
        manzana.setEtapa(etapa);
        manzana.setEnabled(request.isEnabled());
        return manzanaRepository.save(manzana);
    }

    @Transactional
    public void eliminar(Long id) {
        Manzana manzana = obtenerPorId(id);
        manzana.setEnabled(false);
        manzanaRepository.save(manzana);
    }
}
