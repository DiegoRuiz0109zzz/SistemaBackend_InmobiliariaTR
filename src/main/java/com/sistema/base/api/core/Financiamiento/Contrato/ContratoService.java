package com.sistema.base.api.core.Financiamiento.Contrato;

import com.sistema.base.api.core.Empresa.Empresa;
import com.sistema.base.api.core.Empresa.EmpresaRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorialService;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoMedida.ContratoMedidas;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.*;
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
import com.sistema.base.api.service.FileStorageService;
import com.sistema.base.api.utils.NumeroALetrasConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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

    private final FileStorageService fileStorageService;

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

    public SimulacionResponseDTO simularCronograma(SimulacionRequest request) {
        SimulacionResponseDTO response = new SimulacionResponseDTO();
        List<CuotaPreview> cronograma = new ArrayList<>();

        Double montoInicial = request.getMontoInicial() != null ? request.getMontoInicial() : 0.0;
        Double saldoFinanciar = request.getPrecioTotal() - montoInicial;

        if (saldoFinanciar <= 0 || request.getCantidadCuotas() == null || request.getCantidadCuotas() <= 0) {
            response.setCronograma(cronograma);
            response.setMensajeSugerencia("Datos insuficientes para simular.");
            return response;
        }

        int cuotasTotales = request.getCantidadCuotas();
        LocalDate fechaInicial = request.getFechaInicioPago() != null ? request.getFechaInicioPago() : LocalDate.now();

        // 1. Cargamos los bloques definidos por el usuario
        List<BloqueCuotaDTO> bloques = request.getBloquesFlexibles() != null ? request.getBloquesFlexibles() : new ArrayList<>();

        // Mantenemos retrocompatibilidad con las cuotas especiales antiguas si no mandan bloques
        if (bloques.isEmpty() && request.getCuotasEspeciales() != null && request.getCuotasEspeciales() > 0) {
            BloqueCuotaDTO b = new BloqueCuotaDTO();
            b.setCantidad(request.getCuotasEspeciales());
            b.setMonto(request.getMontoCuotaEspecial() != null ? request.getMontoCuotaEspecial() : 0.0);
            bloques.add(b);
        }

        int cuotaActual = 0;
        double saldoRestante = saldoFinanciar;

        // 2. Procesamos todos los bloques personalizados
        for (BloqueCuotaDTO bloque : bloques) {
            int cantidad = (bloque.getCantidad() != null) ? bloque.getCantidad() : 0;
            double monto = (bloque.getMonto() != null) ? bloque.getMonto() : 0.0;

            for (int i = 0; i < cantidad; i++) {
                if (cuotaActual >= cuotasTotales) break;
                LocalDate fechaVencimiento = calcularSiguienteFechaVencimiento(fechaInicial, cuotaActual);
                cronograma.add(CuotaPreview.builder()
                        .numeroCuota(cuotaActual + 1)
                        .monto(monto)
                        .fechaVencimiento(fechaVencimiento)
                        .build());
                saldoRestante -= monto;
                cuotaActual++;
            }
        }

        int cuotasRestantes = cuotasTotales - cuotaActual;
        String mensajeSugerencia = "Cronograma generado correctamente.";

        // 3. Calculamos las cuotas restantes asegurando CERO DECIMALES
        if (cuotasRestantes > 0) {
            // Usamos Math.floor para asegurar que la cuota regular no tenga decimales
            Double cuotaRegular = Math.floor(saldoRestante / cuotasRestantes);
            // La cuota final absorbe la diferencia exacta
            Double cuotaFinal = Math.round((saldoRestante - (cuotaRegular * (cuotasRestantes - 1))) * 100.0) / 100.0;

            for (int i = 0; i < cuotasRestantes; i++) {
                Double montoAsignado = (i == cuotasRestantes - 1) ? cuotaFinal : cuotaRegular;
                LocalDate fechaVencimiento = calcularSiguienteFechaVencimiento(fechaInicial, cuotaActual);
                cronograma.add(CuotaPreview.builder()
                        .numeroCuota(cuotaActual + 1)
                        .monto(montoAsignado)
                        .fechaVencimiento(fechaVencimiento)
                        .build());
                cuotaActual++;
            }

            // =========================================================
            // 4. LÓGICA CORREGIDA DE "ASESOR FINANCIERO"
            // =========================================================
            if (cuotaRegular.equals(cuotaFinal)) {
                mensajeSugerencia = "¡Perfecto! El saldo restante se dividió en " + cuotasRestantes + " cuotas exactas de S/ " + cuotaRegular + ".";
            } else {
                Double cuotaExactaArriba = cuotaRegular + 1;

                // Si el cliente SUMA esto a la inicial, el saldo a financiar BAJA y encaja con la cuota menor
                Double montoASumarAInicial = Math.round((saldoRestante - (cuotaRegular * cuotasRestantes)) * 100.0) / 100.0;

                // Si el cliente RESTA esto a la inicial, el saldo a financiar SUBE y encaja con la cuota mayor
                Double montoARestarAInicial = Math.round(((cuotaExactaArriba * cuotasRestantes) - saldoRestante) * 100.0) / 100.0;

                mensajeSugerencia = String.format("Saldo restante dividido en %d cuotas de S/ %.0f y 1 final de S/ %.2f. " +
                                "💡 SUGERENCIA PARA CUOTAS EXACTAS: Puede SUMAR S/ %.2f a la inicial (quedarán %d cuotas de S/ %.0f) " +
                                "o RESTAR S/ %.2f a la inicial (quedarán %d cuotas de S/ %.0f).",
                        (cuotasRestantes - 1), cuotaRegular, cuotaFinal,
                        montoASumarAInicial, cuotasRestantes, cuotaRegular,
                        montoARestarAInicial, cuotasRestantes, cuotaExactaArriba);
            }
        } else if (cuotasRestantes == 0 && Math.abs(saldoRestante) > 0.01) {
            CuotaPreview ultimaCuota = cronograma.get(cronograma.size() - 1);
            ultimaCuota.setMonto(Math.round((ultimaCuota.getMonto() + saldoRestante) * 100.0) / 100.0);
            mensajeSugerencia = "Los bloques cubrieron el total de meses. Se ajustó la última cuota para cuadrar el saldo a 0.";
        }

        response.setCronograma(cronograma);
        response.setMensajeSugerencia(mensajeSugerencia);
        return response;
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

        // Generamos la descripción dinámica según haya bloques o cuotas fijas
        if (req.getBloquesFlexibles() != null && !req.getBloquesFlexibles().isEmpty()) {
            descBuilder.append("Fraccionado con cronograma flexible definido por el usuario.");
        } else if (req.getCuotasEspeciales() != null && req.getCuotasEspeciales() > 0) {
            descBuilder.append("Fraccionado en ").append(req.getCuotasEspeciales()).append(" cuotas de S/ ").append(req.getMontoCuotaEspecial())
                    .append(" y ").append(cantidadCuotasFijas - req.getCuotasEspeciales()).append(" cuotas con el saldo restante.");
        } else if (cantidadCuotasFijas > 0) {
            descBuilder.append("Fraccionado en ").append(cantidadCuotasFijas).append(" cuotas regulares.");
        } else {
            descBuilder.append("Cronograma pendiente de definición.");
        }

        Double abonoInicialPrometido = (req.getAbonoInicialReal() != null) ? req.getAbonoInicialReal() : 0.0;

        // ✅ MODIFICACIÓN 1: Forzamos a que todo nuevo registro nazca estrictamente como SEPARADO y el lote como RESERVADO.
        EstadoContrato estadoContratoReal = EstadoContrato.SEPARADO;
        EstadoLote estadoLoteReal = EstadoLote.RESERVADO;

        lote.setEstadoVenta(estadoLoteReal);
        loteRepository.save(lote);

        Contrato contrato = Contrato.builder()
                .lote(lote).cliente(cliente).coComprador(coComprador).vendedor(vendedor).cotizacionOrigen(cotizacion)
                .precioTotal(req.getPrecioTotal()).montoInicial(req.getMontoInicialAcordado())
                .montoAbonadoIncial(abonoInicialPrometido).saldoFinanciar(saldoFinanciar)
                .cantidadCuotas(cantidadCuotasFijas) // ✅ MODIFICACIÓN 2: Usamos la variable segura (0 en lugar de null)
                .descripcion(descBuilder.toString()).tipoInicial(req.getTipoInicial())
                .cuotasFlexibles(req.getCuotasFlexibles() != null ? req.getCuotasFlexibles() : false)
                .fechaInicioCronograma(req.getFechaInicioPago())
                .estadoContrato(estadoContratoReal)
                .fechaContrato(LocalDate.now())
                .urlDocumentoFirmado(null)
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

        // NOTA: Como estadoContratoReal es siempre SEPARADO, este bloque ya no se ejecutará al crear el contrato.
        // El cronograma se generará de manera segura recién cuando se llame a actualizarContrato (y pase a ACTIVO).
        if (estadoContratoReal == EstadoContrato.ACTIVO && saldoFinanciar > 0 && cantidadCuotasFijas > 0) {
            SimulacionRequest sim = new SimulacionRequest();
            sim.setPrecioTotal(req.getPrecioTotal());
            sim.setMontoInicial(req.getMontoInicialAcordado());
            sim.setCantidadCuotas(req.getCantidadCuotas());
            sim.setFechaInicioPago(req.getFechaInicioPago());
            sim.setCuotasEspeciales(req.getCuotasEspeciales());
            sim.setMontoCuotaEspecial(req.getMontoCuotaEspecial());
            sim.setBloquesFlexibles(req.getBloquesFlexibles());

            List<CuotaPreview> proyeccion = simularCronograma(sim).getCronograma();
            List<Cuota> cuotasAGuardar = new ArrayList<>();

            int cantidadEspeciales = 0;
            if (req.getBloquesFlexibles() != null && !req.getBloquesFlexibles().isEmpty()) {
                cantidadEspeciales = req.getBloquesFlexibles().stream().mapToInt(BloqueCuotaDTO::getCantidad).sum();
            } else if (req.getCuotasEspeciales() != null) {
                cantidadEspeciales = req.getCuotasEspeciales();
            }

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
        contratoHistorialService.registrarHito(contratoGuardado, tipoHito, descripcionHito, req.getObservacion(), null);

        return contratoGuardado;
    }

    @Transactional
    public Contrato subirDocumentoFirmado(Long contratoId, MultipartFile archivo, String motivo) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        String customName = "CONTRATO_FIRMADO_" + contrato.getEstadoContrato() + "_" + contratoId + "_" + System.currentTimeMillis();
        String savedPath = fileStorageService.storeFileWithCustomName(archivo, "contratos-firmados", customName);

        // La ruta real física que se guarda internamente en la entidad
        String rutaCompleta = "uploads/" + savedPath;
        contrato.setUrlDocumentoFirmado(rutaCompleta);
        Contrato actualizado = contratoRepository.save(contrato);

        String tipoHito = "DOCUMENTO_CARGADO";
        String descripcion = "Se subió el archivo firmado correspondiente al estado: " + contrato.getEstadoContrato();

        // ✅ CORRECCIÓN: Volvemos a mandar la ruta física real (Ej: "uploads/contratos-firmados/...")
        // a la bitácora, ya que tu frontend lee los archivos subidos directamente desde ahí.
        contratoHistorialService.registrarHito(actualizado, tipoHito, descripcion, motivo, rutaCompleta);

        return actualizado;
    }

    @Transactional(readOnly = true)
    public byte[] obtenerDocumentoFirmadoPdf(Long contratoId) {
        Contrato contrato = obtenerPorId(contratoId);

        if (contrato.getUrlDocumentoFirmado() == null || contrato.getUrlDocumentoFirmado().isEmpty()) {
            throw new RuntimeException("Este contrato aún no tiene un documento firmado subido.");
        }

        try {
            Path filePath = Paths.get(contrato.getUrlDocumentoFirmado());
            if (!Files.exists(filePath)) {
                throw new RuntimeException("El archivo físico no se encuentra en el servidor.");
            }
            return Files.readAllBytes(filePath);
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el archivo firmado: " + e.getMessage());
        }
    }

    @Transactional
    public Contrato generarNuevoDocumentoContrato(Long contratoId, String observacion) {
        Contrato contrato = contratoRepository.findById(contratoId).orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
        contrato.setFechaContrato(LocalDate.now());
        Contrato contratoGuardado = contratoRepository.save(contrato);
        contratoHistorialService.registrarHito(contratoGuardado, contrato.getEstadoContrato().name(), "Se generó una nueva versión del documento PDF.", observacion);
        return contratoGuardado;
    }

    @Transactional(readOnly = true)
    public String obtenerAlertaSeparacionVencida(Long contratoId) {
        Contrato contrato = obtenerPorId(contratoId);

        // 1. Verificamos que esté SEPARADO y que la fecha de inicio de cronograma sea null
        if (contrato.getEstadoContrato() != EstadoContrato.SEPARADO || contrato.getFechaInicioCronograma() != null) {
            return null;
        }

        // 2. Obtenemos la Cuota 0 (que guarda la fecha límite de vigencia de la separación)
        Cuota cuota0 = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                .stream().filter(c -> c.getNumeroCuota() == 0).findFirst().orElse(null);

        // 3. Verificamos estrictamente si la fecha ya pasó
        if (cuota0 != null && cuota0.getFechaVencimiento() != null) {
            LocalDate hoy = LocalDate.now();
            if (hoy.isAfter(cuota0.getFechaVencimiento())) {
                long diasAtraso = ChronoUnit.DAYS.between(cuota0.getFechaVencimiento(), hoy);
                return "La fecha límite de separación venció hace " + diasAtraso + " días (" + cuota0.getFechaVencimiento() + ").";
            }
        }

        return null;
    }

    // =========================================================================================
    // ✅ ACTUALIZACIÓN DE CONTRATO (CON LÓGICA DE BLANQUEO DE FIRMA Y NUEVO HITO "CONTRATO_ACTIVO")
    // =========================================================================================
    @Transactional
    public Contrato actualizarContrato(Long id, ContratoRequest request) {
        Contrato contrato = contratoRepository.findById(id).orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
        StringBuilder cambiosGenerales = new StringBuilder();
        boolean huboCambios = false;

        boolean seActivoContrato = false;
        boolean seRegeneroCronograma = false;

        if (request.getClienteId() != null && !Objects.equals(contrato.getCliente().getId(), request.getClienteId())) {
            Cliente titularAnterior = contrato.getCliente();
            Long anteriorClienteId = titularAnterior.getId();
            String nombreAnterior = titularAnterior.getNombres() + " " + (titularAnterior.getApellidos() != null ? titularAnterior.getApellidos() : "");

            Cliente nuevoTitular = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new RuntimeException("Cliente nuevo no encontrado"));
            String nombreNuevo = nuevoTitular.getNombres() + " " + (nuevoTitular.getApellidos() != null ? nuevoTitular.getApellidos() : "");

            contrato.setCliente(nuevoTitular);
            huboCambios = true;

            String urlVistaPreviaActa = "/api/contratos/" + id + "/acta-traspaso-titular?anteriorClienteId=" + anteriorClienteId + "&nuevoClienteId=" + request.getClienteId();
            String descripcionActa = "Se generó y guardó el Acta de Solicitud de Traspaso. Cambio de titular de " + nombreAnterior + " a favor de " + nombreNuevo + ".";

            contratoHistorialService.registrarHito(contrato, "ACTA_GENERADA", descripcionActa, "Trámites", urlVistaPreviaActa);
        }

        if (request.getLoteId() != null && !Objects.equals(contrato.getLote().getId(), request.getLoteId())) {
            Lote loteOrigen = contrato.getLote();
            Lote loteDestino = loteRepository.findById(request.getLoteId())
                    .orElseThrow(() -> new RuntimeException("Lote de destino no encontrado."));

            Long loteOrigenId = loteOrigen.getId();

            loteOrigen.setEstadoVenta(EstadoLote.DISPONIBLE);
            loteRepository.save(loteOrigen);

            EstadoLote nuevoEstadoLote = (contrato.getEstadoContrato() == EstadoContrato.SEPARADO)
                    ? EstadoLote.RESERVADO
                    : EstadoLote.VENDIDO;

            loteDestino.setEstadoVenta(nuevoEstadoLote);
            loteRepository.save(loteDestino);

            contrato.setLote(loteDestino);
            huboCambios = true;

            String urlVistaPreviaActa = "/api/contratos/" + id + "/acta-cambio-lote?loteOrigenId=" + loteOrigenId + "&loteDestinoId=" + request.getLoteId();
            String descripcionActa = "Se generó y guardó el Acta de Cambio de Lote. Origen: Mz " + loteOrigen.getManzana().getNombre() + " Lt " + loteOrigen.getNumero() + " -> Destino: Mz " + loteDestino.getManzana().getNombre() + " Lt " + loteDestino.getNumero();

            contratoHistorialService.registrarHito(contrato, "ACTA_GENERADA", descripcionActa, "Trámites", urlVistaPreviaActa);
        }

        Cuota cuota0 = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId()).stream()
                .filter(c -> c.getNumeroCuota() == 0).findFirst().orElse(null);

        if (request.getFechaLimiteInicial() != null && cuota0 != null) {
            if (!Objects.equals(cuota0.getFechaVencimiento(), request.getFechaLimiteInicial())) {
                cuota0.setFechaVencimiento(request.getFechaLimiteInicial());
                cuotaRepository.save(cuota0);
                cambiosGenerales.append("Fecha límite inicial extendida a ").append(request.getFechaLimiteInicial()).append(". ");
                huboCambios = true;
            }
        }

        if (request.getMontoInicialAcordado() != null && request.getMontoInicialAcordado() > 0) {
            Double antiguaInicial = contrato.getMontoInicial();
            Double montoAbonadoPrevio = (cuota0 != null && cuota0.getMontoPagado() != null) ? cuota0.getMontoPagado() : 0.0;
            Double nuevaInicialCalculada = montoAbonadoPrevio + request.getMontoInicialAcordado();

            if (!Objects.equals(antiguaInicial, nuevaInicialCalculada)) {
                contrato.setMontoInicial(nuevaInicialCalculada);
                contrato.setSaldoFinanciar(contrato.getPrecioTotal() - nuevaInicialCalculada);

                if (cuota0 != null) {
                    cuota0.setMontoTotal(nuevaInicialCalculada);
                    cuota0.setEstado(cuota0.getMontoPagado() > 0 ? EstadoCuota.PAGADO_PARCIAL : EstadoCuota.PENDIENTE);
                    cuotaRepository.save(cuota0);
                }

                cambiosGenerales.append("Inicial reestructurada de S/ ").append(antiguaInicial)
                        .append(" a S/ ").append(nuevaInicialCalculada)
                        .append(" (Se agregaron S/ ").append(request.getMontoInicialAcordado()).append("). ");
                huboCambios = true;
            }
        }

        if (request.getCantidadCuotas() != null && request.getCantidadCuotas() > 0) {
            List<Cuota> cuotasAntiguas = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contrato.getId())
                    .stream().filter(c -> c.getNumeroCuota() > 0).collect(Collectors.toList());
            if (!cuotasAntiguas.isEmpty()) {
                cuotaRepository.deleteAll(cuotasAntiguas);
            }

            contrato.setCantidadCuotas(request.getCantidadCuotas());
            contrato.setFechaInicioCronograma(request.getFechaInicioPago());

            SimulacionRequest sim = new SimulacionRequest();
            sim.setPrecioTotal(contrato.getPrecioTotal());
            sim.setMontoInicial(contrato.getMontoInicial());
            sim.setCantidadCuotas(request.getCantidadCuotas());
            sim.setFechaInicioPago(request.getFechaInicioPago());
            sim.setCuotasEspeciales(request.getCuotasEspeciales());
            sim.setMontoCuotaEspecial(request.getMontoCuotaEspecial());
            sim.setBloquesFlexibles(request.getBloquesFlexibles()); // ✅ Inyectamos bloques flexibles

            List<CuotaPreview> proyeccion = simularCronograma(sim).getCronograma();
            List<Cuota> cuotasAGuardar = new ArrayList<>();

            int cantidadEspeciales = 0;
            if (request.getBloquesFlexibles() != null && !request.getBloquesFlexibles().isEmpty()) {
                cantidadEspeciales = request.getBloquesFlexibles().stream().mapToInt(BloqueCuotaDTO::getCantidad).sum();
            } else if (request.getCuotasEspeciales() != null) {
                cantidadEspeciales = request.getCuotasEspeciales();
            }

            for (CuotaPreview cp : proyeccion) {
                TipoCuota tipo = (cp.getNumeroCuota() <= cantidadEspeciales) ? TipoCuota.ESPECIAL : TipoCuota.MENSUAL;
                cuotasAGuardar.add(Cuota.builder().contrato(contrato).numeroCuota(cp.getNumeroCuota())
                        .tipoCuota(tipo).montoTotal(cp.getMonto()).montoPagado(0.0)
                        .fechaVencimiento(cp.getFechaVencimiento()).estado(EstadoCuota.PENDIENTE).build());
            }
            cuotaRepository.saveAll(cuotasAGuardar);

            contrato.setUrlDocumentoFirmado(null);
            contrato.setFechaContrato(LocalDate.now());

            if (contrato.getEstadoContrato() != EstadoContrato.ACTIVO) {
                contrato.setEstadoContrato(EstadoContrato.ACTIVO);

                Lote lote = contrato.getLote();
                lote.setEstadoVenta(EstadoLote.VENDIDO);
                loteRepository.save(lote);

                seActivoContrato = true;
            } else {
                seRegeneroCronograma = true;
            }
            huboCambios = true;
        }

        if (huboCambios) {
            Contrato actualizado = contratoRepository.save(contrato);

            if (seActivoContrato) {
                contratoHistorialService.registrarHito(actualizado, "CONTRATO_ACTIVO", "Contrato Activo registrado. Pendiente de documento firmado.", request.getObservacion(), null);
            } else if (seRegeneroCronograma) {
                contratoHistorialService.registrarHito(actualizado, "CONTRATO_ACTIVO", "Cronograma regenerado. Pendiente de nuevo documento firmado.", request.getObservacion(), null);
            }

            if (cambiosGenerales.length() > 0) {
                contratoHistorialService.registrarHito(actualizado, "MODIFICACION", cambiosGenerales.toString().trim(), request.getObservacion(), null);
            }

            return actualizado;
        }
        return contrato;
    }

    @Transactional
    public Contrato liberarLote(Long contratoId, String observacion) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        if (contrato.getEstadoContrato() == EstadoContrato.LIBERADO) {
            throw new RuntimeException("Este contrato ya se encuentra liberado.");
        }

        // 1. Liberar el lote dejándolo disponible para una nueva venta
        Lote lote = contrato.getLote();
        lote.setEstadoVenta(EstadoLote.DISPONIBLE);
        loteRepository.save(lote);

        // 2. Cambiar el estado del contrato
        contrato.setEstadoContrato(EstadoContrato.LIBERADO);
        Contrato contratoActualizado = contratoRepository.save(contrato);

        // 3. Registrar el hito en el historial del contrato
        String motivo = (observacion != null && !observacion.trim().isEmpty()) ? observacion : "Liberación de lote por falta de pago o anulación.";
        contratoHistorialService.registrarHito(contratoActualizado, "LOTE_LIBERADO", "El lote regresó a estado DISPONIBLE y el contrato fue cerrado.", motivo, null);

        return contratoActualizado;
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

        long diasValidez = 0;
        if (fechaCuotaCero != null) {
            diasValidez = ChronoUnit.DAYS.between(LocalDate.now(), fechaCuotaCero);
        }

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
        String precioTotalLetras = NumeroALetrasConverter.convertir(contrato.getPrecioTotal(), "SOLES");
        context.setVariable("precioTotalLetras", precioTotalLetras);

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

        Double perimetro = (req.getMlFrente() != null ? req.getMlFrente() : 0.0) +
                (req.getMlDerecha() != null ? req.getMlDerecha() : 0.0) +
                (req.getMlIzquierda() != null ? req.getMlIzquierda() : 0.0) +
                (req.getMlFondo() != null ? req.getMlFondo() : 0.0);

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

        return contratoRepository.save(contrato);
    }

    @Transactional(readOnly = true)
    public byte[] generarActaTraspasoTitularPdf(Long contratoId, Long anteriorClienteId, Long nuevoClienteId) {
        Contrato contrato = obtenerPorId(contratoId);
        Cliente titularActual = clienteRepository.findById(anteriorClienteId)
                .orElseThrow(() -> new RuntimeException("El titular original no existe en la base de datos."));
        Cliente nuevoTitular = clienteRepository.findById(nuevoClienteId)
                .orElseThrow(() -> new RuntimeException("El nuevo cliente no existe en la base de datos."));

        List<Cuota> cuotas = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contratoId);
        double montoAportado = cuotas.stream().mapToDouble(Cuota::getMontoPagado).sum();

        LocalDate hoy = LocalDate.now();
        String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
        String fechaFormateada = "Olmos, " + hoy.getDayOfMonth() + " de " + meses[hoy.getMonthValue() - 1] + " del " + hoy.getYear();

        String areaTotal = "110.26";
        try {
            if (contrato.getMedidas() != null && contrato.getMedidas().getMlFrente() != null && contrato.getMedidas().getMlDerecha() != null) {
                double calculo = contrato.getMedidas().getMlFrente() * contrato.getMedidas().getMlDerecha();
                areaTotal = String.format(java.util.Locale.US, "%.2f", calculo);
            }
        } catch(Exception ignored) {}

        Context context = new Context();
        context.setVariable("contrato", contrato);
        context.setVariable("titularActual", titularActual);
        context.setVariable("nuevoTitular", nuevoTitular);
        context.setVariable("montoAportado", montoAportado);
        context.setVariable("montoAportadoLetras", NumeroALetrasConverter.convertir(montoAportado, ""));
        context.setVariable("fechaEmision", fechaFormateada);
        context.setVariable("areaTotal", areaTotal);

        String htmlContenido = templateEngine.process("acta-traspaso", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContenido, null);
            builder.toStream(outputStream);
            builder.run();
            // Retornamos el array de bytes directamente sin pasar por el disco duro
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al compilar Acta de Traspaso PDF en memoria: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarActaCambioLotePdf(Long contratoId, Long loteOrigenId, Long loteDestinoId) {
        Contrato contrato = obtenerPorId(contratoId);
        Cliente cliente = contrato.getCliente();

        Lote loteOrigen = loteRepository.findById(loteOrigenId)
                .orElseThrow(() -> new RuntimeException("El lote de origen no existe."));
        Lote loteDestino = loteRepository.findById(loteDestinoId)
                .orElseThrow(() -> new RuntimeException("El lote de destino no existe."));

        List<Cuota> cuotas = cuotaRepository.findByContratoIdAndEnabledTrueOrderByNumeroCuotaAsc(contratoId);
        double montoAportado = cuotas.stream().mapToDouble(Cuota::getMontoPagado).sum();

        LocalDate hoy = LocalDate.now();
        String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
        String fechaFormateada = "Olmos, " + hoy.getDayOfMonth() + " de " + meses[hoy.getMonthValue() - 1] + " del " + hoy.getYear();

        Context context = new Context();
        context.setVariable("cliente", cliente);
        context.setVariable("loteOrigen", loteOrigen);
        context.setVariable("loteDestino", loteDestino);
        context.setVariable("montoAportado", montoAportado);
        context.setVariable("montoAportadoLetras", NumeroALetrasConverter.convertir(montoAportado, ""));
        context.setVariable("fechaEmision", fechaFormateada);

        String htmlContenido = templateEngine.process("acta-cambio-lote", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContenido, null);
            builder.toStream(outputStream);
            builder.run();
            // Retornamos el array de bytes directamente sin pasar por el disco duro
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al compilar Acta de Cambio de Lote PDF en memoria: " + e.getMessage());
        }
    }
}