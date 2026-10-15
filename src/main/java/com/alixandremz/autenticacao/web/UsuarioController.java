package com.alixandremz.autenticacao.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alixandremz.autenticacao.dto.UsuarioResumo;
import com.alixandremz.autenticacao.service.UsuarioService;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService servico;

    public UsuarioController(UsuarioService servico) {
        this.servico = servico;
    }

    // o "subject" do token é o e-mail do usuário; Authentication.getName() devolve ele
    @GetMapping("/me")
    public UsuarioResumo eu(Authentication autenticacao) {
        return servico.buscarPorEmail(autenticacao.getName());
    }
}
