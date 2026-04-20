package com.sistema.base.api.core.Lotizacion.Lote;

import com.sistema.base.api.core.Lotizacion.Manzana.Manzana;
import com.sistema.base.api.core.Lotizacion.Manzana.ManzanaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository loteRepository;
    private final ManzanaRepository manzanaRepository;

    @Transactional(readOnly = true)
    public List<Lote> listarTodosActivos() {
        return loteRepository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public List<Lote> listarPorManzana(Long manzanaId) {
        return loteRepository.findByManzanaIdAndEnabledTrue(manzanaId);
    }

    @Transactional(readOnly = true)
    public Lote obtenerPorId(Long id) {
        return loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));
    }

    @Transactional
    public Lote guardar(Lote lote) {
        Manzana manzana = manzanaRepository.findById(lote.getManzana().getId())
                .orElseThrow(() -> new RuntimeException("La manzana asignada no existe"));
        lote.setManzana(manzana);
        return loteRepository.save(lote);
    }

    @Transactional
    public Lote actualizar(Long id, Lote request) {
        Lote lote = obtenerPorId(id);
        Manzana manzana = manzanaRepository.findById(request.getManzana().getId())
                .orElseThrow(() -> new RuntimeException("La manzana asignada no existe"));

        lote.setNumero(request.getNumero());
        lote.setArea(request.getArea());
        lote.setPrecioMetroCuadrado(request.getPrecioMetroCuadrado());
        lote.setPrecioCosto(request.getPrecioCosto());
        lote.setPrecioVenta(request.getPrecioVenta());
        lote.setEstadoVenta(request.getEstadoVenta());
        lote.setManzana(manzana);
        lote.setEnabled(request.isEnabled());

        return loteRepository.save(lote);
    }

    public Double calcularCostoBase(Double area, Double precioMetroCuadrado) {
        if (area == null || precioMetroCuadrado == null || area <= 0 || precioMetroCuadrado <= 0) {
            return 0.0;
        }
        // Redondeamos a 2 decimales por ser moneda
        return Math.round((area * precioMetroCuadrado) * 100.0) / 100.0;
    }

    @Transactional
    public void eliminar(Long id) {
        Lote lote = obtenerPorId(id);
        lote.setEnabled(false);
        loteRepository.save(lote);
    }
}
