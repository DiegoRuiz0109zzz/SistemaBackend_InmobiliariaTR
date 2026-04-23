package com.sistema.base.api.core.Financiamiento.Cotizacion.dtos;

import lombok.Data;

@Data
public class CotizacionRequest {
    private Long loteId;
    private Long interesadoId;
    private Long vendedorId;

    private Double precioTotal;
    private Double montoInicialAcordado;
    private Integer cantidadCuotas;

    private Integer cuotasEspeciales;
    private Double montoCuotaEspecial;

    private Integer diasValidez; // Ej: 7
}