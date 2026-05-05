package com.sistema.base.api.core.Financiamiento.Contrato.dtos;

import com.sistema.base.api.core.Financiamiento.Contrato.TipoInicial;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ContratoRequest {
    private Long loteId;
    private Long clienteId;
    private Long coCompradorId;
    private Long vendedorId;

    private Double precioTotal;
    private Double montoInicialAcordado;
    private Double abonoInicialReal;
    private LocalDate fechaLimiteInicial;

    private Integer cantidadCuotas;
    private LocalDate fechaInicioPago;

    private LocalDate fechaContrato;

    private Integer cuotasEspeciales;
    private Double montoCuotaEspecial;

    private Long cotizacionId; // Para amarrar el contrato a la cotización
    private TipoInicial tipoInicial; // CERO, PARCIAL, TOTAL
    private Boolean cuotasFlexibles; // El interruptor (true/false)

    // NUEVO: Observación manual del vendedor
    private String observacion;

    private List<BloqueCuotaDTO> bloquesFlexibles;
}