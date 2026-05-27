package com.sistema.base.api.core.Financiamiento.Pago.Serie;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SerieService {

    private final SerieRepository serieRepository;

    @Transactional(readOnly = true)
    public List<Serie> listarTodas() {
        return serieRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Serie obtenerPorId(Long id) {
        return serieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serie no encontrada con el ID: " + id));
    }

    @Transactional
    public Serie guardar(Serie serie) {
        // Validamos que no exista una combinación de Tipo y código de Serie duplicada y activa
        return serieRepository.save(serie);
    }

    @Transactional
    public Serie actualizar(Long id, Serie request) {
        Serie serie = obtenerPorId(id);

        serie.setTipoComprobante(request.getTipoComprobante());
        serie.setSerie(request.getSerie());
        serie.setUltimoCorrelativo(request.getUltimoCorrelativo());
        serie.setActivo(request.isActivo());

        return serieRepository.save(serie);
    }

    @Transactional
    public void eliminar(Long id) {
        Serie serie = obtenerPorId(id);
        // Por integridad contable, si ya se usó la serie, es mejor desactivarla.
        // Pero si deseas eliminación física completa, usamos el delete:
        serieRepository.delete(serie);
    }

    @Transactional
    public Serie cambiarEstadoActivo(Long id, boolean activo) {
        Serie serie = obtenerPorId(id);
        serie.setActivo(activo);
        return serieRepository.save(serie);
    }
}
