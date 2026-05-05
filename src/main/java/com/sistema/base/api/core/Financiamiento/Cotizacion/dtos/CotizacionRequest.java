package com.sistema.base.api.core.Financiamiento.Cotizacion.dtos;

import com.sistema.base.api.core.Financiamiento.Contrato.TipoInicial;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.BloqueCuotaDTO;
import lombok.Data;

import java.util.List;

@Data
public class CotizacionRequest {
    private Long loteId;
    private Long interesadoId;
    private Long vendedorId;
    private Long coCompradorId;

    // --- NUEVOS CAMPOS AÑADIDOS ---
    private TipoInicial tipoInicial;
    private Boolean cuotasFlexibles;

    private Double montoCuotaCotizacion;
    private Double saldoFinanciar;

    private Double precioTotal;
    private Double montoInicialAcordado;
    private Integer cantidadCuotas;

    private Integer cuotasEspeciales;
    private Double montoCuotaEspecial;

    private Integer diasValidez; // Ej: 7

    private List<BloqueCuotaDTO> bloquesFlexibles;
}