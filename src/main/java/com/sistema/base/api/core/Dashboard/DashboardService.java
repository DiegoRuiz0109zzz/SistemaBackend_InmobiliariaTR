package com.sistema.base.api.core.Dashboard;

import com.sistema.base.api.core.Dashboard.dtos.DashboardKpiDTO;
import com.sistema.base.api.core.Dashboard.dtos.DashboardResponseDTO;
import com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Lotizacion.Lote.EstadoLote;
import com.sistema.base.api.core.Lotizacion.Lote.LoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LoteRepository loteRepository;
    private final ContratoRepository contratoRepository;
    private final CuotaRepository cuotaRepository;

    public DashboardResponseDTO getDashboardData(Long urbanizacionId, Long etapaId, Long manzanaId, Integer anio) {
        if (anio == null) {
            anio = java.time.LocalDate.now().getYear();
        }

        // Lotes (Usando métodos seguros para evitar caídas del servidor)
        Long totalLotes = safeLong(loteRepository.countTotalLotes(urbanizacionId, etapaId, manzanaId));
        Long lotesVendidos = safeLong(loteRepository.countLotesByEstado(EstadoLote.VENDIDO, urbanizacionId, etapaId, manzanaId));
        Long lotesDisponibles = safeLong(loteRepository.countLotesByEstado(EstadoLote.DISPONIBLE, urbanizacionId, etapaId, manzanaId));

        Double valorTotal = safeDouble(loteRepository.sumValorTotalLotes(urbanizacionId, etapaId, manzanaId));
        Double valorVendido = safeDouble(loteRepository.sumValorLotesByEstado(EstadoLote.VENDIDO, urbanizacionId, etapaId, manzanaId));
        Double valorDisponible = safeDouble(loteRepository.sumValorLotesByEstado(EstadoLote.DISPONIBLE, urbanizacionId, etapaId, manzanaId));
        Double valorReservado = safeDouble(loteRepository.sumValorLotesByEstado(EstadoLote.RESERVADO, urbanizacionId, etapaId, manzanaId));

        Double valorPotencial = valorDisponible + valorReservado;

        Double porcentajeVentasCantidad = (totalLotes > 0) ? (lotesVendidos * 100.0 / totalLotes) : 0.0;
        Double porcentajeVentasMonto = (valorTotal > 0) ? (valorVendido * 100.0 / valorTotal) : 0.0;

        // Financieros
        Double totalCobrado = safeDouble(cuotaRepository.sumMontoPagado(urbanizacionId, etapaId, manzanaId));
        Double totalPorCobrar = safeDouble(cuotaRepository.sumMontoPorCobrar(urbanizacionId, etapaId, manzanaId));
        Double totalFinanciero = totalCobrado + totalPorCobrar;
        Double porcentajeRecaudacion = (totalFinanciero > 0) ? (totalCobrado * 100.0 / totalFinanciero) : 0.0;

        DashboardKpiDTO kpis = DashboardKpiDTO.builder()
                .totalLotes(totalLotes)
                .lotesVendidos(lotesVendidos)
                .lotesDisponibles(lotesDisponibles)
                .valorTotal(valorTotal)
                .valorVendido(valorVendido)
                .valorDisponible(valorDisponible)
                .valorPotencial(valorPotencial)
                .porcentajeVentasCantidad(Math.round(porcentajeVentasCantidad * 100.0) / 100.0)
                .porcentajeVentasMonto(Math.round(porcentajeVentasMonto * 100.0) / 100.0)
                .totalCobrado(totalCobrado)
                .totalPorCobrar(totalPorCobrar)
                .porcentajeRecaudacion(Math.round(porcentajeRecaudacion * 100.0) / 100.0)
                .build();

        // Gráficos
        List<MensualChartDTO> ventasMensuales = contratoRepository.findVentasMensuales(anio, urbanizacionId, etapaId, manzanaId);
        List<MensualChartDTO> proyeccionCobros = cuotaRepository.findProyeccionCobrosMensuales(anio, urbanizacionId, etapaId, manzanaId);

        return DashboardResponseDTO.builder()
                .kpis(kpis)
                .ventasPorMes(fillMonths(ventasMensuales))
                .proyeccionCobrosPorMes(fillMonths(proyeccionCobros))
                .build();
    }

    private List<MensualChartDTO> fillMonths(List<MensualChartDTO> data) {
        String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        List<MensualChartDTO> result = new java.util.ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            final int monthStr = i;
            MensualChartDTO dto = data.stream()
                    .filter(d -> d.getMes() != null && d.getMes().equals(String.valueOf(monthStr)))
                    .findFirst()
                    .orElse(new MensualChartDTO(String.valueOf(i), 0L, 0.0));

            result.add(new MensualChartDTO(meses[i - 1], dto.getCantidadContratos(), dto.getMontoVendido()));
        }
        return result;
    }

    // --- MÉTODOS DE SEGURIDAD (Evitan el NullPointerException) ---
    private Double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private Long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}