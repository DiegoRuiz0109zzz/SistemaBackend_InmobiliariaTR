package com.sistema.base.api.core.Financiamiento.Pago;

import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Cuota.CuotaRepository;
import com.sistema.base.api.core.Financiamiento.Cuota.EstadoCuota;
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
    public Pago registrarPago(Pago pago) {
        // 1. Buscar la cuota a la que se le está haciendo el abono
        Cuota cuota = cuotaRepository.findById(pago.getCuota().getId())
                .orElseThrow(() -> new RuntimeException("La cuota no existe."));

        // 2. Validar que no pague más de lo que debe
        double saldoPendiente = cuota.getMontoTotal() - cuota.getMontoPagado();
        if (pago.getMontoAbonado() > saldoPendiente) {
            throw new RuntimeException("El monto a abonar (" + pago.getMontoAbonado() +
                    ") supera el saldo pendiente de la cuota (" + saldoPendiente + ").");
        }

        // 3. Sumar el pago al acumulado de la cuota
        double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMontoAbonado();
        cuota.setMontoPagado(nuevoMontoPagado);

        // 4. Cambiar el estado automáticamente según el progreso
        if (nuevoMontoPagado >= cuota.getMontoTotal()) {
            cuota.setEstado(EstadoCuota.PAGADO_TOTAL);
        } else {
            cuota.setEstado(EstadoCuota.PAGADO_PARCIAL);
        }

        // Guardar la cuota actualizada
        cuotaRepository.save(cuota);

        // 5. Vincular y guardar el historial del pago
        pago.setCuota(cuota);
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
}
