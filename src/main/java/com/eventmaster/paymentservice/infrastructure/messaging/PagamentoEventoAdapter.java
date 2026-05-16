package com.eventmaster.paymentservice.infrastructure.messaging;

import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.domain.eventos.PagamentoAprovadoEvento;
import com.eventmaster.paymentservice.domain.eventos.PagamentoRejeitadoEvento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import com.eventmaster.paymentservice.infrastructure.persistence.outbox.OutboxEvento;
import com.eventmaster.paymentservice.infrastructure.persistence.outbox.OutboxEventoJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Adaptador de saída: implementa a porta de eventos gravando no Outbox.
 * O Outbox garante atomicidade: evento e pagamento são persistidos na mesma transação JPA.
 * Um dispatcher separado (OutboxEventoDispatcher) lê o Outbox e publica no Kafka de forma assíncrona.
 */
@Component
public class PagamentoEventoAdapter implements PagamentoEventoPort {

    private static final String TOPICO_APROVADO = "pagamento.aprovado";
    private static final String TOPICO_REJEITADO = "pagamento.rejeitado";

    private final OutboxEventoJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PagamentoEventoAdapter(OutboxEventoJpaRepository outboxRepository,
                                   ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publicarPagamentoAprovado(Pagamento pagamento) {
        PagamentoAprovadoEvento evento = new PagamentoAprovadoEvento(
                pagamento.getId(),
                pagamento.getPedidoId(),
                pagamento.getClienteId(),
                pagamento.getValor(),
                LocalDateTime.now());
        salvarNoOutbox(TOPICO_APROVADO, pagamento.getId().toString(), evento);
    }

    @Override
    public void publicarPagamentoRejeitado(Pagamento pagamento) {
        PagamentoRejeitadoEvento evento = new PagamentoRejeitadoEvento(
                pagamento.getId(),
                pagamento.getPedidoId(),
                pagamento.getClienteId(),
                pagamento.getMotivoRejeicao(),
                LocalDateTime.now());
        salvarNoOutbox(TOPICO_REJEITADO, pagamento.getId().toString(), evento);
    }

    private void salvarNoOutbox(String topico, String chave, Object evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            outboxRepository.save(OutboxEvento.builder()
                    .topico(topico)
                    .chave(chave)
                    .payload(payload)
                    .processado(false)
                    .criadoEm(LocalDateTime.now())
                    .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao serializar evento para outbox", e);
        }
    }
}
