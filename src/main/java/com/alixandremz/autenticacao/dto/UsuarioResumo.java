package com.alixandremz.autenticacao.dto;

import java.time.Instant;

import com.alixandremz.autenticacao.model.Papel;
import com.alixandremz.autenticacao.model.Usuario;

public record UsuarioResumo(Long id, String nome, String email, Papel papel, Instant criadoEm) {

    public static UsuarioResumo de(Usuario usuario) {
        return new UsuarioResumo(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.getCriadoEm()
        );
    }
}
