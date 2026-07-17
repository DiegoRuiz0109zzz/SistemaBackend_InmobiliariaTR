package com.sistema.base.api.core.Dashboard;

import com.sistema.base.api.core.Dashboard.dtos.DashboardKpiDTO;
import com.sistema.base.api.core.Dashboard.dtos.DashboardResponseDTO;
import com.sistema.base.api.core.Dashboard.dtos.MensualChartDTO;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Pago.PagoRepository;
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
    private final PagoRepository pagoRepository; // ✅ INYECTADO

    public DashboardResponseDTO getDashboardData(Long urbanizacionId, Long etapaId, Long manzanaId, Integer anio) {
        if (anio == null) {
            anio = java.time.LocalDate.now().getYear();
        }

        // Lotes
        Long totalLotes = safeLong(loteRepository.countTotalLotes(urbanizacionId, etapaId, manzanaId));
        Long lotesVendidos = safeLong(loteRepository.countLotesByEstado(EstadoLote.VENDIDO, urbanizacionId, etapaId, manzanaId));
        Long lotesDisponibles = safeLong(loteRepository.countLotesByEstado(EstadoLote.DISPONIBLE, urbanizacionId, etapaId, manzanaId));

        // Ingresos Potenciales (Lo que falta vender)
        Double valorDisponible = safeDouble(loteRepository.sumValorLotesByEstado(EstadoLote.DISPONIBLE, urbanizacionId, etapaId, manzanaId));
        Double valorReservado = safeDouble(loteRepository.sumValorLotesByEstado(EstadoLote.RESERVADO, urbanizacionId, etapaId, manzanaId));
        Double valorPotencial = valorDisponible + valorReservado;

        // Ingresos Asegurados (Lo que ya se vendió en contratos)
        Double valorVendido = safeDouble(contratoRepository.sumValorVentasReales(urbanizacionId, etapaId, manzanaId));
        Double valorTotal = valorVendido + valorPotencial;

        // Eficiencia de Cobro
        Double totalContratado = safeDouble(contratoRepository.sumPrecioTotalContratos(urbanizacionId, etapaId, manzanaId));
        Double totalCobrado = safeDouble(cuotaRepository.sumMontoPagado(urbanizacionId, etapaId, manzanaId));
        Double totalPorCobrar = safeDouble(cuotaRepository.sumMontoPorCobrar(urbanizacionId, etapaId, manzanaId));

        Double porcentajeEficienciaCobro = (totalContratado > 0) ? (totalCobrado * 100.0 / totalContratado) : 0.0;
        Double porcentajeVentasCantidad = (totalLotes > 0) ? (lotesVendidos * 100.0 / totalLotes) : 0.0;
        Double porcentajeVentasMonto = (valorTotal > 0) ? (valorVendido * 100.0 / valorTotal) : 0.0;

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
                .totalContratado(totalContratado) // ✅ Enviado al front
                .totalRecaudado(totalCobrado)     // ✅ Enviado al front
                .porcentajeRecaudacion(Math.round(porcentajeEficienciaCobro * 100.0) / 100.0)
                .build();

        // Gráficos
        List<MensualChartDTO> ventasMensuales = contratoRepository.findVentasMensuales(anio, urbanizacionId, etapaId, manzanaId);
        List<Object[]> recaudacionMensual = pagoRepository.findRecaudacionMensual(anio, urbanizacionId, etapaId, manzanaId);
        List<MensualChartDTO> proyeccionCobros = cuotaRepository.findProyeccionCobrosMensuales(anio, urbanizacionId, etapaId, manzanaId);

        return DashboardResponseDTO.builder()
                .kpis(kpis)
                .ventasPorMes(fillMonths(ventasMensuales, recaudacionMensual)) // ✅ Combina ventas y recaudación
                .proyeccionCobrosPorMes(fillMonths(proyeccionCobros, null))
                .build();
    }

    // ✅ MÉTODO ACTUALIZADO PARA COMBINAR DATOS
    private List<MensualChartDTO> fillMonths(List<MensualChartDTO> ventas, List<Object[]> recaudaciones) {
        String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        List<MensualChartDTO> result = new java.util.ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            final String monthStr = String.valueOf(i);

            MensualChartDTO dto = ventas.stream()
                    .filter(d -> d.getMes() != null && d.getMes().equals(monthStr))
                    .findFirst()
                    .orElse(new MensualChartDTO(monthStr, 0L, 0.0, 0.0));

            // Buscamos si hubo recaudación en este mes
            Double montoRecaudado = 0.0;
            if (recaudaciones != null) {
                for (Object[] rec : recaudaciones) {
                    if (monthStr.equals(String.valueOf(rec[0]))) {
                        montoRecaudado = rec[1] != null ? ((Number) rec[1]).doubleValue() : 0.0;
                        break;
                    }
                }
            }

            result.add(new MensualChartDTO(meses[i - 1], dto.getCantidadContratos(), dto.getMontoVendido(), montoRecaudado));
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