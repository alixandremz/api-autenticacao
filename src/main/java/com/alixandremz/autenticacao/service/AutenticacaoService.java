package com.alixandremz.autenticacao.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alixandremz.autenticacao.dto.CadastroRequest;
import com.alixandremz.autenticacao.dto.LoginRequest;
import com.alixandremz.autenticacao.dto.TokenResponse;
import com.alixandremz.autenticacao.dto.UsuarioResumo;
import com.alixandremz.autenticacao.model.Papel;
import com.alixandremz.autenticacao.model.Usuario;
import com.alixandremz.autenticacao.repository.UsuarioRepository;

@Service
public class AutenticacaoService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;

    public AutenticacaoService(UsuarioRepository repositorio, PasswordEncoder encoder, TokenService tokenService) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UsuarioResumo cadastrar(CadastroRequest pedido) {
        String email = pedido.email().trim().toLowerCase();
        if (repositorio.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("e-mail já cadastrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(pedido.nome().trim());
        usuario.setEmail(email);
        usuario.setSenha(encoder.encode(pedido.senha()));
        usuario.setPapel(Papel.USER);
        return UsuarioResumo.de(repositorio.save(usuario));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest pedido) {
        Usuario usuario = repositorio.findByEmailIgnoreCase(pedido.email().trim())
                .orElseThrow(() -> new CredenciaisInvalidasException("e-mail ou senha inválidos"));
        // a mesma mensagem para e-mail inexistente e senha errada evita revelar quais e-mails existem
        if (!encoder.matches(pedido.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("e-mail ou senha inválidos");
        }
        return tokenService.gerar(usuario);
    }
}
