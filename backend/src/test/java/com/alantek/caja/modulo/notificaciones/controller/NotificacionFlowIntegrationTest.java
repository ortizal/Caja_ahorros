package com.alantek.caja.modulo.notificaciones.controller;

import com.alantek.caja.modulo.aportaciones.entity.Aportacion;
import com.alantek.caja.modulo.aportaciones.repository.AportacionConfigRepository;
import com.alantek.caja.modulo.aportaciones.repository.AportacionRepository;
import com.alantek.caja.modulo.caja.entity.CajaApertura;
import com.alantek.caja.modulo.caja.repository.CajaAperturaRepository;
import com.alantek.caja.modulo.creditos.entity.Credito;
import com.alantek.caja.modulo.creditos.entity.CuotaCredito;
import com.alantek.caja.modulo.creditos.entity.ProductoCredito;
import com.alantek.caja.modulo.creditos.repository.CreditoRepository;
import com.alantek.caja.modulo.creditos.repository.CuotaCreditoRepository;
import com.alantek.caja.modulo.creditos.repository.ProductoCreditoRepository;
import com.alantek.caja.modulo.notificaciones.entity.Notificacion;
import com.alantek.caja.modulo.notificaciones.repository.NotificacionRepository;
import com.alantek.caja.modulo.seguridad.repository.UsuarioRepository;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificacionFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SocioRepository socioRepository;

    @Autowired
    private CreditoRepository creditoRepository;

    @Autowired
    private ProductoCreditoRepository productoCreditoRepository;

    @Autowired
    private CuotaCreditoRepository cuotaRepository;

    @Autowired
    private AportacionRepository aportacionRepository;

    @Autowired
    private AportacionConfigRepository aportacionConfigRepository;

    @Autowired
    private CajaAperturaRepository cajaAperturaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Test
    void generarAlertasCreaNotificacionesYRespectaDeduplicacion() throws Exception {
        String tokenAdmin = loginToken("admin", "admin123");
        String tokenCajero = loginToken("cajero", "cajero123");

        long adminUserId = usuarioRepository.findByUsername("admin").orElseThrow().getId();
        long cajeroUserId = usuarioRepository.findByUsername("cajero").orElseThrow().getId();

        long socioAdminId = crearSocio("S-9001", "9001000001", adminUserId);
        long socioCajeroId = crearSocio("S-9002", "9001000002", cajeroUserId);

        Credito credito = nuevoCredito(socioAdminId);
        creditoRepository.save(credito);

        LocalDate hoy = LocalDate.now();
        CuotaCredito vencidaConMora = nuevaCuota(credito.getId(), 1, hoy.minusDays(5), "VENCIDA", "50.00");
        CuotaCredito vencidaSinMora = nuevaCuota(credito.getId(), 2, hoy.minusDays(3), "VENCIDA", "50.00");
        CuotaCredito proxima = nuevaCuota(credito.getId(), 3, hoy.plusDays(3), "PENDIENTE", "50.00");
        vencidaConMora.setMora(new BigDecimal("2.50"));
        vencidaConMora = cuotaRepository.save(vencidaConMora);
        vencidaSinMora = cuotaRepository.save(vencidaSinMora);
        proxima = cuotaRepository.save(proxima);

        Aportacion aportacion = new Aportacion();
        aportacion.setSocioId(socioAdminId);
        aportacion.setConfigId(aportacionConfigRepository.findAll().get(0).getId());
        aportacion.setPeriodo(hoy.getYear() + "-" + (hoy.getMonthValue() < 10 ? "0" : "") + hoy.getMonthValue());
        aportacion.setMontoEsperado(new BigDecimal("25.00"));
        aportacion.setEstado("PENDIENTE");
        aportacion = aportacionRepository.save(aportacion);

        CajaApertura caja = new CajaApertura();
        caja.setCajeroId(cajeroUserId);
        caja.setFecha(hoy.minusDays(1));
        caja.setSaldoInicial(new BigDecimal("100.00"));
        caja.setEstado("ABIERTA");
        cajaAperturaRepository.save(caja);

        mvc.perform(post("/api/v1/notificaciones/generar")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        Set<String> tiposAdmin = tiposDe(tokenAdmin);
        assertThat(tiposAdmin)
                .contains("CUOTA_PROXIMA", "CUOTA_VENCIDA", "MORA", "APORTACION_PENDIENTE");

        Set<String> tiposCajero = tiposDe(tokenCajero);
        assertThat(tiposCajero).contains("CIERRE_PENDIENTE");

        long totalAntes = notificacionRepository.count();
        mvc.perform(post("/api/v1/notificaciones/generar")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        assertThat(notificacionRepository.count()).isEqualTo(totalAntes);

        long noLeidas = noLeidasDe(tokenAdmin);
        Set<Long> refsPropias = Set.of(
                vencidaConMora.getId(), vencidaSinMora.getId(), proxima.getId(), aportacion.getId());
        long noLeidasPropias = notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(adminUserId).stream()
                .filter(n -> refsPropias.contains(n.getReferenciaId()))
                .count();
        assertThat(noLeidasPropias).isEqualTo(4);
        assertThat(noLeidas).isGreaterThanOrEqualTo(4);

        Notificacion adminNotificacion = notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(adminUserId).get(0);
        mvc.perform(post("/api/v1/notificaciones/" + adminNotificacion.getId() + "/leida")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));

        assertThat(noLeidasDe(tokenAdmin)).isEqualTo(noLeidas - 1);

        mvc.perform(post("/api/v1/notificaciones/leidas")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        assertThat(noLeidasDe(tokenAdmin)).isZero();
    }

    @Test
    void noPermiteMarcarLeidaUnaNotificacionAjena() throws Exception {
        String tokenAdmin = loginToken("admin", "admin123");
        String tokenCajero = loginToken("cajero", "cajero123");
        long cajeroUserId = usuarioRepository.findByUsername("cajero").orElseThrow().getId();
        long socioCajeroId = crearSocio("S-9003", "9001000003", cajeroUserId);

        LocalDate hoy = LocalDate.now();
        CajaApertura caja = new CajaApertura();
        caja.setCajeroId(cajeroUserId);
        caja.setFecha(hoy.minusDays(1));
        caja.setSaldoInicial(new BigDecimal("100.00"));
        caja.setEstado("ABIERTA");
        cajaAperturaRepository.save(caja);

        mvc.perform(post("/api/v1/notificaciones/generar")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        Notificacion delCajero = notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(cajeroUserId).get(0);

        mvc.perform(post("/api/v1/notificaciones/" + delCajero.getId() + "/leida")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest());

        assertThat(notificacionRepository.findByIdAndUsuarioId(delCajero.getId(), cajeroUserId).orElseThrow().isLeida())
                .isFalse();
    }

    @Test
    void generarRequiereRolAdminYListarRequiereAutenticacion() throws Exception {
        String tokenCajero = loginToken("cajero", "cajero123");

        mvc.perform(post("/api/v1/notificaciones/generar")
                        .header("Authorization", "Bearer " + tokenCajero))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/notificaciones"))
                .andExpect(status().isForbidden());
    }

    private long crearSocio(String codigo, String identificacion, Long usuarioId) {
        Socio socio = new Socio();
        socio.setCodigo(codigo);
        socio.setIdentificacion(identificacion);
        socio.setNombres("Prueba");
        socio.setApellidos("Notificaciones");
        socio.setFechaIngreso(LocalDate.now());
        socio.setEstado("ACTIVO");
        socio.setUsuarioId(usuarioId);
        return socioRepository.save(socio).getId();
    }

    private Credito nuevoCredito(Long socioId) {
        Credito credito = new Credito();
        credito.setSocioId(socioId);
        credito.setProductoId(productoCreditoRepository.findAll().get(0).getId());
        credito.setMontoDesembolsado(new BigDecimal("500.00"));
        credito.setTasaInteres(new BigDecimal("18.0000"));
        credito.setPlazoMeses(12);
        credito.setSaldoCapital(new BigDecimal("500.00"));
        credito.setEstado("VIGENTE");
        return credito;
    }

    private CuotaCredito nuevaCuota(Long creditoId, int numero, LocalDate vencimiento, String estado, String capital) {
        CuotaCredito cuota = new CuotaCredito();
        cuota.setCreditoId(creditoId);
        cuota.setNumeroCuota(numero);
        cuota.setFechaVencimiento(vencimiento);
        cuota.setCapital(new BigDecimal(capital));
        cuota.setInteres(BigDecimal.ZERO);
        cuota.setCuotaTotal(new BigDecimal(capital));
        cuota.setSaldoCapital(new BigDecimal(capital));
        cuota.setMora(BigDecimal.ZERO);
        cuota.setEstado(estado);
        return cuota;
    }

    private Set<String> tiposDe(String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/notificaciones")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = objectMapper.readTree(result.getResponse().getContentAsString());
        Set<String> tipos = new HashSet<>();
        for (JsonNode item : lista) {
            tipos.add(item.get("tipo").asText());
        }
        return tipos;
    }

    private long noLeidasDe(String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/notificaciones/no-leidas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).asLong();
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
