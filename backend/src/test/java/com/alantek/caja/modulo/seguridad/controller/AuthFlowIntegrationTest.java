package com.alantek.caja.modulo.seguridad.controller;

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
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginConCredencialesCorrectasDevuelveTokenYPermisos() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.permisos").isArray())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("permisos")).isNotNull();
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accesoSinTokenEsRechazado() throws Exception {
        mvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cajeroSinPermisoDeSeguridadNoPuedeListarUsuarios() throws Exception {
        String token = loginToken("cajero", "cajero123");

        mvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminConPermisoDeSeguridadListaUsuarios() throws Exception {
        String token = loginToken("admin", "admin123");

        mvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminConPermisoDeSeguridadListaPermisos() throws Exception {
        String token = loginToken("admin", "admin123");

        mvc.perform(get("/api/v1/permisos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber())
                .andExpect(jsonPath("$[0].modulo").isNotEmpty());
    }

    @Test
    void cajeroSinPermisoDeSeguridadNoPuedeListarPermisos() throws Exception {
        String token = loginToken("cajero", "cajero123");

        mvc.perform(get("/api/v1/permisos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cajeroConPermisoDeCajaPuedeListarMovimientosDeSuCaja() throws Exception {
        String token = loginToken("cajero", "cajero123");

        mvc.perform(get("/api/v1/cuentas-bancarias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenInvalidoEsRechazado() throws Exception {
        mvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminarTransaccionFinancieraEstaBloqueadoEnApi() throws Exception {
        String token = loginToken("admin", "admin123");

        for (String ruta : new String[]{
                "/api/v1/socios/1",
                "/api/v1/creditos/1",
                "/api/v1/creditos/1/pagos/1",
                "/api/v1/caja/1/movimientos/1",
                "/api/v1/aportaciones/1/pagos/1",
                "/api/v1/cuentas-bancarias/1/movimientos/1",
                "/api/v1/asientos/1"}) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .delete(ruta)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().is4xxClientError());
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
