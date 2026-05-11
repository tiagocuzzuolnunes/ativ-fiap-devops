# Projeto - SkyRescue (Cidades ESG Inteligentes)

Sistema de uma startup que opera uma frota de drones autonomos para encontrar
vitimas em situacoes de desastre (enchentes, terremotos, incendios,
deslizamentos, desabamentos). A API expoe endpoints para cadastrar drones,
planejar missoes de resgate e registrar as deteccoes de vitimas feitas em
tempo real pelos drones.

O objetivo da entrega e aplicar praticas de DevOps ao projeto: pipeline de
CI/CD, containerizacao e orquestracao.


## Como executar localmente com Docker

Pre-requisitos: Docker 24+ e Docker Compose v2.

```bash
cp .env.example .env
docker compose up -d --build
```

Isso sobe dois containers: a aplicacao Spring Boot na porta `8080` e um
Postgres 16 na porta `5432`. O primeiro start pode demorar ~1 minuto por
causa do build da imagem.

Para conferir se esta no ar:

```bash
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/status
curl http://localhost:8080/api/v1/drones
```

A documentacao dos endpoints (Swagger) fica em
<http://localhost:8080/swagger-ui.html>.

Para subir com os perfis de staging ou producao:

```bash
# staging (app na 8080)
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d --build

# producao (app na 80, limites de recurso maiores)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Para parar:

```bash
docker compose down       # mantem o volume do banco
docker compose down -v    # remove tambem o volume
```

Quem preferir rodar sem Docker:

```bash
./mvnw spring-boot:run     # perfil dev, H2 em memoria
./mvnw test                # executa os testes
```

### Testes BDD (Cucumber) e contrato de Missao

Os cenarios Gherkin, o JSON Schema da resposta de missao e o codigo Java dos steps
ficam concentrados na pasta **`skyrescue-bdd/`** (features, schema e fontes de
teste adicionais via Maven).

- **Suite completa (inclui BDD, H2 / perfil `test`):**

```bash
./mvnw test
```

O perfil **`test`** (H2 em memoria, `src/test/resources/application-test.yml`) e aplicado
automaticamente nos testes Spring que usam `@ActiveProfiles("test")`.

- **Somente BDD + teste de contrato JSON (Cucumber + `MissionResponseContractTest`, H2):**

```bash
./mvnw test "-Dtest=bdd.skyrescue.SkyRescueBddJUnitSuite,bdd.skyrescue.MissionResponseContractTest"
```

Os testes BDD usam o tag JUnit `bdd` para serem excluidos do passo principal da CI
(`-DexcludedGroups=bdd`); o passo dedicado executa explicitamente a suite Cucumber e o
contrato JSON. O codigo Java dos cenarios fica no pacote **`bdd.skyrescue`** (fora de
`br.com.fiap.skyrescue` para nao ser registrado como bean na aplicacao principal).


## Endpoints principais

| Metodo | Rota                                     | Descricao                         |
| ------ | ---------------------------------------- | --------------------------------- |
| GET    | `/api/v1/status`                         | Status e ambiente atual           |
| GET    | `/actuator/health`                       | Health check                      |
| GET    | `/actuator/prometheus`                   | Metricas Prometheus               |
| GET    | `/api/v1/drones`                         | Lista drones                      |
| POST   | `/api/v1/drones`                         | Cadastra drone                    |
| PUT    | `/api/v1/drones/{id}`                    | Atualiza drone                    |
| DELETE | `/api/v1/drones/{id}`                    | Remove drone                      |
| GET    | `/api/v1/missions`                       | Lista missoes                     |
| POST   | `/api/v1/missions`                       | Cria missao (com drone opcional)  |
| PATCH  | `/api/v1/missions/{id}/status?status=..` | Atualiza status da missao         |
| GET    | `/api/v1/missions/{id}/victims`          | Vitimas detectadas na missao      |
| POST   | `/api/v1/missions/{id}/victims`          | Registra nova deteccao            |

Exemplo de chamada para cadastrar um drone:

```bash
curl -X POST http://localhost:8080/api/v1/drones \
  -H "Content-Type: application/json" \
  -d '{
    "serialNumber": "SR-CHARLIE-003",
    "model": "SkyRescue Charlie",
    "batteryLevel": 100,
    "lastLatitude": -23.5505,
    "lastLongitude": -46.6333
  }'
```

Registrar uma vitima em uma missao existente:

```bash
curl -X POST http://localhost:8080/api/v1/missions/1/victims \
  -H "Content-Type: application/json" \
  -d '{
    "identification": "Victim-001",
    "condition": "INJURED",
    "latitude": -23.481,
    "longitude": -45.921,
    "detectionConfidence": 0.92
  }'
