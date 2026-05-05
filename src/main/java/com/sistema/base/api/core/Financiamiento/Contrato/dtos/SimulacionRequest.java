package com.sistema.base.api.core.Financiamiento.Contrato.dtos;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SimulacionRequest {
    private Double precioTotal;
    private Double montoInicial;
    private Integer cantidadCuotas;


    private LocalDate fechaInicioPago;

    // --- NUEVO: Simulación Flexible ---
    private Integer cuotasEspeciales;
    private Double montoCuotaEspecial;

    private List<BloqueCuotaDTO> bloquesFlexibles;
}