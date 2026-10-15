package com.utp.anarkiagames;

import com.jayway.jsonpath.JsonPath;
import com.utp.anarkiagames.repository.TokenRecuperacionRepository;
import com.utp.anarkiagames.service.CompraConfirmadaEvent;
import com.utp.anarkiagames.service.CorreoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Correo de confirmación de compra y recuperación de contraseña. El envío real se reemplaza por un mock. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorreoIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private TokenRecuperacionRepository tokenRepository;
    @MockitoBean
    private CorreoService correoService;

    @Test
    void laCompraEnviaUnaConfirmacionConLosCodigos() throws Exception {
        final String admin = login("admin@test.com", "admin12345").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        final String tokenAdmin = JsonPath.read(admin, "$.access_token");
        final String torneo = mvc.perform(post("/admin/torneos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Copa Correo","juego":"Tekken 8","fechaInicio":"%s"}"""
                                .formatted(LocalDateTime.now().plusDays(10).withNano(0))))
                .andReturn().getResponse().getContentAsString();
        final long torneoId = ((Number) JsonPath.read(torneo, "$.id")).longValue();
        mvc.perform(post("/admin/torneos/" + torneoId + "/tipos-ticket")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"ESPECTADOR","stockMaximo":10,"precio":2500}"""))
                .andExpect(status().isCreated());

        final String email = emailUnico();
        final String cliente = registrar(email);
        mvc.perform(post("/torneos/" + torneoId + "/inscripciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"ESPECTADOR","cantidad":2,"tokenId":"tok_ok"}"""))
                .andExpect(status().isCreated());

        final ArgumentCaptor<CompraConfirmadaEvent> captor = ArgumentCaptor.forClass(CompraConfirmadaEvent.class);
        verify(correoService, timeout(2000)).enviarConfirmacionCompra(captor.capture());
        final CompraConfirmadaEvent compra = captor.getValue();
        assertEquals(email, compra.email());
        assertEquals(2, compra.codigos().size());
        assertEquals(5000, compra.monto());
    }

    @Test
    void restablecerPasswordConElEnlaceDelCorreo() throws Exception {
        final String email = emailUnico();
        registrar(email);

        final String token = solicitarToken(email);
        restablecer(token, "nuevaClave99").andExpect(status().isOk());

        login(email, "nuevaClave99").andExpect(status().isOk());
        login(email, "secreta123").andExpect(status().isUnauthorized());
        restablecer(token, "otraClave99").andExpect(status().isBadRequest());
    }

    @Test
    void unPedidoNuevoInvalidaElEnlaceAnterior() throws Exception {
        final String email = emailUnico();
        registrar(email);

        final String primero = solicitarToken(email);
        final String segundo = solicitarToken(email);

        restablecer(primero, "nuevaClave99").andExpect(status().isBadRequest());
        restablecer(segundo, "nuevaClave99").andExpect(status().isOk());
    }

    @Test
    void enlaceExpiradoNoSirve() throws Exception {
        final String email = emailUnico();
        registrar(email);
        final String token = solicitarToken(email);

        tokenRepository.findAll().stream()
                .filter(t -> t.getUsuario().getEmail().equals(email))
                .forEach(t -> {
                    t.setExpiraEn(LocalDateTime.now().minusMinutes(1));
                    tokenRepository.save(t);
                });

        restablecer(token, "nuevaClave99").andExpect(status().isBadRequest());
    }

    @Test
    void emailNoRegistradoRespondeIgualYNoEnviaNada() throws Exception {
        mvc.perform(post("/auth/olvide-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}""".formatted(emailUnico())))
                .andExpect(status().isOk());
        verify(correoService, never()).enviarRecuperacion(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void tokenInventadoYPasswordCortaSeRechazan() throws Exception {
        restablecer("no-existe", "nuevaClave99").andExpect(status().isBadRequest());
        restablecer("no-existe", "corta").andExpect(status().isBadRequest());
    }

    // ---------- utilidades ----------

    private String solicitarToken(final String email) throws Exception {
        mvc.perform(post("/auth/olvide-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}""".formatted(email)))
                .andExpect(status().isOk());
        final ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correoService, org.mockito.Mockito.atLeastOnce())
                .enviarRecuperacion(eq(email), any(), enlace.capture(), eq(15L));
        final String ultimo = enlace.getValue();
        assertTrue(ultimo.startsWith("http://localhost:5173/restablecer-password?token="));
        return ultimo.substring(ultimo.indexOf("token=") + "token=".length());
    }

    private ResultActions restablecer(final String token, final String password) throws Exception {
        return mvc.perform(post("/auth/restablecer-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"%s","password":"%s"}""".formatted(token, password)));
    }

    private ResultActions login(final String email, final String password) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password)));
    }

    private String registrar(final String email) throws Exception {
        final String respuesta = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cliente","email":"%s","password":"secreta123"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.access_token");
    }

    private static String emailUnico() {
        return "cliente-" + UUID.randomUUID() + "@test.com";
    }
}
