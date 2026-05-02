package com.sistema.base.api.core.Financiamiento.Contrato;

import com.sistema.base.api.core.Empresa.Empresa;
import com.sistema.base.api.core.Empresa.EmpresaRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorial;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorialRepository;
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
    private final ContratoHistorialRepository contratoHistorialRepository;
    private final TemplateEngine templateEngine;

    public List<CuotaPreview> simularCronograma(SimulacionRequest request) {
        List<CuotaPreview> cronograma = new ArrayList<>();
        Double saldoFinanciar = request.getPrecioTotal() - request.getMontoInicial();
        if (saldoFinanciar <= 0 || request.getCantidadCuotas() <= 0) return cronograma;
        int cuotasTotales = request.getCantidadCuotas();
        int cuotasEspeciales = (request.getCuotasEspeciales() != null) ? request.getCuotasEspeciales() : 0;
        Double montoEspecial = (request.getMontoCuotaEspecial() != null) ? request.getMontoCuotaEspecial() : 0.0;
        Double saldoRestante = saldoFinanciar;
        if (cuotasEspeciales > 0 && montoEspecial > 0) { saldoRestante -= (cuotasEspeciales * montoEspecial); }
        int cuotasNormales = cuotasTotales - cuotasEspeciales;
        Double cuotaBase = (cuotasNormales > 0) ? Math.round((saldoRestante / cuotasNormales) * 100.0) / 100.0 : 0.0;
        LocalDate fechaInicial = request.getFechaInicioPago();
        int diaPreferido = fechaInicial.getDayOfMonth();
        YearMonth mesActual = YearMonth.from(fechaInicial);
        for (int i = 0; i < cuotasTotales; i++) {
            Double montoAsignado = (i < cuotasEspeciales) ? montoEspecial : cuotaBase;
            if (i == cuotasTotales - 1 && cuotasNormales > 0) {
                Double totalEspeciales = cuotasEspeciales * montoEspecial;
                Double totalNormales = cuotaBase * (cuotasNormales - 1);
                montoAsignado = Math.round((saldoFinanciar - totalEspeciales - totalNormales) * 100.0) / 100.0;
            }
            YearMonth mesCuota = mesActual.plusMonths(i);
            int diaReal = Math.min(diaPreferido, mesCuota.lengthOfMonth());
            LocalDate fechaVencimiento = mesCuota.atDay(diaReal);
            cronograma.add(CuotaPreview.builder().numeroCuota(i + 1).monto(montoAsignado).fechaVencimiento(fechaVencimiento).build());
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
                .fechaContrato(null) // Nace en null, se actualiza al generar el documento
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

        return contratoGuardado; // Retorna sin generar PDF ni historial.
    }

    // =========================================================================
    // NUEVO ENDPOINT MAESTRO: VALIDA, GENERA PDF Y GUARDA EL HISTORIAL
    // =========================================================================
    @Transactional
    public Contrato generarNuevoDocumentoContrato(Long contratoId, String observacion) {

        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        // EXTRAEMOS EL ESTADO DIRECTAMENTE DE LA BASE DE DATOS
        EstadoContrato estadoActual = contrato.getEstadoContrato();

        // 1. REGLA: Bloquear duplicados
        if (contratoHistorialRepository.existsByContratoIdAndEstado(contratoId, estadoActual.name())) {
            throw new RuntimeException("ERROR: El contrato ya tiene un documento generado para el estado " + estadoActual.name() + ". No se puede volver a generar.");
        }

        // 2. REGLA: Actualizar Fecha Hito (Ya no seteamos el estado porque ya es el correcto)
        contrato.setFechaContrato(LocalDate.now());
        Contrato contratoGuardado = contratoRepository.save(contrato);

        try {
            // 3. Preparar nombres para el archivo (Formato: ID_DNI_NOMBRESYAPELLIDOS_ESTADO)
            String dni = contratoGuardado.getCliente().getNumeroDocumento();
            String nombresApellidos = (contratoGuardado.getCliente().getNombres() + "_" + contratoGuardado.getCliente().getApellidos())
                    .replaceAll("\\s+", "_").toUpperCase(); // Reemplaza espacios por subguiones

            // 4. Generar PDF Físico y Guardar
            byte[] pdfBytes = generarDocumentoPdfBytes(contratoGuardado);
            String rutaFisica = guardarPdfEnDisco(pdfBytes, contratoGuardado.getId(), dni, nombresApellidos, estadoActual.name());

            // 5. Crear Historial (la fechaRegistro se pone sola gracias a @PrePersist)
            ContratoHistorial historial = ContratoHistorial.builder()
                    .contrato(contratoGuardado)
                    .estado(estadoActual.name())
                    .rutaDocumentoPdf(rutaFisica)
                    .observacion(observacion)
                    .build();

            contratoHistorialRepository.save(historial);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar o guardar el documento PDF: " + e.getMessage());
        }

        return contratoGuardado;
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

    private String guardarPdfEnDisco(byte[] pdfBytes, Long contratoId, String dni, String nombresApellidos, String estado) throws Exception {
        String carpetaDestino = "uploads/contratos_pdf/";
        Path rutaDirectorio = Paths.get(carpetaDestino);

        if (!Files.exists(rutaDirectorio)) {
            Files.createDirectories(rutaDirectorio);
        }

        // FORMATO REQUERIDO: ID_DNI_NOMBRES_ESTADO.pdf
        // Ejemplo: 15_72384732_CARLOS_RUIZ_MENDOZA_SEPARADO.pdf
        String nombreArchivo = contratoId + "_" + dni + "_" + nombresApellidos + "_" + estado + ".pdf";

        Path rutaArchivo = rutaDirectorio.resolve(nombreArchivo);
        Files.write(rutaArchivo, pdfBytes);

        return rutaArchivo.toString();
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

        // Reutiliza el método privado que ya tienes armado con Thymeleaf
        return generarDocumentoPdfBytes(contrato);
    }

}