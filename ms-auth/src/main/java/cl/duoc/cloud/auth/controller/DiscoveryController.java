package cl.duoc.cloud.auth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.auth.security.ClaveService;

/**
 * Metadatos OIDC. El autorizador JWT de AWS API Gateway consulta
 * /.well-known/openid-configuration del issuer, lee de ahi el jwks_uri y con
 * esas llaves publicas verifica la firma de cada token entrante.
 */
@RestController
public class DiscoveryController {

    private final ClaveService claves;
    private final String issuer;

    public DiscoveryController(ClaveService claves, @Value("${oidc.issuer}") String issuer) {
        this.claves = claves;
        this.issuer = issuer;
    }

    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> configuracion() {
        return Map.of(
                "issuer", issuer,
                "token_endpoint", issuer + "/auth/login",
                "userinfo_endpoint", issuer + "/auth/userinfo",
                "jwks_uri", issuer + "/.well-known/jwks.json",
                "response_types_supported", List.of("token", "id_token"),
                "grant_types_supported", List.of("password"),
                "subject_types_supported", List.of("public"),
                "id_token_signing_alg_values_supported", List.of("RS256"),
                "scopes_supported", List.of("openid", "productos.leer", "productos.escribir",
                        "pedidos.leer", "pedidos.escribir"),
                "claims_supported", List.of("sub", "iss", "aud", "exp", "iat", "jti",
                        "email", "name", "preferred_username", "roles", "scope"));
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return claves.jwks().toJSONObject();
    }
}
