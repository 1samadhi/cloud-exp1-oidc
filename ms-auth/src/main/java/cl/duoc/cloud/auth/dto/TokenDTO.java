package cl.duoc.cloud.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenDTO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        String scope) {
}
