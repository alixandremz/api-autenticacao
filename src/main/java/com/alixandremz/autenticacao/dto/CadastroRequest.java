package com.alixandremz.autenticacao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank(message = "o nome é obrigatório")
        @Size(max = 80, message = "o nome deve ter no máximo 80 caracteres")
        String nome,

        @NotBlank(message = "o e-mail é obrigatório")
        @Email(message = "e-mail inválido")
        @Size(max = 160, message = "o e-mail deve ter no máximo 160 caracteres")
        String email,

        @NotBlank(message = "a senha é obrigatória")
        @Size(min = 8, max = 100, message = "a senha deve ter entre 8 e 100 caracteres")
        String senha
) {
}
