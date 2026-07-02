package com.sistema.base.api.core.Financiamiento.Pago.Sunat;

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

    public Map<String, Object> emitirComprobante(Pago pago, String tipoComprobanteId, String serie, String numeroCorrelativo, String tipoIgv, String tipoDoc, String ruc, String razonSocial, String direccionFactura) {
        Cuota cuota = pago.getCuota();
        Cliente cliente = cuota.getContrato().getCliente();

        String nombreManzana = (cuota.getContrato().getLote().getManzana() != null)
                ? cuota.getContrato().getLote().getManzana().getNombre()
                : "";

        String numeroLote = (cuota.getContrato().getLote().getNumero() != null)
                ? String.valueOf(cuota.getContrato().getLote().getNumero())
                : "";

        String codigoLote = "MZ " + nombreManzana + "-" + numeroLote;

        String numeroCuotaStr = (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() > 0) ? String.valueOf(cuota.getNumeroCuota()) : "INICIAL";

        String descripcionDinamica = "ANTICIPO RECIBIDO:SALDO A CUOTA " + numeroCuotaStr + " " + codigoLote +
                ". PROYECTO DE NOMINADO LOTIZACION OLMOS,SECTOR OLMOS,DISTRITO OLMOS ,LAMBAYEQUE.";

        String codigoTipoDocumento = "FACTURA".equalsIgnoreCase(tipoComprobanteId) ? "01" : "03";

        String codigoTipoDocIdentidad = (tipoDoc != null && !tipoDoc.isEmpty())
                ? tipoDoc
                : ("01".equals(codigoTipoDocumento) ? "6" : "1");

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
        payload.put("codigo_tipo_operacion", "0101");
        payload.put("fecha_de_vencimiento", java.time.LocalDate.now().toString());

        // --- Forma de pago ---
        payload.put("forma_de_pago", "Contado");

        // --- Datos del Cliente ---
        Map<String, Object> receptor = new HashMap<>();
        receptor.put("codigo_tipo_documento_identidad", codigoTipoDocIdentidad);
        receptor.put("codigo_pais", "PE");
        receptor.put("correo_electronico", cliente.getEmail() != null ? cliente.getEmail() : "");
        receptor.put("telefono", "-");

        // ✅ LÓGICA CONDICIONAL: Factura usa datos del Request, Boleta usa datos de la BD
        if ("FACTURA".equalsIgnoreCase(tipoComprobanteId)) {
            // Hacemos un pequeño fallback para evitar nulos en caso de que accidentalmente no envíen el RUC
            receptor.put("numero_documento", (ruc != null && !ruc.isEmpty()) ? ruc : cliente.getNumeroDocumento());
            receptor.put("apellidos_y_nombres_o_razon_social", (razonSocial != null && !razonSocial.isEmpty()) ? razonSocial : cliente.getNombres() + " " + (cliente.getApellidos() != null ? cliente.getApellidos() : ""));
            receptor.put("direccion", (direccionFactura != null && !direccionFactura.isEmpty()) ? direccionFactura : "-");
        } else {
            receptor.put("numero_documento", cliente.getNumeroDocumento());
            receptor.put("apellidos_y_nombres_o_razon_social", cliente.getNombres() + " " + (cliente.getApellidos() != null ? cliente.getApellidos() : ""));
            receptor.put("direccion", cliente.getDireccion() != null ? cliente.getDireccion() : "-");
        }

        payload.put("datos_del_cliente_o_receptor", receptor);

        // --- Cálculos ---
        double montoTotal = pago.getMontoAbonado();
        double valorUnitario = ("20".equals(tipoIgv) || "30".equals(tipoIgv)) ? montoTotal : (montoTotal / 1.18);
        double igv = ("20".equals(tipoIgv) || "30".equals(tipoIgv)) ? 0.0 : (montoTotal - valorUnitario);

        // --- Totales ---
        Map<String, Object> totales = new HashMap<>();
        totales.put("total_exportacion", 0);
        totales.put("total_operaciones_gravadas", ("10".equals(tipoIgv)) ? Math.round(valorUnitario * 100.0) / 100.0 : 0);
        totales.put("total_operaciones_exoneradas", "20".equals(tipoIgv) ? Math.round(montoTotal * 100.0) / 100.0 : 0);
        totales.put("total_operaciones_inafectas", "30".equals(tipoIgv) ? Math.round(montoTotal * 100.0) / 100.0 : 0);
        totales.put("total_operaciones_gratuitas", 0);
        totales.put("total_igv", Math.round(igv * 100.0) / 100.0);
        totales.put("total_impuestos", Math.round(igv * 100.0) / 100.0);
        totales.put("total_valor", Math.round(valorUnitario * 100.0) / 100.0);
        totales.put("total_venta", Math.round(montoTotal * 100.0) / 100.0);
        payload.put("totales", totales);

        // --- Ítems ---
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();

        item.put("codigo_interno", codigoLote);
        item.put("codigo_producto_sunat", "51121703");
        item.put("unidad_de_medida", "NIU");
        item.put("descripcion", descripcionDinamica);
        item.put("cantidad", 1);
        item.put("valor_unitario", Math.round(valorUnitario * 100.0) / 100.0);
        item.put("codigo_tipo_precio", "01");
        item.put("codigo_tipo_afectacion_igv", tipoIgv);
        item.put("porcentaje_igv", "10".equals(tipoIgv) ? 18 : 0);
        item.put("precio_unitario", Math.round((valorUnitario + igv) * 100.0) / 100.0);
        item.put("total_base_igv", Math.round(valorUnitario * 100.0) / 100.0);
        item.put("total_igv", Math.round(igv * 100.0) / 100.0);
        item.put("total_impuestos", Math.round(igv * 100.0) / 100.0);
        item.put("total_valor_item", Math.round(valorUnitario * 100.0) / 100.0);
        item.put("total_item", Math.round(montoTotal * 100.0) / 100.0);
        items.add(item);

        payload.put("items", items);

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