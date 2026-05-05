package com.sistema.base.api.core.Financiamiento.Contrato;

import com.sistema.base.api.core.Empresa.Empresa;
import com.sistema.base.api.core.Empresa.EmpresaRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorial;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorialRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.BloqueCuotaDTO;
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
import java.util.Map;

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

    // ==========================================
    // 1. EL SIMULADOR (Modo Exclusivo por Tramos)
    // ==========================================
    public List<CuotaPreview> simularCronograma(SimulacionRequest request) {
        Double saldoFinanciar = request.getPrecioTotal() - request.getMontoInicial();
        if (saldoFinanciar <= 0) {
            throw new IllegalArgumentException("El saldo a financiar debe ser mayor a 0.");
        }

        // Si no manda bloques, inicializamos una lista vacía para auto-completar todo
        List<BloqueCuotaDTO> bloques = request.getBloquesFlexibles();
        if (bloques == null) {
            bloques = new ArrayList<>();
        }

        List<CuotaPreview> cronograma = new ArrayList<>();
        double totalProgramado = 0.0;
        int totalCuotasProgramadas = 0;

        // 1. Sumamos lo que el vendedor SÍ ingresó manualmente
        for (BloqueCuotaDTO bloque : bloques) {
            if (bloque.getCantidad() <= 0 || bloque.getMonto() <= 0) {
                throw new IllegalArgumentException("La cantidad y el monto de cada tramo deben ser mayores a 0.");
            }
            totalProgramado += (bloque.getCantidad() * bloque.getMonto());
            totalCuotasProgramadas += bloque.getCantidad();
        }

        totalProgramado = Math.round(totalProgramado * 100.0) / 100.0;
        double diferencia = Math.round((saldoFinanciar - totalProgramado) * 100.0) / 100.0;

        // 🔥 LA MAGIA: AUTO-COMPLETADO INTELIGENTE
        if (diferencia > 0) {
            int cuotasRestantes = (request.getCantidadCuotas() != null ? request.getCantidadCuotas() : 0) - totalCuotasProgramadas;

            if (cuotasRestantes > 0) {
                // Calculamos de cuánto debe ser la cuota para rellenar el hueco
                double montoAuto = Math.round((diferencia / cuotasRestantes) * 100.0) / 100.0;

                BloqueCuotaDTO bloqueAuto = new BloqueCuotaDTO();
                bloqueAuto.setCantidad(cuotasRestantes);
                bloqueAuto.setMonto(montoAuto);
                bloqueAuto.setTipo("MENSUAL"); // El relleno siempre será mensual normal

                // Hacemos la lista modificable y le agregamos el bloque automático
                bloques = new ArrayList<>(bloques);
                bloques.add(bloqueAuto);

                // Actualizamos el request para que si otra función lo usa, tenga el tramo completo
                request.setBloquesFlexibles(bloques);

            } else {
                throw new IllegalArgumentException("El monto programado no cubre la deuda y ya no quedan cuotas disponibles para auto-completar.");
            }
        } else if (diferencia < 0) {
            throw new IllegalArgumentException(String.format("Te has excedido por S/ %.2f. Revisa los tramos ingresados.", Math.abs(diferencia)));
        } else if (diferencia == 0 && request.getCantidadCuotas() != null && totalCuotasProgramadas < request.getCantidadCuotas()) {
            throw new IllegalArgumentException("La deuda está cubierta, pero sobran " + (request.getCantidadCuotas() - totalCuotasProgramadas) + " cuotas en el plazo solicitado.");
        }

        // 2. Generar el cronograma iterando bloque por bloque (incluyendo el auto-generado)
        LocalDate fechaInicial = request.getFechaInicioPago();
        int diaPreferido = fechaInicial.getDayOfMonth();
        YearMonth mesActual = YearMonth.from(fechaInicial);
        int numeroCuotaGlobal = 1;

        for (int b = 0; b < bloques.size(); b++) {
            BloqueCuotaDTO bloque = bloques.get(b);
            TipoCuota tipoAsignado = (bloque.getTipo() != null && bloque.getTipo().equalsIgnoreCase("ESPECIAL"))
                    ? TipoCuota.ESPECIAL : TipoCuota.MENSUAL;

            for (int i = 0; i < bloque.getCantidad(); i++) {
                double montoAsignado = bloque.getMonto();

                // 🔥 AJUSTE PERFECTO AL CÉNTIMO: En la última cuota del último bloque
                if (b == bloques.size() - 1 && i == bloque.getCantidad() - 1) {
                    double sumaAnterior = 0.0;
                    for(CuotaPreview cp : cronograma) sumaAnterior += cp.getMonto();
                    montoAsignado = Math.round((saldoFinanciar - sumaAnterior) * 100.0) / 100.0;
                }

                YearMonth mesCuota = mesActual.plusMonths(numeroCuotaGlobal - 1);
                int diaReal = Math.min(diaPreferido, mesCuota.lengthOfMonth());
                LocalDate fechaVencimiento = mesCuota.atDay(diaReal);

                cronograma.add(CuotaPreview.builder()
                        .numeroCuota(numeroCuotaGlobal)
                        .monto(montoAsignado)
                        .fechaVencimiento(fechaVencimiento)
                        .tipoCuota(tipoAsignado)
                        .build());

                numeroCuotaGlobal++;
            }
        }
        return cronograma;
    }

    // ==========================================
    // 2. EL PRE-CÁLCULO (Resumen Simplificado)
    // ==========================================
    public Map<String, Object> calcularResumenSimulacion(SimulacionRequest request) {
        Double saldoFinanciar = request.getPrecioTotal() - request.getMontoInicial();

        double totalProgramado = 0.0;
        int totalCuotasProgramadas = 0;

        if (request.getBloquesFlexibles() != null) {
            for (BloqueCuotaDTO bloque : request.getBloquesFlexibles()) {
                if (bloque.getCantidad() != null && bloque.getMonto() != null) {
                    totalProgramado += (bloque.getCantidad() * bloque.getMonto());
                    totalCuotasProgramadas += bloque.getCantidad();
                }
            }
        }

        totalProgramado = Math.round(totalProgramado * 100.0) / 100.0;
        double diferenciaReal = Math.round((saldoFinanciar - totalProgramado) * 100.0) / 100.0;

        // Variables clave
        boolean excede = diferenciaReal < 0;
        double diferenciaAbsoluta = Math.abs(diferenciaReal); // Siempre positivo para no confundir al frontend
        String recomendacion = "";

        // Lógica de recomendación
        if (diferenciaReal > 0) {
            if (request.getCantidadCuotas() != null && request.getCantidadCuotas() > totalCuotasProgramadas) {
                int cuotasRestantes = request.getCantidadCuotas() - totalCuotasProgramadas;
                double cuotaSugerida = Math.round((diferenciaReal / cuotasRestantes) * 100.0) / 100.0;
                recomendacion = String.format("Faltan S/ %.2f. 💡 Sugerencia: Agrega %d cuotas de S/ %.2f", diferenciaAbsoluta, cuotasRestantes, cuotaSugerida);
            } else {
                recomendacion = String.format("Faltan S/ %.2f. 💡 Sugerencia: Agrega 1 cuota final de S/ %.2f", diferenciaAbsoluta, diferenciaAbsoluta);
            }
        } else if (excede) {
            recomendacion = String.format("⚠️ Te has excedido por S/ %.2f. Reduce el monto o la cantidad de los tramos.", diferenciaAbsoluta);
        } else {
            recomendacion = "✅ ¡Perfecto! Los tramos cubren la deuda exacta.";
        }

        // Armamos el JSON final solo con lo que pediste
        Map<String, Object> resumen = new java.util.HashMap<>();
        resumen.put("diferencia", diferenciaAbsoluta);
        resumen.put("excede", excede);
        resumen.put("recomendacion", recomendacion);

        return resumen;
    }

    // ==========================================
    // 3. GENERAR EL CONTRATO REAL EN BASE DE DATOS
    // ==========================================
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

        // --- AUDITORÍA DE TRAMOS ---
        String textoTramos = "Estándar";
        int totalCuotasGeneradas = 0;

        if (req.getBloquesFlexibles() != null && !req.getBloquesFlexibles().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < req.getBloquesFlexibles().size(); i++) {
                var b = req.getBloquesFlexibles().get(i);
                sb.append(b.getCantidad()).append(" de S/").append(b.getMonto());
                if (i < req.getBloquesFlexibles().size() - 1) {
                    sb.append(" | ");
                }
                totalCuotasGeneradas += b.getCantidad();
            }
            textoTramos = sb.toString();
        } else {
            // SI EL FRONTEND NO MANDA TRAMOS (ES OPCIONAL), USAMOS LA CONFIGURACIÓN ESTÁNDAR
            textoTramos = "Cronograma regular de " + req.getCantidadCuotas() + " cuotas";
            totalCuotasGeneradas = (req.getCantidadCuotas() != null) ? req.getCantidadCuotas() : 0;
        }

        Double abonoInicialPrometido = (req.getAbonoInicialReal() != null) ? req.getAbonoInicialReal() : 0.0;
        if (abonoInicialPrometido >= req.getMontoInicialAcordado()) lote.setEstadoVenta(EstadoLote.VENDIDO);
        else lote.setEstadoVenta(EstadoLote.RESERVADO);
        loteRepository.save(lote);

        EstadoContrato estadoContratoReal = (abonoInicialPrometido >= req.getMontoInicialAcordado()) ? EstadoContrato.ACTIVO : EstadoContrato.SEPARADO;

        Contrato contrato = Contrato.builder()
                .lote(lote).cliente(cliente).coComprador(coComprador).vendedor(vendedor).cotizacionOrigen(cotizacion)
                .precioTotal(req.getPrecioTotal()).montoInicial(req.getMontoInicialAcordado())
                .montoAbonadoIncial(abonoInicialPrometido).saldoFinanciar(saldoFinanciar)
                .cantidadCuotas(totalCuotasGeneradas) // Tomamos el total sumado de los tramos
                .detalleTramos(textoTramos) // GUARDAMOS EL RESUMEN DEL CRONOGRAMA
                .descripcion("Cuota inicial de S/ " + req.getMontoInicialAcordado() + ". Fraccionado en tramos: " + textoTramos)
                .observacion(req.getObservacion()).tipoInicial(req.getTipoInicial())
                .cuotasFlexibles(req.getCuotasFlexibles() != null ? req.getCuotasFlexibles() : false)
                .fechaInicioCronograma(req.getFechaInicioPago())
                .estadoContrato(estadoContratoReal)
                .fechaContrato(null)
                .build();

        Contrato contratoGuardado = contratoRepository.save(contrato);

        // --- CREAR CUOTA INICIAL (CUOTA 0) ---
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

        // --- GENERAR CRONOGRAMA REUTILIZANDO EL SIMULADOR ---
        if (saldoFinanciar > 0) {
            SimulacionRequest sim = new SimulacionRequest();
            sim.setPrecioTotal(req.getPrecioTotal());
            sim.setMontoInicial(req.getMontoInicialAcordado());
            sim.setFechaInicioPago(req.getFechaInicioPago());
            sim.setBloquesFlexibles(req.getBloquesFlexibles());

            // 🔥 ESTA ES LA LÍNEA QUE NOS FALTABA AÑADIR 🔥
            sim.setCantidadCuotas(req.getCantidadCuotas());

            // ¡LLAMADA MAGISTRAL! Aprovechamos el método que ya hace los cálculos de fechas
            List<CuotaPreview> proyeccion = simularCronograma(sim);

            List<Cuota> cuotasAGuardar = new ArrayList<>();

            for (CuotaPreview cp : proyeccion) {
                cuotasAGuardar.add(Cuota.builder()
                        .contrato(contratoGuardado)
                        .numeroCuota(cp.getNumeroCuota())
                        .tipoCuota(cp.getTipoCuota())
                        .montoTotal(cp.getMonto())
                        .montoPagado(0.0)
                        .fechaVencimiento(cp.getFechaVencimiento())
                        .estado(EstadoCuota.PENDIENTE)
                        .build());
            }
            cuotaRepository.saveAll(cuotasAGuardar);
        }

        if (cotizacion != null) {
            cotizacion.setEstado(com.sistema.base.api.core.Financiamiento.Cotizacion.EstadoCotizacion.CONVERTIDA_A_CONTRATO);
            cotizacionRepository.save(cotizacion);
        }

        return contratoGuardado;
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