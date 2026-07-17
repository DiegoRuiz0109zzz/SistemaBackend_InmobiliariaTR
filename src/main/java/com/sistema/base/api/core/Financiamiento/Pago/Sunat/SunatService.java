package com.sistema.base.api.core.Financiamiento.Pago.Sunat;

import com.sistema.base.api.core.Empresa.Empresa;
import com.sistema.base.api.core.Financiamiento.Contrato.Contrato;
import com.sistema.base.api.core.Financiamiento.Cuota.Cuota;
import com.sistema.base.api.core.Financiamiento.Pago.Pago;
import com.sistema.base.api.core.Usuario.Clientes.Cliente;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SunatService {

    private final RestTemplate restTemplate;

    public SunatService() {
        this.restTemplate = getRestTemplateInseguro();
    }

    @Value("${sunat.api.url}")
    private String apiUrl;

    @Value("${sunat.api.token}")
    private String apiToken;

    public Map<String, Object> emitirComprobanteMultiple(List<Pago> pagos, String tipoComprobanteId, String serie, String numeroCorrelativo, String tipoIgv, String tipoDoc, String ruc, String razonSocial, String direccionFactura, Empresa empresa) {

        // Tomamos el contrato del primer pago (todos pertenecen al mismo contrato)
        Contrato contratoBase = pagos.get(0).getCuota().getContrato();
        Cliente cliente = contratoBase.getCliente();

        String codigoTipoDocumento = "FACTURA".equalsIgnoreCase(tipoComprobanteId) ? "01" : "03";
        String codigoTipoDocIdentidad = (tipoDoc != null && !tipoDoc.isEmpty()) ? tipoDoc : ("01".equals(codigoTipoDocumento) ? "6" : "1");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken);

        Map<String, Object> payload = new HashMap<>();

        // --- Cabecera ---
        payload.put("codigo_tipo_documento", codigoTipoDocumento);
        payload.put("serie_documento", serie);
        payload.put("numero_documento", numeroCorrelativo);
        payload.put("fecha_de_emision", java.time.LocalDate.now().toString());
        payload.put("hora_de_emision", java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        payload.put("codigo_tipo_moneda", "PEN");

        // ✅ CORREGIDO: 0104 es el código obligatorio de SUNAT para Anticipos
        payload.put("codigo_tipo_operacion", "0101");

        payload.put("fecha_de_vencimiento", java.time.LocalDate.now().toString());
        payload.put("forma_de_pago", "Contado");

        // ✅ CORREGIDO: Limpiamos la palabra "TRANSFERENCIA " para que solo viaje "BCP 123456"
        String obsMetodo = pagos.get(0).getMetodoPago() != null ? pagos.get(0).getMetodoPago().replace("TRANSFERENCIA ", "").trim() : "";
        String obsOperacion = pagos.get(0).getNumeroOperacion() != null ? pagos.get(0).getNumeroOperacion() : "";
        String observacionSunat = (obsMetodo + " " + obsOperacion).trim();

        if (!observacionSunat.isEmpty()) {
            payload.put("observaciones", observacionSunat);
        }

        // --- Datos del Emisor (Tu Empresa) ---
        if (empresa != null) {
            Map<String, Object> emisor = new HashMap<>();
            emisor.put("codigo_tipo_documento_identidad", "6"); // 6 = RUC
            emisor.put("numero_documento", empresa.getRuc());
            emisor.put("apellidos_y_nombres_o_razon_social", empresa.getRazonSocial());
            emisor.put("nombre_comercial", empresa.getRazonSocial());
            emisor.put("direccion", empresa.getDireccion() != null ? empresa.getDireccion() : "-");
            emisor.put("codigo_pais", "PE");
            payload.put("datos_del_emisor", emisor);
        }

        // --- Datos del Cliente ---
        Map<String, Object> receptor = new HashMap<>();
        receptor.put("codigo_tipo_documento_identidad", codigoTipoDocIdentidad);
        receptor.put("codigo_pais", "PE");
        receptor.put("correo_electronico", cliente.getEmail() != null ? cliente.getEmail() : "");
        receptor.put("telefono", "-");

        if ("FACTURA".equalsIgnoreCase(tipoComprobanteId)) {
            receptor.put("numero_documento", (ruc != null && !ruc.isEmpty()) ? ruc : cliente.getNumeroDocumento());
            receptor.put("apellidos_y_nombres_o_razon_social", (razonSocial != null && !razonSocial.isEmpty()) ? razonSocial : cliente.getNombres() + " " + (cliente.getApellidos() != null ? cliente.getApellidos() : ""));
            receptor.put("direccion", (direccionFactura != null && !direccionFactura.isEmpty()) ? direccionFactura : "-");
        } else {
            receptor.put("numero_documento", cliente.getNumeroDocumento());
            receptor.put("apellidos_y_nombres_o_razon_social", cliente.getNombres() + " " + (cliente.getApellidos() != null ? cliente.getApellidos() : ""));
            receptor.put("direccion", cliente.getDireccion() != null ? cliente.getDireccion() : "-");
        }
        payload.put("datos_del_cliente_o_receptor", receptor);

        // --- ITERACIÓN DE ÍTEMS Y SUMATORIA TOTAL ---
        List<Map<String, Object>> items = new ArrayList<>();
        double sumaTotalVenta = 0.0;
        double sumaTotalValor = 0.0;
        double sumaTotalIgv = 0.0;

        String nombreManzana = (contratoBase.getLote().getManzana() != null) ? contratoBase.getLote().getManzana().getNombre() : "";
        String numeroLote = (contratoBase.getLote().getNumero() != null) ? String.valueOf(contratoBase.getLote().getNumero()) : "";
        String codigoLote = "MZ " + nombreManzana + "-" + numeroLote;

        for (Pago pago : pagos) {
            double montoItem = pago.getMontoAbonado();
            double valorUnitarioItem = ("20".equals(tipoIgv) || "30".equals(tipoIgv)) ? montoItem : (montoItem / 1.18);
            double igvItem = ("20".equals(tipoIgv) || "30".equals(tipoIgv)) ? 0.0 : (montoItem - valorUnitarioItem);

            sumaTotalVenta += montoItem;
            sumaTotalValor += valorUnitarioItem;
            sumaTotalIgv += igvItem;

            // ✅ CORREGIDO: Se quita la concatenación repetida de "codigoLote"
            String descripcionFila = pago.getDescripcion();

            Map<String, Object> item = new HashMap<>();
            item.put("codigo_interno", codigoLote);
            item.put("codigo_producto_sunat", "51121703");
            item.put("unidad_de_medida", "NIU");
            item.put("descripcion", descripcionFila);
            item.put("cantidad", 1);
            item.put("valor_unitario", Math.round(valorUnitarioItem * 100.0) / 100.0);
            item.put("codigo_tipo_precio", "01");
            item.put("codigo_tipo_afectacion_igv", tipoIgv);
            item.put("porcentaje_igv", "10".equals(tipoIgv) ? 18 : 0);
            item.put("precio_unitario", Math.round((valorUnitarioItem + igvItem) * 100.0) / 100.0);
            item.put("total_base_igv", Math.round(valorUnitarioItem * 100.0) / 100.0);
            item.put("total_igv", Math.round(igvItem * 100.0) / 100.0);
            item.put("total_impuestos", Math.round(igvItem * 100.0) / 100.0);
            item.put("total_valor_item", Math.round(valorUnitarioItem * 100.0) / 100.0);
            item.put("total_item", Math.round(montoItem * 100.0) / 100.0);

            items.add(item);
        }
        payload.put("items", items);

        // --- Totales de la Cabecera ---
        Map<String, Object> totales = new HashMap<>();
        totales.put("total_exportacion", 0);
        totales.put("total_operaciones_gravadas", ("10".equals(tipoIgv)) ? Math.round(sumaTotalValor * 100.0) / 100.0 : 0);
        totales.put("total_operaciones_exoneradas", "20".equals(tipoIgv) ? Math.round(sumaTotalVenta * 100.0) / 100.0 : 0);
        totales.put("total_operaciones_inafectas", "30".equals(tipoIgv) ? Math.round(sumaTotalVenta * 100.0) / 100.0 : 0);
        totales.put("total_operaciones_gratuitas", 0);
        totales.put("total_igv", Math.round(sumaTotalIgv * 100.0) / 100.0);
        totales.put("total_impuestos", Math.round(sumaTotalIgv * 100.0) / 100.0);
        totales.put("total_valor", Math.round(sumaTotalValor * 100.0) / 100.0);
        totales.put("total_venta", Math.round(sumaTotalVenta * 100.0) / 100.0);
        payload.put("totales", totales);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error crítico al conectar con SUNAT: " + e.getMessage());
        }
    }

    private RestTemplate getRestTemplateInseguro() {
        try {
            var sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                    .build();
            var sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();
            var httpClient = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build();
            return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        } catch (Exception e) {
            throw new RuntimeException("Error configurando cliente HTTP: " + e.getMessage());
        }
    }
}