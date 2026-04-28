package com.sistema.base.api.core.Financiamiento.Contrato;

public enum TipoInicial {
    CERO,       // Entró sin pagar inicial (financia el 100%)
    PARCIAL,    // Dio un abono, pero aún no completa la inicial requerida
    TOTAL       // Pagó la inicial completa
}
