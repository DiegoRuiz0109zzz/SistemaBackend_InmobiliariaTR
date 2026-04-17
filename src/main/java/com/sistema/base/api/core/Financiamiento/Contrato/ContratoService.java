package com.sistema.base.api.core.Financiamiento.Contrato;



import com.sistema.base.api.core.Financiamiento.Contrato.dtos.CuotaPreview;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.SimulacionRequest;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;
import com.sistema.base.api.core.Lotizacion.Lote.EstadoLote;
import com.sistema.base.api.core.Lotizacion.Lote.Lote;
import com.sistema.base.api.core.Lotizacion.Lote.LoteRepository;
import com.sistema.base.api.core.Usuario.Clientes.ClienteRepository;
import com.sistema.base.api.core.Vendedores.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final CuotaRepository cuotaRepository;
    private final LoteRepository loteRepository;
    private final ClienteRepository clienteRepository;
    private final VendedorRepository vendedorRepository;

    // 1. EL SIMULADOR (No guarda en base de datos, solo calcula)
    public List<CuotaPreview> simularCronograma(SimulacionRequest request) {
        List<CuotaPreview> cronograma = new ArrayList<>();

        Double saldoFinanciar = request.getPrecioTotal() - request.getMontoInicial();
        if (saldoFinanciar <= 0 || request.getCantidadCuotas() <= 0) {
            return cronograma; // Retorna vacío si se paga al contado
        }

        // Calculamos la cuota base redondeada a 2 decimales
        Double cuotaBase = Math.round((saldoFinanciar / request.getCantidadCuotas()) * 100.0) / 100.0;

        // Ajuste de centavos para la última cuota
        Double totalProyectado = cuotaBase * (request.getCantidadCuotas() - 1);
        Double ultimaCuota = Math.round((saldoFinanciar - totalProyectado) * 100.0) / 100.0;

        LocalDate fechaActual = request.getFechaInicioPago();

        for (int i = 1; i <= request.getCantidadCuotas(); i++) {
            Double montoAsignado = (i == request.getCantidadCuotas()) ? ultimaCuota : cuotaBase;

            cronograma.add(CuotaPreview.builder()
                    .numeroCuota(i)
                    .monto(montoAsignado)
                    .fechaVencimiento(fechaActual)
                    .build());

            // Sumar 1 mes exacto para la siguiente cuota
            fechaActual = fechaActual.plusMonths(1);
        }

        return cronograma;
    }

    // 2. GUARDAR CONTRATO REAL Y GENERAR CUOTAS
    @Transactional
    public Contrato generarContrato(Contrato contrato, LocalDate fechaInicioPago) {

        // A. Validar que las entidades existan
        Lote lote = loteRepository.findById(contrato.getLote().getId())
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

        if (lote.getEstadoVenta() == EstadoLote.VENDIDO) {
            throw new RuntimeException("Este lote ya se encuentra vendido.");
        }

        // B. Cálculos internos por seguridad (No confiamos en el cálculo del frontend)
        Double saldoFinanciar = contrato.getPrecioTotal() - contrato.getMontoInicial();
        contrato.setSaldoFinanciar(saldoFinanciar);

        // C. Guardar el Contrato padre
        Contrato contratoGuardado = contratoRepository.save(contrato);

        // D. Cambiar el estado del Lote a VENDIDO
        lote.setEstadoVenta(EstadoLote.VENDIDO);
        loteRepository.save(lote);

        // E. Generar y guardar las Cuotas automáticamente en BD
        if (saldoFinanciar > 0 && contrato.getCantidadCuotas() > 0) {
            SimulacionRequest simulacion = new SimulacionRequest();
            simulacion.setPrecioTotal(contrato.getPrecioTotal());
            simulacion.setMontoInicial(contrato.getMontoInicial());
            simulacion.setCantidadCuotas(contrato.getCantidadCuotas());
            simulacion.setFechaInicioPago(fechaInicioPago);

            List<CuotaPreview> proyeccion = simularCronograma(simulacion);

            List<Cuota> cuotasAGuardar = new ArrayList<>();
            for (CuotaPreview preview : proyeccion) {
                Cuota nuevaCuota = Cuota.builder()
                        .contrato(contratoGuardado)
                        .numeroCuota(preview.getNumeroCuota())
                        .montoTotal(preview.getMonto())
                        .montoPagado(0.0)
                        .fechaVencimiento(preview.getFechaVencimiento())
                        .estado(EstadoCuota.PENDIENTE)
                        .enabled(true)
                        .build();
                cuotasAGuardar.add(nuevaCuota);
            }
            // Guardado masivo de cuotas
            cuotaRepository.saveAll(cuotasAGuardar);
        }

        return contratoGuardado;
    }

    @Transactional(readOnly = true)
    public List<Contrato> listarTodos() {
        return contratoRepository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public Contrato obtenerPorId(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
    }
}
