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

        Double precioOficina = contrato.getLote().getPrecioVenta() != null ? contrato.getLote().getPrecioVenta() : 0.0;
        Double precioVenta = contrato.getPrecioTotal() != null ? contrato.getPrecioTotal() : 0.0;
        Double diferencia = Math.max(0, precioVenta - precioOficina);

        Vendedor vendedor = contrato.getVendedor();
        User jefe = vendedor != null ? vendedor.getJefeVentas() : null;

        // Si el vendedor no tiene un jefe asignado, significa que él mismo es el Jefe de Ventas
        boolean esVentaDirectaJefe = (jefe == null);

        if (esVentaDirectaJefe) {
            // =========================================================
            // ESCENARIO 1: VENTA DIRECTA DEL JEFE (1 SOLO REGISTRO)
            // =========================================================

            Double baseJefe = 400.0; // Gana la base por hacer la venta
            Double bonoGlobalJefe = 100.0; // Gana el bono global por su rol
            Double porcentajeTotal = 50.0; // Suma del 35% (venta) + 15% (jefatura)
            Double bonoDiffTotal = diferencia * 0.50;
            Double totalComisionJefe = baseJefe + bonoGlobalJefe + bonoDiffTotal;

            Comision comisionUnica = Comision.builder()
                    .contrato(contrato)
                    .rolBeneficiario(RolComision.JEFE_VENTAS) // O VENDEDOR, según prefieras que se muestre en tu tabla
                    .vendedor(vendedor)
                    .jefeVentas(null)
                    .precioOficinaLote(precioOficina)
                    .precioVentaContrato(precioVenta)
                    .diferenciaPrecio(diferencia)
                    .montoBase(baseJefe) // ✅ 400
                    .montoBonoGlobal(bonoGlobalJefe) // ✅ 100
                    .porcentajeBonoDiferencia(porcentajeTotal) // ✅ 50% explícito
                    .montoBonoDiferencia(bonoDiffTotal)
                    .totalComision(totalComisionJefe)
                    .estadoPago(EstadoComision.PENDIENTE)
                    .build();
            comisionRepository.save(comisionUnica);

        } else {
            // =========================================================
            // ESCENARIO 2: VENTA NORMAL (Vendedor + Jefe en 2 registros)
            // =========================================================

            // A) Comisión exclusiva del Vendedor
            Double baseVendedor = 400.0;
            Double bonoGlobalVendedor = 0.0;
            Double bonoDiffVendedor = diferencia * 0.35;
            Double totalVendedor = baseVendedor + bonoGlobalVendedor + bonoDiffVendedor;

            Comision comisionVendedor = Comision.builder()
                    .contrato(contrato)
                    .rolBeneficiario(RolComision.VENDEDOR)
                    .vendedor(vendedor)
                    .jefeVentas(jefe)
                    .precioOficinaLote(precioOficina)
                    .precioVentaContrato(precioVenta)
                    .diferenciaPrecio(diferencia)
                    .montoBase(baseVendedor) // ✅ 400
                    .montoBonoGlobal(bonoGlobalVendedor) // ✅ 0
                    .porcentajeBonoDiferencia(35.0) // ✅ 35%
                    .montoBonoDiferencia(bonoDiffVendedor)
                    .totalComision(totalVendedor)
                    .estadoPago(EstadoComision.PENDIENTE)
                    .build();
            comisionRepository.save(comisionVendedor);

            // B) Comisión exclusiva del Jefe
            Double baseJefe = 0.0;
            Double bonoGlobalJefe = 100.0;
            Double bonoDiffJefe = diferencia * 0.15;
            Double totalJefe = baseJefe + bonoGlobalJefe + bonoDiffJefe;

            Comision comisionJefe = Comision.builder()
                    .contrato(contrato)
                    .rolBeneficiario(RolComision.JEFE_VENTAS)
                    .vendedor(vendedor)
                    .jefeVentas(jefe)
                    .precioOficinaLote(precioOficina)
                    .precioVentaContrato(precioVenta)
                    .diferenciaPrecio(diferencia)
                    .montoBase(baseJefe) // ✅ 0
                    .montoBonoGlobal(bonoGlobalJefe) // ✅ 100
                    .porcentajeBonoDiferencia(15.0) // ✅ 15%
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