# Payment Service — EventMaster

Microsserviço de pagamentos do ecossistema **EventMaster**.
Processa pagamentos via **PIX**, **Cartão de Crédito** e **Boleto Bancário** usando arquitetura hexagonal (Ports & Adapters),
SAGA coreografado via Kafka e padrão Outbox para garantia de entrega de eventos.

---

## Stack

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.14 |
| PostgreSQL | 16 |
| Apache Kafka | 7.6.0 (Confluent) |
| MapStruct | 1.5.5 |
| Lombok | — |
| Docker / Docker Compose | — |

---

## Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop) instalado e rodando
- Git

> Para rodar **localmente sem Docker** (modo dev), você também precisará de JDK 21 e Maven 3.9+.

---

## Opção 1 — Rodar com Docker Compose (recomendado)

Sobe tudo em um único comando: PostgreSQL, Kafka, Zookeeper e a aplicação.

```bash
# 1. Clonar o repositório
git clone <url-do-repositorio>
cd event-master-payment-service

# 2. Subir todos os containers
docker compose up --build
```

O Docker irá compilar a aplicação, aguardar os health checks do PostgreSQL e do Kafka,
e só então iniciar a aplicação. A primeira execução pode levar 3–5 minutos.

Aguarde a mensagem nos logs:
```
payment-app | Started PaymentserviceApplication in X.XXX seconds
```

**Parar:**
```bash
docker compose down          # para os containers
docker compose down -v       # para e remove os dados do banco
```

---

## Opção 2 — Rodar localmente (modo dev)

```bash
# 1. Subir apenas a infraestrutura
docker compose up postgres zookeeper kafka -d

# 2. Rodar a aplicação
mvn spring-boot:run
```

A aplicação conecta em `localhost:5432` e `localhost:9092` conforme `application.properties`.

---

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/pagamentos` | Cria e processa um pagamento |
| `GET` | `/pagamentos/{id}` | Consulta o status de um pagamento |
| `POST` | `/webhooks/boleto/confirmacao` | Confirma o pagamento de um boleto (chamado pelo gateway) |

Swagger UI (documentação interativa):
```
http://localhost:8080/swagger-ui.html
```

---

## Códigos de resposta HTTP

### POST `/pagamentos`

| Código | Significado | Quando ocorre |
|---|---|---|
| `201 Created` | Pagamento **aprovado** ou boleto **gerado** | PIX/Cartão aprovado, ou boleto emitido (status `AGUARDANDO_PAGAMENTO`). Header `Location` aponta para o recurso |
| `200 OK` | Pagamento **rejeitado** | Request válido, mas a regra de negócio ou o gateway recusou. Consulte o campo `motivoRejeicao` |
| `400 Bad Request` | Request inválido | Campo obrigatório ausente ou formato incorreto |
| `409 Conflict` | Pagamento duplicado | Já existe um pagamento para o mesmo `pedidoId`. O corpo retorna o `pagamentoId` original |

### POST `/webhooks/boleto/confirmacao`

| Código | Significado | Quando ocorre |
|---|---|---|
| `200 OK` | Boleto confirmado | Pagamento aprovado com sucesso |
| `400 Bad Request` | Request inválido | `pagamentoId` ausente ou pagamento não é boleto / não está aguardando |
| `404 Not Found` | Pagamento não encontrado | UUID não existe |

### GET `/pagamentos/{id}`

| Código | Significado | Quando ocorre |
|---|---|---|
| `200 OK` | Pagamento encontrado | Retorna o estado atual |
| `404 Not Found` | Pagamento inexistente | O UUID informado não existe no banco |

### Campos de `motivoRejeicao` (quando status = `REJEITADO`)

| Valor | Causa |
|---|---|
| `LIMITE_EXCEDIDO` | Valor acima do limite permitido para o método |
| `NAO_AUTORIZADO` | Moeda não aceita (todos os métodos exigem BRL) |
| `CARTAO_INVALIDO` | Dados do cartão ausentes ou recusados pelo gateway |
| `CARTAO_EXPIRADO` | Cartão recusado por expiração |
| `TIMEOUT` | Boleto não pago até a data de vencimento |
| `ERRO_PROCESSAMENTO` | Método de pagamento não suportado pelo sistema |

---

## Métodos de pagamento suportados

| Método | Moeda | Mínimo | Máximo | Processamento |
|---|---|---|---|---|
| `PIX` | BRL | R$ 0,01 | R$ 5.000,00 | Síncrono |
| `CARTAO_CREDITO` | BRL | R$ 1,00 | R$ 10.000,00 | Síncrono |
| `BOLETO` | BRL | — | — | Assíncrono |

Para `CARTAO_CREDITO`, campos adicionais obrigatórios:
- `numeroCartao` — qualquer número com formato válido
- `dataExpiracao` — formato `MM/yy` (ex: `12/29`)
- `cvv` — 3 ou 4 dígitos

Para `BOLETO`, a resposta inclui `linhaDigitavel` e `dataVencimento` (3 dias a partir da criação).
O status fica `AGUARDANDO_PAGAMENTO` até a confirmação via webhook ou expiração.

### Simulando aprovação e rejeição (cartão)

A validação do cartão usa um **stub** que simula o gateway externo (adquirente):

| Número do cartão | Resultado |
|---|---|
| Qualquer número que **não** termine em `0002` ou `0069` | Aprovado |
| Termina em `0002` (ex: `4111111111110002`) | Rejeitado — CARTAO_INVALIDO |
| Termina em `0069` (ex: `4111111111110069`) | Rejeitado — CARTAO_EXPIRADO |

---

## Testando a API

### Insomnia / Postman

A coleção com todos os cenários está em:

```
docs/insomnia-payment-service.json
```

**Importar no Insomnia:** `File → Import` → selecione o arquivo.  
**Importar no Postman:** `Import` → selecione o arquivo (formato compatível).

O environment `Base URL` já aponta para `http://localhost:8080`. Cenários incluídos:

