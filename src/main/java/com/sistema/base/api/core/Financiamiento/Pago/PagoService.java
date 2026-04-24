package com.sistema.base.api.core.Financiamiento.Pago;

import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;
import com.sistema.base.api.core.Usuario.Clientes.Cliente;
import com.sistema.base.api.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<Pago> listarPorCuota(Long cuotaId) {
        return pagoRepository.findByCuotaIdAndEnabledTrue(cuotaId);
    }

    // ✅ MÉTODO CORREGIDO: Sanitiza el nombre y usa guiones bajos
    private String generarNombreVoucher(Cliente cliente, MultipartFile file) {
        String dni = cliente.getNumeroDocumento();
        String primerNombre = cliente.getNombres().trim().split("\\s+")[0].toUpperCase();
        String primerApellido = cliente.getApellidos().trim().split("\\s+")[0].toUpperCase();

        // Obtenemos el nombre original y reemplazamos ESPACIOS y signos raros por guiones bajos
        String originalFilename = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
        originalFilename = originalFilename.replaceAll("[\\s+]", "_");

        // Formato final LIMPIO: 70820348_DIEGO_RUIZ_WhatsApp_Image_2026.jpeg
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

            // Guardamos el archivo y le agregamos el prefijo "uploads/" para heredar los permisos públicos
            String savedPath = fileStorageService.storeFileWithCustomName(voucherFile, "vouchers", customFileName);
            fotoVoucherUrl = "uploads/" + savedPath;
        }

        Pago pago = Pago.builder()
                .cuota(cuota)
                .montoAbonado(montoAbonado)
                .metodoPago(metodoPago)
                .numeroOperacion(numeroOperacion)
                .fotoVoucherUrl(fotoVoucherUrl)
                .estado(EstadoPago.PROCESADO)
                .build();

        double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMontoAbonado();
        cuota.setMontoPagado(nuevoMontoPagado);

        if (nuevoMontoPagado >= cuota.getMontoTotal()) {
            cuota.setEstado(EstadoCuota.PAGADO_TOTAL);
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
            throw new RuntimeException("Este pago ya fue procesado y validado.");
        }

        Cuota cuota = pago.getCuota();
        String fotoVoucherUrl = null;

        if (voucherFile != null && !voucherFile.isEmpty()) {
            Cliente cliente = cuota.getContrato().getCliente();
            String customFileName = generarNombreVoucher(cliente, voucherFile);

            // Guardamos el archivo y le agregamos el prefijo "uploads/"
            String savedPath = fileStorageService.storeFileWithCustomName(voucherFile, "vouchers", customFileName);
            fotoVoucherUrl = "uploads/" + savedPath;
        }

        pago.setMetodoPago(metodoPago);
        pago.setNumeroOperacion(numeroOperacion);
        if (fotoVoucherUrl != null) {
            pago.setFotoVoucherUrl(fotoVoucherUrl);
        }
        pago.setEstado(EstadoPago.PROCESADO);

        double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMontoAbonado();
        cuota.setMontoPagado(nuevoMontoPagado);

        if (nuevoMontoPagado >= cuota.getMontoTotal()) {
            cuota.setEstado(EstadoCuota.PAGADO_TOTAL);
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