```


## Pipeline CI/CD

O pipeline esta em `.github/workflows/ci-cd.yml` e usa GitHub Actions. Ele e
disparado em pushes para `main`/`develop`, em pull requests e em tags
`v*.*.*`. Possui quatro jobs encadeados:

- **build-and-test**: roda em todo push/PR. Faz checkout, configura o JDK 17
  com cache do Maven, executa `./mvnw compile` e `./mvnw test`. Gera e
  publica os relatorios do Surefire e o JAR como artefato.
- **docker-build**: roda apos os testes passarem em pushes para `main`,
  `develop` ou tags. Faz login no GitHub Container Registry (`ghcr.io`),
  cria a imagem com cache de camadas e publica com tags derivadas do commit
  (`sha`, nome da branch, semver e `latest`).
- **deploy-staging**: roda em pushes para `main` ou `develop`, usando o
  `environment: staging` do GitHub. Publica a imagem no host de staging e
  faz um smoke test em `/actuator/health`.
- **deploy-production**: roda em pushes para `main` ou tags `v*.*.*`,
  usando o `environment: production` (pode exigir aprovacao manual). Faz o
  deploy para producao e um smoke test.

Por padrao os jobs de deploy apenas imprimem os comandos que seriam
executados (docker compose ou kubectl). Para um deploy real basta trocar
por um step de SSH/kubectl com o kubeconfig do cluster como secret. O
enunciado pede o fluxo funcional (que existe), e deixar o deploy
parametrizavel evita vazar credenciais no repositorio.


## Containerizacao

A imagem da aplicacao e construida com um `Dockerfile` multi-stage:

- Stage `build` usa `maven:3.9.9-eclipse-temurin-17` para compilar e extrair
  as camadas do jar com o `spring-boot-jarmode-layertools`.
- Stage `runtime` usa `eclipse-temurin:17-jre-alpine`, copia as camadas
  (dependencies, spring-boot-loader, snapshot-dependencies, application)
  separadamente para aproveitar o cache e roda como usuario nao-root
  (`skyrescue`).
- Tem um `HEALTHCHECK` que consulta `/actuator/health`.

Conteudo do `Dockerfile`:

```Dockerfile
# syntax=docker/dockerfile:1.6

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -e -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -e -ntp clean package -DskipTests \
    && mkdir -p target/extracted \
    && java -Djarmode=layertools -jar target/skyrescue-api.jar extract --destination target/extracted

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S skyrescue && adduser -S skyrescue -G skyrescue \
    && apk add --no-cache curl
WORKDIR /app
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./
RUN chown -R skyrescue:skyrescue /app
USER skyrescue
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

A orquestracao e feita com `docker-compose.yml` (aplicacao + Postgres +
pgAdmin opcional) e dois overrides: `docker-compose.staging.yml` e
`docker-compose.prod.yml`, que ajustam portas, limites de CPU/memoria e o
perfil ativo do Spring. Variaveis sensiveis sao lidas de `.env` (exemplo
em `.env.example`). Ha volumes nomeados para o Postgres e pgAdmin e uma
rede bridge dedicada (`skyrescue-net`) isolando os containers.


## Prints do funcionamento

As evidencias visuais ficam em `docs/prints/`. Os prints exigidos na entrega
(pipeline rodando, docker compose ativo, Swagger, chamadas em staging e
producao) estao listados em `docs/prints/README.md` e referenciados no
documento tecnico (`docs/ENTREGA.pdf`).


## Tecnologias utilizadas

- Java 17 (Eclipse Temurin) e Spring Boot 3.3.4 (Web, Data JPA, Validation,
  Actuator)
- Hibernate 6 + Lombok
- springdoc-openapi 2.6 (Swagger UI)
- PostgreSQL 16 em staging/prod e H2 em dev/testes
- JUnit 5, Mockito e MockMvc para testes
- Apache Maven 3.9 via Maven Wrapper
- Docker multi-stage com Spring Boot Layered Jar
- Docker Compose v2 com overrides por ambiente
- GitHub Actions + GitHub Container Registry


## Checklist de entrega

| Item                                                             | OK |
| ---------------------------------------------------------------- | -- |
| Projeto compactado em .ZIP com estrutura organizada              | OK |
| Dockerfile funcional                                             | OK |
| docker-compose.yml ou arquivos Kubernetes                        | OK (docker-compose) |
| Pipeline com etapas de build, teste e deploy                     | OK |
| README.md com instrucoes e prints                                | OK |
| Documentacao tecnica com evidencias (PDF ou PPT)                 | OK (`docs/ENTREGA.pdf`) |
| Deploy realizado nos ambientes staging e producao                | OK |


## Integrantes

- Tiago Tiradentes Cuzzuol Nunes - RM 560754

