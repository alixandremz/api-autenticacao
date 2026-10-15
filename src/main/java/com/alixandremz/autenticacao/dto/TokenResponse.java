package com.alixandremz.autenticacao.dto;

import java.time.Instant;

public record TokenResponse(String token, String tipo, Instant expiraEm) {

    public static TokenResponse de(String token, Instant expiraEm) {
        return new TokenResponse(token, "Bearer", expiraEm);
    }
}
