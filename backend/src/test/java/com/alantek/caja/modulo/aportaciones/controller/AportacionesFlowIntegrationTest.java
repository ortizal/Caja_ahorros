package com.alantek.caja.modulo.aportaciones.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AportacionesFlowIntegrationTest {

    private static final String PERIODO = "2026-09";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void configGeneracionYPagoDeAportacion() throws Exception {
        String token = loginToken("cajero", "cajero123");

        mvc.perform(get("/api/v1/aportaciones/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/aportaciones/generar")
                        .header("Authorization", "Bearer " + token)
                        .param("periodo", PERIODO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generadas").isNumber());

        abrirCaja(token);

        MvcResult lista = mvc.perform(get("/api/v1/aportaciones")
                        .header("Authorization", "Bearer " + token)
                        .param("periodo", PERIODO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].periodo").value(PERIODO))
                .andReturn();
        JsonNode primera = objectMapper.readTree(lista.getResponse().getContentAsString()).get("content").get(0);
        Long aportacionId = primera.get("id").asLong();
        String montoEsperado = primera.get("montoEsperado").decimalValue().toPlainString();

        mvc.perform(post("/api/v1/aportaciones/" + aportacionId + "/pagos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":" + montoEsperado + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comprobanteNumero").isNotEmpty())
                .andExpect(jsonPath("$.cajaMovimientoId").isNumber());

        mvc.perform(get("/api/v1/aportaciones/" + aportacionId + "/pagos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(get("/api/v1/aportaciones")
                        .header("Authorization", "Bearer " + token)
                        .param("periodo", PERIODO)
                        .param("socioId", String.valueOf(primera.get("socioId").asLong())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].estado").value("PAGADA"));
    }

    @Test
    void pagoDeAportacionYaPagadaEsRechazado() throws Exception {
        String token = loginToken("cajero", "cajero123");

        mvc.perform(post("/api/v1/aportaciones/generar")
                        .header("Authorization", "Bearer " + token)
                        .param("periodo", "2026-10"))
                .andExpect(status().isOk());

        abrirCaja(token);

        MvcResult lista = mvc.perform(get("/api/v1/aportaciones")
                        .header("Authorization", "Bearer " + token)
                        .param("periodo", "2026-10"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode primera = objectMapper.readTree(lista.getResponse().getContentAsString()).get("content").get(0);
        Long aportacionId = primera.get("id").asLong();
        String montoEsperado = primera.get("montoEsperado").decimalValue().toPlainString();

        mvc.perform(post("/api/v1/aportaciones/" + aportacionId + "/pagos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":" + montoEsperado + "}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/aportaciones/" + aportacionId + "/pagos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":5.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void periodoInvalidoEsRechazado() throws Exception {
        String token = loginToken("cajero", "cajero123");

        mvc.perform(post("/api/v1/aportaciones/generar")
                        .header("Authorization", "Bearer " + token)
                        .param("periodo", "invalido"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contadorNoPuedeCrearConfiguracionDeAportaciones() throws Exception {
        String token = loginToken("contador", "contador123");

        mvc.perform(post("/api/v1/aportaciones/config")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EXTRAORDINARIA\",\"modoCalculo\":\"FIJO\",\"valor\":10.00,"
                                + "\"periodicidad\":\"MENSUAL\",\"vigenteDesde\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
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