| Pasta | Cenário |
|---|---|
| PIX | Aprovado, Limite excedido, Moeda inválida |
| Cartão de Crédito | Aprovado, CARTAO_INVALIDO, CARTAO_EXPIRADO, Limite excedido, Campos ausentes |
| Boleto | Gerado (AGUARDANDO_PAGAMENTO), Moeda inválida, Confirmar via webhook, Consultar por ID |
| Consultas e Casos de Erro | Buscar por ID, Idempotência (409), Campos ausentes (400), Inexistente (404) |

### Exemplos rápidos via curl

**PIX aprovado:**
```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "pedidoId":        "11111111-1111-1111-1111-111111111111",
    "clienteId":       "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "valor":           150.00,
    "moeda":           "BRL",
    "metodoPagamento": "PIX"
  }'
```

**Cartão aprovado:**
```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "pedidoId":        "22222222-2222-2222-2222-222222222222",
    "clienteId":       "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "valor":           500.00,
    "moeda":           "BRL",
    "metodoPagamento": "CARTAO_CREDITO",
    "numeroCartao":    "4111111111111111",
    "dataExpiracao":   "12/29",
    "cvv":             "123"
  }'
```

**Cartão rejeitado pelo gateway:**
```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "pedidoId":        "33333333-3333-3333-3333-333333333333",
    "clienteId":       "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "valor":           200.00,
    "moeda":           "BRL",
    "metodoPagamento": "CARTAO_CREDITO",
    "numeroCartao":    "4111111111110002",
    "dataExpiracao":   "12/29",
    "cvv":             "123"
  }'
```

**Boleto gerado:**
```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "pedidoId":        "44444444-4444-4444-4444-444444444444",
    "clienteId":       "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "valor":           350.00,
    "moeda":           "BRL",
    "metodoPagamento": "BOLETO"
  }'
```

**Confirmar pagamento de boleto (webhook):**
```bash
curl -X POST http://localhost:8080/webhooks/boleto/confirmacao \
  -H "Content-Type: application/json" \
  -d '{
    "pagamentoId": "<id-retornado-na-criacao>",
    "dataPagamento": "2026-05-20",
    "valorPago": 350.00
  }'
```

**Consultar pagamento:**
```bash
curl http://localhost:8080/pagamentos/{id}
```

---

## Status de pagamento

