package cl.duoc.cloud.facturacion.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import cl.duoc.cloud.facturacion.model.Factura;
import cl.duoc.cloud.facturacion.repository.FacturaRepository;

@SpringBootTest
@Transactional
class FacturaV1ControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private FacturaRepository repositorio;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void elEndpointPublicoNoExigeToken() throws Exception {
        mockMvc.perform(get("/api/v1/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("ms-facturacion"));
    }

    @Test
    void sinTokenNoSePuedeConsultarUnaBoleta() throws Exception {
        mockMvc.perform(get("/api/v1/facturas/orden/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenSeEncuentraLaBoletaExistente() throws Exception {
        repositorio.save(new Factura(42L, "usuario-1", 15000L));

        mockMvc.perform(get("/api/v1/facturas/orden/42")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idOrden").value(42))
                .andExpect(jsonPath("$.total").value(15000));
    }

    @Test
    void unaOrdenSinBoletaDa404() throws Exception {
        mockMvc.perform(get("/api/v1/facturas/orden/999")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }
}
