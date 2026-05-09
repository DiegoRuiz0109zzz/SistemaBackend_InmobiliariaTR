package com.sistema.base.api.core.Financiamiento.Contrato;

import com.sistema.base.api.core.Empresa.Empresa;
import com.sistema.base.api.core.Empresa.EmpresaRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorialService;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.ContratoRequest;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.CuotaPreview;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.SimulacionRequest;
import com.sistema.base.api.core.Financiamiento.Cotizacion.Cotizacion;
import com.sistema.base.api.core.Financiamiento.Cotizacion.CotizacionRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;
import com.sistema.base.api.core.Financiamiento.Cuota.TipoCuota;
import com.sistema.base.api.core.Financiamiento.Pago.EstadoPago;
import com.sistema.base.api.core.Financiamiento.Pago.Pago;
import com.sistema.base.api.core.Financiamiento.Pago.PagoRepository;
import com.sistema.base.api.core.Lotizacion.Lote.EstadoLote;
import com.sistema.base.api.core.Lotizacion.Lote.Lote;
import com.sistema.base.api.core.Lotizacion.Lote.LoteRepository;
import com.sistema.base.api.core.Usuario.Clientes.Cliente;
import com.sistema.base.api.core.Usuario.Clientes.ClienteRepository;
import com.sistema.base.api.core.Vendedores.Vendedor;
import com.sistema.base.api.core.Vendedores.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final CuotaRepository cuotaRepository;
    private final LoteRepository loteRepository;
    private final ClienteRepository clienteRepository;
    private final VendedorRepository vendedorRepository;
    private final PagoRepository pagoRepository;
    private final EmpresaRepository empresaRepository;
    private final CotizacionRepository cotizacionRepository;

    private final ContratoHistorialService contratoHistorialService;
    private final TemplateEngine templateEngine;

    // ✅ MÉTODO AUXILIAR PARA CALCULAR LA FECHA INTELIGENTE
    private LocalDate calcularSiguienteFechaVencimiento(LocalDate fechaInicial, int mesesASumar) {
        boolean esFinDeMes = fechaInicial.getDayOfMonth() == fechaInicial.lengthOfMonth();
        LocalDate fechaBase = fechaInicial.plusMonths(mesesASumar);

        if (esFinDeMes) {
            return fechaBase.withDayOfMonth(fechaBase.lengthOfMonth());
        } else {
            int diaOriginal = fechaInicial.getDayOfMonth();
            int maxDiaMesDestino = fechaBase.lengthOfMonth();
            return fechaBase.withDayOfMonth(Math.min(diaOriginal, maxDiaMesDestino));
        }
    }

    public List<CuotaPreview> simularCronograma(SimulacionRequest request) {
        List<CuotaPreview> cronograma = new ArrayList<>();
        Double saldoFinanciar = request.getPrecioTotal() - request.getMontoInicial();
        if (saldoFinanciar <= 0 || request.getCantidadCuotas() <= 0) return cronograma;

        int cuotasTotales = request.getCantidadCuotas();
        int cuotasEspeciales = (request.getCuotasEspeciales() != null) ? request.getCuotasEspeciales() : 0;
        Double montoEspecial = (request.getMontoCuotaEspecial() != null) ? request.getMontoCuotaEspecial() : 0.0;
        Double saldoRestante = saldoFinanciar;

        if (cuotasEspeciales > 0 && montoEspecial > 0) {
            saldoRestante -= (cuotasEspeciales * montoEspecial);
        }

        int cuotasNormales = cuotasTotales - cuotasEspeciales;
        Double cuotaBase = (cuotasNormales > 0) ? Math.round((saldoRestante / cuotasNormales) * 100.0) / 100.0 : 0.0;

        LocalDate fechaInicial = request.getFechaInicioPago();

        for (int i = 0; i < cuotasTotales; i++) {
            Double montoAsignado = (i < cuotasEspeciales) ? montoEspecial : cuotaBase;

            if (i == cuotasTotales - 1 && cuotasNormales > 0) {
                Double totalEspeciales = cuotasEspeciales * montoEspecial;
                Double totalNormales = cuotaBase * (cuotasNormales - 1);
                montoAsignado = Math.round((saldoFinanciar - totalEspeciales - totalNormales) * 100.0) / 100.0;
            }

            // ✅ USAMOS EL MÉTODO INTELIGENTE PARA CALCULAR LA FECHA
            LocalDate fechaVencimiento = calcularSiguienteFechaVencimiento(fechaInicial, i);

            cronograma.add(CuotaPreview.builder()
                    .numeroCuota(i + 1)
                    .monto(montoAsignado)
                    .fechaVencimiento(fechaVencimiento)
                    .build());
        }
        return cronograma;
    }

    @Transactional
    public Contrato generarContrato(ContratoRequest req) {

        Lote lote = loteRepository.findById(req.getLoteId()).orElseThrow(() -> new RuntimeException("El lote no existe."));
        if (lote.getEstadoVenta() == EstadoLote.VENDIDO) throw new RuntimeException("Lote vendido.");
        Cliente cliente = clienteRepository.findById(req.getClienteId()).orElseThrow(() -> new RuntimeException("Cliente no existe."));
        Cliente coComprador = null;
        if (req.getCoCompradorId() != null) {
            coComprador = clienteRepository.findById(req.getCoCompradorId()).orElseThrow(() -> new RuntimeException("Co-comprador no existe."));
        }
        Vendedor vendedor = vendedorRepository.findById(req.getVendedorId()).orElseThrow(() -> new RuntimeException("Vendedor no existe."));
        Cotizacion cotizacion = null;
        if (req.getCotizacionId() != null) cotizacion = cotizacionRepository.findById(req.getCotizacionId()).orElse(null);

        Double saldoFinanciar = req.getPrecioTotal() - req.getMontoInicialAcordado();
        StringBuilder descBuilder = new StringBuilder();
        descBuilder.append("Cuota Inicial de S/ ").append(req.getMontoInicialAcordado()).append(". ");
        if (req.getCuotasEspeciales() != null && req.getCuotasEspeciales() > 0) {
            descBuilder.append("Fraccionado en ").append(req.getCuotasEspeciales()).append(" cuotas de S/ ").append(req.getMontoCuotaEspecial())
                    .append(" y ").append(req.getCantidadCuotas() - req.getCuotasEspeciales()).append(" cuotas con el saldo restante.");
        } else {
            descBuilder.append("Fraccionado en ").append(req.getCantidadCuotas()).append(" cuotas regulares.");
        }

        Double abonoInicialPrometido = (req.getAbonoInicialReal() != null) ? req.getAbonoInicialReal() : 0.0;
        if (abonoInicialPrometido >= req.getMontoInicialAcordado()) lote.setEstadoVenta(EstadoLote.VENDIDO);
        else lote.setEstadoVenta(EstadoLote.RESERVADO);
        loteRepository.save(lote);

        EstadoContrato estadoContratoReal = (abonoInicialPrometido >= req.getMontoInicialAcordado()) ? EstadoContrato.ACTIVO : EstadoContrato.SEPARADO;

        Contrato contrato = Contrato.builder()
                .lote(lote).cliente(cliente).coComprador(coComprador).vendedor(vendedor).cotizacionOrigen(cotizacion)
                .precioTotal(req.getPrecioTotal()).montoInicial(req.getMontoInicialAcordado())
                .montoAbonadoIncial(abonoInicialPrometido).saldoFinanciar(saldoFinanciar).cantidadCuotas(req.getCantidadCuotas())
                .descripcion(descBuilder.toString()).observacion(req.getObservacion()).tipoInicial(req.getTipoInicial())
                .cuotasFlexibles(req.getCuotasFlexibles() != null ? req.getCuotasFlexibles() : false)
                .fechaInicioCronograma(req.getFechaInicioPago())
                .estadoContrato(estadoContratoReal)
                .fechaContrato(LocalDate.now())
                .build();

        Contrato contratoGuardado = contratoRepository.save(contrato);

        Cuota cuota0 = Cuota.builder().contrato(contratoGuardado).numeroCuota(0).tipoCuota(TipoCuota.INICIAL)
                .montoTotal(req.getMontoInicialAcordado()).montoPagado(0.0)
                .fechaVencimiento((req.getFechaLimiteInicial() != null) ? req.getFechaLimiteInicial() : LocalDate.now())
                .estado(EstadoCuota.PENDIENTE).build();
        cuota0 = cuotaRepository.save(cuota0);

        if (abonoInicialPrometido > 0) {
            Pago pagoPendiente = Pago.builder().cuota(cuota0).montoAbonado(abonoInicialPrometido)
                    .estado(EstadoPago.POR_VALIDAR).metodoPago("POR CONFIRMAR EN CAJA").build();
            pagoRepository.save(pagoPendiente);
        }

        if (saldoFinanciar > 0 && req.getCantidadCuotas() > 0) {
            SimulacionRequest sim = new SimulacionRequest();
            sim.setPrecioTotal(req.getPrecioTotal()); sim.setMontoInicial(req.getMontoInicialAcordado());
            sim.setCantidadCuotas(req.getCantidadCuotas()); sim.setFechaInicioPago(req.getFechaInicioPago());
            sim.setCuotasEspeciales(req.getCuotasEspeciales()); sim.setMontoCuotaEspecial(req.getMontoCuotaEspecial());

            // La llamada a simularCronograma ya incluye la lógica de fin de mes
            List<CuotaPreview> proyeccion = simularCronograma(sim);
            List<Cuota> cuotasAGuardar = new ArrayList<>();
            int cantidadEspeciales = (req.getCuotasEspeciales() != null) ? req.getCuotasEspeciales() : 0;

            for (CuotaPreview cp : proyeccion) {
                TipoCuota tipoDeCuota = (cp.getNumeroCuota() <= cantidadEspeciales) ? TipoCuota.ESPECIAL : TipoCuota.MENSUAL;
                cuotasAGuardar.add(Cuota.builder().contrato(contratoGuardado).numeroCuota(cp.getNumeroCuota())
                        .tipoCuota(tipoDeCuota).montoTotal(cp.getMonto()).montoPagado(0.0)
                        .fechaVencimiento(cp.getFechaVencimiento()).estado(EstadoCuota.PENDIENTE).build());
            }
            cuotaRepository.saveAll(cuotasAGuardar);
        }

        if (cotizacion != null) {
            cotizacion.setEstado(com.sistema.base.api.core.Financiamiento.Cotizacion.EstadoCotizacion.CONVERTIDA_A_CONTRATO);
            cotizacionRepository.save(cotizacion);
        }

        String tipoHito = (contratoGuardado.getEstadoContrato() == EstadoContrato.SEPARADO) ? "CONTRATO_SEPARADO" : "CONTRATO_ACTIVO";
        String descripcionHito = (contratoGuardado.getEstadoContrato() == EstadoContrato.SEPARADO) ? "Contrato de Separación registrado en el sistema." : "Contrato Activo registrado en el sistema.";
        contratoHistorialService.registrarHito(contratoGuardado, tipoHito, descripcionHito, req.getObservacion());

        return contratoGuardado;
    }

    @Transactional
    public Contrato generarNuevoDocumentoContrato(Long contratoId, String observacion) {

        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        EstadoContrato estadoActual = contrato.getEstadoContrato();

        contrato.setFechaContrato(LocalDate.now());
        Contrato contratoGuardado = contratoRepository.save(contrato);

        String tipoHito = (estadoActual == EstadoContrato.SEPARADO) ? "CONTRATO_SEPARADO" : "CONTRATO_ACTIVO";
        String descripcionHito = "Se registró un hito manual para la generación del documento.";
        contratoHistorialService.registrarHito(contratoGuardado, tipoHito, descripcionHito, observacion);

        return contratoGuardado;
    }

    @Transactional
    public Contrato actualizarContrato(Long id, ContratoRequest request) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        StringBuilder cambios = new StringBuilder();
        boolean huboCambios = false;

        if (request.getClienteId() == null) {
            throw new IllegalArgumentException("El contrato no puede quedarse vacío. Debe existir al menos un Titular.");
        }

        Cliente titularAntiguo = contrato.getCliente();
        Cliente coCompradorAntiguo = contrato.getCoComprador();
        Lote loteAntiguo = contrato.getLote();

        if (!Objects.equals(titularAntiguo.getId(), request.getClienteId())) {
            Cliente nuevoTitular = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new RuntimeException("El nuevo Titular indicado no existe."));
            contrato.setCliente(nuevoTitular);

            cambios.append(String.format("Cambio de Titular principal: de '%s %s' a '%s %s'. ",
                    titularAntiguo.getNombres(), titularAntiguo.getApellidos(),
                    nuevoTitular.getNombres(), nuevoTitular.getApellidos()));
            huboCambios = true;
        }

        Long idCoCompradorActual = (coCompradorAntiguo != null) ? coCompradorAntiguo.getId() : null;
        if (!Objects.equals(idCoCompradorActual, request.getCoCompradorId())) {
            if (request.getCoCompradorId() == null) {
                contrato.setCoComprador(null);
                cambios.append(String.format("Se eliminó a '%s %s' del contrato. ",
                        coCompradorAntiguo.getNombres(), coCompradorAntiguo.getApellidos()));
            } else {
                Cliente nuevoCoComprador = clienteRepository.findById(request.getCoCompradorId())
                        .orElseThrow(() -> new RuntimeException("El nuevo Co-Comprador indicado no existe."));
                contrato.setCoComprador(nuevoCoComprador);

                if (coCompradorAntiguo == null) {
                    cambios.append(String.format("Se agregó al nuevo Comprador '%s %s'. ",
                            nuevoCoComprador.getNombres(), nuevoCoComprador.getApellidos()));
                } else {
                    cambios.append(String.format("Cambio de Co-Comprador: de '%s %s' a '%s %s'. ",
                            coCompradorAntiguo.getNombres(), coCompradorAntiguo.getApellidos(),
                            nuevoCoComprador.getNombres(), nuevoCoComprador.getApellidos()));
                }
            }
            huboCambios = true;
        }

        if (request.getLoteId() != null && !Objects.equals(loteAntiguo.getId(), request.getLoteId())) {
            Lote nuevoLote = loteRepository.findById(request.getLoteId())
                    .orElseThrow(() -> new RuntimeException("El nuevo lote indicado no existe."));
            contrato.setLote(nuevoLote);

            cambios.append(String.format("Cambio de Lote: del Lote ID %d al Lote ID %d. ",
                    loteAntiguo.getId(), nuevoLote.getId()));
            huboCambios = true;
        }
        Contrato actualizado = contratoRepository.save(contrato);

        if (huboCambios) {
            contratoHistorialService.registrarHito(actualizado, "MODIFICACION", cambios.toString().trim(), request.getObservacion());
        }

        return actualizado;
    }

    @Transactional(readOnly = true)
    public List<Contrato> listarTodos() {
        return contratoRepository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public Contrato obtenerPorId(Long id) {
        return contratoRepository.findById(id).orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
    }

    private String getImagenBase64(String nombreArchivo) {
        try {
            String rutaLocal = "uploads/imagens/" + nombreArchivo;
            Path path = Paths.get(rutaLocal);
            if (Files.exists(path)) {
                byte[] imageBytes = Files.readAllBytes(path);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private byte[] generarDocumentoPdfBytes(Contrato contrato) {
        List<Cuota> cuotas = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId());
        LocalDate fechaCuotaCero = cuotas.stream()
                .filter(c -> (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) || c.getTipoCuota() == TipoCuota.INICIAL)
                .map(Cuota::getFechaVencimiento).findFirst().orElse(null);
        List<Cuota> cuotasMensuales = cuotas.stream()
                .filter(c -> c.getNumeroCuota() != null && c.getNumeroCuota() > 0).toList();
        Empresa empresa = empresaRepository.findById(1L).orElse(null);

        Context context = new Context();
        context.setVariable("contrato", contrato);
        context.setVariable("empresa", empresa);
        context.setVariable("fechaCuotaCero", fechaCuotaCero);
        context.setVariable("cuotasMensuales", cuotasMensuales);
        context.setVariable("imgCabecera", getImagenBase64("cabezera.png"));
        context.setVariable("imgPie", getImagenBase64("pie.png"));
        context.setVariable("imgFondo", getImagenBase64("fondo.png"));

        String template = (contrato.getEstadoContrato() == EstadoContrato.SEPARADO) ? "ficha-separacion" : "compromiso-venta";
        String htmlContenido = templateEngine.process(template, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContenido, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error fatal al fabricar el PDF: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarVistaPreviaPdf(Long contratoId) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        return generarDocumentoPdfBytes(contrato);
    }
}