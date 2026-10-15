package com.alixandremz.autenticacao.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alixandremz.autenticacao.dto.CadastroRequest;
import com.alixandremz.autenticacao.dto.LoginRequest;
import com.alixandremz.autenticacao.dto.TokenResponse;
import com.alixandremz.autenticacao.dto.UsuarioResumo;
import com.alixandremz.autenticacao.service.AutenticacaoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AutenticacaoService servico;

    public AuthController(AutenticacaoService servico) {
        this.servico = servico;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResumo> cadastrar(@Valid @RequestBody CadastroRequest pedido) {
        UsuarioResumo criado = servico.cadastrar(pedido);
        return ResponseEntity.created(URI.create("/usuarios/" + criado.id())).body(criado);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest pedido) {
        return servico.login(pedido);
    }
}
