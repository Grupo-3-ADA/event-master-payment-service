# Guia de Implementação — Método de Pagamento Boleto

## Leia antes de começar — Como o boleto funciona no mundo real

Antes de tocar em qualquer código, é fundamental entender o fluxo de negócio do boleto,
porque ele é **fundamentalmente diferente** de PIX e Cartão de Crédito.

### O fluxo do ponto de vista do cliente

1. O cliente escolhe "pagar com boleto" no checkout
2. O sistema gera o boleto (com linha digitável e data de vencimento) e o retorna
3. O cliente copia a linha digitável e paga no banco dele (app, internet banking, lotérica, etc.)
4. O sistema **aguarda** — o pagamento pode ocorrer em minutos ou em dias

> O `payment-service` **não sabe** quando o cliente pagou. Quem detecta isso é o **banco/gateway**
> (ex: Asaas, PagSeguro, Bradesco). Eles monitoram o sistema bancário em tempo real.

---

### Como a confirmação de pagamento chega até o sistema

Existem duas formas:

#### Forma 1 — Webhook (mais comum e recomendada)

O gateway chama automaticamente um endpoint do `payment-service` assim que detecta o pagamento:

```
POST /webhooks/boleto/confirmacao
{
  "pagamentoId": "uuid-gerado-na-criação-do-boleto",
  "dataPagamento": "2026-05-20",
  "valorPago": 150.00
}
```

O sistema recebe, localiza o pagamento pelo `pagamentoId`, confirma a aprovação e publica o evento
`pagamento.aprovado` para os demais serviços do ecossistema reagirem (ex: liberar ingresso).

**O `pagamentoId` é a chave de correlação:** você o retornou ao gateway no momento da geração do boleto,
e ele te devolve esse mesmo ID na confirmação. É assim que o sistema sabe qual boleto foi pago.

#### Forma 2 — Polling (gateways mais simples)

Um job agendado no próprio `payment-service` pergunta periodicamente ao gateway:

```
GET /gateway/boletos/{codigoBoleto}/status
→ { "status": "PAGO" }
```

Se a resposta for `PAGO`, o sistema aprova o pagamento e publica o evento. Menos eficiente que o webhook,
mas funciona com integrações mais simples.

---

### O que acontece se o cliente não pagar?

Um job agendado (`@Scheduled`) roda diariamente, verifica todos os boletos com
status `AGUARDANDO_PAGAMENTO` e `dataVencimento` anterior à data atual,
e os rejeita automaticamente com `MotivoRejeicao.TIMEOUT`.

---

### Resumo do ciclo de vida do boleto

```
Cliente solicita boleto
        ↓
Sistema gera boleto → status: AGUARDANDO_PAGAMENTO
        ↓
        ├── Cliente paga antes do vencimento
        │       ↓
        │   Gateway detecta → chama webhook → status: APROVADO
        │       ↓
        │   Evento pagamento.aprovado publicado
        │
        └── Cliente não paga até o vencimento
                ↓
            Job diário detecta vencimento → status: REJEITADO (TIMEOUT)
                ↓
            Evento pagamento.rejeitado publicado
```

---

## Contexto técnico

Este documento descreve como adicionar suporte a Boleto Bancário no `payment-service`.
O serviço usa **arquitetura hexagonal** (Ports & Adapters), **DDD** e o padrão **Strategy** para processadores de pagamento.
Leia este guia inteiro antes de começar a codificar.

---

## Por que Boleto é diferente de PIX e Cartão

PIX e Cartão têm processamento **síncrono**: a resposta (aprovado/rejeitado) vem na mesma requisição.

Boleto é **assíncrono**: o boleto é gerado na hora, mas o pagamento pode ocorrer em até _N_ dias úteis.
Isso implica em:

| Aspecto | PIX / Cartão | Boleto |
|---|---|---|
| Resultado imediato | Sim | Não — apenas o boleto é gerado |
| Estado final na criação | APROVADO ou REJEITADO | AGUARDANDO_PAGAMENTO |
| Confirmação | Inline | Via webhook externo (banco/gateway) |
| Expiração | N/A | Job agendado após vencimento |

---

## O que precisa ser criado

### 1. Novo status no enum `StatusPagamento`

**Arquivo:** `domain/enums/StatusPagamento.java`

