# gateway-server

**API Gateway** do sistema. É a porta única de entrada: recebe as requisições externas e encaminha para o microsserviço correspondente, resolvendo o destino pelo Eureka.

## Stack

| Item | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3 |

| Porta | Baseado em |
|---|---|
| 8080 | Spring Cloud Gateway (WebFlux) |

## Rotas

| Prefixo | Destino |
|---|---|
| `/autenticacao-service/**` | `lb://autenticacao-service/` |
| `/energia-service/**` | `lb://energia-service/` |
| `/supermercado-service/**` | `lb://supermercado-service/` |
| `/remedios-service/**` | `lb://remedios-service/` |
| `/gastos-service/**` | `lb://gastos-service/` |
| `/salario-service/**` | `lb://salario-service/` |
| `/saude-service/**` | `lb://saude-service/` |

O `lb://` faz o gateway consultar o **Eureka** e balancear entre as instâncias registradas.

O prefixo da rota **não é removido**: cada serviço tem `context-path` igual ao próprio nome, então `/saude-service/treinos/mais_recente` chega ao destino exatamente assim.

## CORS

Configurado por rota, via `metadata`, com a origem permitida vindo de `frontend` no Vault.

## Autenticação

O gateway **não valida** o JWT — ele apenas repassa o cabeçalho `Authorization`. Quem valida é cada microsserviço de destino.

## Configuração

A aplicação não guarda configuração própria: ela busca tudo no arranque, via `spring.config.import`.

| Origem | O que vem de lá |
|---|---|
| **Vault** (`secret/application`) | segredos compartilhados: `JWTSecret`, credenciais de e-mail, AWS, Eureka |
| **Vault** (`secret/<nome-do-serviço>`) | segredos próprios, como as credenciais do banco |
| **Config Server** | `server.port`, `context-path`, datasource e demais propriedades |

### Variável de ambiente obrigatória

| Variável | Para que serve |
|---|---|
| `VAULT_TOKEN` | token de acesso ao Vault |

`VAULT_TOKEN` **não tem valor padrão**. Sem ela, o Spring envia a string literal `${VAULT_TOKEN}` ao Vault, recebe `403` e — como `spring.cloud.vault.fail-fast` vem desligado — o erro só aparece bem depois, disfarçado de placeholder não resolvido (`${...} is malformed`). Se quiser que a falha apareça na hora, ligue `spring.cloud.vault.fail-fast: true`.

Também são necessários `VAULT_HOST`, `VAULT_PORT` e `VAULT_SCHEME` quando o Vault não está em `localhost:8200` via `http`, e `CONFIG_SERVER_USER` / `CONFIG_SERVER_PASS` nos serviços que leem do Config Server.

## Como executar

```bash
# build
./mvnw clean package

# execução
VAULT_TOKEN=<seu-token> java -jar target/gateway-server-*.jar --spring.profiles.active=dev
```

A aplicação sobe em `http://localhost:8080`.

### Docker

O `Dockerfile` espera o jar já na raiz do projeto, com o nome `sistema-gateway-server.jar`:

```bash
./mvnw clean package
cp target/gateway-server-*.jar sistema-gateway-server.jar

docker build \
  --build-arg VAULT_HOST=<host> \
  --build-arg VAULT_TOKEN=<token> \
  -t gateway-server .
```
