package com.sistema.base.api.core.Reportes;

import com.sistema.base.api.core.Financiamiento.Contrato.EstadoContrato;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteMaestroDTO {
    private Long idContrato;
    private Long nroContrato;
    private LocalDate fechaContrato;
    private String nombreVendedor;
    private String nombreCliente;
    private String numeroLote;
    private String urbanizacion;
    private String etapa;
    private String manzana;
    private Double precioOficinaLote;
    private Double precioVentaFinal;
    private Integer cuotasPagas;
    private Integer cuotasPendientes;
    private Integer cuotasVencidas;
    private EstadoContrato estadoContrato;
}
