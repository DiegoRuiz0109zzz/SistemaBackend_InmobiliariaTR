package com.sistema.base.api.core.Financiamiento.Pago;

import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;
import com.sistema.base.api.core.Financiamiento.Pago.dtos.PagoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;

    @Transactional(readOnly = true)
    public List<Pago> listarPorCuota(Long cuotaId) {
        return pagoRepository.findByCuotaIdAndEnabledTrue(cuotaId);
    }

    @Transactional
    public Pago registrarPago(PagoRequest request) {
        // 1. Buscar la cuota a la que se le está haciendo el abono
        Cuota cuota = cuotaRepository.findById(request.getCuotaId())
                .orElseThrow(() -> new RuntimeException("La cuota no existe."));

        // 2. Validar que no pague más de lo que debe
        double saldoPendiente = cuota.getMontoTotal() - cuota.getMontoPagado();
        if (request.getMontoAbonado() > saldoPendiente) {
            throw new RuntimeException("El monto a pagar (" + request.getMontoAbonado() +
                    ") supera el saldo pendiente de la cuota (" + saldoPendiente + ").");
        }

        // 3. Construir el nuevo Pago con los datos completos de Caja
        Pago pago = Pago.builder()
                .cuota(cuota)
                .montoAbonado(request.getMontoAbonado())
                .metodoPago(request.getMetodoPago())
                .numeroOperacion(request.getNumeroOperacion())
                .fotoVoucherUrl(request.getFotoVoucherUrl())
                .build();

        // 4. Sumar el pago al acumulado de la cuota
        double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMontoAbonado();
        cuota.setMontoPagado(nuevoMontoPagado);

        // 5. Cambiar el estado de la cuota inteligentemente
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

        // Reversar el monto en la cuota
        cuota.setMontoPagado(cuota.getMontoPagado() - pago.getMontoAbonado());

        // Ajustar el estado si se anula
        if (cuota.getMontoPagado() <= 0) {
            cuota.setEstado(EstadoCuota.PENDIENTE);
        } else {
            cuota.setEstado(EstadoCuota.PAGADO_PARCIAL);
        }
        cuotaRepository.save(cuota);

        // Borrado lógico del pago
        pago.setEnabled(false);
        pagoRepository.save(pago);
    }

    @Transactional
    public Pago procesarPagoPendiente(Long pagoId, String metodoPago, String numeroOperacion, String fotoVoucherUrl) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado."));

        if (pago.getEstado() == EstadoPago.PROCESADO) {
            throw new RuntimeException("Este pago ya fue procesado y validado anteriormente.");
        }

        Cuota cuota = pago.getCuota();

        // 1. Actualizamos los datos reales del pago
        pago.setMetodoPago(metodoPago);
        pago.setNumeroOperacion(numeroOperacion);
        pago.setFotoVoucherUrl(fotoVoucherUrl);
        pago.setEstado(EstadoPago.PROCESADO);

        // 2. Ahora sí le sumamos el dinero a la Cuota (porque ya entró a caja)
        double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMontoAbonado();
        cuota.setMontoPagado(nuevoMontoPagado);

        // 3. Ajustamos el estado de la cuota
        if (nuevoMontoPagado >= cuota.getMontoTotal()) {
            cuota.setEstado(EstadoCuota.PAGADO_TOTAL);
        } else {
            cuota.setEstado(EstadoCuota.PAGADO_PARCIAL);
        }

        cuotaRepository.save(cuota);
        return pagoRepository.save(pago);
    }


}
