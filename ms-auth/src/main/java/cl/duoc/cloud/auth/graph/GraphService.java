package cl.duoc.cloud.auth.graph;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Crea usuarios en el tenant de Entra ID a traves de Microsoft Graph.
 *
 * Este servicio es la razon por la que ms-auth deja de ser un Identity Provider
 * y pasa a ser un BFF: el navegador no puede hablar con Graph directamente
 * porque haria falta un secreto de cliente, y el JavaScript de una SPA es
 * publico. El secreto vive aqui, en el servidor.
 *
 * El token se pide con el flujo client_credentials: la aplicacion se autentica
 * a si misma, sin ningun usuario de por medio, usando permisos de aplicacion
 * (User.ReadWrite.All) concedidos con consentimiento de administrador.
 */
@Service
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);
    private static final String GRAPH = "https://graph.microsoft.com/v1.0";

    private final RestClient login = RestClient.create();
    private final RestClient graph = RestClient.create();

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String dominio;
    private final String rolUsuarioId;
    private final String servicePrincipalApp;

    public GraphService(
            @Value("${graph.tenant-id:}") String tenantId,
            @Value("${graph.client-id:}") String clientId,
            @Value("${graph.client-secret:}") String clientSecret,
            @Value("${graph.dominio:}") String dominio,
            @Value("${graph.rol-usuario-id:}") String rolUsuarioId,
            @Value("${graph.sp-aplicacion:}") String servicePrincipalApp) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.dominio = dominio;
        this.rolUsuarioId = rolUsuarioId;
        this.servicePrincipalApp = servicePrincipalApp;
    }

    /** Sin configuracion de Graph el endpoint de registro responde 503. */
    public boolean configurado() {
        return !tenantId.isBlank() && !clientId.isBlank() && !clientSecret.isBlank();
    }

    private String tokenDeAplicacion() {
        MultiValueMap<String, String> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("client_id", clientId);
        cuerpo.add("client_secret", clientSecret);
        cuerpo.add("grant_type", "client_credentials");
        cuerpo.add("scope", "https://graph.microsoft.com/.default");

        @SuppressWarnings("unchecked")
        Map<String, Object> respuesta = login.post()
                .uri("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token", tenantId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(cuerpo)
                .retrieve()
                .body(Map.class);

        return respuesta == null ? null : (String) respuesta.get("access_token");
    }

    /**
     * Crea el usuario y le asigna el rol USER de la aplicacion.
     *
     * @return el userPrincipalName resultante
     */
    public String crearUsuario(String nombre, String alias, String password) {
        String token = tokenDeAplicacion();
        String upn = alias + "@" + dominio;

        Map<String, Object> nuevo = Map.of(
                "accountEnabled", true,
                "displayName", nombre,
                "mailNickname", alias,
                "userPrincipalName", upn,
                "passwordProfile", Map.of(
                        "forceChangePasswordNextSignIn", false,
                        "password", password));

        @SuppressWarnings("unchecked")
        Map<String, Object> creado = graph.post()
                .uri(GRAPH + "/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(nuevo)
                .retrieve()
                .body(Map.class);

        String idUsuario = creado == null ? null : (String) creado.get("id");
        log.info("Usuario creado en Entra ID: {} ({})", upn, idUsuario);

        if (idUsuario != null && !rolUsuarioId.isBlank() && !servicePrincipalApp.isBlank()) {
            asignarRolUsuario(token, idUsuario);
        }
        return upn;
    }

    /**
     * Sin esta asignacion el token del usuario no traeria el claim "roles" y el
     * backend no podria distinguir un cliente de un administrador.
     */
    private void asignarRolUsuario(String token, String idUsuario) {
        try {
            graph.post()
                    .uri(GRAPH + "/users/{id}/appRoleAssignments", idUsuario)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "principalId", idUsuario,
                            "resourceId", servicePrincipalApp,
                            "appRoleId", rolUsuarioId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Rol USER asignado a {}", idUsuario);
        } catch (Exception e) {
            // El usuario ya existe: se avisa pero no se deshace la creacion.
            log.warn("No se pudo asignar el rol USER a {}: {}", idUsuario, e.getMessage());
        }
    }

    /** Alias validos: minusculas, numeros, punto y guion. */
    public static boolean aliasValido(String alias) {
        return alias != null && alias.matches("[a-z0-9][a-z0-9._-]{2,30}");
    }

    public List<String> dominios() {
        return List.of(dominio);
    }
}
