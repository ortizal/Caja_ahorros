package com.alantek.caja.modulo.reportes.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReporteIntegrationTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sociosCsvDevuelveCsv() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult result = mvc.perform(get("/api/v1/reportes/socios.csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"socios.csv\""))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertThat(csv).contains("Codigo");
        assertThat(csv).contains("Identificacion");
        assertThat(csv).contains("SOC-DEMO-01");
    }

    @Test
    void sociosXlsxBienFormado() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult result = mvc.perform(get("/api/v1/reportes/socios.xlsx")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(XLSX))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"socios.xlsx\""))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 0x50);
        assertThat(bytes[1]).isEqualTo((byte) 0x4B);
    }

    @Test
    void carteraPdfBienFormado() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult result = mvc.perform(get("/api/v1/reportes/cartera.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"cartera.pdf\""))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) '%');
        assertThat(bytes[1]).isEqualTo((byte) 'P');
        assertThat(bytes[2]).isEqualTo((byte) 'D');
        assertThat(bytes[3]).isEqualTo((byte) 'F');
    }

    @Test
    void cajaXlsxBienFormado() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult result = mvc.perform(get("/api/v1/reportes/caja.xlsx")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(XLSX))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"caja.xlsx\""))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 0x50);
        assertThat(bytes[1]).isEqualTo((byte) 0x4B);
    }

    @Test
    void cajaCsvIncluyeEncabezados() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult result = mvc.perform(get("/api/v1/reportes/caja.csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"caja.csv\""))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("SaldoInicial");
    }

    @Test
    void reportesRequierenPermisoEspecifico() throws Exception {
        String token = loginToken("socio", "socio123");

        mvc.perform(get("/api/v1/reportes/socios.csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reportes/cartera.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reportes/caja.xlsx")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportesRequierenAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/reportes/socios.xlsx"))
                .andExpect(status().isForbidden());
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