Adicione `AGUARDANDO_PAGAMENTO` ao enum seguindo o padrão **Enum com comportamento** já adotado no projeto:

```java
public enum StatusPagamento {
    PENDENTE(false),
    PROCESSANDO(false),
    AGUARDANDO_PAGAMENTO(false),  // novo — estado estável para boleto
    APROVADO(true),
    REJEITADO(true),
    CANCELADO(true);

    private final boolean estadoFinal;

    StatusPagamento(boolean estadoFinal) {
        this.estadoFinal = estadoFinal;
    }

    public boolean isEstadoFinal() {
        return estadoFinal;
    }
}
```

> **Por que não usar PENDENTE?**
> PENDENTE é um estado transitório (criado em memória, nunca persistido para PIX/Cartão).
> AGUARDANDO_PAGAMENTO é um estado estável que pode durar dias — semântica completamente diferente.

> **Por que `false`?**
> O método `Pagamento.estaEmEstadoFinal()` delega para `status.isEstadoFinal()`.
> Adicionando `AGUARDANDO_PAGAMENTO(false)`, esse método automaticamente retorna `false` para boletos aguardando — sem nenhuma alteração em `Pagamento`.

---

### 2. Atualizar o agregado `Pagamento`

**Arquivo:** `domain/model/Pagamento.java`

Adicione o método de transição e a query de estado:

```java
public void aguardarPagamento() {
    this.status = StatusPagamento.AGUARDANDO_PAGAMENTO;
    this.atualizadoEm = LocalDateTime.now();
}

public boolean estaAguardandoPagamento() {
    return StatusPagamento.AGUARDANDO_PAGAMENTO == this.status;
}
```

`estaEmEstadoFinal()` **não precisa ser alterado** — o enum já cuida disso.

---

### 3. Novo evento de domínio `BoletoGeradoEvento`

**Arquivo:** `domain/eventos/BoletoGeradoEvento.java`

```java
package com.eventmaster.paymentservice.domain.eventos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoletoGeradoEvento {

    private UUID pagamentoId;
    private UUID pedidoId;
    private UUID clienteId;
    private BigDecimal valor;
    private String linhaDigitavel;
    private LocalDate dataVencimento;
    private LocalDateTime ocorridoEm;
}
```

---

### 4. Resultado específico do boleto `ResultadoBoleto`

**Arquivo:** `application/processador/ResultadoBoleto.java`

O boleto não retorna aprovado/rejeitado — retorna os dados do boleto gerado.
Crie um resultado especializado:

```java
package com.eventmaster.paymentservice.application.processador;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ResultadoBoleto {

    private final String linhaDigitavel;
    private final LocalDate dataVencimento;

    private ResultadoBoleto(String linhaDigitavel, LocalDate dataVencimento) {
        this.linhaDigitavel = linhaDigitavel;
        this.dataVencimento = dataVencimento;
    }

    public static ResultadoBoleto gerado(String linhaDigitavel, LocalDate dataVencimento) {
        return new ResultadoBoleto(linhaDigitavel, dataVencimento);
    }
}
```

---

### 5. Nova porta de saída `BoletoGatewayPort`

**Arquivo:** `application/port/out/BoletoGatewayPort.java`

```java
package com.eventmaster.paymentservice.application.port.out;

import com.eventmaster.paymentservice.application.processador.ResultadoBoleto;
import com.eventmaster.paymentservice.domain.model.Pagamento;

/**
 * Porta de saída para geração de boleto via gateway bancário.
 * Em produção: Bradesco, Itaú, Asaas, PagSeguro, etc.
 * No ambiente atual: satisfeita por BoletoGatewayStub.
 */
public interface BoletoGatewayPort {

    ResultadoBoleto gerarBoleto(Pagamento pagamento, int diasVencimento);
}
```

---

### 6. Stub do gateway de boleto

**Arquivo:** `infrastructure/autorizador/BoletoGatewayStub.java`

