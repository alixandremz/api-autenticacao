package com.alixandremz.autenticacao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.alixandremz.autenticacao.model.Papel;
import com.alixandremz.autenticacao.model.Usuario;
import com.alixandremz.autenticacao.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ApiAutenticacaoTests {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder encoder;

    @BeforeEach
    void limpar() {
        usuarios.deleteAll();
    }

    private void cadastrar(String nome, String email, String senha) throws Exception {
        mvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isCreated());
    }

    private String login(String email, String senha) throws Exception {
        String resposta = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = TOKEN.matcher(resposta);
        matcher.find();
        return matcher.group(1);
    }

    private void criarAdmin(String nome, String email, String senha) {
        Usuario admin = new Usuario();
        admin.setNome(nome);
        admin.setEmail(email);
        admin.setSenha(encoder.encode(senha));
        admin.setPapel(Papel.ADMIN);
        usuarios.save(admin);
    }

    @Test
    void cadastroRetorna201() throws Exception {
        mvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("USER"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void cadastroComEmailDuplicadoRetorna409() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", "senha123");
        mvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outra\",\"email\":\"ana@exemplo.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cadastroComSenhaCurtaRetorna400() throws Exception {
        mvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", "senha123");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@exemplo.com\",\"senha\":\"errada123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginComEmailInexistenteRetorna401() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem@exemplo.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRetornaToken() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", "senha123");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@exemplo.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty());
    }

    @Test
    void meSemTokenRetorna401() throws Exception {
        mvc.perform(get("/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meComTokenInvalidoRetorna401() throws Exception {
        mvc.perform(get("/usuarios/me").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meComTokenRetornaUsuarioLogado() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", "senha123");
        String token = login("ana@exemplo.com", "senha123");

        mvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"));
    }

    @Test
    void adminComUsuarioComumRetorna403() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", "senha123");
        String token = login("ana@exemplo.com", "senha123");

        mvc.perform(get("/admin/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminComAdminRetorna200() throws Exception {
        criarAdmin("Root", "root@exemplo.com", "senha123");
        String token = login("root@exemplo.com", "senha123");

        mvc.perform(get("/admin/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("root@exemplo.com"));
    }
}
