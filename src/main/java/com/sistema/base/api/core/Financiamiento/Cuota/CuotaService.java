package com.sistema.base.api.core.Financiamiento.Cuota;

import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CuotaService {

    private final CuotaRepository cuotaRepository;
    private final ContratoRepository contratoRepository;

    @Transactional(readOnly = true)
    public List<Cuota> listarPorContrato(Long contratoId) {
        return cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contratoId);
    }

    @Transactional(readOnly = true)
    public Cuota obtenerPorId(Long id) {
        return cuotaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuota no encontrada"));
    }

    @Transactional
    public Cuota actualizar(Long id, Cuota request) {
        Cuota cuota = obtenerPorId(id);

        // Actualizamos los campos permitidos administrativamente
        cuota.setFechaVencimiento(request.getFechaVencimiento());
        cuota.setMontoTotal(request.getMontoTotal());
        cuota.setEstado(request.getEstado());

        // ✅ Añadimos la capacidad de corregir el tipo de cuota
        if (request.getTipoCuota() != null) {
            cuota.setTipoCuota(request.getTipoCuota());
        }

        return cuotaRepository.save(cuota);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void actualizarCuotasVencidas() {
        LocalDate hoy = LocalDate.now();

        // 1. Buscamos todas las cuotas que deben dinero y su fecha ya pasó
        // (Asegúrate de crear este método en tu CuotaRepository)
        List<Cuota> cuotasExpiradas = cuotaRepository.findByEstadoInAndFechaVencimientoBefore(
                Arrays.asList(EstadoCuota.PENDIENTE, EstadoCuota.PAGADO_PARCIAL),
                hoy
        );

        // 2. Las pasamos a estado VENCIDA (o ATRASADA, según tu Enum)
        for (Cuota cuota : cuotasExpiradas) {
            cuota.setEstado(EstadoCuota.VENCIDO);
        }

        cuotaRepository.saveAll(cuotasExpiradas);
        System.out.println("Cron Job: Se actualizaron " + cuotasExpiradas.size() + " cuotas a VENCIDA.");
    }
}