# Documentacao Tecnica - Cidades ESG Inteligentes (SkyRescue)

Documento de entrega da fase "Navegando pelo mundo DevOps".

**Exportar para PDF ou PPT:** abra este arquivo no VS Code (extensao "Markdown
PDF") ou use Pandoc, por exemplo:

```bash
pandoc docs/ENTREGA.md -o docs/ENTREGA.pdf
```

O arquivo gerado **`docs/ENTREGA.pdf`** (ou `.pptx`) deve ser incluido no ZIP da
entrega, com os topicos abaixo e o **checklist obrigatorio** no final.


## Titulo do projeto e integrantes

**SkyRescue — Sistema de Resgate por Drones** (Cidades ESG Inteligentes)

| Nome                            | RM         |
| ------------------------------- | ---------- |
| Tiago Tiradentes Cuzzuol Nunes  | RM 560754  |

A startup simula operacao de drones autonomos para localizar vitimas em
desastres (enchentes, terremotos, deslizamentos, incendios, desabamentos),
com API para drones, missoes e registro de vitimas.


## Descricao do pipeline (ferramenta, etapas e logica)

Usamos o **GitHub Actions** (`.github/workflows/ci-cd.yml`). A escolha foi
pela integracao direta com o repositorio no GitHub, por ser gratuito para
projetos publicos e por publicar imagens Docker no proprio GitHub
Container Registry (GHCR).

O workflow tem quatro jobs encadeados:

1. `build-and-test` - roda em todo push e pull request. Faz checkout,
   configura JDK 17 (Temurin) com cache do Maven, executa
   `./mvnw clean compile`, depois `./mvnw test -DexcludedGroups=bdd` (testes
   unitarios/integracao com **H2**), em seguida o passo dedicado de
   **testes BDD** (Cucumber + contrato JSON, tambem com H2), publica o
   relatorio Surefire e empacota o JAR como artefato.
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

| Gatilho                       | build-and-test | docker-build | deploy-staging | deploy-production |
| ----------------------------- | :------------: | :----------: | :------------: | :---------------: |
| Pull request para main/dev    |      sim       |      -       |       -        |         -         |
| Push em `develop`             |      sim       |     sim      |      sim       |         -         |
| Push em `main`                |      sim       |     sim      |      sim       |        sim        |
| Tag `v*.*.*`                  |      sim       |     sim      |      sim       |        sim        |
| `workflow_dispatch` manual    |      sim       |     sim      |       -        |         -         |

Para o deploy real basta adicionar um secret com o kubeconfig do cluster
(ou credenciais SSH do host) e substituir os `echo` dos steps de deploy
por um step de `kubectl` ou `docker compose`. Os steps atuais imprimem os
comandos para nao vazar credenciais no repositorio.


## Docker (arquitetura, comandos, imagem)

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


## Prints do pipeline e dos ambientes (evidencias no PDF)

Inclua no PDF as capturas correspondentes aos itens do enunciado:

- Pipeline com **build**, **testes** (e passo BDD, se disponivel) e **deploy**.
- Ambientes **staging** e **producao** em funcionamento (health, Swagger ou
  chamada HTTP).

Os arquivos de imagem podem ficar em `docs/prints/` (veja `docs/prints/README.md`).
Sugestao de nomes:

- `github-pipeline.png` — visao geral do workflow verde.
- `docker-containers.png` — `docker compose ps` com containers saudaveis.
- `staging-health-or-swagger.png` / `production-health-or-swagger.png` —
  evidencias de staging e producao.


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

5. **Testes automatizados e BDD**. Alem dos testes de servico e contexto
   Spring, adicionamos a suite **Cucumber** em `skyrescue-bdd/` e um teste de
   **contrato JSON** para a resposta de missao, executados na CI com H2.


## Checklist de Entrega (obrigatório — preencher no PDF)

Substitua `☐` por `☑` ao concluir cada item antes de enviar o ZIP e o PDF.

| Item                                                             | OK |
| ---------------------------------------------------------------- | -- |
| Projeto compactado em .ZIP com estrutura organizada              | ☐ |
| Dockerfile funcional                                             | ☐ |
| docker-compose.yml ou arquivos Kubernetes                        | ☐ |
| Pipeline com etapas de build, teste e deploy                     | ☐ |
| README.md com instrucoes e prints                                | ☐ |
| Documentacao tecnica com evidencias (PDF ou PPT)                 | ☐ |
| Deploy realizado nos ambientes staging e producao                | ☐ |
