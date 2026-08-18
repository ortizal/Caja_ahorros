package com.alantek.caja.modulo.caja.controller;

import com.alantek.caja.modulo.caja.entity.CajaApertura;
import com.alantek.caja.modulo.caja.repository.CajaAperturaRepository;
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
class CajaFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CajaAperturaRepository cajaAperturaRepository;

    @Test
    void aperturaMisCajasMovimientoSaldoYArqueo() throws Exception {
        cerrarCajasAbiertas();
        String token = loginToken("cajero", "cajero123");

        MvcResult apertura = mvc.perform(post("/api/v1/caja/apertura")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"saldoInicial\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andReturn();
        Long cajaId = objectMapper.readTree(apertura.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/v1/caja/mias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(cajaId));

        mvc.perform(post("/api/v1/caja/" + cajaId + "/movimientos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"DEPOSITO\",\"monto\":50.00,\"descripcion\":\"Depósito de prueba\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DEPOSITO"));

        MvcResult saldo = mvc.perform(get("/api/v1/caja/" + cajaId + "/saldo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode saldoJson = objectMapper.readTree(saldo.getResponse().getContentAsString());
        assertThat(saldoJson.get("saldoActual").decimalValue()).isEqualByComparingTo("150.00");

        mvc.perform(post("/api/v1/caja/" + cajaId + "/arqueo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"saldoFisico\":150.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diferencia").value(0.0));
    }

    @Test
    void movimientoSinCajaAbiertaEsRechazado() throws Exception {
        cerrarCajasAbiertas();
        String token = loginToken("cajero", "cajero123");

        mvc.perform(post("/api/v1/caja/999/movimientos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"DEPOSITO\",\"monto\":10.00}"))
                .andExpect(status().isBadRequest());
    }

    private void cerrarCajasAbiertas() {
        for (CajaApertura apertura : cajaAperturaRepository.findByEstadoOrderByOpenedAtAsc("ABIERTA")) {
            apertura.setEstado("CERRADA");
            apertura.setClosedAt(java.time.Instant.now());
            cajaAperturaRepository.save(apertura);
        }
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