```java
package com.eventmaster.paymentservice.infrastructure.autorizador;

import com.eventmaster.paymentservice.application.port.out.BoletoGatewayPort;
import com.eventmaster.paymentservice.application.processador.ResultadoBoleto;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class BoletoGatewayStub implements BoletoGatewayPort {

    @Override
    public ResultadoBoleto gerarBoleto(Pagamento pagamento, int diasVencimento) {
        // Linha digitável fictícia para demo — gerada com base no ID do pagamento
        String linhaDigitavel = "34191.09008 63521.980006 61980.069008 1 "
                + pagamento.getValor().toString().replace(".", "");
        LocalDate vencimento = LocalDate.now().plusDays(diasVencimento);
        return ResultadoBoleto.gerado(linhaDigitavel, vencimento);
    }
}
```

---

### 7. Processador de Boleto

**Arquivo:** `application/processador/impl/ProcessadorBoleto.java`

**Passo 7a — Adicione campos em `Pagamento`:**
```java
private String linhaDigitavel;
private LocalDate dataVencimento;
```

E o método de transição:
```java
public void registrarBoleto(String linhaDigitavel, LocalDate dataVencimento) {
    this.linhaDigitavel = linhaDigitavel;
    this.dataVencimento = dataVencimento;
    this.status = StatusPagamento.AGUARDANDO_PAGAMENTO;
    this.atualizadoEm = LocalDateTime.now();
}
```

**Passo 7b — O processador:**

> `ProcessadorBoleto` implementa `ProcessadorPagamento` diretamente, **sem estender `ProcessadorPagamentoBase`**.
> Isso é intencional: a classe base encapsula o fluxo de **autorização síncrona** (moeda → limite → autorizador externo).
> Boleto tem um fluxo diferente — não usa `AutorizadorExternoPort`, usa `BoletoGatewayPort`.
> Forçar herança aqui seria uma abstração incorreta.

```java
@Component
public class ProcessadorBoleto implements ProcessadorPagamento {

    private static final String MOEDA_ACEITA = "BRL";
    private static final int DIAS_VENCIMENTO = 3;

    private final BoletoGatewayPort boletoGateway;

    public ProcessadorBoleto(BoletoGatewayPort boletoGateway) {
        this.boletoGateway = boletoGateway;
    }

    @Override
    public TipoMetodoPagamento tipoPagamento() {
        return TipoMetodoPagamento.BOLETO;
    }

    @Override
    public ResultadoProcessamento processar(Pagamento pagamento) {
        if (!MOEDA_ACEITA.equals(pagamento.getMoeda())) {
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.NAO_AUTORIZADO);
        }
        ResultadoBoleto boleto = boletoGateway.gerarBoleto(pagamento, DIAS_VENCIMENTO);
        pagamento.registrarBoleto(boleto.getLinhaDigitavel(), boleto.getDataVencimento());
        return ResultadoProcessamento.aguardando();
    }
}
```

**Passo 7c — Adicione `aguardando()` em `ResultadoProcessamento`:**

O projeto usa **Command pattern** em `ResultadoProcessamento`: cada instância carrega a própria
ação de finalização como um lambda. Isso elimina qualquer `if/else` no `PagamentoService`.

Adicione apenas este factory method — o serviço **não precisa ser modificado**:

```java
public static ResultadoProcessamento aguardando() {
    return new ResultadoProcessamento((pagamento, repositorio, eventos) -> {
        // Pagamento já foi atualizado por registrarBoleto() dentro do processador
        Pagamento salvo = repositorio.salvar(pagamento);
        eventos.publicarBoletoGerado(salvo);
        return salvo;
    });
}
```

---

### 8. `PagamentoService.executarProcessamento()` — nenhuma alteração necessária

Graças ao Command pattern já aplicado no projeto, o método atual é:

```java
private Pagamento executarProcessamento(Pagamento pagamento) {
    pagamento.iniciarProcessamento();
    pagamento = repositorio.salvar(pagamento);

    ProcessadorPagamento processador = processadores.getOrDefault(
            pagamento.getMetodoPagamento(), ProcessadorNaoSuportado.INSTANCIA);

    ResultadoProcessamento resultado = processador.processar(pagamento);
    return resultado.finalizar(pagamento, repositorio, eventoPublicador);
}
```

**Zero ifs.** O `resultado.finalizar()` delega para o lambda correspondente (`aprovado`, `rejeitado`
ou `aguardando`) sem que o serviço precise saber qual é. Adicionar Boleto não altera esta classe.

---

### 9. Nova porta e adaptador para publicar `BoletoGeradoEvento`

