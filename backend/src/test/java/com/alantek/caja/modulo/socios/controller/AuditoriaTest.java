package com.alantek.caja.modulo.socios.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditoriaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Test
    void crearSocioRegistraAuditoriaConDetalleJson() throws Exception {
        String token = loginToken();

        String body = """
                {"identificacion":"1105999999","nombres":"María","apellidos":"Gómez",
                 "fechaIngreso":"2026-01-10","estado":"ACTIVO",
                 "beneficiarios":[{"nombres":"Luis Gómez","parentesco":"HIJO","porcentaje":100}]}
                """;

        MvcResult result = mvc.perform(post("/api/v1/socios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        Long socioId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        List<Auditoria> registros = auditoriaRepository.filtrar("socios",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));

        assertThat(registros)
                .filteredOn(a -> socioId.equals(a.getRegistroId()) && "CREAR".equals(a.getAccion()))
                .isNotEmpty();

        Auditoria creacion = registros.stream()
                .filter(a -> "CREAR".equals(a.getAccion()) && socioId.equals(a.getRegistroId()))
                .findFirst().orElseThrow();

        assertThat(creacion.getTablaAfectada()).isEqualTo("socios");
        assertThat(creacion.getValorNuevo()).contains("1105999999");
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
