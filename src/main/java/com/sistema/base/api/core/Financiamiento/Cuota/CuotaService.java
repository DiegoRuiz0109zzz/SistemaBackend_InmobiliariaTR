package com.sistema.base.api.core.Financiamiento.Cuota;

import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CuotaService {

    private final CuotaRepository cuotaRepository;
    private final ContratoRepository contratoRepository; // Se asumirá que lo crearás después

    @Transactional(readOnly = true)
    public List<Cuota> listarPorContrato(Long contratoId) {
        return cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contratoId);
    }

    @Transactional(readOnly = true)
    public Cuota obtenerPorId(Long id) {
        return cuotaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuota no encontrada"));
    }

    // La actualización de montos normalmente se hace a través de los Pagos (Amortizaciones),
    // pero dejamos este método para ajustes administrativos manuales.
    @Transactional
    public Cuota actualizar(Long id, Cuota request) {
        Cuota cuota = obtenerPorId(id);
        cuota.setFechaVencimiento(request.getFechaVencimiento());
        cuota.setMontoTotal(request.getMontoTotal());
        cuota.setEstado(request.getEstado());
        return cuotaRepository.save(cuota);
    }
}