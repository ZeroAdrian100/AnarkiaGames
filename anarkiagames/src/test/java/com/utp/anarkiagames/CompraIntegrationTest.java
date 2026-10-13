package com.utp.anarkiagames;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo contra H2: seguridad, administrador inicial, consultas JPQL y reglas de compra.
 * Cada test crea su propio torneo y cliente, así no dependen entre sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompraIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private String admin;
    private long torneoId;

    @BeforeEach
    void setUp() throws Exception {
        admin = login("admin@test.com", "admin12345");
        torneoId = crearTorneo(LocalDateTime.now().plusDays(10));
        crearTipoTicket(torneoId, "ESPECTADOR", 10, 2000);
        crearTipoTicket(torneoId, "COMPETIDOR", 2, 5000);
    }

    @Test
    void espectadorCompraVariasVecesYElStockBaja() throws Exception {
        final String cliente = registrar();

        comprar(cliente, torneoId, """
                {"tipo":"ESPECTADOR","cantidad":3,"tokenId":"tok_ok"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.monto").value(6000));
        comprar(cliente, torneoId, """
                {"tipo":"ESPECTADOR","cantidad":2,"tokenId":"tok_ok"}""")
                .andExpect(status().isCreated());

        mvc.perform(get("/torneos/" + torneoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiposTicket[?(@.tipo == 'ESPECTADOR')].disponibles").value(contains(5)))
                .andExpect(jsonPath("$.tiposTicket[?(@.tipo == 'COMPETIDOR')].disponibles").value(contains(2)));

        mvc.perform(get("/torneos/mis-inscripciones").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void espectadorNoPuedeSerCompetidorEnElMismoTorneo() throws Exception {
        final String cliente = registrar();
        comprar(cliente, torneoId, """
                {"tipo":"ESPECTADOR","tokenId":"tok_ok"}""")
                .andExpect(status().isCreated());

        comprar(cliente, torneoId, """
                {"tipo":"COMPETIDOR","tokenId":"tok_ok"}""")
                .andExpect(status().isConflict());
    }

    @Test
    void competidorSoloSeInscribeUnaVez() throws Exception {
        final String cliente = registrar();
        comprar(cliente, torneoId, """
                {"tipo":"COMPETIDOR","tokenId":"tok_ok"}""")
                .andExpect(status().isCreated());

        comprar(cliente, torneoId, """
                {"tipo":"COMPETIDOR","tokenId":"tok_ok"}""")
                .andExpect(status().isConflict());
    }

    @Test
    void sinCuposDevuelve409() throws Exception {
        comprar(registrar(), torneoId, """
                {"tipo":"COMPETIDOR","tokenId":"tok_ok"}""").andExpect(status().isCreated());
        comprar(registrar(), torneoId, """
                {"tipo":"COMPETIDOR","tokenId":"tok_ok"}""").andExpect(status().isCreated());

        comprar(registrar(), torneoId, """
                {"tipo":"COMPETIDOR","tokenId":"tok_ok"}""")
                .andExpect(status().isConflict());
    }

    @Test
    void pagoRechazadoDevuelve402YNoGuardaNada() throws Exception {
        final String cliente = registrar();
        comprar(cliente, torneoId, """
                {"tipo":"ESPECTADOR","cantidad":2,"tokenId":"tok_rechazado"}""")
                .andExpect(status().isPaymentRequired());

        mvc.perform(get("/torneos/mis-inscripciones").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void compraSinTokenDevuelve401() throws Exception {
        mvc.perform(post("/torneos/" + torneoId + "/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"ESPECTADOR"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenNoSirveComoAccessToken() throws Exception {
        final String respuesta = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson(emailUnico())))
                .andReturn().getResponse().getContentAsString();
        final String refresh = JsonPath.read(respuesta, "$.refresh_token");

        mvc.perform(get("/torneos/mis-inscripciones").header(HttpHeaders.AUTHORIZATION, "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refresh").header(HttpHeaders.AUTHORIZATION, "Bearer " + refresh))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoAccedeAAdmin() throws Exception {
        mvc.perform(post("/admin/torneos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + registrar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(torneoJson(LocalDateTime.now().plusDays(5))))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroValidaYRechazaEmailDuplicado() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ana","email":"no-es-email","password":"123"}"""))
                .andExpect(status().isBadRequest());

        final String email = emailUnico();
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registroJson(email)))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registroJson(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.com","password":"incorrecta"}"""))
                .andExpect(status().isUnauthorized());
    }

    // ---------- utilidades ----------

    private ResultActions comprar(final String token, final long torneo, final String json) throws Exception {
        return mvc.perform(post("/torneos/" + torneo + "/inscripciones")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String login(final String email, final String password) throws Exception {
        final String respuesta = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.access_token");
    }

    private String registrar() throws Exception {
        final String respuesta = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson(emailUnico())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.access_token");
    }

    private long crearTorneo(final LocalDateTime fechaInicio) throws Exception {
        final String respuesta = mvc.perform(post("/admin/torneos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(torneoJson(fechaInicio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private void crearTipoTicket(final long torneo, final String tipo, final int stock, final long precio) throws Exception {
        mvc.perform(post("/admin/torneos/" + torneo + "/tipos-ticket")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"%s","stockMaximo":%d,"precio":%d}""".formatted(tipo, stock, precio)))
                .andExpect(status().isCreated());
    }

    private static String torneoJson(final LocalDateTime fechaInicio) {
        return """
                {"nombre":"Copa de prueba","juego":"Tekken 8","fechaInicio":"%s"}"""
                .formatted(fechaInicio.withNano(0));
    }

    private static String registroJson(final String email) {
        return """
                {"name":"Cliente","email":"%s","password":"secreta123"}""".formatted(email);
    }

    private static String emailUnico() {
        return "cliente-" + UUID.randomUUID() + "@test.com";
    }
}
