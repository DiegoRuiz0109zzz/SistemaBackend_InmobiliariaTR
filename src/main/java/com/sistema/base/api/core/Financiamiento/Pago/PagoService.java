package com.sistema.base.api.core.Financiamiento.Pago;

import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoRepository;
import com.sistema.base.api.core.Financiamiento.Contrato.EstadoContrato;
import com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial.ContratoHistorialService;
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
    private final ContratoRepository contratoRepository;
    private final LoteRepository loteRepository;
    private final ContratoHistorialService contratoHistorialService;

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

        int diasRetraso = 0;
        boolean pagoADestiempo = false;
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
                .diasRetraso(diasRetraso)
                .pagoADestiempo(pagoADestiempo)
                .build();

        double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMontoAbonado();
        cuota.setMontoPagado(nuevoMontoPagado);

        if (nuevoMontoPagado >= cuota.getMontoTotal()) {
            cuota.setEstado(pagoADestiempo ? EstadoCuota.PAGADO_DESTIEMPO : EstadoCuota.PAGADO_TOTAL);

            if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() == 0) {
                Contrato contrato = cuota.getContrato();
                contrato.setEstadoContrato(EstadoContrato.ACTIVO);

                // ✅ RESET DOCUMENTAL: Se limpia para exigir la subida del contrato definitivo
                contrato.setUrlDocumentoFirmado(null);
                contratoRepository.save(contrato);

                Lote lote = contrato.getLote();
                lote.setEstadoVenta(EstadoLote.VENDIDO);
                loteRepository.save(lote);

                contratoHistorialService.registrarHito(
                        contrato,
                        "CONTRATO_ACTIVO",
                        "Inicial completada. Se requiere subir el Contrato de Compra-Venta definitivo firmado.",
                        "Pago validado (" + metodoPago + ")"
                );
            }
        } else {
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
            throw new RuntimeException("Este pago ya fue procesado.");
        }

        Cuota cuota = pago.getCuota();
        String fotoVoucherUrl = null;
        if (voucherFile != null && !voucherFile.isEmpty()) {
            Cliente cliente = cuota.getContrato().getCliente();
            String customFileName = generarNombreVoucher(cliente, voucherFile);
            String savedPath = fileStorageService.storeFileWithCustomName(voucherFile, "vouchers", customFileName);
            fotoVoucherUrl = "uploads/" + savedPath;
        }

        int diasRetraso = 0;
        boolean pagoADestiempo = false;
        LocalDate fechaDelPago = pago.getFechaPago();

        if (fechaDelPago.isAfter(cuota.getFechaVencimiento())) {
            pagoADestiempo = true;
            diasRetraso = (int) ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), fechaDelPago);
        }

        pago.setMetodoPago(metodoPago);
        pago.setNumeroOperacion(numeroOperacion);
        if (fotoVoucherUrl != null) pago.setFotoVoucherUrl(fotoVoucherUrl);
        pago.setEstado(EstadoPago.PROCESADO);
        pago.setDiasRetraso(diasRetraso);
        pago.setPagoADestiempo(pagoADestiempo);

        double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMontoAbonado();
        cuota.setMontoPagado(nuevoMontoPagado);

        if (nuevoMontoPagado >= cuota.getMontoTotal()) {
            cuota.setEstado(pagoADestiempo ? EstadoCuota.PAGADO_DESTIEMPO : EstadoCuota.PAGADO_TOTAL);

            if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() == 0) {
                Contrato contrato = cuota.getContrato();
                contrato.setEstadoContrato(EstadoContrato.ACTIVO);

                // ✅ RESET DOCUMENTAL: Se limpia para exigir la subida del contrato definitivo
                contrato.setUrlDocumentoFirmado(null);
                contratoRepository.save(contrato);

                Lote lote = contrato.getLote();
                lote.setEstadoVenta(EstadoLote.VENDIDO);
                loteRepository.save(lote);

                contratoHistorialService.registrarHito(
                        contrato,
                        "CONTRATO_ACTIVO",
                        "Inicial completada mediante proceso pendiente. Se requiere subir el Contrato oficial firmado.",
                        "Pago procesado en caja (" + metodoPago + ")"
                );
            }
        } else {
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
        cuota.setEstado(cuota.getMontoPagado() <= 0 ? EstadoCuota.PENDIENTE : EstadoCuota.PAGADO_PARCIAL);

        cuotaRepository.save(cuota);
        pago.setEnabled(false);
        pagoRepository.save(pago);
    }
}