package com.sistema.base.api.core.Financiamiento.Comision;

import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import com.sistema.base.api.core.Vendedores.Vendedor;
import com.sistema.base.api.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComisionService {

    private final ComisionRepository comisionRepository;

    @Transactional
    public void evaluarYGenerarComisiones(Contrato contrato) {
        if (comisionRepository.existsByContratoId(contrato.getId())) {
            return;
        }

        // ✅ Cálculos de Diferencia (Ahora sí los guardaremos)
        Double precioOficina = contrato.getLote().getPrecioVenta() != null ? contrato.getLote().getPrecioVenta() : 0.0;
        Double precioVenta = contrato.getPrecioTotal() != null ? contrato.getPrecioTotal() : 0.0;
        Double diferencia = Math.max(0, precioVenta - precioOficina);

        Vendedor vendedor = contrato.getVendedor();
        User jefe = vendedor != null ? vendedor.getJefeVentas() : null;

        boolean esVentaDirectaJefe = (jefe == null);

        if (esVentaDirectaJefe) {
            // =========================================================
            // ESCENARIO 2: VENTA DIRECTA DEL JEFE
            // =========================================================
            Double baseTotal = 400.0 + 100.0;
            Double bonoDiffTotal = (diferencia * 0.35) + (diferencia * 0.15);
            Double comisionTotal = baseTotal + bonoDiffTotal;

            Comision comisionJefe = Comision.builder()
                    .contrato(contrato)
                    .rolBeneficiario(RolComision.JEFE_VENTAS)
                    .vendedor(vendedor)
                    .jefeVentas(null)
                    .precioOficinaLote(precioOficina) // ✅ GUARDADO
                    .precioVentaContrato(precioVenta) // ✅ GUARDADO
                    .diferenciaPrecio(diferencia)     // ✅ GUARDADO
                    .montoBase(baseTotal)
                    .montoBonoDiferencia(bonoDiffTotal)
                    .totalComision(comisionTotal)
                    .estadoPago(EstadoComision.PENDIENTE)
                    .build();
            comisionRepository.save(comisionJefe);

        } else {
            // =========================================================
            // ESCENARIO 1: VENTA NORMAL (Vendedor + Jefe)
            // =========================================================

            // A) Comisión del Vendedor
            Double baseVendedor = 400.0;
            Double bonoDiffVendedor = diferencia * 0.35;
            Double totalVendedor = baseVendedor + bonoDiffVendedor;

            Comision comisionVendedor = Comision.builder()
                    .contrato(contrato)
                    .rolBeneficiario(RolComision.VENDEDOR)
                    .vendedor(vendedor)
                    .jefeVentas(null)
                    .precioOficinaLote(precioOficina) // ✅ GUARDADO
                    .precioVentaContrato(precioVenta) // ✅ GUARDADO
                    .diferenciaPrecio(diferencia)     // ✅ GUARDADO
                    .montoBase(baseVendedor)
                    .montoBonoDiferencia(bonoDiffVendedor)
                    .totalComision(totalVendedor)
                    .estadoPago(EstadoComision.PENDIENTE)
                    .build();
            comisionRepository.save(comisionVendedor);

            // B) Comisión del Jefe (Bono Global + Porcentaje)
            Double baseJefe = 100.0;
            Double bonoDiffJefe = diferencia * 0.15;
            Double totalJefe = baseJefe + bonoDiffJefe;

            Comision comisionJefe = Comision.builder()
                    .contrato(contrato)
                    .rolBeneficiario(RolComision.JEFE_VENTAS)
                    .vendedor(null)
                    .jefeVentas(jefe)
                    .precioOficinaLote(precioOficina) // ✅ GUARDADO
                    .precioVentaContrato(precioVenta) // ✅ GUARDADO
                    .diferenciaPrecio(diferencia)     // ✅ GUARDADO
                    .montoBase(baseJefe)
                    .montoBonoDiferencia(bonoDiffJefe)
                    .totalComision(totalJefe)
                    .estadoPago(EstadoComision.PENDIENTE)
                    .build();
            comisionRepository.save(comisionJefe);
        }
    }

    @Transactional(readOnly = true)
    public List<Comision> listarTodas() {
        return comisionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Comision> listarPorContrato(Long contratoId) {
        return comisionRepository.findByContratoId(contratoId);
    }

    @Transactional
    public Comision actualizarEstadoPago(Long id, EstadoComision nuevoEstado, String observacion) {
        Comision comision = comisionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("La comisión solicitada no existe."));

        comision.setEstadoPago(nuevoEstado);
        comision.setObservacionPago(observacion);

        if (nuevoEstado == EstadoComision.PAGADO) {
            comision.setFechaPago(java.time.LocalDateTime.now());
        } else {
            comision.setFechaPago(null);
        }

        return comisionRepository.save(comision);
    }
}