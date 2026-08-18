package com.alantek.caja.modulo.ahorros.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AhorrosFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void productoCuentaDepositoRetiroMovimientosYCapitalizacion() throws Exception {
        String token = loginToken("cajero", "cajero123");

        mvc.perform(get("/api/v1/productos-ahorro")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        MvcResult producto = mvc.perform(post("/api/v1/productos-ahorro")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"PROGRAMADO E2E\",\"tasaInteres\":3.5000,"
                                + "\"periodicidadCapitalizacion\":\"MENSUAL\",\"saldoMinimo\":50.00,"
                                + "\"limiteRetirosMes\":2,\"vigenteDesde\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long productoId = objectMapper.readTree(producto.getResponse().getContentAsString()).get("id").asLong();

        Long socioId = primerSocioId(token);
        abrirCaja(token);

        MvcResult cuenta = mvc.perform(post("/api/v1/cuentas-ahorro")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCuenta").isNotEmpty())
                .andExpect(jsonPath("$.saldo").value(0.0))
                .andReturn();
        Long cuentaId = objectMapper.readTree(cuenta.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/cuentas-ahorro/" + cuentaId + "/depositos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoResultante").value(100.0))
                .andExpect(jsonPath("$.comprobanteNumero").isNotEmpty());

        mvc.perform(post("/api/v1/cuentas-ahorro/" + cuentaId + "/retiros")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":30.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoResultante").value(70.0));

        mvc.perform(get("/api/v1/cuentas-ahorro/" + cuentaId + "/movimientos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        MvcResult capitalizacion = mvc.perform(post("/api/v1/ahorros/capitalizar")
                        .header("Authorization", "Bearer " + token)
                        .param("anio", "2030")
                        .param("mes", "12"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode cap = objectMapper.readTree(capitalizacion.getResponse().getContentAsString());
        assertThat(cap.get("cuentasCapitalizadas").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(cap.get("totalInteres").decimalValue()).isGreaterThan(BigDecimal.ZERO);

        mvc.perform(get("/api/v1/cuentas-ahorro/" + cuentaId + "/movimientos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[2].tipo").value("INTERES"));
    }

    @Test
    void retiroValidaSaldoMinimoYLimiteMensual() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult producto = mvc.perform(post("/api/v1/productos-ahorro")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"PLAZO E2E\",\"tasaInteres\":4.0000,"
                                + "\"periodicidadCapitalizacion\":\"ANUAL\",\"saldoMinimo\":50.00,"
                                + "\"limiteRetirosMes\":1,\"vigenteDesde\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long productoId = objectMapper.readTree(producto.getResponse().getContentAsString()).get("id").asLong();

        Long socioId = primerSocioId(token);
        abrirCaja(token);

        MvcResult cuenta = mvc.perform(post("/api/v1/cuentas-ahorro")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long cuentaId = objectMapper.readTree(cuenta.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/cuentas-ahorro/" + cuentaId + "/depositos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":60.00}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/cuentas-ahorro/" + cuentaId + "/retiros")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":20.00}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/cuentas-ahorro/" + cuentaId + "/retiros")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":10.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoResultante").value(50.0));

        mvc.perform(post("/api/v1/cuentas-ahorro/" + cuentaId + "/retiros")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void capitalizacionDuplicadaEsRechazada() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult producto = mvc.perform(post("/api/v1/productos-ahorro")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"VISTA E2E\",\"tasaInteres\":2.0000,"
                                + "\"periodicidadCapitalizacion\":\"MENSUAL\",\"vigenteDesde\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long productoId = objectMapper.readTree(producto.getResponse().getContentAsString()).get("id").asLong();

        Long socioId = primerSocioId(token);
        abrirCaja(token);

        MvcResult cuenta = mvc.perform(post("/api/v1/cuentas-ahorro")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socioId\":" + socioId + ",\"productoId\":" + productoId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long cuentaId = objectMapper.readTree(cuenta.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/cuentas-ahorro/" + cuentaId + "/depositos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":100.00}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/ahorros/capitalizar")
                        .header("Authorization", "Bearer " + token)
                        .param("anio", "2035")
                        .param("mes", "6"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/ahorros/capitalizar")
                        .header("Authorization", "Bearer " + token)
                        .param("anio", "2035")
                        .param("mes", "6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contadorNoPuedeCrearProductoDeAhorro() throws Exception {
        String token = loginToken("contador", "contador123");

        mvc.perform(post("/api/v1/productos-ahorro")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"NO DEBE CREARSE\",\"tasaInteres\":1.0000,"
                                + "\"periodicidadCapitalizacion\":\"MENSUAL\",\"vigenteDesde\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    private Long primerSocioId(String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/socios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode socio : body.get("content")) {
            if ("ACTIVO".equals(socio.get("estado").asText())) {
                return socio.get("id").asLong();
            }
        }
        throw new IllegalStateException("No hay socios ACTIVOS para la prueba");
    }

    private void abrirCaja(String token) throws Exception {
        mvc.perform(post("/api/v1/caja/apertura")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"saldoInicial\":100.00}"));
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }
}
