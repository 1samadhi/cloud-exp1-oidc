package cl.duoc.cloud.rabbitadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import cl.duoc.cloud.rabbitadmin.service.RabbitAdminService;

/**
 * Pruebas del controlador: autorizacion (solo ADMIN) y validacion (400 con
 * datos mal formados). La logica de RabbitMQ en si se prueba en
 * RabbitAdminServiceTest, con AmqpAdmin mockeado.
 */
@SpringBootTest
class RabbitAdminV1ControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private RabbitAdminService servicio;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void sinTokenNoSePuedeCrearUnaCola() throws Exception {
        mockMvc.perform(post("/v1/admin/colas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"q.prueba\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioSinRolAdminNoPuedeCrearColas() throws Exception {
        mockMvc.perform(post("/v1/admin/colas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"q.prueba\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAdminCreaUnaColaValida() throws Exception {
        mockMvc.perform(post("/v1/admin/colas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"q.prueba\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void unNombreVacioEsRechazadoCon400() throws Exception {
        mockMvc.perform(post("/v1/admin/colas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unTipoDeExchangeInvalidoEsRechazadoCon400() throws Exception {
        org.mockito.Mockito.doThrow(new cl.duoc.cloud.rabbitadmin.service.ParametroInvalidoException("tipo invalido"))
                .when(servicio).crearExchange(any());

        mockMvc.perform(post("/v1/admin/exchanges")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"x\",\"tipo\":\"inventado\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarUnaColaQueNoExisteDa404() throws Exception {
        given(servicio.eliminarCola("no-existe")).willReturn(false);

        mockMvc.perform(delete("/v1/admin/colas/no-existe")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}
