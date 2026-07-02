package com.sistema.base.api.core.Financiamiento.Contrato.dtos;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SimulacionRequest {
    private Double precioTotal;
    private Double montoInicial;
    private Integer cantidadCuotas;

    // El cliente elige la fecha, el día de esta fecha fijará los pagos mensuales
    private LocalDate fechaInicioPago;

    private List<BloqueCuotaDTO> bloquesFlexibles;

    // --- NUEVO: Simulación Flexible ---
    private Integer cuotasEspeciales;  // Ej: 3
    private Double montoCuotaEspecial; // Ej: 1000.0
}