package com.sistema.base.api.core.Financiamiento.Contrato;



import com.sistema.base.api.core.Financiamiento.Contrato.dtos.ContratoRequest;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.CuotaPreview;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.SimulacionRequest;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;
import com.sistema.base.api.core.Financiamiento.Pago.Pago;
import com.sistema.base.api.core.Financiamiento.Pago.PagoRepository;
import com.sistema.base.api.core.Lotizacion.Lote.EstadoLote;
import com.sistema.base.api.core.Lotizacion.Lote.Lote;
import com.sistema.base.api.core.Lotizacion.Lote.LoteRepository;
import com.sistema.base.api.core.Usuario.Clientes.ClienteRepository;
import com.sistema.base.api.core.Vendedores.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
    private final PagoRepository pagoRepository;

    // 1. EL SIMULADOR (No guarda en base de datos, solo calcula)
    public List<CuotaPreview> simularCronograma(SimulacionRequest request) {
        List<CuotaPreview> cronograma = new ArrayList<>();
        Double saldoFinanciar = request.getPrecioTotal() - request.getMontoInicial();

        if (saldoFinanciar <= 0 || request.getCantidadCuotas() <= 0) return cronograma;

        int cuotasTotales = request.getCantidadCuotas();
        int cuotasEspeciales = (request.getCuotasEspeciales() != null) ? request.getCuotasEspeciales() : 0;
        Double montoEspecial = (request.getMontoCuotaEspecial() != null) ? request.getMontoCuotaEspecial() : 0.0;

        // Descontar las cuotas especiales del saldo
        Double saldoRestante = saldoFinanciar;
        if (cuotasEspeciales > 0 && montoEspecial > 0) {
            saldoRestante -= (cuotasEspeciales * montoEspecial);
        }

        // Calcular la cuota base para los meses restantes
        int cuotasNormales = cuotasTotales - cuotasEspeciales;
        Double cuotaBase = (cuotasNormales > 0) ? Math.round((saldoRestante / cuotasNormales) * 100.0) / 100.0 : 0.0;

        // Configuración del calendario estricto
        LocalDate fechaInicial = request.getFechaInicioPago();
        int diaPreferido = fechaInicial.getDayOfMonth(); // Ej: Si es 30, guardamos el 30
        YearMonth mesActual = YearMonth.from(fechaInicial);

        for (int i = 0; i < cuotasTotales; i++) {
            // Asignar monto: Si estamos dentro de las N primeras cuotas, usamos el especial
            Double montoAsignado = (i < cuotasEspeciales) ? montoEspecial : cuotaBase;

            // Ajuste perfecto de centavos en la última cuota para que el total cuadre exacto
            if (i == cuotasTotales - 1 && cuotasNormales > 0) {
                Double totalEspeciales = cuotasEspeciales * montoEspecial;
                Double totalNormales = cuotaBase * (cuotasNormales - 1);
                montoAsignado = Math.round((saldoFinanciar - totalEspeciales - totalNormales) * 100.0) / 100.0;
            }

            // Calcular fecha garantizando el día preferido (ej: ajusta el 30 al 28 en febrero)
            YearMonth mesCuota = mesActual.plusMonths(i);
            int diaReal = Math.min(diaPreferido, mesCuota.lengthOfMonth());
            LocalDate fechaVencimiento = mesCuota.atDay(diaReal);

            cronograma.add(CuotaPreview.builder()
                    .numeroCuota(i + 1)
                    .monto(montoAsignado)
                    .fechaVencimiento(fechaVencimiento)
                    .build());
        }
        return cronograma;
    }

    // 2. GUARDAR CONTRATO REAL Y GENERAR CUOTAS
    @Transactional
    public Contrato generarContrato(ContratoRequest req) {
        Lote lote = loteRepository.findById(req.getLoteId()).orElseThrow();
        if (lote.getEstadoVenta() == EstadoLote.VENDIDO) {
            throw new RuntimeException("Este lote ya se encuentra vendido.");
        }

        Double saldoFinanciar = req.getPrecioTotal() - req.getMontoInicialAcordado();

        // 1. AUTOGENERAR LA DESCRIPCIÓN DEL CRONOGRAMA
        StringBuilder descBuilder = new StringBuilder();
        descBuilder.append("Cuota Inicial de S/ ").append(req.getMontoInicialAcordado()).append(". ");

        if (req.getCuotasEspeciales() != null && req.getCuotasEspeciales() > 0) {
            int cuotasRestantes = req.getCantidadCuotas() - req.getCuotasEspeciales();
            descBuilder.append("Fraccionado en ")
                    .append(req.getCuotasEspeciales()).append(" cuotas de S/ ").append(req.getMontoCuotaEspecial())
                    .append(" y ").append(cuotasRestantes).append(" cuotas con el saldo restante.");
        } else {
            descBuilder.append("Fraccionado en ").append(req.getCantidadCuotas()).append(" cuotas regulares.");
        }

        // 2. CREAR EL CONTRATO PADRE
        Contrato contrato = Contrato.builder()
                .lote(lote)
                .cliente(clienteRepository.findById(req.getClienteId()).orElseThrow())
                .vendedor(vendedorRepository.findById(req.getVendedorId()).orElseThrow())
                .precioTotal(req.getPrecioTotal())
                .montoInicial(req.getMontoInicialAcordado())
                .saldoFinanciar(saldoFinanciar)
                .cantidadCuotas(req.getCantidadCuotas())
                .descripcion(descBuilder.toString()) // Guardamos el texto generado
                .observacion(req.getObservacion())   // Guardamos la nota del vendedor

                // NUEVO: Guardamos la fecha legal del documento (o la de hoy por defecto)
                .fechaContrato(req.getFechaContrato() != null ? req.getFechaContrato().atStartOfDay() : LocalDateTime.now())

                .build();
        Contrato contratoGuardado = contratoRepository.save(contrato);

        // 3. EVALUAR LA INTENCIÓN DE PAGO (PARA EL ESTADO DEL LOTE)
        Double abonoInicialPrometido = (req.getAbonoInicialReal() != null) ? req.getAbonoInicialReal() : 0.0;

        // Si el cliente promete dar la inicial completa, el lote se asegura como VENDIDO.
        // Si promete dar menos (o nada por ahora), se marca como SEPARADO.
        if (abonoInicialPrometido >= req.getMontoInicialAcordado()) {
            lote.setEstadoVenta(EstadoLote.VENDIDO);
        } else {
            lote.setEstadoVenta(EstadoLote.SEPARADO);
        }
        loteRepository.save(lote);

        // 4. CREAR LA CUOTA 0 (DEUDA GENERADA, ESPERANDO A CAJA)
        Cuota cuota0 = Cuota.builder()
                .contrato(contratoGuardado)
                .numeroCuota(0)
                .montoTotal(req.getMontoInicialAcordado())
                .montoPagado(0.0) // Nace en 0 porque Tesorería debe registrar el ingreso del dinero
                .fechaVencimiento((req.getFechaLimiteInicial() != null) ? req.getFechaLimiteInicial() : LocalDate.now())
                .estado(EstadoCuota.PENDIENTE) // Nace pendiente de pago
                .build();
        cuotaRepository.save(cuota0);

        // 5. GENERAR EL RESTO DEL CRONOGRAMA (Cuotas 1 a N)
        if (saldoFinanciar > 0 && req.getCantidadCuotas() > 0) {
            SimulacionRequest sim = new SimulacionRequest();
            sim.setPrecioTotal(req.getPrecioTotal());
            sim.setMontoInicial(req.getMontoInicialAcordado());
            sim.setCantidadCuotas(req.getCantidadCuotas());
            sim.setFechaInicioPago(req.getFechaInicioPago());
            sim.setCuotasEspeciales(req.getCuotasEspeciales());
            sim.setMontoCuotaEspecial(req.getMontoCuotaEspecial());

            List<CuotaPreview> proyeccion = simularCronograma(sim);
            List<Cuota> cuotasAGuardar = new ArrayList<>();

            for (CuotaPreview cp : proyeccion) {
                cuotasAGuardar.add(Cuota.builder()
                        .contrato(contratoGuardado)
                        .numeroCuota(cp.getNumeroCuota())
                        .montoTotal(cp.getMonto())
                        .montoPagado(0.0)
                        .fechaVencimiento(cp.getFechaVencimiento())
                        .estado(EstadoCuota.PENDIENTE)
                        .build());
            }
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
