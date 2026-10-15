# api-autenticacao

API REST de **autenticação** feita para **estudar Spring Boot, Spring Web e Spring Security**.
Foco deste projeto: **cadastro e login de usuários**, geração e validação de **JWT** e
**autorização por papel** (USER/ADMIN).

Tem uma entidade principal, **Usuario**, com senha guardada como hash e um papel.

---

## O que estou estudando aqui

| Área | Conceitos praticados |
|------|----------------------|
| Spring Security | `SecurityFilterChain`, requisições **stateless**, CSRF desabilitado |
| Senhas | `BCryptPasswordEncoder` — hash de mão única, nunca senha em texto puro |
| JWT | `JwtEncoder`/`JwtDecoder` (Nimbus), claims (`sub`, `exp`, `papel`), assinatura HMAC |
| Autorização | `hasRole("ADMIN")` e conversão da claim `papel` em autoridade `ROLE_*` |
| Erros de segurança | `AuthenticationEntryPoint` (401) e `AccessDeniedHandler` (403) em JSON |
| Spring Web (REST) | `@RestController`, `@PostMapping`/`@GetMapping`, `@RequestBody`, `ResponseEntity`, códigos HTTP (200, 201, 400, 401, 403, 409) |
| Identidade | `Authentication` para descobrir **quem** está logado a partir do token |
| Bean Validation | `@NotBlank`, `@Email`, `@Size` |
| Spring Data JPA | `JpaRepository`, `findByEmailIgnoreCase`, `existsByEmailIgnoreCase` |

---

## Stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC, Spring Data JPA, Spring Security, OAuth2 Resource Server (JWT via Nimbus)
- Bean Validation
- H2 em memória (com SQL logado)
- Maven (via Maven Wrapper `./mvnw`, não precisa ter Maven instalado)

> Sobre o JWT: em vez de adicionar uma biblioteca de terceiros (jjwt, java-jwt), o projeto usa
> o suporte **nativo** do Spring Security a JWT (`spring-boot-starter-security-oauth2-resource-server`),
> que já traz o Nimbus. Menos dependências, código mais alinhado ao framework.

---

## Modelo de dados

```
Usuario: id, nome, email (único), senha (hash BCrypt), papel, criadoEm
```

- **Papel**: enum `USER` ou `ADMIN`.
- A senha **nunca** aparece nas respostas: o DTO `UsuarioResumo` não expõe o campo.

---

## Como funciona o fluxo

```
1. POST /auth/cadastro  → cria o usuário (papel USER, senha vira hash)
2. POST /auth/login     → confere a senha e devolve um JWT assinado
3. requisições seguintes → header  Authorization: Bearer <token>
4. o filtro do Spring Security valida a assinatura e a expiração do token
5. as claims (e-mail, papel) viram a identidade e as permissões da requisição
```

O token é **stateless**: o servidor não guarda sessão; toda a informação necessária vai
assinada dentro do próprio token.

---

## Endpoints

| Método | Caminho | Descrição | Acesso | Sucesso | Erros |
|--------|---------|-----------|--------|---------|-------|
| POST | `/auth/cadastro` | cria um usuário | público | `201` | `400`, `409` |
| POST | `/auth/login` | autentica e devolve o token | público | `200` | `400`, `401` |
| GET | `/usuarios/me` | dados do usuário do token | autenticado | `200` | `401` |
| GET | `/admin/usuarios` | lista todos os usuários | ADMIN | `200` | `401`, `403` |

### Cadastro

```bash
curl -X POST http://localhost:8080/auth/cadastro \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Ana","email":"ana@exemplo.com","senha":"senha123"}'
# 201 Created
# {"id":1,"nome":"Ana","email":"ana@exemplo.com","papel":"USER","criadoEm":"..."}
```

### Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@exemplo.com","senha":"senha123"}'
```

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "expiraEm": "2026-10-15T18:00:00Z" }
```

Senha errada ou e-mail inexistente devolvem **401** com a **mesma** mensagem
(`"e-mail ou senha inválidos"`), para não revelar quais e-mails existem.

### Usando o token

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl http://localhost:8080/usuarios/me -H "Authorization: Bearer $TOKEN"
# {"id":1,"nome":"Ana","email":"ana@exemplo.com","papel":"USER","criadoEm":"..."}
```

### Endpoint só para ADMIN

```bash
curl http://localhost:8080/admin/usuarios -H "Authorization: Bearer $TOKEN"
# se o token for de um USER → 403 Forbidden
```

---

## Decisões e detalhes técnicos

- **Senha com BCrypt.** `BCryptPasswordEncoder` gera um hash com salt aleatório. Verificar é
  `encoder.matches(senhaDigitada, hashSalvo)`; a senha original é irrecuperável.
- **E-mail normalizado.** Guardado em minúsculas e comparado com `IgnoreCase`, então
  `Ana@Exemplo.com` e `ana@exemplo.com` são o mesmo usuário.
- **Segredo do JWT no properties.** Aqui fica em `application.properties` por ser estudo; em
  produção viria de variável de ambiente / secret manager. A chave precisa de **≥ 32 bytes**
  para o HMAC-SHA256.
- **`SessionCreationPolicy.STATELESS`.** Sem `HttpSession`; o Spring não cria nem usa sessão.
- **Claim `papel` → `ROLE_`.** O `JwtAuthenticationConverter` lê a claim `papel` e cria a
  autoridade `ROLE_ADMIN` (é o `ROLE_` que faz `hasRole("ADMIN")` casar).
- **401 vs 403.** *Sem* token válido → `AuthenticationEntryPoint` (401). Token válido mas *sem*
  permissão → `AccessDeniedHandler` (403). Esses dois acontecem no filtro, **antes** do
  controller, por isso não passam pelo `@RestControllerAdvice`.
- **`open-in-view=false`.** O lazy só resolve dentro da transação do service.

---

## Como rodar

Usa H2 em memória; os dados somem ao parar a aplicação.

```bash
cd api-autenticacao
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Console do H2 em `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:autenticacao`, usuário `sa`, sem senha).

Também há um `Dockerfile` (multi-stage, usuário sem privilégios):

```bash
docker build -t api-autenticacao .
docker run -p 8080:8080 api-autenticacao
```

---

## Testes

```bash
./mvnw test
```

Cobrem:

- cadastro (`201`) e cadastro com e-mail duplicado (`409`);
- senha curta → `400`;
- login com senha errada e com e-mail inexistente → `401`;
- login devolve token (com `tipo` e `expiraEm`);
- `/usuarios/me` sem token e com token inválido → `401`;
- `/usuarios/me` com token devolve o usuário logado;
- `/admin/usuarios` com usuário comum → `403`; com ADMIN → `200`.

> Criar um ADMIN nos testes não passa pelo cadastro (que sempre cria USER): o teste insere
> direto no repositório com o papel `ADMIN`, simulando um usuário privilegiado já existente.

---

## Próximos passos (ideias para continuar estudando)

- **Refresh token** para renovar o access token sem novo login.
- **Revogação/logout** (lista de tokens inválidos ou troca de segredo).
- Recuperação de senha por e-mail e troca de senha autenticada.
- Migrar o segredo para variável de ambiente e usar `HS512`/par de chaves RSA.
- **Roles mais granulares** e `@PreAuthorize` por método/posse do recurso.
- Aplicar isso em projetos anteriores (`blog`, `rede-social`): trocar o autor do corpo da
  requisição pela identidade do token.
- Trocar H2 por **PostgreSQL** + `docker-compose` e usar **Testcontainers** nos testes.
