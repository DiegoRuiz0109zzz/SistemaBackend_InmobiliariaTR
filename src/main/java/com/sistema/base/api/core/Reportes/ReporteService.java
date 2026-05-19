package com.sistema.base.api.core.Reportes;

import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ContratoRepository contratoRepository;
    private final CuotaRepository cuotaRepository;

    public List<ReporteMaestroDTO> obtenerReporteMaestro() {
        List<Contrato> contratos = contratoRepository.findByEnabledTrue();
        List<ReporteMaestroDTO> reporte = new ArrayList<>();

        for (Contrato contrato : contratos) {
            List<Cuota> cuotas = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId());

            int cuotasPagas = 0;
            int cuotasVencidas = 0;
            int cuotasPendientes = 0;
            LocalDate hoy = LocalDate.now();

            for (Cuota cuota : cuotas) {
                if (cuota.getEstado() == EstadoCuota.PAGADO_TOTAL || cuota.getEstado() == EstadoCuota.PAGADO_DESTIEMPO) {
                    cuotasPagas++;
                } else if ((cuota.getEstado() == EstadoCuota.PENDIENTE || cuota.getEstado() == EstadoCuota.PAGADO_PARCIAL)
                        && cuota.getFechaVencimiento() != null && cuota.getFechaVencimiento().isBefore(hoy)) {
                    cuotasVencidas++;
                } else if (cuota.getEstado() == EstadoCuota.VENCIDO || cuota.getEstado() == EstadoCuota.VENCIDA) {
                    // Por si acaso existen estados vencidos explícitamente en BD
                    cuotasVencidas++;
                } else {
                    cuotasPendientes++;
                }
            }

            String nombreVendedor = "N/A";
            if (contrato.getVendedor() != null) {
                nombreVendedor = contrato.getVendedor().getNombres();
                if (contrato.getVendedor().getApellidos() != null) {
                    nombreVendedor += " " + contrato.getVendedor().getApellidos();
                }
            }

            String nombreCliente = "N/A";
            if (contrato.getCliente() != null) {
                nombreCliente = contrato.getCliente().getNombres();
                if (contrato.getCliente().getApellidos() != null) {
                    nombreCliente += " " + contrato.getCliente().getApellidos();
                }
            }

            String numeroLote = "N/A";
            String urbanizacion = "N/A";
            String etapa = "N/A";
            String manzana = "N/A";
            Double precioOficinaLote = 0.0;

            if (contrato.getLote() != null) {
                numeroLote = contrato.getLote().getNumero();
                precioOficinaLote = contrato.getLote().getPrecioVenta();
                if (contrato.getLote().getManzana() != null) {
                    manzana = contrato.getLote().getManzana().getNombre();
                    if (manzana == null) manzana = contrato.getLote().getManzana().getLetra();
                    if (contrato.getLote().getManzana().getEtapa() != null) {
                        etapa = contrato.getLote().getManzana().getEtapa().getNombre();
                        if (etapa == null && contrato.getLote().getManzana().getEtapa().getNumero() != null) {
                            etapa = contrato.getLote().getManzana().getEtapa().getNumero().toString();
                        }
                        if (contrato.getLote().getManzana().getEtapa().getUrbanizacion() != null) {
                            urbanizacion = contrato.getLote().getManzana().getEtapa().getUrbanizacion().getNombre();
                        }
                    }
                }
            }

            ReporteMaestroDTO dto = ReporteMaestroDTO.builder()
                    .idContrato(contrato.getId())
                    .nroContrato(contrato.getId()) // Se usa el ID por ahora
                    .fechaContrato(contrato.getFechaContrato())
                    .nombreVendedor(nombreVendedor.trim())
                    .nombreCliente(nombreCliente.trim())
                    .numeroLote(numeroLote)
                    .urbanizacion(urbanizacion)
                    .etapa(etapa)
                    .manzana(manzana)
                    .precioOficinaLote(precioOficinaLote)
                    .precioVentaFinal(contrato.getPrecioTotal())
                    .cuotasPagas(cuotasPagas)
                    .cuotasPendientes(cuotasPendientes)
                    .cuotasVencidas(cuotasVencidas)
                    .estadoContrato(contrato.getEstadoContrato())
                    .build();

            reporte.add(dto);
        }

        return reporte;
    }
}
