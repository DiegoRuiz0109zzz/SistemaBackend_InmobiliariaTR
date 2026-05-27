package com.sistema.base.api.core.Financiamiento.Pago;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sistema.base.api.core.Empresa.Empresa;
import com.sistema.base.api.core.Empresa.EmpresaRepository;
import com.sistema.base.api.core.Financiamiento.Comision.ComisionService;
import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.EstadoContrato;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorialService;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;
import com.sistema.base.api.core.Financiamiento.Pago.Serie.Serie;
import com.sistema.base.api.core.Financiamiento.Pago.Serie.SerieRepository;
import com.sistema.base.api.core.Lotizacion.Lote.EstadoLote;
import com.sistema.base.api.core.Lotizacion.Lote.Lote;
import com.sistema.base.api.core.Lotizacion.Lote.LoteRepository;
import com.sistema.base.api.core.Usuario.Clientes.Cliente;
import com.sistema.base.api.service.FileStorageService;
import com.sistema.base.api.utils.NumeroALetrasConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;
    private final FileStorageService fileStorageService;
    private final ContratoRepository contratoRepository;
    private final LoteRepository loteRepository;
    private final ContratoHistorialService contratoHistorialService;
    private final ComisionService comisionService;
    private final EmpresaRepository empresaRepository;

    private final SerieRepository serieRepository; // ✅ INYECTADO PARA CONTROL DE SERIES

    private final TemplateEngine templateEngine;

    @Transactional(readOnly = true)
    public List<Pago> listarPorCuota(Long cuotaId) {
        return pagoRepository.findByCuotaIdAndEnabledTrue(cuotaId);
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPagos() {
        return pagoRepository.findAll();
    }

    private String generarNombreVoucher(Cliente cliente, MultipartFile file) {
        String dni = cliente.getNumeroDocumento();
        String primerNombre = cliente.getNombres().trim().split("\\s+")[0].toUpperCase();
        String primerApellido = cliente.getApellidos().trim().split("\\s+")[0].toUpperCase();

        String originalFilename = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
        originalFilename = originalFilename.replaceAll("[\\s+]", "_");

        return dni + "_" + primerNombre + "_" + primerApellido + "_" + originalFilename;
    }

    // ✅ MÉTODO ANTI-CONCURRENCIA: Bloquea la serie temporalmente y asigna el correlativo
    private void asignarNotaVenta(Pago p) {
        Serie serieActiva = serieRepository.findActiveSerieForUpdate(TipoComprobante.NOTA_VENTA)
                .orElseThrow(() -> new RuntimeException("No se encontró una serie activa para Notas de Venta."));

        int nuevoCorrelativo = serieActiva.getUltimoCorrelativo() + 1;
        serieActiva.setUltimoCorrelativo(nuevoCorrelativo);
        serieRepository.save(serieActiva);

        String numeroFormateado = String.format("%s-%06d", serieActiva.getSerie(), nuevoCorrelativo);

        p.setTipoComprobante(TipoComprobante.NOTA_VENTA);
        p.setSerieComprobante(serieActiva.getSerie());
        p.setNumeroComprobante(numeroFormateado);
    }

    // =========================================================================================
    // ✅ REGISTRAR PAGO DIRECTO (CON ABONO EN CASCADA)
    // =========================================================================================
    @Transactional
    public List<Pago> registrarPago(Long cuotaId, Double montoAbonado, String metodoPago, String numeroOperacion, String descripcion, MultipartFile voucherFile) {
        Cuota cuotaInicialRequest = cuotaRepository.findById(cuotaId)
                .orElseThrow(() -> new RuntimeException("La cuota no existe."));
        Contrato contrato = cuotaInicialRequest.getContrato();

        // 1. Obtener todas las cuotas pendientes del contrato (ordenadas de la más antigua a la más nueva)
        List<Cuota> cuotasPendientes = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream()
                .filter(c -> c.getMontoPagado() < c.getMontoTotal())
                .collect(Collectors.toList());

        // 2. Validar que el monto no exceda la deuda total
        double deudaTotal = cuotasPendientes.stream().mapToDouble(c -> c.getMontoTotal() - c.getMontoPagado()).sum();
        if (montoAbonado > deudaTotal) {
            throw new RuntimeException("El monto a abonar (S/ " + montoAbonado + ") supera la deuda total pendiente del contrato (S/ " + deudaTotal + ").");
        }

        // 3. Subir el voucher físico (se sube una sola vez y se comparte la URL en los fragmentos de pago)
        String fotoVoucherUrl = null;
        if (voucherFile != null && !voucherFile.isEmpty()) {
            Cliente cliente = contrato.getCliente();
            String customFileName = generarNombreVoucher(cliente, voucherFile);
            String savedPath = fileStorageService.storeFileWithCustomName(voucherFile, "vouchers", customFileName);
            fotoVoucherUrl = "uploads/" + savedPath;
        }

        Double montoRestante = montoAbonado;
        List<Pago> pagosGenerados = new ArrayList<>();
        LocalDate fechaPagoActual = LocalDate.now();

        // 4. Repartir el dinero en cascada
        for (Cuota c : cuotasPendientes) {
            if (montoRestante <= 0) break;

            Double saldoCuota = c.getMontoTotal() - c.getMontoPagado();
            Double montoAAplicar = Math.min(montoRestante, saldoCuota);

            // Calcular días de retraso
            int diasRetraso = 0;
            boolean pagoADestiempo = false;
            if (fechaPagoActual.isAfter(c.getFechaVencimiento())) {
                pagoADestiempo = true;
                diasRetraso = (int) ChronoUnit.DAYS.between(c.getFechaVencimiento(), fechaPagoActual);
            }

            // Crear el fragmento de pago
            Pago pago = Pago.builder()
                    .cuota(c)
                    .montoAbonado(montoAAplicar)
                    .metodoPago(metodoPago)
                    .numeroOperacion(numeroOperacion)
                    .descripcion(descripcion != null ? descripcion : "Abono a cuota " + c.getNumeroCuota())
                    .fotoVoucherUrl(fotoVoucherUrl)
                    .estado(EstadoPago.PROCESADO)
                    .diasRetraso(diasRetraso)
                    .pagoADestiempo(pagoADestiempo)
                    .build();

            // Asignar número correlativo
            asignarNotaVenta(pago);

            // Actualizar el estado de la cuota
            c.setMontoPagado(c.getMontoPagado() + montoAAplicar);
            if (c.getMontoPagado() >= c.getMontoTotal()) {
                c.setEstado(pagoADestiempo ? EstadoCuota.PAGADO_DESTIEMPO : EstadoCuota.PAGADO_TOTAL);

                // Si justo pagó el 100% de la cuota 0 (Separación)
                if (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) {
                    contratoHistorialService.registrarHito(contrato, "INICIAL_COMPLETADA",
                            "El monto de separación/inicial ha sido cancelado al 100%. Pendiente de estructuración de cronograma y activación manual.", "Pago validado");
                }
            } else {
                c.setEstado(EstadoCuota.PAGADO_PARCIAL);
            }

            cuotaRepository.save(c);
            pagosGenerados.add(pagoRepository.save(pago));

            // Restar de la "billetera" temporal
            montoRestante -= montoAAplicar;
        }

        // 5. Evaluar comisiones
        double totalPagadoHastaHoy = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream().mapToDouble(Cuota::getMontoPagado).sum();
        if (totalPagadoHastaHoy >= 2500.0) {
            comisionService.evaluarYGenerarComisiones(contrato);
        }

        return pagosGenerados;
    }

    // =========================================================================================
    // ✅ PROCESAR PAGO PENDIENTE (REPARTICIÓN EN CASCADA)
    // =========================================================================================
    @Transactional
    public List<Pago> procesarPagoPendiente(Long pagoId, String metodoPago, String numeroOperacion, String descripcion, MultipartFile voucherFile) {
        Pago pagoOriginal = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado."));

        if (pagoOriginal.getEstado() == EstadoPago.PROCESADO) {
            throw new RuntimeException("Este pago ya fue procesado.");
        }

        Contrato contrato = pagoOriginal.getCuota().getContrato();
        Double montoTotalAbonado = pagoOriginal.getMontoAbonado();

        List<Cuota> cuotasPendientes = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream()
                .filter(c -> c.getMontoPagado() < c.getMontoTotal())
                .collect(Collectors.toList());

        double deudaTotal = cuotasPendientes.stream().mapToDouble(c -> c.getMontoTotal() - c.getMontoPagado()).sum();
        if (montoTotalAbonado > deudaTotal) {
            throw new RuntimeException("El abono pendiente supera la deuda total actual del contrato.");
        }

        String fotoVoucherUrl = pagoOriginal.getFotoVoucherUrl();
        if (voucherFile != null && !voucherFile.isEmpty()) {
            Cliente cliente = contrato.getCliente();
            String customFileName = generarNombreVoucher(cliente, voucherFile);
            String savedPath = fileStorageService.storeFileWithCustomName(voucherFile, "vouchers", customFileName);
            fotoVoucherUrl = "uploads/" + savedPath;
        }

        Double montoRestante = montoTotalAbonado;
        List<Pago> pagosProcesados = new ArrayList<>();
        LocalDate fechaDelPago = pagoOriginal.getFechaPago() != null ? pagoOriginal.getFechaPago() : LocalDate.now();

        boolean esPrimerPago = true;

        for (Cuota c : cuotasPendientes) {
            if (montoRestante <= 0) break;

            Double saldoCuota = c.getMontoTotal() - c.getMontoPagado();
            Double montoAAplicar = Math.min(montoRestante, saldoCuota);

            int diasRetraso = 0;
            boolean pagoADestiempo = false;
            if (fechaDelPago.isAfter(c.getFechaVencimiento())) {
                pagoADestiempo = true;
                diasRetraso = (int) ChronoUnit.DAYS.between(c.getFechaVencimiento(), fechaDelPago);
            }

            Pago pagoActual;
            if (esPrimerPago) {
                // Modificamos la entidad del pago pendiente original para reutilizarla
                pagoActual = pagoOriginal;
                pagoActual.setCuota(c);
                pagoActual.setMontoAbonado(montoAAplicar);
                esPrimerPago = false;
            } else {
                // Creamos nuevas entidades en la BD para el excedente
                pagoActual = Pago.builder()
                        .cuota(c)
                        .montoAbonado(montoAAplicar)
                        .fechaPago(fechaDelPago)
                        .fechaRegistro(pagoOriginal.getFechaRegistro())
                        .build();
            }

            pagoActual.setMetodoPago(metodoPago);
            pagoActual.setNumeroOperacion(numeroOperacion);
            pagoActual.setDescripcion(descripcion != null ? descripcion : "Abono a cuota " + c.getNumeroCuota());
            pagoActual.setFotoVoucherUrl(fotoVoucherUrl);
            pagoActual.setEstado(EstadoPago.PROCESADO);
            pagoActual.setDiasRetraso(diasRetraso);
            pagoActual.setPagoADestiempo(pagoADestiempo);

            asignarNotaVenta(pagoActual);

            c.setMontoPagado(c.getMontoPagado() + montoAAplicar);
            if (c.getMontoPagado() >= c.getMontoTotal()) {
                c.setEstado(pagoADestiempo ? EstadoCuota.PAGADO_DESTIEMPO : EstadoCuota.PAGADO_TOTAL);
                if (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) {
                    contratoHistorialService.registrarHito(contrato, "INICIAL_COMPLETADA",
                            "El monto de separación/inicial ha sido cancelado al 100%. Pendiente de estructuración de cronograma y activación manual.", "Pago procesado en caja");
                }
            } else {
                c.setEstado(EstadoCuota.PAGADO_PARCIAL);
            }

            cuotaRepository.save(c);
            pagosProcesados.add(pagoRepository.save(pagoActual));

            montoRestante -= montoAAplicar;
        }

        double totalPagadoHastaHoy = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream().mapToDouble(Cuota::getMontoPagado).sum();
        if (totalPagadoHastaHoy >= 2500.0) {
            comisionService.evaluarYGenerarComisiones(contrato);
        }

        return pagosProcesados;
    }

    @Transactional
    public void anularPago(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        Cuota cuota = pago.getCuota();
        cuota.setMontoPagado(cuota.getMontoPagado() - pago.getMontoAbonado());
        cuota.setEstado(cuota.getMontoPagado() <= 0 ? EstadoCuota.PENDIENTE : EstadoCuota.PAGADO_PARCIAL);

        cuotaRepository.save(cuota);
        pago.setEnabled(false);
        pagoRepository.save(pago);
    }

    @Transactional
    public void recalcularAtrasosPorContrato(Long contratoId) {
        List<Cuota> cuotas = cuotaRepository.findByContratoIdOrderByNumeroCuotaAsc(contratoId);

        for (Cuota cuota : cuotas) {
            List<Pago> pagos = pagoRepository.findByCuotaId(cuota.getId());

            for (Pago pago : pagos) {
                if (pago.getFechaPago() != null && cuota.getFechaVencimiento() != null) {
                    long diasDiferencia = ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), pago.getFechaPago());

                    if (diasDiferencia > 0) {
                        pago.setDiasRetraso((int) diasDiferencia);
                        pago.setPagoADestiempo(true);
                    } else {
                        pago.setDiasRetraso(0);
                        pago.setPagoADestiempo(false);
                    }
                    pagoRepository.save(pago);
                }
            }
        }
    }

    private String getImagenBase64(String nombreArchivo) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("uploads/imagens/" + nombreArchivo);
            if (java.nio.file.Files.exists(path)) {
                return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(path));
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Transactional(readOnly = true)
    public byte[] generarNotaVentaPdf(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        if (pago.getNumeroComprobante() == null) {
            throw new RuntimeException("Este pago aún no tiene una Nota de Venta asociada.");
        }

        Cliente cliente =
                pago.getCuota().getContrato().getCliente();

        Empresa empresa = empresaRepository.findById(1L).orElse(null);

        String montoEnLetras = com.sistema.base.api.utils.NumeroALetrasConverter.convertir(pago.getMontoAbonado(), "SOLES");

        // ✅ CARGAMOS EL CONTEXTO CON LA IMAGEN BASE64
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariable("pago", pago);
        context.setVariable("cliente", cliente);
        context.setVariable("montoEnLetras", montoEnLetras);
        context.setVariable("empresa", empresa);

        // Asumo que tu logo es PNG. Si es JPG, cambia el nombre a "logo_terranort.jpg"
        context.setVariable("imgLogo", getImagenBase64("logo_terranort.png"));

        String html = templateEngine.process("nota-venta", context);

        try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error crítico al compilar el PDF: " + e.getMessage());
        }
    }

}