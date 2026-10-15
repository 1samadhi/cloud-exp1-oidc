package cl.duoc.cloud.notificaciones.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import cl.duoc.cloud.notificaciones.mensajeria.PublicadorEventos;

@SpringBootTest
class NotificacionV1ControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private PublicadorEventos publicador;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void elEndpointPublicoNoExigeToken() throws Exception {
        mockMvc.perform(get("/api/v1/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("ms-notificaciones"));
    }

    @Test
    void sinTokenNoSePuedePublicarUnaPrueba() throws Exception {
        mockMvc.perform(post("/api/v1/notificaciones/prueba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenYDatosValidosPublicaLaPrueba() throws Exception {
        mockMvc.perform(post("/api/v1/notificaciones/prueba")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idOrden":1,"idUsuario":"u1","correo":"cliente@duocuc.cl","total":1000,"items":[]}
                                """))
                .andExpect(status().isAccepted());
        org.mockito.Mockito.verify(publicador).publicarCorreoDePrueba(any());
    }

    @Test
    void unCorreoMalFormadoEsRechazadoCon400() throws Exception {
        mockMvc.perform(post("/api/v1/notificaciones/prueba")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idOrden":1,"idUsuario":"u1","correo":"no-es-un-correo","total":1000,"items":[]}
                                """))
                .andExpect(status().isBadRequest());
    }
}
