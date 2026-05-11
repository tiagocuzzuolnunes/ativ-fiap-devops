# Projeto - Cidades ESG Inteligentes

**SkyRescue** — API REST para resgate com drones (cadastro de drones, missoes e vitimas).

Sistema de uma startup que opera uma frota de drones autonomos para encontrar
vitimas em situacoes de desastre (enchentes, terremotos, incendios,
deslizamentos, desabamentos). O foco da entrega sao praticas de DevOps: CI/CD,
containerizacao e orquestracao.

## Conteudo do ZIP (estrutura minima)

O arquivo `.ZIP` da entrega deve conter o **repositorio completo** (recomenda-se
`./mvnw clean` antes de compactar para nao enviar a pasta `target/`). Estrutura
alinhada ao enunciado:

```text
seu-projeto/
├── Dockerfile
├── docker-compose.yml
├── docker-compose.staging.yml
├── docker-compose.prod.yml
├── .dockerignore
├── .env.example
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
├── src/
├── skyrescue-bdd/          # testes BDD (Cucumber), schema JSON e features
├── docs/
│   ├── ENTREGA.md          # base para gerar o PDF/PPT
│   └── prints/             # evidencias (imagens); ver secao "Prints"
└── .github/
    └── workflows/
        └── ci-cd.yml
```

Inclua tambem: codigo-fonte completo, configuracao de CI/CD, scripts Maven
Wrapper, `.env.example`, e (opcional) logs ou prints em `docs/prints/`.

## Como executar localmente com Docker

Pre-requisitos: Docker 24+ e Docker Compose v2.

1. Copie as variaveis de ambiente de exemplo:

```bash
cp .env.example .env
```

2. Suba os containers (build da imagem da aplicacao + Postgres):

```bash
docker compose up -d --build
```

Isso sobe a aplicacao Spring Boot na porta `8080` e o Postgres 16 na porta
`5432`. O primeiro start pode demorar ~1 minuto por causa do build da imagem.

3. Verifique se esta no ar:

```bash
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/status
curl http://localhost:8080/api/v1/drones
```

A documentacao dos endpoints (Swagger) fica em
<http://localhost:8080/swagger-ui.html>.

**Perfis Compose (staging / producao):**

```bash
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

**Parar:**

```bash
docker compose down
docker compose down -v
```

**Sem Docker (desenvolvimento com H2):**

```bash
./mvnw spring-boot:run
./mvnw test
```

## Pipeline CI/CD

Ferramenta: **GitHub Actions** (arquivo `.github/workflows/ci-cd.yml`).

**Gatilhos:** push em `main` / `develop`, pull requests para essas branches,
tags `v*.*.*`, e execucao manual (`workflow_dispatch`).

**Job `build-and-test` (build + testes):**

1. Checkout do codigo e JDK 17 (Eclipse Temurin) com cache Maven.
2. **Compilar:** `./mvnw clean compile`
3. **Testes unitarios/integracao (H2, perfil `test`):** `./mvnw test -DexcludedGroups=bdd`
4. **Testes BDD + contrato JSON (Cucumber, H2):** `./mvnw test "-Dtest=bdd.skyrescue.SkyRescueBddJUnitSuite,bdd.skyrescue.MissionResponseContractTest"`
5. Publicacao dos relatorios Surefire e do JAR como artefatos.

**Job `docker-build`:** apos testes em push para `main`/`develop` ou tags;
login no **GHCR**, build/push da imagem Docker com tags (sha, branch, semver,
`latest` na branch padrao).

**Jobs `deploy-staging` e `deploy-production`:** usam `environment` do GitHub;
por padrao exibem os comandos de deploy e um smoke test (`curl` no health).
Substitua os `echo` por SSH/`kubectl`/`docker compose` reais quando houver
secrets (kubeconfig, host, etc.).

## Containerização

A imagem e **multi-stage**:

- **Build:** `maven:3.9.9-eclipse-temurin-17` compila o projeto e extrai camadas
  com `spring-boot-jarmode-layertools`.
- **Runtime:** `eclipse-temurin:17-jre-alpine`, camadas copiadas separadamente
  (cache de build), usuario **nao-root** (`skyrescue`), `HEALTHCHECK` em
  `/actuator/health`.

**Dockerfile (referencia):**

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

**Orquestracao:** `docker-compose.yml` (app + Postgres + pgAdmin opcional) e
overrides `docker-compose.staging.yml` e `docker-compose.prod.yml`. Variaveis
em `.env` (modelo em `.env.example`); rede `skyrescue-net`.

## Prints do funcionamento

Coloque as **evidencias** (capturas de tela ou links publicos) em
`docs/prints/`. Lista sugerida de arquivos: veja `docs/prints/README.md`.

Inclua, conforme o enunciado da disciplina:

- Pipeline rodando (**build**, **testes** — inclusive passo BDD se visivel —,
  **deploy** / jobs de staging e producao).
- **Docker Compose** ativo (`docker compose ps`, containers saudaveis).
- **Swagger** ou chamadas HTTP locais.
- **Staging** e **producao** (URLs configuradas no workflow, ex. health ou
  Swagger), se aplicavel ao seu deploy.

Opcional: anexar logs de CI ou de `docker compose logs` na pasta `docs/prints/`
ou no PDF.

## Tecnologias utilizadas

- Java 17 (Eclipse Temurin) e Spring Boot 3.3.4 (Web, Data JPA, Validation,
  Actuator)
- Hibernate 6 + Lombok
- springdoc-openapi 2.6 (Swagger UI)
- PostgreSQL 16 (Compose / staging / prod) e H2 (dev e testes)
- JUnit 5, Mockito, MockMvc; **Cucumber** + **JUnit Platform** (BDD em
  `skyrescue-bdd/`)
- Apache Maven 3.9 (Maven Wrapper)
- Docker multi-stage + Spring Boot layered JAR
- Docker Compose v2 (overrides por ambiente)
- GitHub Actions + GitHub Container Registry (GHCR)

## Checklist de Entrega (obrigatório)

Preencha substituindo `☐` por `☑` (ou marque no PDF) antes de enviar.

| Item                                                             | OK |
| ---------------------------------------------------------------- | -- |
| Projeto compactado em .ZIP com estrutura organizada              | ☐ |
| Dockerfile funcional                                             | ☐ |
| docker-compose.yml ou arquivos Kubernetes                        | ☐ |
| Pipeline com etapas de build, teste e deploy                     | ☐ |
| README.md com instrucoes e prints                                | ☐ |
| Documentacao tecnica com evidencias (PDF ou PPT)                | ☐ |
| Deploy realizado nos ambientes staging e producao                | ☐ |

**Documentacao em PDF ou PPT:** exporte `docs/ENTREGA.md` (por exemplo com
Pandoc ou extensao "Markdown PDF" no VS Code) para **`docs/ENTREGA.pdf`** ou
entregue o `.pptx` equivalente, com os topicos pedidos no enunciado.

---

## Detalhes da API SkyRescue

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

Exemplo — cadastrar drone:

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

Registrar vitima:

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

## Testes BDD (Cucumber) e contrato de missao

Cenarios Gherkin, JSON Schema e steps em **`skyrescue-bdd/`**; glue Java em
`bdd.skyrescue`.

```bash
./mvnw test
./mvnw test "-Dtest=bdd.skyrescue.SkyRescueBddJUnitSuite,bdd.skyrescue.MissionResponseContractTest"
```

## Integrantes

- Tiago Tiradentes Cuzzuol Nunes — RM 560754
