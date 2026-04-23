package com.sistema.base.api.core.Financiamiento.Pago.dtos;

import lombok.Data;

@Data
public class PagoRequest {
    private Long cuotaId;
    private Double montoAbonado;
    private String metodoPago;
    private String numeroOperacion;
    private String fotoVoucherUrl; // Si implementas la subida a S3 o local más adelante
}