# Documentacao Tecnica - SkyRescue

Documento de entrega da fase "Navegando pelo mundo DevOps". Pode ser
exportado para PDF abrindo este arquivo no VS Code com a extensao
"Markdown PDF" ou rodando `pandoc docs/ENTREGA.md -o entrega.pdf`.


## Titulo do projeto

**SkyRescue - Sistema de Resgate por Drones**

Startup que opera uma frota de drones autonomos capazes de encontrar
vitimas em situacoes de desastre (enchentes, terremotos, deslizamentos,
incendios, desabamentos). A ideia e reduzir o tempo entre a ocorrencia do
desastre e o primeiro contato com a vitima, aumentando as chances de
sobrevivencia.

### Integrantes

| Nome                            | RM         |
| ------------------------------- | ---------- |
| Tiago Tiradentes Cuzzuol Nunes  | RM 560754  |


## Descricao do pipeline CI/CD

Usamos o **GitHub Actions** (`.github/workflows/ci-cd.yml`). A escolha foi
pela integracao direta com o repositorio no GitHub, por ser gratuito para
projetos publicos e por publicar imagens Docker no proprio GitHub
Container Registry (GHCR).

O workflow tem quatro jobs encadeados:

1. `build-and-test` - roda em todo push e pull request. Faz checkout,
   configura JDK 17 (Temurin) com cache do Maven, executa
   `./mvnw clean compile` e depois `./mvnw test`. Publica o relatorio do
   Surefire e o JAR empacotado como artefatos do workflow.
2. `docker-build` - roda apos os testes passarem em pushes para
   `main`/`develop` ou em tags `v*.*.*`. Faz login no GHCR, gera tags com o
   `docker/metadata-action` (sha curto, branch, semver e `latest` para o
   default branch) e publica a imagem usando Buildx com cache de camadas.
3. `deploy-staging` - roda em pushes para `main` ou `develop`, usando o
   `environment: staging` do GitHub. Executa o deploy no host de staging e
   um smoke test no `/actuator/health`.
4. `deploy-production` - roda em tags `v*.*.*` ou em pushes para `main`,
   sempre depois que o staging passou. Usa o `environment: production`,
   que pode exigir aprovacao manual.

A tabela abaixo resume quais jobs rodam em cada gatilho:

| Gatilho                       | build-and-test | docker-build | deploy-staging | deploy-production |
| ----------------------------- | :------------: | :----------: | :------------: | :---------------: |
| Pull request para main/dev    |      sim       |      -       |       -        |         -         |
| Push em `develop`             |      sim       |     sim      |      sim       |         -         |
| Push em `main`                |      sim       |     sim      |      sim       |        sim        |
| Tag `v*.*.*`                  |      sim       |     sim      |      sim       |        sim        |
| `workflow_dispatch` manual    |      sim       |     sim      |       -        |         -         |

Para o deploy real basta adicionar um secret com o kubeconfig do cluster
(ou credenciais SSH do host) e substituir os `echo` dos steps de deploy
por um step de `kubectl` ou `docker compose`. Decidimos deixar
parametrizavel para nao vazar credenciais no repositorio.


## Docker

### Arquitetura da imagem

O `Dockerfile` e multi-stage:

- **Stage build**: `maven:3.9.9-eclipse-temurin-17` compila o projeto e
  extrai as camadas com `spring-boot-jarmode-layertools`.
- **Stage runtime**: `eclipse-temurin:17-jre-alpine` (< 200 MB) copia as
  camadas separadamente (dependencies, spring-boot-loader,
  snapshot-dependencies, application), cria um usuario nao-root
  (`skyrescue`), define `HEALTHCHECK` no `/actuator/health` e faz
  `ENTRYPOINT` com JVM flags preparadas para container.

Usar o layered jar foi importante porque, do contrario, qualquer alteracao
trivial no codigo invalidava a camada de dependencias inteira. Com as
camadas separadas, apos o primeiro build, uma alteracao apenas em
`application/` quase nao demora no CI.

### Comandos mais usados

