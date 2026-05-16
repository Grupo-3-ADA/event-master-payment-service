# EventMaster — Payment Service

Microsserviço de processamento de pagamentos do ecossistema **EventMaster**, implementado com **Clean Architecture (Hexagonal)** em Java 21 + Spring Boot 3.

---

## Como Executar

### Pré-requisito único

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e rodando

### Subir tudo com um comando

Na raiz do projeto (onde está o `docker-compose.yml`):

```bash
docker compose up --build
```

Isso irá:
1. Compilar a aplicação Java
2. Subir o PostgreSQL e aguardar ele estar pronto
3. Subir o Kafka (+ Zookeeper) e aguardar ele estar pronto
4. Subir a aplicação Spring Boot somente após a infraestrutura estar saudável

A aplicação estará disponível em `http://localhost:8080` após todos os serviços subirem (pode levar ~2–3 minutos no primeiro build).

### Parar tudo

```bash
docker compose down
```

Para remover também os dados do banco:

```bash
docker compose down -v
```

---

## Testando a API

### Swagger UI (recomendado)

Acesse `http://localhost:8080/swagger-ui.html` para explorar e testar os endpoints interativamente.

### Via curl

**Criar pagamento com PIX:**

```bash
curl -s -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "pedidoId": "550e8400-e29b-41d4-a716-446655440000",
    "clienteId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "valor": 150.00,
    "moeda": "BRL",
    "metodoPagamento": "PIX"
  }'
```

**Criar pagamento com Cartão de Crédito:**

```bash
curl -s -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "pedidoId": "550e8400-e29b-41d4-a716-446655440000",
    "clienteId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "valor": 299.90,
    "moeda": "BRL",
    "metodoPagamento": "CARTAO_CREDITO",
    "numeroCartao": "4111111111111111",
    "dataExpiracao": "12/27",
    "cvv": "123"
  }'
```

**Consultar pagamento por ID:**

```bash
curl -s http://localhost:8080/pagamentos/{id}
```

---

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/pagamentos` | Cria e processa um pagamento |
| `GET` | `/pagamentos/{id}` | Consulta o status de um pagamento |

---

## Métodos de Pagamento

| Método | Moeda | Valor Mínimo | Valor Máximo |
|---|---|---|---|
| `PIX` | BRL | R$ 0,01 | R$ 5.000,00 |
| `CARTAO_CREDITO` | BRL | R$ 1,00 | R$ 10.000,00 |

Para `CARTAO_CREDITO`, campos adicionais obrigatórios:
- `numeroCartao`: validado pelo algoritmo de Luhn (13–19 dígitos)
- `dataExpiracao`: formato `MM/yy` (ex: `12/27`)
- `cvv`: 3 ou 4 dígitos numéricos

---

## Status de Pagamento

| Status | Descrição |
|---|---|
| `PENDENTE` | Recebido, aguardando processamento |
| `PROCESSANDO` | Em processamento |
| `APROVADO` | Aprovado com sucesso |
| `REJEITADO` | Recusado (ver campo `motivoRejeicao`) |
| `CANCELADO` | Cancelado |

---

## Arquitetura

O serviço adota **Clean Architecture (Hexagonal / Ports & Adapters)**:

```
┌─────────────────────────────────────────────────────────┐
│                    INFRAESTRUTURA                        │
│  REST Controller │ Kafka Consumer/Producer │ JPA/Postgres│
└────────────┬─────────────────┬──────────────────┬───────┘
             │ [Porta Entrada] │ [Porta Entrada]  │ [Porta Saída]
┌────────────▼─────────────────▼──────────────────▼───────┐
│                    APLICAÇÃO                             │
│  GerenciarPagamentoUseCase  →  PagamentoService          │
│           ProcessadorPix (Strategy)                      │
│           ProcessadorCartaoCredito (Strategy)            │
└────────────────────────────┬─────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────┐
│                    DOMÍNIO                               │
│  Pagamento (Agregado Raiz) · Enums · Eventos de Domínio  │
└──────────────────────────────────────────────────────────┘
```

---

## Stack

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.14 |
| PostgreSQL | 16 |
| Apache Kafka | 7.6.0 (Confluent) |
| MapStruct | 1.5.5 |

---

## Eventos Kafka

Ao processar um pagamento, o serviço publica eventos para que outros serviços do ecossistema possam reagir (padrão SAGA por coreografia):

| Tópico | Quando |
|---|---|
| `pagamento.aprovado` | Pagamento aprovado com sucesso |
| `pagamento.rejeitado` | Pagamento recusado |

O serviço também consome o tópico `pagamento.reprocessar` para reprocessar pagamentos pendentes sob demanda.