| Status | Descrição |
|---|---|
| `PENDENTE` | Criado em memória — transitório (nunca persistido para PIX/Cartão) |
| `PROCESSANDO` | Primeiro estado persistido — processamento em andamento |
| `AGUARDANDO_PAGAMENTO` | Boleto gerado — aguardando confirmação de pagamento |
| `APROVADO` | Aprovado com sucesso |
| `REJEITADO` | Recusado (ver campo `motivoRejeicao`) |
| `CANCELADO` | Cancelado |

---

## Tópicos Kafka

| Tópico | Direção | Descrição |
|---|---|---|
| `pagamento.reprocessar` | Entrada | Solicita reprocessamento de pagamento travado |
| `pagamento.aprovado` | Saída | Pagamento aprovado — outros serviços reagem |
| `pagamento.rejeitado` | Saída | Pagamento rejeitado — outros serviços reagem |
| `boleto.gerado` | Saída | Boleto emitido — notifica serviços de email/notificação |

Os eventos de saída são publicados via **Outbox Pattern**: gravados no banco na mesma transação
do pagamento e enviados ao Kafka por um dispatcher agendado. O intervalo é configurável via
`outbox.dispatch.delay-ms` no `application.properties` (padrão: 3 segundos).

---

## Arquitetura

```
src/main/java/com/eventmaster/paymentservice/
│
├── domain/                        # Núcleo — zero dependência de framework
│   ├── model/Pagamento.java       # Agregado raiz
│   ├── enums/                     # StatusPagamento, TipoMetodoPagamento, MotivoRejeicao
│   ├── eventos/                   # PagamentoAprovadoEvento, PagamentoRejeitadoEvento, BoletoGeradoEvento
│   └── exceptions/                # PagamentoNaoEncontradoException, PagamentoDuplicadoException
│
├── application/                   # Casos de uso e portas
│   ├── port/in/                   # GerenciarPagamentoUseCase
│   ├── port/out/                  # PagamentoRepositorioPort, PagamentoEventoPort,
│   │                              # AutorizadorExternoPort, BoletoGatewayPort
│   ├── processador/               # Strategy: ProcessadorPagamento, ProcessadorNaoSuportado (Null Object)
│   ├── processador/impl/          # ProcessadorPix, ProcessadorCartaoCredito, ProcessadorBoleto
│   ├── comando/                   # CriarPagamentoComando
│   └── service/PagamentoService   # Orquestração do caso de uso
│
└── infrastructure/                # Adapters — detalhes técnicos isolados
    ├── persistence/               # JPA Entity, Repository, Mapper, Outbox
    ├── messaging/                 # Kafka Consumer, Evento Adapter, Outbox Dispatcher,
    │                              # BoletoExpiracaoJob (expira boletos vencidos diariamente às 08h)
    ├── autorizador/               # AutorizadorExternoStub, BoletoGatewayStub
    ├── config/                    # KafkaConfig
    └── web/                       # PagamentoController, BoletoWebhookController,
                                   # DTOs, Mappers, ManipuladorGlobalExcecao
```

### Design Patterns aplicados

| Padrão | Onde |
|---|---|
| Hexagonal Architecture (Ports & Adapters) | Estrutura geral |
| Strategy | `ProcessadorPagamento` — PIX, Cartão e Boleto extensíveis sem alterar o service |
| Template Method | `ProcessadorPagamentoBase` — define fluxo (moeda → limite → autorizador) |
| Null Object | `ProcessadorNaoSuportado` — fallback sem null check no service |
| Command | `ResultadoProcessamento` + lambda — finalizar() sem if/else no service |
| Outbox | `PagamentoEventoAdapter` + `OutboxEventoDispatcher` — atomicidade com Kafka |
| Repository | `PagamentoRepositorioPort` → `PagamentoPersistenceAdapter` |
| Factory Method | `Pagamento.novo()` e `Pagamento.reconstituir()` |
| Adapter | Todos os adaptadores de infraestrutura |

---

## Fora do escopo desta versão

| Funcionalidade | Observação |
|---|---|
| Autenticação JWT | Entrada via API Gateway — não implementado nesta versão |
| Dead Letter Queue | Consumer Kafka sem DLQ configurada |
| Flyway / Liquibase | Schema via `ddl-auto=update` — adequado apenas para desenvolvimento |
