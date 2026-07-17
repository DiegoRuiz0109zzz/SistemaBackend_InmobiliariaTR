package com.sistema.base.api.core.Financiamiento.Pago.Sunat;

import com.sistema.base.api.core.Financiamiento.Pago.TipoComprobante;
import lombok.Data;

@Data
public class PagoSunatRequest {
    private Long cuotaId;
    private Double montoAbonado;
    private String metodoPago;
    private String descripcion;
    private String numeroOperacion;
    private TipoComprobante tipoComprobante; // Enum: BOLETA, FACTURA
    private String serie;                   // Ej: B001
    private String tipoIgv;                 // Ej: "20"
    private String tipoDoc;                 // Ej: "6" para RUC

    // ✅ NUEVOS CAMPOS PARA FACTURA
    private String ruc;
    private String razonSocial;
    private String direccionFactura;
}
