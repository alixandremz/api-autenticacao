package com.alixandremz.autenticacao.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alixandremz.autenticacao.dto.UsuarioResumo;
import com.alixandremz.autenticacao.service.UsuarioService;

// protegido por "hasRole("ADMIN")" no SecurityFilterChain
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioService servico;

    public AdminController(UsuarioService servico) {
        this.servico = servico;
    }

    @GetMapping("/usuarios")
    public List<UsuarioResumo> listar() {
        return servico.listar();
    }
}