Adicione em `PagamentoEventoPort`:
```java
void publicarBoletoGerado(Pagamento pagamento);
```

Implemente em `PagamentoEventoAdapter`:
```java
@Override
public void publicarBoletoGerado(Pagamento pagamento) {
    BoletoGeradoEvento evento = new BoletoGeradoEvento(
            pagamento.getId(),
            pagamento.getPedidoId(),
            pagamento.getClienteId(),
            pagamento.getValor(),
            pagamento.getLinhaDigitavel(),
            pagamento.getDataVencimento(),
            LocalDateTime.now());
    salvarNoOutbox("boleto.gerado", pagamento.getId().toString(), evento);
}
```

---

### 10. Webhook de confirmação de pagamento

**Arquivo:** `infrastructure/web/BoletoWebhookController.java`

Quando o banco confirmar o pagamento, ele chamará este endpoint:

```java
@RestController
@RequestMapping("/webhooks/boleto")
public class BoletoWebhookController {

    private final GerenciarPagamentoUseCase gerenciarPagamento;

    @PostMapping("/confirmacao")
    public ResponseEntity<Void> confirmarPagamento(@RequestBody ConfirmacaoBoletoDTO dto) {
        gerenciarPagamento.confirmarPagamentoBoleto(dto.getPagamentoId());
        return ResponseEntity.ok().build();
    }
}
```

Adicione `confirmarPagamentoBoleto(UUID id)` ao `GerenciarPagamentoUseCase` e implemente no service:
```java
// Busca o pagamento, chama pagamento.aprovar(), salva e publica PagamentoAprovadoEvento
```

---

### 11. Job de expiração de boletos

**Arquivo:** `infrastructure/messaging/BoletoExpiracaoJob.java`

```java
@Component
public class BoletoExpiracaoJob {

    private final PagamentoRepositorioPort repositorio;
    private final PagamentoEventoPort eventoPublicador;

    @Scheduled(cron = "0 0 8 * * *") // todo dia às 08h
    @Transactional
    public void expirarBoletosVencidos() {
        // Buscar pagamentos AGUARDANDO_PAGAMENTO com dataVencimento < hoje
        // Para cada um: pagamento.rejeitar(MotivoRejeicao.TIMEOUT), salvar, publicar evento
    }
}
```

Adicione ao `PagamentoRepositorioPort`:
```java
List<Pagamento> buscarBoletosVencidos(LocalDate dataReferencia);
```

---

## Resumo das mudanças por camada

```
domain/
  enums/StatusPagamento.java          → adicionar AGUARDANDO_PAGAMENTO
  model/Pagamento.java                → adicionar campos boleto + registrarBoleto()
  eventos/BoletoGeradoEvento.java     → NOVO

application/
  processador/ResultadoProcessamento  → adicionar aguardando()
  processador/ResultadoBoleto.java    → NOVO
  processador/impl/ProcessadorBoleto  → NOVO
  port/out/BoletoGatewayPort.java     → NOVO
  port/out/PagamentoEventoPort.java   → adicionar publicarBoletoGerado()
  port/out/PagamentoRepositorioPort   → adicionar buscarBoletosVencidos()
  port/in/GerenciarPagamentoUseCase   → adicionar confirmarPagamentoBoleto()
  service/PagamentoService.java       → adaptar executarProcessamento() + novo método

infrastructure/
  autorizador/BoletoGatewayStub.java  → NOVO
  persistence/PagamentoEntity.java    → adicionar colunas boleto
  messaging/PagamentoEventoAdapter    → implementar publicarBoletoGerado()
  messaging/BoletoExpiracaoJob.java   → NOVO
  web/BoletoWebhookController.java    → NOVO
```

---

## Tópico Kafka a criar

| Tópico | Produzido por | Consumido por |
|---|---|---|
| `boleto.gerado` | payment-service | order-service (para notificar o cliente) |

Adicione em `KafkaConfig.java`:
```java
@Bean
public NewTopic topicoBoletoGerado() {
    return TopicBuilder.name("boleto.gerado").partitions(1).replicas(1).build();
}
```

---

## Atenção — dados sensíveis

A `linhaDigitavel` não é dado PCI sensível (é pública por natureza).
Porém, **nunca persista** dados bancários do emissor do boleto sem criptografia.
