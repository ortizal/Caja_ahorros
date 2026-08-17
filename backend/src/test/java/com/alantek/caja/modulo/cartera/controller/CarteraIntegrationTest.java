package com.alantek.caja.modulo.cartera.controller;

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
class CarteraIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void resumenDevuelveIndicadores() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult result = mvc.perform(get("/api/v1/dashboard/resumen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("sociosActivos").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(body.get("creditosVigentes").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(body.get("carteraColocada").decimalValue()).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO);
        assertThat(body.get("carteraVencida").decimalValue()).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO);
        assertThat(body.get("porcentajeMorosidad").decimalValue()).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO);
        assertThat(body.get("cajasAbiertas").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(body.get("disponibleCaja")).isNotNull();
        assertThat(body.get("disponibleBancos")).isNotNull();
    }

    @Test
    void resumenRequiereAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/dashboard/resumen"))
                .andExpect(status().isForbidden());
    }

    @Test
    void graficosDevuelveSeriesCompletas() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult result = mvc.perform(get("/api/v1/dashboard/graficos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode colocacion = body.get("colocacionPorMes");
        JsonNode cobranza = body.get("cobranzaPorMes");
        JsonNode flujo = body.get("flujoCajaPorMes");
        JsonNode cartera = body.get("carteraPorEstado");

        assertThat(colocacion.isArray()).isTrue();
        assertThat(colocacion.size()).isEqualTo(12);
        assertThat(cobranza.size()).isEqualTo(12);
        assertThat(flujo.size()).isEqualTo(6);
        for (JsonNode punto : colocacion) {
            assertThat(punto.get("mes").asText()).matches("\\d{4}-\\d{2}");
            assertThat(punto.get("monto").decimalValue()).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO);
        }
        for (JsonNode punto : flujo) {
            assertThat(punto.get("ingresos")).isNotNull();
            assertThat(punto.get("egresos")).isNotNull();
        }
        assertThat(cartera.isArray()).isTrue();
    }

    @Test
    void graficosRequiereAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/dashboard/graficos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarCarteraConTokenValido() throws Exception {
        String token = loginToken("credito", "credito123");

        MvcResult result = mvc.perform(get("/api/v1/cartera")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
    }

    @Test
    void listarCarteraFiltraPorEstado() throws Exception {
        String token = loginToken("credito", "credito123");

        MvcResult result = mvc.perform(get("/api/v1/cartera")
                        .param("estado", "VENCIDA")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode item : body) {
            assertThat(item.get("estado").asText()).isEqualTo("VENCIDA");
        }
    }

    @Test
    void listarCarteraRequiereAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/cartera"))
                .andExpect(status().isForbidden());
    }

    @Test
    void morosidadDevuelveIndicadores() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult result = mvc.perform(get("/api/v1/cartera/morosidad")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("cuotasVencidas").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(body.get("saldoVencido").decimalValue()).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO);
        assertThat(body.get("carteraColocada").decimalValue()).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO);
        assertThat(body.get("porcentajeMorosidad").decimalValue()).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO);
        assertThat(body.get("creditosEnMora").asInt()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void exportarCarteraDevuelveCsv() throws Exception {
        String token = loginToken("cajero", "cajero123");

        MvcResult result = mvc.perform(get("/api/v1/reportes/cartera")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"cartera.csv\""))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertThat(csv).contains("cuota_id");
        assertThat(csv).contains("dias_vencido");
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
