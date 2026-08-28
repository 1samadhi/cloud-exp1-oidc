package cl.duoc.cloud.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.auth.dto.LoginDTO;
import cl.duoc.cloud.auth.dto.TokenDTO;
import cl.duoc.cloud.auth.model.Usuario;
import cl.duoc.cloud.auth.security.TokenService;
import cl.duoc.cloud.auth.service.UsuarioService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarios;
    private final TokenService tokens;

    public AuthController(UsuarioService usuarios, TokenService tokens) {
        this.usuarios = usuarios;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO peticion) throws Exception {
        Usuario usuario = usuarios.autenticar(peticion.username(), peticion.password()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "invalid_grant",
                    "error_description", "Credenciales invalidas"));
        }
        return ResponseEntity.ok(new TokenDTO(
                tokens.accessToken(usuario),
                tokens.idToken(usuario),
                "Bearer",
                tokens.vigenciaSegundos(),
                String.join(" ", usuario.scopes())));
    }

    /** Endpoint userinfo de OIDC: devuelve los claims del token presentado. */
    @GetMapping("/userinfo")
    public Map<String, Object> userinfo(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getClaims();
    }
}
