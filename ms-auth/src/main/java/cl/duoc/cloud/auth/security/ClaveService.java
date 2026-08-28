package cl.duoc.cloud.auth.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

import jakarta.annotation.PostConstruct;

/**
 * Custodia el par de llaves RSA con el que se firman los tokens.
 *
 * La llave se persiste en disco porque el autorizador JWT de AWS API Gateway
 * cachea el JWKS: si cada reinicio generara una llave nueva, los tokens ya
 * emitidos dejarian de validar hasta que expirara esa cache.
 */
@Service
public class ClaveService {

    private static final Logger log = LoggerFactory.getLogger(ClaveService.class);

    private final Path rutaLlave;
    private RSAKey llave;

    public ClaveService(@Value("${oidc.ruta-llave}") String rutaLlave) {
        this.rutaLlave = Path.of(rutaLlave);
    }

    @PostConstruct
    void inicializar() throws Exception {
        if (Files.exists(rutaLlave)) {
            llave = RSAKey.parse(Files.readString(rutaLlave));
            log.info("Llave RSA cargada desde {} (kid={})", rutaLlave, llave.getKeyID());
            return;
        }
        llave = generar();
        try {
            Files.createDirectories(rutaLlave.getParent());
            Files.writeString(rutaLlave, llave.toJSONString());
            log.info("Llave RSA generada y guardada en {} (kid={})", rutaLlave, llave.getKeyID());
        } catch (Exception e) {
            log.warn("No se pudo persistir la llave en {}: queda solo en memoria y los tokens "
                    + "dejaran de validar tras un reinicio. Causa: {}", rutaLlave, e.getMessage());
        }
    }

    private RSAKey generar() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .keyUse(KeyUse.SIGNATURE)
                .generate();
    }

    /** Llave completa, con la parte privada. Solo para firmar. */
    public RSAKey llavePrivada() {
        return llave;
    }

    /** Conjunto de llaves publicas que se publica en /.well-known/jwks.json */
    public JWKSet jwks() {
        return new JWKSet(llave.toPublicJWK());
    }
}
