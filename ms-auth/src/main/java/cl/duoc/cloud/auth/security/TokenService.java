package cl.duoc.cloud.auth.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import cl.duoc.cloud.auth.model.Usuario;

/**
 * Emite los tokens del Identity Provider firmados en RS256.
 *
 * Se usa RS256 y no HS256 porque el autorizador JWT de AWS API Gateway valida
 * la firma descargando las llaves publicas del JWKS: con un secreto simetrico
 * compartido no habria forma de que AWS verificara el token.
 */
@Service
public class TokenService {

    private final ClaveService claves;
    private final String issuer;
    private final String audiencia;
    private final long vigenciaSegundos;

    public TokenService(ClaveService claves,
            @Value("${oidc.issuer}") String issuer,
            @Value("${oidc.audiencia}") String audiencia,
            @Value("${oidc.vigencia-segundos}") long vigenciaSegundos) {
        this.claves = claves;
        this.issuer = issuer;
        this.audiencia = audiencia;
        this.vigenciaSegundos = vigenciaSegundos;
    }

    public String accessToken(Usuario usuario) throws JOSEException {
        return firmar(base(usuario)
                .claim("scope", String.join(" ", usuario.scopes()))
                .claim("roles", usuario.roles())
                .build());
    }

    /** ID Token de OIDC: identidad del usuario, sin permisos de acceso. */
    public String idToken(Usuario usuario) throws JOSEException {
        return firmar(base(usuario)
                .claim("email", usuario.email())
                .claim("name", usuario.nombre())
                .claim("preferred_username", usuario.username())
                .build());
    }

    public long vigenciaSegundos() {
        return vigenciaSegundos;
    }

    private JWTClaimsSet.Builder base(Usuario usuario) {
        Instant ahora = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(usuario.username())
                .audience(List.of(audiencia))
                .issueTime(java.util.Date.from(ahora))
                .expirationTime(java.util.Date.from(ahora.plusSeconds(vigenciaSegundos)))
                .jwtID(UUID.randomUUID().toString())
                .claim("email", usuario.email());
    }

    private String firmar(JWTClaimsSet claims) throws JOSEException {
        JWSHeader cabecera = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(claves.llavePrivada().getKeyID())
                .type(com.nimbusds.jose.JOSEObjectType.JWT)
                .build();
        SignedJWT jwt = new SignedJWT(cabecera, claims);
        jwt.sign(new RSASSASigner(claves.llavePrivada().toPrivateKey()));
        return jwt.serialize();
    }
}
