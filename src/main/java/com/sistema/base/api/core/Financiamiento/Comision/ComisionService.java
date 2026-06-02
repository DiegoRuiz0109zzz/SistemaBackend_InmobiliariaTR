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

        boolean esVentaDirectaJefe = (jefe == null);

        if (esVentaDirectaJefe) {
            // =========================================================
            // ESCENARIO 1: VENTA DIRECTA DEL JEFE (Se divide en 2 partes)
            // =========================================================

            // PARTE 1: El Jefe cobrando como VENDEDOR (Base 400 + 35%)
            Double baseComoVendedor = 400.0;
            Double bonoGlobalComoVendedor = 0.0;
            Double bonoDiffComoVendedor = diferencia * 0.35;
            Double totalComoVendedor = baseComoVendedor + bonoGlobalComoVendedor + bonoDiffComoVendedor;

            Comision comisionComoVendedor = Comision.builder()
                    .contrato(contrato)
                    .rolBeneficiario(RolComision.VENDEDOR)
                    .vendedor(vendedor)
                    .jefeVentas(null)
                    .precioOficinaLote(precioOficina)
                    .precioVentaContrato(precioVenta)
                    .diferenciaPrecio(diferencia)
                    .montoBase(baseComoVendedor) // ✅ 400
                    .montoBonoGlobal(bonoGlobalComoVendedor) // ✅ 0
                    .porcentajeBonoDiferencia(35.0) // ✅ 35% EXPLÍCITO
                    .montoBonoDiferencia(bonoDiffComoVendedor)
                    .totalComision(totalComoVendedor)
                    .estadoPago(EstadoComision.PENDIENTE)
                    .build();
            comisionRepository.save(comisionComoVendedor);

            // PARTE 2: El Jefe cobrando su bono de JEFE (Bono Global 100 + 15%)
            Double baseComoJefe = 0.0;
            Double bonoGlobalComoJefe = 100.0;
            Double bonoDiffComoJefe = diferencia * 0.15;
            Double totalComoJefe = baseComoJefe + bonoGlobalComoJefe + bonoDiffComoJefe;

            Comision comisionComoJefe = Comision.builder()
                    .contrato(contrato)
                    .rolBeneficiario(RolComision.JEFE_VENTAS)
                    .vendedor(vendedor)
                    .jefeVentas(null)
                    .precioOficinaLote(precioOficina)
                    .precioVentaContrato(precioVenta)
                    .diferenciaPrecio(diferencia)
                    .montoBase(baseComoJefe) // ✅ 0
                    .montoBonoGlobal(bonoGlobalComoJefe) // ✅ 100
                    .porcentajeBonoDiferencia(15.0) // ✅ 15% EXPLÍCITO
                    .montoBonoDiferencia(bonoDiffComoJefe)
                    .totalComision(totalComoJefe)
                    .estadoPago(EstadoComision.PENDIENTE)
                    .build();
            comisionRepository.save(comisionComoJefe);

        } else {
            // =========================================================
            // ESCENARIO 2: VENTA NORMAL (Vendedor + Jefe)
            // =========================================================

            // A) Comisión del Vendedor (Base 400 + 35%)
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
                    .porcentajeBonoDiferencia(35.0) // ✅ 35% EXPLÍCITO
                    .montoBonoDiferencia(bonoDiffVendedor)
                    .totalComision(totalVendedor)
                    .estadoPago(EstadoComision.PENDIENTE)
                    .build();
            comisionRepository.save(comisionVendedor);

            // B) Comisión del Jefe (Bono Global 100 + 15%)
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
                    .porcentajeBonoDiferencia(15.0) // ✅ 15% EXPLÍCITO
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