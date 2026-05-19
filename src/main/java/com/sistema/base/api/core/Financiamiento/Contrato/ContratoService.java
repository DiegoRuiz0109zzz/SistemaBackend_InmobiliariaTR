package com.sistema.base.api.core.Financiamiento.Contrato;

import com.sistema.base.api.core.Empresa.Empresa;
import com.sistema.base.api.core.Empresa.EmpresaRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorialService;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoMedida.ContratoMedidas;
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
import com.sistema.base.api.service.FileStorageService; // ✅ NUEVO IMPORT
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile; // ✅ NUEVO IMPORT
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.time.temporal.ChronoUnit;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    // ✅ INYECTAMOS EL SERVICIO DE ARCHIVOS
    private final FileStorageService fileStorageService;

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
        Cliente coComprador = (req.getCoCompradorId() != null) ? clienteRepository.findById(req.getCoCompradorId()).orElse(null) : null;
        Vendedor vendedor = vendedorRepository.findById(req.getVendedorId()).orElseThrow(() -> new RuntimeException("Vendedor no existe."));
        Cotizacion cotizacion = (req.getCotizacionId() != null) ? cotizacionRepository.findById(req.getCotizacionId()).orElse(null) : null;

        Double saldoFinanciar = req.getPrecioTotal() - req.getMontoInicialAcordado();
        StringBuilder descBuilder = new StringBuilder();
        descBuilder.append("Cuota Inicial de S/ ").append(req.getMontoInicialAcordado()).append(". ");

        int cantidadCuotasFijas = (req.getCantidadCuotas() != null) ? req.getCantidadCuotas() : 0;

        if (req.getCuotasEspeciales() != null && req.getCuotasEspeciales() > 0) {
            descBuilder.append("Fraccionado en ").append(req.getCuotasEspeciales()).append(" cuotas de S/ ").append(req.getMontoCuotaEspecial())
                    .append(" y ").append(cantidadCuotasFijas - req.getCuotasEspeciales()).append(" cuotas con el saldo restante.");
        } else if (cantidadCuotasFijas > 0) {
            descBuilder.append("Fraccionado en ").append(cantidadCuotasFijas).append(" cuotas regulares.");
        } else {
            descBuilder.append("Cronograma pendiente de definición.");
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
                .descripcion(descBuilder.toString()).tipoInicial(req.getTipoInicial())
                .cuotasFlexibles(req.getCuotasFlexibles() != null ? req.getCuotasFlexibles() : false)
                .fechaInicioCronograma(req.getFechaInicioPago())
                .estadoContrato(estadoContratoReal)
                .fechaContrato(LocalDate.now())
                .urlDocumentoFirmado(null) // ✅ El documento nace vacío para alertar subida
                .build();

        Contrato contratoGuardado = contratoRepository.save(contrato);

        Cuota cuota0 = Cuota.builder().contrato(contratoGuardado).numeroCuota(0).tipoCuota(TipoCuota.INICIAL)
                .montoTotal(req.getMontoInicialAcordado()).montoPagado(0.0)
                .fechaVencimiento((req.getFechaLimiteInicial() != null) ? req.getFechaLimiteInicial() : LocalDate.now())
                .estado(EstadoCuota.PENDIENTE).build();
        cuotaRepository.save(cuota0);

        if (abonoInicialPrometido > 0) {
            Pago pagoPendiente = Pago.builder().cuota(cuota0).montoAbonado(abonoInicialPrometido)
                    .estado(EstadoPago.POR_VALIDAR).metodoPago("POR CONFIRMAR EN CAJA").build();
            pagoRepository.save(pagoPendiente);
        }

        if (estadoContratoReal == EstadoContrato.ACTIVO && saldoFinanciar > 0 && cantidadCuotasFijas > 0) {
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

        String tipoHito = (contratoGuardado.getEstadoContrato() == EstadoContrato.SEPARADO) ? "CONTRATO_SEPARADO" : "CONTRATO_ACTIVO";
        String descripcionHito = (contratoGuardado.getEstadoContrato() == EstadoContrato.SEPARADO) ? "Contrato de Separación registrado. Pendiente de documento firmado." : "Contrato Activo registrado. Pendiente de documento firmado.";
        contratoHistorialService.registrarHito(contratoGuardado, tipoHito, descripcionHito, req.getObservacion());

        return contratoGuardado;
    }

    // ✅ NUEVO MÉTODO: CASOS 1 Y 2 - SUBIDA Y REEMPLAZO DE DOCUMENTOS
    @Transactional
    public Contrato subirDocumentoFirmado(Long contratoId, MultipartFile archivo, String motivo) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        // 1. Guardar el archivo físico (Se guarda una sola vez en disco)
        // Eliminamos el .pdf del customName porque tu fileStorageService ya extrae la extensión original
        String customName = "CONTRATO_FIRMADO_" + contrato.getEstadoContrato() + "_" + contratoId + "_" + System.currentTimeMillis();
        String savedPath = fileStorageService.storeFileWithCustomName(archivo, "contratos-firmados", customName);

        // Creamos una variable con la ruta exacta
        String rutaCompleta = "uploads/" + savedPath;

        // 2. Actualizar la URL en la tabla principal
        contrato.setUrlDocumentoFirmado(rutaCompleta);
        Contrato actualizado = contratoRepository.save(contrato);

        // 3. Registrar en el historial y enviarle la ruta del PDF
        String tipoHito = "DOCUMENTO_CARGADO";
        String descripcion = "Se subió el archivo firmado correspondiente al estado: " + contrato.getEstadoContrato();

        // ✅ AQUÍ ESTÁ EL CAMBIO: Pasamos 'rutaCompleta' como el 5to parámetro
        contratoHistorialService.registrarHito(actualizado, tipoHito, descripcion, motivo, rutaCompleta);

        return actualizado;
    }

    @Transactional
    public Contrato generarNuevoDocumentoContrato(Long contratoId, String observacion) {
        Contrato contrato = contratoRepository.findById(contratoId).orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
        contrato.setFechaContrato(LocalDate.now());
        Contrato contratoGuardado = contratoRepository.save(contrato);
        contratoHistorialService.registrarHito(contratoGuardado, contrato.getEstadoContrato().name(), "Se generó una nueva versión del documento PDF.", observacion);
        return contratoGuardado;
    }

    @Transactional
    public Contrato actualizarContrato(Long id, ContratoRequest request) {
        Contrato contrato = contratoRepository.findById(id).orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
        StringBuilder cambios = new StringBuilder();
        boolean huboCambios = false;

        // Lógica de cambio de Titular, Co-Comprador y Lote...
        if (!Objects.equals(contrato.getCliente().getId(), request.getClienteId())) {
            Cliente nuevo = clienteRepository.findById(request.getClienteId()).orElseThrow();
            cambios.append("Cambio Titular a ").append(nuevo.getNombres()).append(". ");
            contrato.setCliente(nuevo);
            huboCambios = true;
        }

        // ✅ NUEVO: ACTUALIZAR FECHA DE LA CUOTA 0 (Aumentar fecha limite)
        if (request.getFechaLimiteInicial() != null) {
            Cuota cuota0 = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId()).stream()
                    .filter(c -> c.getNumeroCuota() == 0).findFirst().orElse(null);
            if (cuota0 != null && !Objects.equals(cuota0.getFechaVencimiento(), request.getFechaLimiteInicial())) {
                cuota0.setFechaVencimiento(request.getFechaLimiteInicial());
                cuotaRepository.save(cuota0);
                cambios.append("Fecha límite inicial extendida a ").append(request.getFechaLimiteInicial()).append(". ");
                huboCambios = true;
            }
        }

        // ✅ NUEVO: GENERAR CRONOGRAMA SI PASA A ACTIVO POR EDICIÓN
        if (contrato.getEstadoContrato() == EstadoContrato.ACTIVO && request.getCantidadCuotas() != null && request.getCantidadCuotas() > 0) {
            boolean tieneCronograma = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                    .stream().anyMatch(c -> c.getNumeroCuota() > 0);

            if (!tieneCronograma) {
                contrato.setCantidadCuotas(request.getCantidadCuotas());
                contrato.setFechaInicioCronograma(request.getFechaInicioPago());

                SimulacionRequest sim = new SimulacionRequest();
                sim.setPrecioTotal(contrato.getPrecioTotal()); sim.setMontoInicial(contrato.getMontoInicial());
                sim.setCantidadCuotas(request.getCantidadCuotas()); sim.setFechaInicioPago(request.getFechaInicioPago());
                sim.setCuotasEspeciales(request.getCuotasEspeciales()); sim.setMontoCuotaEspecial(request.getMontoCuotaEspecial());

                List<CuotaPreview> proyeccion = simularCronograma(sim);
                List<Cuota> cuotasAGuardar = new ArrayList<>();
                for (CuotaPreview cp : proyeccion) {
                    TipoCuota tipo = (cp.getNumeroCuota() <= (request.getCuotasEspeciales() != null ? request.getCuotasEspeciales() : 0)) ? TipoCuota.ESPECIAL : TipoCuota.MENSUAL;
                    cuotasAGuardar.add(Cuota.builder().contrato(contrato).numeroCuota(cp.getNumeroCuota())
                            .tipoCuota(tipo).montoTotal(cp.getMonto()).montoPagado(0.0)
                            .fechaVencimiento(cp.getFechaVencimiento()).estado(EstadoCuota.PENDIENTE).build());
                }
                cuotaRepository.saveAll(cuotasAGuardar);
                cambios.append("Cronograma generado tras activación. ");
                huboCambios = true;
            }
        }

        if (huboCambios) {
            Contrato actualizado = contratoRepository.save(contrato);
            contratoHistorialService.registrarHito(actualizado, "MODIFICACION", cambios.toString().trim(), request.getObservacion());
            return actualizado;
        }
        return contrato;
    }

    @Transactional(readOnly = true)
    public List<Contrato> listarTodos() { return contratoRepository.findByEnabledTrue(); }

    @Transactional(readOnly = true)
    public Contrato obtenerPorId(Long id) { return contratoRepository.findById(id).orElseThrow(); }

    private String getImagenBase64(String nombreArchivo) {
        try {
            Path path = Paths.get("uploads/imagens/" + nombreArchivo);
            if (Files.exists(path)) return "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
        } catch (Exception ignored) {}
        return null;
    }

    private byte[] generarDocumentoPdfBytes(Contrato contrato) {
        List<Cuota> cuotas = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId());

        LocalDate fechaCuotaCero = cuotas.stream()
                .filter(c -> c.getNumeroCuota() == 0)
                .map(Cuota::getFechaVencimiento)
                .findFirst()
                .orElse(null);

        // ✅ CÁLCULO DINÁMICO DE DÍAS DE VALIDEZ
        long diasValidez = 0;
        if (fechaCuotaCero != null) {
            // Calculamos la diferencia entre hoy y el vencimiento de la separación
            diasValidez = ChronoUnit.DAYS.between(LocalDate.now(), fechaCuotaCero);
        }

        //LocalDate fechaCuotaCero = cuotas.stream().filter(c -> c.getNumeroCuota() == 0).map(Cuota::getFechaVencimiento).findFirst().orElse(null);
        List<Cuota> cuotasMensuales = cuotas.stream().filter(c -> c.getNumeroCuota() > 0).collect(Collectors.toList());
        Empresa empresa = empresaRepository.findById(1L).orElse(null);

        Context context = new Context();
        context.setVariable("contrato", contrato);
        context.setVariable("empresa", empresa);
        context.setVariable("fechaCuotaCero", fechaCuotaCero);
        context.setVariable("diasValidez", diasValidez > 0 ? diasValidez : 0);
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
        } catch (Exception e) { throw new RuntimeException("Error PDF: " + e.getMessage()); }
    }

    @Transactional(readOnly = true)
    public byte[] generarVistaPreviaPdf(Long contratoId) {
        Contrato contrato = obtenerPorId(contratoId);
        return generarDocumentoPdfBytes(contrato);
    }

    @Transactional
    public Contrato registrarMedidasYPerimetro(Long contratoId, ContratoRequest req) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        // Calculamos el perímetro en el momento
        Double perimetro = (req.getMlFrente() != null ? req.getMlFrente() : 0.0) +
                (req.getMlDerecha() != null ? req.getMlDerecha() : 0.0) +
                (req.getMlIzquierda() != null ? req.getMlIzquierda() : 0.0) +
                (req.getMlFondo() != null ? req.getMlFondo() : 0.0);

        // Creamos o actualizamos la entidad de medidas
        ContratoMedidas medidas = (contrato.getMedidas() != null) ? contrato.getMedidas() : new ContratoMedidas();

        medidas.setContrato(contrato);
        medidas.setMlFrente(req.getMlFrente());
        medidas.setMlDerecha(req.getMlDerecha());
        medidas.setMlIzquierda(req.getMlIzquierda());
        medidas.setMlFondo(req.getMlFondo());
        medidas.setColindanciaFrente(req.getColindanciaFrente());
        medidas.setColindanciaDerecha(req.getColindanciaDerecha());
        medidas.setColindanciaIzquierda(req.getColindanciaIzquierda());
        medidas.setColindanciaFondo(req.getColindanciaFondo());
        medidas.setPerimetro(perimetro);

        contrato.setMedidas(medidas);

        // Al guardar el contrato, por CascadeType.ALL, se guarda la tabla de medidas
        return contratoRepository.save(contrato);
    }
}