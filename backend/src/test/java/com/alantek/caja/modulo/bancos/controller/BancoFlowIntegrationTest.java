package com.alantek.caja.modulo.bancos.controller;

import com.alantek.caja.modulo.bancos.repository.BancoMovimientoRepository;
import com.alantek.caja.modulo.seguridad.entity.Auditoria;
import com.alantek.caja.modulo.seguridad.repository.AuditoriaRepository;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BancoFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BancoMovimientoRepository movimientoRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Test
    void conciliacionYMovimientosConciliadosNoEditables() throws Exception {
        String token = loginToken();

        MvcResult cuenta = mvc.perform(post("/api/v1/cuentas-bancarias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"banco\":\"Banco Prueba\",\"numeroCuenta\":\"0099001100\","
                                + "\"tipo\":\"AHORROS\",\"saldoContable\":1000.00}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long cuentaId = objectMapper.readTree(cuenta.getResponse().getContentAsString()).get("id").asLong();

        MvcResult movimiento = mvc.perform(post("/api/v1/cuentas-bancarias/" + cuentaId + "/movimientos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"DEPOSITO\",\"monto\":200.00,\"fecha\":\"2026-08-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoContable").value(1200.0))
                .andReturn();
        Long movimientoId = objectMapper.readTree(movimiento.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/v1/conciliacion-bancaria?cuentaId=" + cuentaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodo\":\"2026-08\",\"saldoBancario\":1200.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diferencia").value(0.0));

        List<Auditoria> conciliaciones = auditoriaRepository.filtrar("conciliacion_bancaria",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));
        assertThat(conciliaciones)
                .anySatisfy(a -> {
                    assertThat(a.getAccion()).isEqualTo("CREAR");
                    assertThat(a.getUsuarioId()).isNotNull();
                    assertThat(a.getCreatedAt()).isNotNull();
                });

        mvc.perform(get("/api/v1/cuentas-bancarias/" + cuentaId + "/movimientos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(movimientoId));

        assertThat(movimientoRepository.findById(movimientoId).orElseThrow().getConciliado()).isTrue();

        mvc.perform(put("/api/v1/cuentas-bancarias/" + cuentaId + "/movimientos/" + movimientoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EGRESO\",\"monto\":999.00,\"fecha\":\"2026-08-01\"}"))
                .andExpect(status().is4xxClientError());

        mvc.perform(delete("/api/v1/cuentas-bancarias/" + cuentaId + "/movimientos/" + movimientoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());

        assertThat(movimientoRepository.findById(movimientoId).orElseThrow().getConciliado()).isTrue();
    }

    private String loginToken() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }
}
