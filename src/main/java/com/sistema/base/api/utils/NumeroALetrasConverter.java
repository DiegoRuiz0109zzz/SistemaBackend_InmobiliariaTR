package com.sistema.base.api.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumeroALetrasConverter {

    private static final String[] UNIDADES = {"", "UN ", "DOS ", "TRES ", "CUATRO ", "CINCO ", "SEIS ", "SIETE ", "OCHO ", "NUEVE "};
    private static final String[] DECENAS = {"DIEZ ", "ONCE ", "DOCE ", "TRECE ", "CATORCE ", "QUINCE ", "DIECISEIS ", "DIECISIETE ", "DIECIOCHO ", "DIECINUEVE ", "VEINTE ", "TREINTA ", "CUARENTA ", "CINCUENTA ", "SESENTA ", "SETENTA ", "OCHENTA ", "NOVENTA "};
    private static final String[] CENTENAS = {"", "CIENTO ", "DOSCIENTOS ", "TRESCIENTOS ", "CUATROCIENTOS ", "QUINIENTOS ", "SEISCIENTOS ", "SETECIENTOS ", "OCHOCIENTOS ", "NOVECIENTOS "};

    public static String convertir(Double numero, String moneda) {
        BigDecimal bd = new BigDecimal(numero).setScale(2, RoundingMode.HALF_UP);
        long entero = bd.longValue();
        int centavos = Integer.parseInt(bd.toString().split("\\.")[1]);

        String letras = convertirNumero(entero);
        if (letras.trim().isEmpty()) {
            letras = "CERO ";
        }

        // Formato exacto: "DOS MIL OCHOCIENTOS CINCUENTA CON 00/100 SOLES"
        return letras + "CON " + String.format("%02d", centavos) + "/100 " + moneda.toUpperCase();
    }

    private static String convertirNumero(long numero) {
        if (numero < 0) return "MENOS " + convertirNumero(-Math.abs(numero));
        if (numero == 0) return "";
        if (numero < 10) return UNIDADES[(int) numero];
        if (numero < 20) return DECENAS[(int) numero - 10];
        if (numero < 30) return numero == 20 ? "VEINTE " : "VEINTI" + UNIDADES[(int) numero - 20];
        if (numero < 100) return DECENAS[(int) (numero / 10) + 8] + ((numero % 10 != 0) ? "Y " + convertirNumero(numero % 10) : "");
        if (numero == 100) return "CIEN ";
        if (numero < 1000) return CENTENAS[(int) (numero / 100)] + convertirNumero(numero % 100);
        if (numero == 1000) return "MIL ";
        if (numero < 2000) return "MIL " + convertirNumero(numero % 1000);
        if (numero < 1000000) {
            return convertirNumero(numero / 1000) + "MIL " + convertirNumero(numero % 1000);
        }
        if (numero == 1000000) return "UN MILLON ";
        if (numero < 2000000) return "UN MILLON " + convertirNumero(numero % 1000000);

        return convertirNumero(numero / 1000000) + "MILLONES " + convertirNumero(numero % 1000000);
    }
}