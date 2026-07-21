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
import com.sistema.base.api.core.Financiamiento.Pago.Sunat.SunatService;
import com.sistema.base.api.core.Usuario.Clientes.Cliente;
import com.sistema.base.api.service.FileStorageService;
import com.sistema.base.api.utils.NumeroALetrasConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
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
import java.util.Map;
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
    private final SunatService sunatService;

    @Transactional(readOnly = true)
    public List<Pago> listarPorCuota(Long cuotaId) {
        return pagoRepository.findByCuotaIdAndEnabledTrue(cuotaId);
    }

    @Transactional(readOnly = true)
    public Page<Pago> listarPagosPaginadosConFiltros(
            int page, int size,
            String metodoPago, // Este valor vendrá del frontend como "TRANSFERENCIA", "DEPOSITO", "EFECTIVO", etc.
            TipoComprobante tipoComprobante,
            EstadoPago estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Specification<Pago> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ✅ LÓGICA DE FILTRADO DINÁMICO
            if (metodoPago != null && !metodoPago.trim().isEmpty()) {
                if ("TRANSFERENCIA".equalsIgnoreCase(metodoPago)) {
                    // Agrupamos todos los bancos que son transferencias
                    predicates.add(root.get("metodoPago").in("BCP", "INTERBANK", "BBVA", "SCOTIABANK", "CAJA PIURA", "BANCO NACION", "MI BANCO"));
                } else if ("DEPOSITO".equalsIgnoreCase(metodoPago)) {
                    // Agrupamos todos los depósitos en cuenta
                    predicates.add(root.get("metodoPago").in("DEP.CTA.BCP", "DEP.CTA.INTERBANK"));
                } else {
                    // Para EFECTIVO, YAPE, u otros que no requieren agrupación, buscamos igualdad
                    predicates.add(cb.equal(root.get("metodoPago"), metodoPago));
                }
            }

            if (tipoComprobante != null) {
                predicates.add(cb.equal(root.get("tipoComprobante"), tipoComprobante));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaPago"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaPago"), fechaHasta));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return pagoRepository.findAll(spec, pageable);
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

        List<String> nombresCuotasAfectadas = new ArrayList<>();

        // ✅ NUEVA LÓGICA DE DESCRIPCIÓN UNIFICADA
        String nombreManzana = (contrato.getLote().getManzana() != null) ? contrato.getLote().getManzana().getNombre() : "";
        String codigoLote = "MZ " + nombreManzana + "-" + contrato.getLote().getNumero();
        String fechaFmt = fechaPagoActual.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        for (Cuota c : cuotasPendientes) {
            if (montoRestante <= 0) break;

            Double saldoCuota = c.getMontoTotal() - c.getMontoPagado();
            Double montoAAplicar = Math.min(montoRestante, saldoCuota);

            boolean pagoADestiempo = fechaPagoActual.isAfter(c.getFechaVencimiento());
            int diasRetraso = pagoADestiempo ? (int) ChronoUnit.DAYS.between(c.getFechaVencimiento(), fechaPagoActual) : 0;

            String nombreCuota = (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) ? "INICIAL" : "CUOTA " + c.getNumeroCuota();

            if (!nombresCuotasAfectadas.contains(nombreCuota)) {
                nombresCuotasAfectadas.add(nombreCuota);
            }

            // ✅ FORMATEAMOS LA DESCRIPCIÓN EXACTA PARA ANTICIPOS
            String descDinamica;
            if (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) {
                descDinamica = "POR EL MONTO DE SEPARACION ANTICIPO RECIBIDO: " + codigoLote + " PROYECTO DENOMINADO LOTIZACION OLMOS,SECTOR OLMOS,DISTRITO DE OLMOS - LAMBAYEQUE. DEL " + fechaFmt + " **PAGO ANTICIPADO**";
            } else {
                descDinamica = "ANTICIPO RECIBIDO: CUOTA " + c.getNumeroCuota() + " , " + codigoLote + " PROYECTO DENOMINADO LOTIZACION OLMOS,SECTOR OLMOS,DISTRITO DE OLMOS - LAMBAYEQUE. DEL " + fechaFmt + " **PAGO ANTICIPADO**";
            }

            if (descripcion != null && !descripcion.trim().isEmpty()) {
                descDinamica += " - " + descripcion;
            }

            Pago pago = Pago.builder()
                    .cuota(c)
                    .montoAbonado(montoAAplicar)
                    .metodoPago(metodoPago)
                    .numeroOperacion(numeroOperacion)
                    .descripcion(descDinamica) // Se guarda la nueva descripción
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
                            "Inicial cancelada al 100%. Pendiente de estructuración y activación.", "Pago validado", null);
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

        String resumenCuotas = String.join(", ", nombresCuotasAfectadas);

        if (esIngresoCaja) {
            String urlVistaPrevia = "/api/pagos/recibo/" + numeroComprobanteAgrupado + "/pdf";
            contratoHistorialService.registrarHito(contrato, "INGRESO_CAJA",
                    "Se recibió S/ " + montoAbonado + " en EFECTIVO físico correspondiente a (" + resumenCuotas + "). Pendiente de depósito bancario bajo el recibo " + numeroComprobanteAgrupado, "Caja", urlVistaPrevia);
        } else {
            if (nombresCuotasAfectadas.size() > 1) {
                String urlVistaPrevia = "/api/pagos/comprobante/" + numeroComprobanteAgrupado + "/pdf";
                contratoHistorialService.registrarHito(contrato, "NOTA_ABONO_CUOTAS",
                        "Se registró un abono de S/ " + montoAbonado + " correspondiente a (" + resumenCuotas + ") bajo el comprobante " + numeroComprobanteAgrupado, "Banco", urlVistaPrevia);
            }
        }

        return pagosGenerados;
    }

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

        List<String> nombresCuotasAfectadas = new ArrayList<>();

        // ✅ NUEVA LÓGICA DE DESCRIPCIÓN UNIFICADA
        String nombreManzana = (contrato.getLote().getManzana() != null) ? contrato.getLote().getManzana().getNombre() : "";
        String codigoLote = "MZ " + nombreManzana + "-" + contrato.getLote().getNumero();
        String fechaFmt = fechaDelPago.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        for (Cuota c : cuotasPendientes) {
            if (montoRestante <= 0) break;

            Double saldoCuota = c.getMontoTotal() - c.getMontoPagado();
            Double montoAAplicar = Math.min(montoRestante, saldoCuota);

            boolean pagoADestiempo = fechaDelPago.isAfter(c.getFechaVencimiento());
            int diasRetraso = pagoADestiempo ? (int) ChronoUnit.DAYS.between(c.getFechaVencimiento(), fechaDelPago) : 0;

            String nombreCuota = (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) ? "INICIAL" : "CUOTA " + c.getNumeroCuota();

            if (!nombresCuotasAfectadas.contains(nombreCuota)) {
                nombresCuotasAfectadas.add(nombreCuota);
            }

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

            // ✅ FORMATEAMOS LA DESCRIPCIÓN EXACTA PARA ANTICIPOS
            String descDinamica;
            if (c.getNumeroCuota() != null && c.getNumeroCuota() == 0) {
                descDinamica = "POR EL MONTO DE SEPARACION ANTICIPO RECIBIDO: " + codigoLote + " PROYECTO DENOMINADO LOTIZACION OLMOS,SECTOR OLMOS,DISTRITO DE OLMOS - LAMBAYEQUE. DEL " + fechaFmt + " **PAGO ANTICIPADO**";
            } else {
                descDinamica = "ANTICIPO RECIBIDO: CUOTA " + c.getNumeroCuota() + " , " + codigoLote + " PROYECTO DENOMINADO LOTIZACION OLMOS,SECTOR OLMOS,DISTRITO DE OLMOS - LAMBAYEQUE. DEL " + fechaFmt + " **PAGO ANTICIPADO**";
            }

            if (descripcion != null && !descripcion.trim().isEmpty()) {
                descDinamica += " - " + descripcion;
            }

            pagoActual.setMetodoPago(metodoPago);
            pagoActual.setNumeroOperacion(numeroOperacion);
            pagoActual.setDescripcion(descDinamica); // Se guarda la nueva descripción
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
                            "Inicial cancelada al 100%. Pendiente de estructuración y activación.", "Pago validado", null);
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

        String resumenCuotas = String.join(", ", nombresCuotasAfectadas);

        if (esIngresoCaja) {
            String urlVistaPrevia = "/api/pagos/recibo/" + numeroComprobanteAgrupado + "/pdf";
            contratoHistorialService.registrarHito(contrato, "INGRESO_CAJA",
                    "Se procesó un ingreso de S/ " + montoTotalAbonado + " en EFECTIVO físico correspondiente a (" + resumenCuotas + "). Pendiente de depósito en caja. Recibo: " + numeroComprobanteAgrupado, "Caja", urlVistaPrevia);
        } else {
            if (nombresCuotasAfectadas.size() > 1) {
                String urlVistaPrevia = "/api/pagos/comprobante/" + numeroComprobanteAgrupado + "/pdf";
                contratoHistorialService.registrarHito(contrato, "NOTA_ABONO_CUOTAS",
                        "Se procesó abono de S/ " + montoTotalAbonado + " tras validar transferencia bancaria por (" + resumenCuotas + "). Comprobante: " + numeroComprobanteAgrupado, "Banco", urlVistaPrevia);
            }
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
    public byte[] generarComprobanteElectronicoPdf(String numeroComprobante) {
        List<Pago> pagos = pagoRepository.findByNumeroComprobante(numeroComprobante);
        if (pagos.isEmpty()) throw new RuntimeException("Comprobante no encontrado.");

        Contrato contrato = pagos.get(0).getCuota().getContrato();
        Empresa empresa = empresaRepository.findById(1L).orElse(null);
        double totalAbonado = pagos.stream().mapToDouble(Pago::getMontoAbonado).sum();

        Context context = new Context();
        context.setVariable("pagos", pagos);
        context.setVariable("pagoBase", pagos.get(0)); // Contiene el tipo (FACTURA/BOLETA) y número (F001-...)
        context.setVariable("totalAbonado", totalAbonado);
        context.setVariable("cliente", contrato.getCliente());
        context.setVariable("contrato", contrato);
        context.setVariable("montoEnLetras", NumeroALetrasConverter.convertir(totalAbonado, "SOLES"));
        context.setVariable("empresa", empresa);
        context.setVariable("imgLogo", getImagenBase64("logo_terranort.png"));

        return compilarPdf("comprobante-electronico", context);
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
                    "Caja",
                    savedPath
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

    @Transactional
    public List<Pago> registrarPagoYEmitirSunat(Long cuotaId, Double montoAbonado, String metodoPago,
                                                String numeroOperacion,
                                                String descripcionPersonalizada,
                                                TipoComprobante tipoComprobante, String prefijoSerie, String tipoIgv, String tipoDoc,
                                                String ruc, String razonSocial, String direccionFactura) {

        Cuota cuotaInicialRequest = cuotaRepository.findById(cuotaId).orElseThrow(() -> new RuntimeException("Cuota no encontrada"));
        Contrato contrato = cuotaInicialRequest.getContrato();
        List<Cuota> cuotasPendientes = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream().filter(c -> c.getMontoPagado() < c.getMontoTotal()).collect(Collectors.toList());

        double deudaTotal = cuotasPendientes.stream().mapToDouble(c -> c.getMontoTotal() - c.getMontoPagado()).sum();
        if (montoAbonado > deudaTotal) throw new RuntimeException("El monto a abonar supera la deuda total.");

        String igvDefinitivo = (tipoIgv != null && !tipoIgv.trim().isEmpty()) ? tipoIgv : "20";
        Serie serieActiva = serieRepository.findActiveSerieForUpdate(tipoComprobante)
                .orElseThrow(() -> new RuntimeException("No hay serie activa para " + tipoComprobante.name()));

        int nuevoCorrelativo = serieActiva.getUltimoCorrelativo() + 1;
        serieActiva.setUltimoCorrelativo(nuevoCorrelativo);
        serieRepository.save(serieActiva);

        String numeroComprobanteAgrupado = prefijoSerie + "-" + String.format("%06d", nuevoCorrelativo);

        Double montoRestante = montoAbonado;
        List<Pago> pagosGenerados = new ArrayList<>();
        LocalDate fechaPagoActual = LocalDate.now();

        for (Cuota c : cuotasPendientes) {
            if (montoRestante <= 0) break;

            Double saldoCuota = c.getMontoTotal() - c.getMontoPagado();
            Double montoAAplicar = Math.min(montoRestante, saldoCuota);

            Pago pago = Pago.builder()
                    .cuota(c).montoAbonado(montoAAplicar).metodoPago(metodoPago)
                    .numeroOperacion(numeroOperacion)
                    .estado(EstadoPago.PROCESADO).tipoComprobante(tipoComprobante)
                    .numeroComprobante(numeroComprobanteAgrupado).fechaRegistro(LocalDate.now()).fechaPago(fechaPagoActual).build();

            // ✅ FORMATEAMOS LA DESCRIPCIÓN EXACTAMENTE COMO EL XML DE MUESTRA
            String nombreManzana = (c.getContrato().getLote().getManzana() != null) ? c.getContrato().getLote().getManzana().getNombre() : "";
            String codigoLote = "MZ " + nombreManzana + "-" + c.getContrato().getLote().getNumero();
            String descDinamica;

            if (c.getNumeroCuota() == 0) {
                descDinamica = "A CUENTA INICIAL. " + codigoLote + ". PROYECTO DENOMINADO LOTIZACION OLMOS,SECTOR OLMOS,DISTRITO OLMOS ,LAMBAYEQUE.";
            } else {
                descDinamica = "A CUENTA CUOTA " + c.getNumeroCuota() + ". " + codigoLote + ". PROYECTO DENOMINADO LOTIZACION OLMOS,SECTOR OLMOS,DISTRITO OLMOS ,LAMBAYEQUE.";
            }

            // Si el usuario escribió algo extra en el formulario, se añade al final
            if (descripcionPersonalizada != null && !descripcionPersonalizada.trim().isEmpty()) {
                descDinamica += " - " + descripcionPersonalizada;
            }

            pago.setDescripcion(descDinamica);

            c.setMontoPagado(c.getMontoPagado() + montoAAplicar);
            c.setEstado(c.getMontoPagado() >= c.getMontoTotal() ? EstadoCuota.PAGADO_TOTAL : EstadoCuota.PAGADO_PARCIAL);
            cuotaRepository.save(c);
            pagosGenerados.add(pagoRepository.save(pago));
            montoRestante -= montoAAplicar;
        }

        double totalPagadoHastaHoy = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream().mapToDouble(Cuota::getMontoPagado).sum();

        if (totalPagadoHastaHoy >= 2500.0) {
            comisionService.evaluarYGenerarComisiones(contrato);
        }

        Empresa empresa = empresaRepository.findById(1L).orElseThrow(() -> new RuntimeException("Empresa no configurada"));

        Map<String, Object> res = sunatService.emitirComprobanteMultiple(pagosGenerados, tipoComprobante.name(), prefijoSerie, String.valueOf(nuevoCorrelativo), igvDefinitivo, tipoDoc, ruc, razonSocial, direccionFactura, empresa);

        if (res != null && (Boolean) res.getOrDefault("success", false)) {
            Map<String, Object> data = (Map<String, Object>) res.get("data");
            Map<String, Object> links = (Map<String, Object>) res.get("links");
            for (Pago p : pagosGenerados) {
                p.setEnlacePdfSunat((String) links.get("pdf"));
                p.setEnlaceXmlSunat((String) links.get("xml"));
                p.setEstadoSunat("ACEPTADO");
                pagoRepository.save(p);
            }
            return pagosGenerados;
        }
        throw new RuntimeException("Error en SUNAT: " + res.get("message"));
    }

    @Transactional
    public List<Pago> convertirNotaAbonoABoleta(String numeroNotaAbono, String prefijoSerieBoleta, String tipoIgv, String ruc, String razonSocial, String direccionFactura) {
        // 1. Buscamos los pagos asociados a la Nota de Abono existente
        List<Pago> pagosNota = pagoRepository.findByNumeroComprobante(numeroNotaAbono);
        if (pagosNota.isEmpty()) {
            throw new RuntimeException("No se encontró la Nota de Abono con el número: " + numeroNotaAbono);
        }

        // Validamos que el documento origen sea realmente una Nota de Abono
        if (pagosNota.get(0).getTipoComprobante() != TipoComprobante.NOTA_ABONO) {
            throw new RuntimeException("El documento a convertir no es una Nota de Abono.");
        }

        Contrato contrato = pagosNota.get(0).getCuota().getContrato();
        Empresa empresa = empresaRepository.findById(1L).orElseThrow(() -> new RuntimeException("Empresa no configurada"));

        // 2. Obtenemos la nueva serie activa para BOLETA
        Serie serieActiva = serieRepository.findActiveSerieForUpdate(TipoComprobante.BOLETA)
                .orElseThrow(() -> new RuntimeException("No se encontró una serie activa para BOLETA"));

        int nuevoCorrelativo = serieActiva.getUltimoCorrelativo() + 1;
        serieActiva.setUltimoCorrelativo(nuevoCorrelativo);
        serieRepository.save(serieActiva);

        String nuevoNumeroBoleta = prefijoSerieBoleta + "-" + String.format("%06d", nuevoCorrelativo);
        String igvDefinitivo = (tipoIgv != null && !tipoIgv.trim().isEmpty()) ? tipoIgv : "20";

        List<Pago> pagosActualizados = new ArrayList<>();

        // 3. Actualizamos los registros de pago para que apunten a la nueva Boleta
        for (Pago pago : pagosNota) {
            pago.setTipoComprobante(TipoComprobante.BOLETA);
            pago.setNumeroComprobante(nuevoNumeroBoleta);
            pagosActualizados.add(pagoRepository.save(pago));
        }

        // 4. Emitimos el comprobante electrónico a través de la API de SUNAT
        Map<String, Object> res = sunatService.emitirComprobanteMultiple(
                pagosActualizados,
                TipoComprobante.BOLETA.name(),
                prefijoSerieBoleta,
                String.valueOf(nuevoCorrelativo),
                igvDefinitivo,
                "1", // 1 para Boleta de Venta
                ruc,
                razonSocial,
                direccionFactura,
                empresa
        );

        // 5. Validamos la respuesta de SUNAT y guardamos los enlaces devueltos
        if (res != null && (Boolean) res.getOrDefault("success", false)) {
            Map<String, Object> links = (Map<String, Object>) res.get("links");
            for (Pago p : pagosActualizados) {
                p.setEnlacePdfSunat((String) links.get("pdf"));
                p.setEnlaceXmlSunat((String) links.get("xml"));
                p.setEstadoSunat("ACEPTADO");
                pagoRepository.save(p);
            }

            // Registramos el hito en el historial del contrato
            contratoHistorialService.registrarHito(
                    contrato,
                    "NOTA_ABONO_A_BOLETA",
                    "Se convirtió la Nota de Abono " + numeroNotaAbono + " a la Boleta Electrónica " + nuevoNumeroBoleta,
                    "SUNAT",
                    (String) links.get("pdf")
            );

            return pagosActualizados;
        }

        throw new RuntimeException("Error al emitir la boleta en SUNAT: " + res.get("message"));
    }

}