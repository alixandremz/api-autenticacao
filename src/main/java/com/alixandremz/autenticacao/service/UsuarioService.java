package com.alixandremz.autenticacao.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alixandremz.autenticacao.dto.UsuarioResumo;
import com.alixandremz.autenticacao.model.Usuario;
import com.alixandremz.autenticacao.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;

    public UsuarioService(UsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public UsuarioResumo buscarPorEmail(String email) {
        return UsuarioResumo.de(obterPorEmail(email));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumo> listar() {
        return repositorio.findAllByOrderByNomeAsc().stream().map(UsuarioResumo::de).toList();
    }

    public Usuario obterPorEmail(String email) {
        return repositorio.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NaoEncontradoException("usuário não encontrado: " + email));
    }
}
