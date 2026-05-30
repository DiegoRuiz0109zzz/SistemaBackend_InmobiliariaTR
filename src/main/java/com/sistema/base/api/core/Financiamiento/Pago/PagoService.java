package com.sistema.base.api.core.Financiamiento.Pago;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sistema.base.api.core.Empresa.Empresa;
import com.sistema.base.api.core.Empresa.EmpresaRepository;
import com.sistema.base.api.core.Financiamiento.Comision.ComisionService;
import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorialService;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;
import com.sistema.base.api.core.Financiamiento.Pago.DepositoBancario.DepositoBancario;
import com.sistema.base.api.core.Financiamiento.Pago.DepositoBancario.DepositoBancarioRepository;
import com.sistema.base.api.core.Financiamiento.Pago.Serie.Serie;
import com.sistema.base.api.core.Financiamiento.Pago.Serie.SerieRepository;
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
    private final ContratoHistorialService contratoHistorialService;
    private final ComisionService comisionService;
    private final EmpresaRepository empresaRepository;
    private final SerieRepository serieRepository;
    private final TemplateEngine templateEngine;
    private final DepositoBancarioRepository depositoBancarioRepository;

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
        return dni + "_" + primerNombre + "_" + primerApellido + "_" + originalFilename.replaceAll("[\\s+]", "_");
    }

    private String generarNuevoNumeroComprobante(TipoComprobante tipoComprobante) {
        Serie serieActiva = serieRepository.findActiveSerieForUpdate(tipoComprobante)
                .orElseThrow(() -> new RuntimeException("No se encontró una serie activa para " + tipoComprobante.name()));

        int nuevoCorrelativo = serieActiva.getUltimoCorrelativo() + 1;
        serieActiva.setUltimoCorrelativo(nuevoCorrelativo);
        serieRepository.save(serieActiva);

        return String.format("%s-%06d", serieActiva.getSerie(), nuevoCorrelativo);
    }

    // =========================================================================================
    // ✅ REGISTRAR PAGO DIRECTO
    // =========================================================================================
    @Transactional
    public List<Pago> registrarPago(Long cuotaId, Double montoAbonado, String metodoPago, String numeroOperacion, String descripcion, MultipartFile voucherFile) {
        Cuota cuotaInicialRequest = cuotaRepository.findById(cuotaId)
                .orElseThrow(() -> new RuntimeException("La cuota no existe."));
        Contrato contrato = cuotaInicialRequest.getContrato();

        List<Cuota> cuotasPendientes = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream().filter(c -> c.getMontoPagado() < c.getMontoTotal()).collect(Collectors.toList());

        double deudaTotal = cuotasPendientes.stream().mapToDouble(c -> c.getMontoTotal() - c.getMontoPagado()).sum();
        if (montoAbonado > deudaTotal) {
            throw new RuntimeException("El monto a abonar (S/ " + montoAbonado + ") supera la deuda total pendiente (S/ " + deudaTotal + ").");
        }

        String fotoVoucherUrl = null;
        if (voucherFile != null && !voucherFile.isEmpty()) {
            String customFileName = generarNombreVoucher(contrato.getCliente(), voucherFile);
            fotoVoucherUrl = "uploads/" + fileStorageService.storeFileWithCustomName(voucherFile, "vouchers", customFileName);
        }

        boolean esIngresoCaja = "EFECTIVO".equalsIgnoreCase(metodoPago) && (numeroOperacion == null || numeroOperacion.trim().isEmpty());

        TipoComprobante tipoDoc = esIngresoCaja ? TipoComprobante.RECIBO_INGRESO : TipoComprobante.NOTA_ABONO;
        EstadoPago estadoDelPago = esIngresoCaja ? EstadoPago.POR_VALIDAR : EstadoPago.PROCESADO;

        String numeroComprobanteAgrupado = generarNuevoNumeroComprobante(tipoDoc);

        Double montoRestante = montoAbonado;
        List<Pago> pagosGenerados = new ArrayList<>();
        LocalDate fechaPagoActual = LocalDate.now();

        for (Cuota c : cuotasPendientes) {
            if (montoRestante <= 0) break;

            Double saldoCuota = c.getMontoTotal() - c.getMontoPagado();
            Double montoAAplicar = Math.min(montoRestante, saldoCuota);

            boolean pagoADestiempo = fechaPagoActual.isAfter(c.getFechaVencimiento());
            int diasRetraso = pagoADestiempo ? (int) ChronoUnit.DAYS.between(c.getFechaVencimiento(), fechaPagoActual) : 0;

            // ✅ LÓGICA DE DESCRIPCIÓN DINÁMICA (Abono vs A Cuenta vs Saldo)
            String nombreCuota = (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) ? "INICIAL" : "CUOTA " + c.getNumeroCuota();
            String descDinamica;

            if (Double.compare(montoAAplicar, saldoCuota) == 0) { // Si paga todo lo que restaba de la cuota
                if (c.getMontoPagado() == 0.0) {
                    descDinamica = "ABONO DE " + nombreCuota;
                } else {
                    descDinamica = "SALDO DE " + nombreCuota;
                }
            } else { // Si el monto no alcanza para pagar toda la cuota
                descDinamica = "A CUENTA " + nombreCuota;
            }

            // Agregamos la descripción opcional enviada por el cajero (si existe)
            if (descripcion != null && !descripcion.trim().isEmpty()) {
                descDinamica += " - " + descripcion;
            }

            Pago pago = Pago.builder()
                    .cuota(c)
                    .montoAbonado(montoAAplicar)
                    .metodoPago(metodoPago)
                    .numeroOperacion(numeroOperacion)
                    .descripcion(descDinamica) // ✅ Asignamos la descripción inteligente
                    .fotoVoucherUrl(fotoVoucherUrl)
                    .estado(estadoDelPago)
                    .diasRetraso(diasRetraso)
                    .pagoADestiempo(pagoADestiempo)
                    .tipoComprobante(tipoDoc)
                    .numeroComprobante(numeroComprobanteAgrupado)
                    .build();

            c.setMontoPagado(c.getMontoPagado() + montoAAplicar);

            if (c.getMontoPagado() >= c.getMontoTotal()) {
                c.setEstado(pagoADestiempo ? EstadoCuota.PAGADO_DESTIEMPO : EstadoCuota.PAGADO_TOTAL);
                if (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) {
                    contratoHistorialService.registrarHito(contrato, "INICIAL_COMPLETADA",
                            "Inicial cancelada al 100%. Pendiente de estructuración y activación.", "Pago validado");
                }
            } else {
                c.setEstado(EstadoCuota.PAGADO_PARCIAL);
            }

            cuotaRepository.save(c);
            pagosGenerados.add(pagoRepository.save(pago));
            montoRestante -= montoAAplicar;
        }

        double totalPagadoHastaHoy = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream().mapToDouble(Cuota::getMontoPagado).sum();
        if (totalPagadoHastaHoy >= 2500.0) comisionService.evaluarYGenerarComisiones(contrato);

        if (esIngresoCaja) {
            String urlVistaPrevia = "/api/pagos/recibo/" + numeroComprobanteAgrupado + "/pdf";
            contratoHistorialService.registrarHito(contrato, "INGRESO_CAJA",
                    "Se recibió S/ " + montoAbonado + " en EFECTIVO físico. Pendiente de depósito bancario bajo el recibo " + numeroComprobanteAgrupado, "Caja", urlVistaPrevia);
        } else {
            String urlVistaPrevia = "/api/pagos/comprobante/" + numeroComprobanteAgrupado + "/pdf";
            contratoHistorialService.registrarHito(contrato, "NOTA_ABONO_CUOTAS",
                    "Se registró un abono de S/ " + montoAbonado + " bajo el comprobante " + numeroComprobanteAgrupado, "Banco", urlVistaPrevia);
        }

        return pagosGenerados;
    }

    // =========================================================================================
    // ✅ PROCESAR PAGO PENDIENTE
    // =========================================================================================
    @Transactional
    public List<Pago> procesarPagoPendiente(Long pagoId, String metodoPago, String numeroOperacion, String descripcion, MultipartFile voucherFile) {
        Pago pagoOriginal = pagoRepository.findById(pagoId).orElseThrow(() -> new RuntimeException("Pago no encontrado."));
        if (pagoOriginal.getEstado() == EstadoPago.PROCESADO) throw new RuntimeException("Este pago ya fue procesado.");

        Contrato contrato = pagoOriginal.getCuota().getContrato();
        Double montoTotalAbonado = pagoOriginal.getMontoAbonado();

        List<Cuota> cuotasPendientes = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream().filter(c -> c.getMontoPagado() < c.getMontoTotal()).collect(Collectors.toList());

        double deudaTotal = cuotasPendientes.stream().mapToDouble(c -> c.getMontoTotal() - c.getMontoPagado()).sum();
        if (montoTotalAbonado > deudaTotal) {
            throw new RuntimeException("El abono pendiente (S/ " + montoTotalAbonado + ") supera la deuda total actual del contrato.");
        }

        String fotoVoucherUrl = pagoOriginal.getFotoVoucherUrl();
        if (voucherFile != null && !voucherFile.isEmpty()) {
            fotoVoucherUrl = "uploads/" + fileStorageService.storeFileWithCustomName(voucherFile, "vouchers", generarNombreVoucher(contrato.getCliente(), voucherFile));
        }

        boolean esIngresoCaja = "EFECTIVO".equalsIgnoreCase(metodoPago) && (numeroOperacion == null || numeroOperacion.trim().isEmpty());

        TipoComprobante tipoDoc = esIngresoCaja ? TipoComprobante.RECIBO_INGRESO : TipoComprobante.NOTA_ABONO;
        EstadoPago estadoDelPago = esIngresoCaja ? EstadoPago.POR_VALIDAR : EstadoPago.PROCESADO;

        String numeroComprobanteAgrupado = generarNuevoNumeroComprobante(tipoDoc);
        List<Pago> pagosProcesados = new ArrayList<>();
        LocalDate fechaDelPago = pagoOriginal.getFechaPago() != null ? pagoOriginal.getFechaPago() : LocalDate.now();

        Double montoRestante = montoTotalAbonado;
        boolean esPrimerPago = true;

        for (Cuota c : cuotasPendientes) {
            if (montoRestante <= 0) break;

            Double saldoCuota = c.getMontoTotal() - c.getMontoPagado();
            Double montoAAplicar = Math.min(montoRestante, saldoCuota);

            boolean pagoADestiempo = fechaDelPago.isAfter(c.getFechaVencimiento());
            int diasRetraso = pagoADestiempo ? (int) ChronoUnit.DAYS.between(c.getFechaVencimiento(), fechaDelPago) : 0;

            Pago pagoActual;
            if (esPrimerPago) {
                pagoActual = pagoOriginal;
                pagoActual.setCuota(c);
                pagoActual.setMontoAbonado(montoAAplicar);
                esPrimerPago = false;
            } else {
                pagoActual = Pago.builder()
                        .cuota(c)
                        .montoAbonado(montoAAplicar)
                        .fechaPago(fechaDelPago)
                        .fechaRegistro(pagoOriginal.getFechaRegistro())
                        .build();
            }

            // ✅ LÓGICA DE DESCRIPCIÓN DINÁMICA
            String nombreCuota = (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) ? "INICIAL" : "CUOTA " + c.getNumeroCuota();
            String descDinamica;

            if (Double.compare(montoAAplicar, saldoCuota) == 0) {
                if (c.getMontoPagado() == 0.0) {
                    descDinamica = "ABONO DE " + nombreCuota;
                } else {
                    descDinamica = "SALDO DE " + nombreCuota;
                }
            } else {
                descDinamica = "A CUENTA " + nombreCuota;
            }

            if (descripcion != null && !descripcion.trim().isEmpty()) {
                descDinamica += " - " + descripcion;
            }

            pagoActual.setMetodoPago(metodoPago);
            pagoActual.setNumeroOperacion(numeroOperacion);
            pagoActual.setDescripcion(descDinamica); // ✅ Asignamos la descripción inteligente
            pagoActual.setFotoVoucherUrl(fotoVoucherUrl);
            pagoActual.setEstado(estadoDelPago);
            pagoActual.setDiasRetraso(diasRetraso);
            pagoActual.setPagoADestiempo(pagoADestiempo);
            pagoActual.setTipoComprobante(tipoDoc);
            pagoActual.setNumeroComprobante(numeroComprobanteAgrupado);

            c.setMontoPagado(c.getMontoPagado() + montoAAplicar);

            if (c.getMontoPagado() >= c.getMontoTotal()) {
                c.setEstado(pagoADestiempo ? EstadoCuota.PAGADO_DESTIEMPO : EstadoCuota.PAGADO_TOTAL);
                if (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) {
                    contratoHistorialService.registrarHito(contrato, "INICIAL_COMPLETADA",
                            "Inicial cancelada al 100%. Pendiente de estructuración y activación.", "Pago validado");
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

        if (esIngresoCaja) {
            String urlVistaPrevia = "/api/pagos/recibo/" + numeroComprobanteAgrupado + "/pdf";
            contratoHistorialService.registrarHito(contrato, "INGRESO_CAJA",
                    "Se procesó un ingreso de S/ " + montoTotalAbonado + " en EFECTIVO físico. Pendiente de depósito en caja. Recibo: " + numeroComprobanteAgrupado, "Caja", urlVistaPrevia);
        } else {
            String urlVistaPrevia = "/api/pagos/comprobante/" + numeroComprobanteAgrupado + "/pdf";
            contratoHistorialService.registrarHito(contrato, "NOTA_ABONO_CUOTAS",
                    "Se procesó abono de S/ " + montoTotalAbonado + " tras validar transferencia bancaria. Comprobante: " + numeroComprobanteAgrupado, "Banco", urlVistaPrevia);
        }

        return pagosProcesados;
    }

    @Transactional
    public void anularPago(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId).orElseThrow(() -> new RuntimeException("Pago no encontrado"));
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
                    pago.setDiasRetraso(diasDiferencia > 0 ? (int) diasDiferencia : 0);
                    pago.setPagoADestiempo(diasDiferencia > 0);
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
        Pago pago = pagoRepository.findById(pagoId).orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        if (pago.getNumeroComprobante() == null || pago.getNumeroComprobante().isEmpty()) {
            throw new RuntimeException("El pago no tiene un comprobante asignado.");
        }

        return generarNotaAbonoMultiPdf(pago.getNumeroComprobante());
    }

    @Transactional(readOnly = true)
    public byte[] generarNotaAbonoMultiPdf(String numeroComprobante) {
        List<Pago> pagos = pagoRepository.findByNumeroComprobante(numeroComprobante);
        if (pagos.isEmpty()) throw new RuntimeException("Comprobante no encontrado.");

        Contrato contrato = pagos.get(0).getCuota().getContrato();
        Empresa empresa = empresaRepository.findById(1L).orElse(null);
        double totalAbonado = pagos.stream().mapToDouble(Pago::getMontoAbonado).sum();

        Context context = new Context();
        context.setVariable("pagos", pagos);
        context.setVariable("pagoBase", pagos.get(0));
        context.setVariable("totalAbonado", totalAbonado);
        context.setVariable("cliente", contrato.getCliente());
        context.setVariable("contrato", contrato);
        context.setVariable("montoEnLetras", NumeroALetrasConverter.convertir(totalAbonado, "SOLES"));
        context.setVariable("empresa", empresa);
        context.setVariable("imgLogo", getImagenBase64("logo_terranort.png"));

        return compilarPdf("nota-abono", context);
    }

    @Transactional(readOnly = true)
    public byte[] generarReciboIngresoPdf(String numeroComprobante) {
        List<Pago> pagos = pagoRepository.findByNumeroComprobante(numeroComprobante);
        if (pagos.isEmpty()) throw new RuntimeException("Recibo no encontrado.");

        Contrato contrato = pagos.get(0).getCuota().getContrato();
        Empresa empresa = empresaRepository.findById(1L).orElse(null);
        double totalAbonado = pagos.stream().mapToDouble(Pago::getMontoAbonado).sum();

        Context context = new Context();
        context.setVariable("pagos", pagos);
        context.setVariable("pagoBase", pagos.get(0));
        context.setVariable("totalAbonado", totalAbonado);
        context.setVariable("cliente", contrato.getCliente());
        context.setVariable("contrato", contrato);
        context.setVariable("montoEnLetras", NumeroALetrasConverter.convertir(totalAbonado, "SOLES"));
        context.setVariable("empresa", empresa);
        context.setVariable("imgLogo", getImagenBase64("logo_terranort.png"));

        return compilarPdf("recibo-ingreso", context);
    }

    private byte[] compilarPdf(String templateName, Context context) {
        String html = templateEngine.process(templateName, context);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error crítico al compilar PDF (" + templateName + "): " + e.getMessage());
        }
    }

    @Transactional
    public List<Pago> conciliarReciboCaja(String numeroRecibo, List<String> bancos, List<String> operaciones, List<Double> montos, List<MultipartFile> vouchers) {

        List<Pago> pagosEnCaja = pagoRepository.findByNumeroComprobante(numeroRecibo);
        if (pagosEnCaja.isEmpty()) {
            throw new RuntimeException("No se encontró ningún registro de caja con el recibo: " + numeroRecibo);
        }

        double totalEsperado = pagosEnCaja.stream().mapToDouble(Pago::getMontoAbonado).sum();
        double totalDepositado = montos.stream().mapToDouble(Double::doubleValue).sum();

        if (totalDepositado < totalEsperado) {
            throw new RuntimeException("Descuadre de caja: Se esperaban S/ " + totalEsperado + " pero los depósitos suman S/ " + totalDepositado);
        }

        Contrato contrato = pagosEnCaja.get(0).getCuota().getContrato();
        LocalDate hoy = LocalDate.now();
        List<String> operacionesConcatenadas = new ArrayList<>();

        for (int i = 0; i < montos.size(); i++) {
            String fotoVoucherUrl = null;
            if (vouchers != null && vouchers.size() > i && vouchers.get(i) != null && !vouchers.get(i).isEmpty()) {
                MultipartFile file = vouchers.get(i);
                String cleanName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename()).replaceAll("[\\s+]", "_");
                String customName = "DEP_" + operaciones.get(i) + "_" + cleanName;
                fotoVoucherUrl = "uploads/" + fileStorageService.storeFileWithCustomName(file, "vouchers", customName);
            }

            DepositoBancario deposito = DepositoBancario.builder()
                    .numeroReciboCaja(numeroRecibo)
                    .banco(bancos.get(i))
                    .numeroOperacion(operaciones.get(i))
                    .monto(montos.get(i))
                    .fotoVoucherUrl(fotoVoucherUrl)
                    .fechaDeposito(hoy)
                    .build();

            depositoBancarioRepository.save(deposito);
            operacionesConcatenadas.add(bancos.get(i) + " Op:" + operaciones.get(i));
        }

        String opsUnidas = String.join(" | ", operacionesConcatenadas);

        for (Pago pago : pagosEnCaja) {
            pago.setEstado(EstadoPago.PROCESADO);
            pago.setNumeroOperacion(opsUnidas);
            pagoRepository.save(pago);
        }

        contratoHistorialService.registrarHito(contrato, "CONCILIACION_CAJA",
                "El dinero físico del recibo " + numeroRecibo + " fue depositado exitosamente en las cuentas bancarias. (" + opsUnidas + ")", "Tesorería", null);

        return pagosEnCaja;
    }

    @Transactional
    public void subirReciboFirmado(String numeroRecibo, MultipartFile archivoFirmado) {
        List<Pago> pagos = pagoRepository.findByNumeroComprobante(numeroRecibo);
        if (pagos.isEmpty()) {
            throw new RuntimeException("No se encontró ningún registro con el recibo: " + numeroRecibo);
        }

        Contrato contrato = pagos.get(0).getCuota().getContrato();

        if (archivoFirmado != null && !archivoFirmado.isEmpty()) {
            String cleanName = org.springframework.util.StringUtils.cleanPath(archivoFirmado.getOriginalFilename()).replaceAll("[\\s+]", "_");
            String customName = "FIRMADO_" + numeroRecibo + "_" + cleanName;

            String savedPath = "uploads/" + fileStorageService.storeFileWithCustomName(archivoFirmado, "recibos_firmados", customName);

            contratoHistorialService.registrarHito(
                    contrato,
                    "RECIBO_FIRMADO",
                    "Se adjuntó el recibo físico firmado por el cliente correspondiente al ingreso " + numeroRecibo + ".",
                    "Caja"
            );
        } else {
            throw new RuntimeException("Debe adjuntar un documento válido.");
        }
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> obtenerReporteCajaFisica() {
        List<Pago> pagosEnCaja = pagoRepository.findByEstado(EstadoPago.POR_VALIDAR);

        double totalEfectivoEnOficina = pagosEnCaja.stream().mapToDouble(Pago::getMontoAbonado).sum();

        java.util.Map<String, List<Pago>> pagosAgrupadosPorRecibo = pagosEnCaja.stream()
                .collect(Collectors.groupingBy(p -> p.getNumeroComprobante() != null ? p.getNumeroComprobante() : "SIN_RECIBO_ANTIGUO"));

        java.util.Map<String, Object> reporte = new java.util.HashMap<>();
        reporte.put("totalEfectivoSoles", totalEfectivoEnOficina);
        reporte.put("cantidadRecibosPendientes", pagosAgrupadosPorRecibo.size());
        reporte.put("detalleRecibos", pagosAgrupadosPorRecibo);

        return reporte;
    }
}