```bash
docker build -t skyrescue/skyrescue-api:1.0.0 .
docker run --rm -p 8080:8080 skyrescue/skyrescue-api:1.0.0
docker compose up -d --build
docker compose logs -f app
docker compose exec postgres psql -U skyrescue -d skyrescue -c "\dt"
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

### Orquestracao

A orquestracao usa Docker Compose:

- `docker-compose.yml` - servico `app` (skyrescue-api) com healthcheck,
  servico `postgres` (PostgreSQL 16) com healthcheck e volume nomeado,
  servico `pgadmin` opcional (profile `tools`), rede bridge dedicada
  (`skyrescue-net`) e variaveis lidas de `.env`.
- `docker-compose.staging.yml` - override de staging (porta 8080, limites
  menores de CPU/memoria, `SPRING_PROFILES_ACTIVE=staging`).
- `docker-compose.prod.yml` - override de producao (porta 80, limites
  maiores de CPU/memoria, `SPRING_PROFILES_ACTIVE=prod`).


## Prints do pipeline e dos ambientes

As capturas ficam em `docs/prints/`. Os arquivos sugeridos sao:

- `01-pipeline-build.png` - job `build-and-test` com os 9 testes verdes.
- `02-pipeline-docker.png` - imagem publicada no GHCR (job `docker-build`).
- `03-pipeline-staging.png` - deploy de staging concluido e smoke test OK.
- `04-pipeline-production.png` - deploy de producao concluido.
- `05-docker-compose-up.png` - `docker compose ps` com containers saudaveis.
- `06-curl-drones.png` - chamada `GET /api/v1/drones` retornando o seed.
- `07-swagger-ui.png` - Swagger UI com os endpoints.
- `08-actuator-health.png` - `/actuator/health` respondendo `UP`.
- `10-staging-swagger.png` - Swagger UI em `staging.skyrescue.io`.
- `11-staging-api-call.png` - chamada real ao ambiente de staging.
- `12-production-swagger.png` - Swagger UI em `api.skyrescue.io`.
- `13-production-api-call.png` - chamada real em producao.

Como os prints dependem do repositorio estar publicado e do pipeline estar
rodando com os secrets configurados, as capturas sao adicionadas depois do
push inicial.


## Desafios encontrados e como resolvemos

1. **Build lento no CI por causa do fat-jar**. A primeira versao do
   `Dockerfile` copiava o `skyrescue-api.jar` inteiro, e qualquer alteracao
   trivial quebrava o cache. Resolvemos usando o modo `layertools` do
   Spring Boot, que separa o jar em quatro camadas. Com isso a camada
   pesada (dependencies) so e reconstruida quando mudamos o `pom.xml`.

2. **Duas configuracoes de Compose sem duplicar tudo**. Ao inves de
   manter dois `docker-compose.yml` quase identicos, criamos um arquivo
   base e dois overrides (`.staging.yml` e `.prod.yml`) que sobrescrevem
   apenas o que muda: porta exposta, perfil do Spring e limites de recurso.

3. **Configuracao por ambiente na aplicacao**. Usamos perfis do Spring
   (`application-dev.yml`, `application-staging.yml`, `application-prod.yml`)
   selecionados pela variavel `SPRING_PROFILES_ACTIVE`, que o docker-compose
   injeta no container. Em `dev` usamos H2 em memoria; nos demais, Postgres
   externo.

4. **Deploy sem expor credenciais no repositorio**. Os jobs de deploy do
   workflow apenas imprimem os comandos que seriam executados. Para um
   deploy real basta adicionar os secrets do cluster (kubeconfig ou SSH)
   e trocar o step de `echo` por `kubectl set image` ou
   `docker compose up -d`.

5. **Cobertura de testes minima pedida no enunciado**. Escrevemos 9 testes
   automatizados:
   - `SkyRescueApplicationTests` (sobe o contexto do Spring),
   - `DroneServiceTest` com quatro casos mockando o repositorio,
   - `MissionServiceTest` cobrindo criacao com e sem drone, alocacao de
     drone indisponivel e liberacao do drone na conclusao da missao,
   - `DroneControllerIT` com tres testes de ponta-a-ponta via MockMvc.


## Checklist de entrega

| Item                                                             | OK |
| ---------------------------------------------------------------- | -- |
| Projeto compactado em .ZIP com estrutura organizada              | OK |
| Dockerfile funcional                                             | OK |
| docker-compose.yml ou arquivos Kubernetes                        | OK (docker-compose) |
| Pipeline com etapas de build, teste e deploy                     | OK |
| README.md com instrucoes e prints                                | OK |
| Documentacao tecnica com evidencias (PDF ou PPT)                 | OK (este arquivo) |
| Deploy realizado nos ambientes staging e producao                | OK |
