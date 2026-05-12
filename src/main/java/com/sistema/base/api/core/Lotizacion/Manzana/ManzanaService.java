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
    // ✅ NUEVO: Método paginado con orden Ascendente y doble filtro (Buscador + Etapa)
    @Transactional(readOnly = true)
    public Page<Manzana> listarPaginado(int page, int size, String search, Long etapaId) {
        // ✅ CAMBIO CLAVE: Ahora ordenamos por "nombre" de forma ascendente (A -> Z)
        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasEtapa = etapaId != null;

        if (hasEtapa && hasSearch) {
            return manzanaRepository.findByEnabledTrueAndEtapaIdAndNombreContainingIgnoreCase(etapaId, search, pageable);
        }
        else if (hasEtapa) {
            return manzanaRepository.findByEnabledTrueAndEtapaId(etapaId, pageable);
        }
        else if (hasSearch) {
            return manzanaRepository.findByEnabledTrueAndNombreContainingIgnoreCase(search, pageable);
        }
        else {
            return manzanaRepository.findByEnabledTrue(pageable);
        }
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
