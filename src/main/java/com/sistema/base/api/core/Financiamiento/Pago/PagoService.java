package com.sistema.base.api.core.Financiamiento.Pago;

import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.EstadoContrato;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;

import com.sistema.base.api.core.Lotizacion.Lote.EstadoLote;
import com.sistema.base.api.core.Lotizacion.Lote.Lote;
import com.sistema.base.api.core.Lotizacion.Lote.LoteRepository;
import com.sistema.base.api.core.Usuario.Clientes.Cliente;
import com.sistema.base.api.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;
    private final FileStorageService fileStorageService;

    // ✅ Inyectamos los repositorios necesarios para actualizar Contrato y Lote
    private final ContratoRepository contratoRepository;
    private final LoteRepository loteRepository;

    @Transactional(readOnly = true)
    public List<Pago> listarPorCuota(Long cuotaId) {
        return pagoRepository.findByCuotaIdAndEnabledTrue(cuotaId);
    }

    private String generarNombreVoucher(Cliente cliente, MultipartFile file) {
        String dni = cliente.getNumeroDocumento();
        String primerNombre = cliente.getNombres().trim().split("\\s+")[0].toUpperCase();
        String primerApellido = cliente.getApellidos().trim().split("\\s+")[0].toUpperCase();

        String originalFilename = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
        originalFilename = originalFilename.replaceAll("[\\s+]", "_");

        return dni + "_" + primerNombre + "_" + primerApellido + "_" + originalFilename;
    }

    @Transactional
    public Pago registrarPago(Long cuotaId, Double montoAbonado, String metodoPago, String numeroOperacion, MultipartFile voucherFile) {
        Cuota cuota = cuotaRepository.findById(cuotaId)
                .orElseThrow(() -> new RuntimeException("La cuota no existe."));

        double saldoPendiente = cuota.getMontoTotal() - cuota.getMontoPagado();
        if (montoAbonado > saldoPendiente) {
            throw new RuntimeException("El monto a pagar supera el saldo pendiente.");
        }

        String fotoVoucherUrl = null;

        if (voucherFile != null && !voucherFile.isEmpty()) {
            Cliente cliente = cuota.getContrato().getCliente();
            String customFileName = generarNombreVoucher(cliente, voucherFile);

            String savedPath = fileStorageService.storeFileWithCustomName(voucherFile, "vouchers", customFileName);
            fotoVoucherUrl = "uploads/" + savedPath;
        }

        // ✅ NUEVA LÓGICA: Calcular pago a destiempo y días de retraso
        int diasRetraso = 0;
        boolean pagoADestiempo = false;

        // Asumimos que el pago se está registrando HOY
        LocalDate fechaPagoActual = LocalDate.now();

        if (fechaPagoActual.isAfter(cuota.getFechaVencimiento())) {
            pagoADestiempo = true;
            diasRetraso = (int) ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), fechaPagoActual);
        }

        Pago pago = Pago.builder()
                .cuota(cuota)
                .montoAbonado(montoAbonado)
                .metodoPago(metodoPago)
                .numeroOperacion(numeroOperacion)
                .fotoVoucherUrl(fotoVoucherUrl)
                .estado(EstadoPago.PROCESADO)
                // Usamos los campos que agregaste en la Entidad Pago
                .diasRetraso(diasRetraso)
                .pagoADestiempo(pagoADestiempo)
                .build();

        double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMontoAbonado();
        cuota.setMontoPagado(nuevoMontoPagado);

        // ✅ Verificamos si la cuota fue pagada en su totalidad
        if (nuevoMontoPagado >= cuota.getMontoTotal()) {

            // 🔥 LÓGICA ACTUALIZADA: Decidir si se pagó a tiempo o a destiempo
            if (pagoADestiempo) {
                cuota.setEstado(EstadoCuota.PAGADO_DESTIEMPO);
            } else {
                cuota.setEstado(EstadoCuota.PAGADO_TOTAL);
            }

            // ✅ LÓGICA DE CUOTA INICIAL (Se mantiene igual)
            if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() == 0) {
                Contrato contrato = cuota.getContrato();
                contrato.setEstadoContrato(EstadoContrato.ACTIVO);
                contratoRepository.save(contrato);

                Lote lote = contrato.getLote();
                lote.setEstadoVenta(EstadoLote.VENDIDO);
                loteRepository.save(lote);
            }

        } else {
            // Si no pagó el total, se queda como parcial
            cuota.setEstado(EstadoCuota.PAGADO_PARCIAL);
        }

        cuotaRepository.save(cuota);
        return pagoRepository.save(pago);
    }

    @Transactional
    public Pago procesarPagoPendiente(Long pagoId, String metodoPago, String numeroOperacion, MultipartFile voucherFile) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado."));

        if (pago.getEstado() == EstadoPago.PROCESADO) {
            throw new RuntimeException("Este pago ya fue procesado y validado.");
        }

        Cuota cuota = pago.getCuota();
        String fotoVoucherUrl = null;

        if (voucherFile != null && !voucherFile.isEmpty()) {
            Cliente cliente = cuota.getContrato().getCliente();
            String customFileName = generarNombreVoucher(cliente, voucherFile);

            String savedPath = fileStorageService.storeFileWithCustomName(voucherFile, "vouchers", customFileName);
            fotoVoucherUrl = "uploads/" + savedPath;
        }

        // ✅ NUEVA LÓGICA: Calcular pago a destiempo y días de retraso
        int diasRetraso = 0;
        boolean pagoADestiempo = false;

        // Usamos la fecha en que se registró el pago original
        LocalDate fechaDelPago = pago.getFechaPago().toLocalDate();

        if (fechaDelPago.isAfter(cuota.getFechaVencimiento())) {
            pagoADestiempo = true;
            diasRetraso = (int) ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), fechaDelPago);
        }

        pago.setMetodoPago(metodoPago);
        pago.setNumeroOperacion(numeroOperacion);
        if (fotoVoucherUrl != null) {
            pago.setFotoVoucherUrl(fotoVoucherUrl);
        }
        pago.setEstado(EstadoPago.PROCESADO);
        pago.setDiasRetraso(diasRetraso); // Setear los días
        pago.setPagoADestiempo(pagoADestiempo); // Setear el flag

        double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMontoAbonado();
        cuota.setMontoPagado(nuevoMontoPagado);

        // ✅ Verificamos si la cuota fue pagada en su totalidad
        if (nuevoMontoPagado >= cuota.getMontoTotal()) {

            // 🔥 LÓGICA ACTUALIZADA: Decidir si se pagó a tiempo o a destiempo
            if (pagoADestiempo) {
                cuota.setEstado(EstadoCuota.PAGADO_DESTIEMPO);
            } else {
                cuota.setEstado(EstadoCuota.PAGADO_TOTAL);
            }

            // ✅ LÓGICA DE CUOTA INICIAL (Se mantiene igual)
            if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() == 0) {
                Contrato contrato = cuota.getContrato();
                contrato.setEstadoContrato(EstadoContrato.ACTIVO);
                contratoRepository.save(contrato);

                Lote lote = contrato.getLote();
                lote.setEstadoVenta(EstadoLote.VENDIDO);
                loteRepository.save(lote);
            }

        } else {
            // Si no pagó el total, se queda como parcial
            cuota.setEstado(EstadoCuota.PAGADO_PARCIAL);
        }

        cuotaRepository.save(cuota);
        return pagoRepository.save(pago);
    }

    @Transactional
    public void anularPago(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        Cuota cuota = pago.getCuota();
        cuota.setMontoPagado(cuota.getMontoPagado() - pago.getMontoAbonado());

        if (cuota.getMontoPagado() <= 0) {
            cuota.setEstado(EstadoCuota.PENDIENTE);
        } else {
            cuota.setEstado(EstadoCuota.PAGADO_PARCIAL);
        }
        cuotaRepository.save(cuota);

        pago.setEnabled(false);
        pagoRepository.save(pago);
    }
}