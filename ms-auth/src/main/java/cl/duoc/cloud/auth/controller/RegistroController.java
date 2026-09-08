package cl.duoc.cloud.auth.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.auth.dto.RegistroDTO;
import cl.duoc.cloud.auth.graph.GraphService;

/**
 * Registro de usuarios en el tenant de Entra ID.
 *
 * Sustituye al flujo de autoservicio de Entra External ID, que no se pudo usar
 * porque Azure for Students restringe las regiones de despliegue y ninguna
 * coincide con las que admite un directorio CIAM. Ver docs/02-entra-id.md.
 *
 * El endpoint es anonimo a proposito: quien se registra todavia no tiene cuenta
 * y por lo tanto no puede presentar un token.
 */
@RestController
@RequestMapping("/auth")
public class RegistroController {

    private static final Logger log = LoggerFactory.getLogger(RegistroController.class);

    private final GraphService graph;

    public RegistroController(GraphService graph) {
        this.graph = graph;
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registrar(@RequestBody RegistroDTO peticion) {
        if (!graph.configurado()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "El registro no esta configurado en este entorno"));
        }
        if (peticion.nombre() == null || peticion.nombre().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio"));
        }
        if (!GraphService.aliasValido(peticion.usuario())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "El usuario admite entre 3 y 31 caracteres: minusculas, numeros, punto y guion"));
        }
        if (peticion.password() == null || peticion.password().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La contrasenia necesita al menos 8 caracteres"));
        }

        try {
            String upn = graph.crearUsuario(peticion.nombre(), peticion.usuario(), peticion.password());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "usuario", upn,
                    "mensaje", "Cuenta creada. Ya puedes iniciar sesion con Microsoft."));
        } catch (Exception e) {
            log.warn("Fallo el registro de {}: {}", peticion.usuario(), e.getMessage());
            // Graph responde 400 tanto si el usuario existe como si la
            // contrasenia no cumple la politica del tenant.
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No se pudo crear la cuenta. Puede que el usuario ya exista "
                            + "o que la contrasenia no cumpla la politica del directorio."));
        }
    }
}
