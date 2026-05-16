package com.eventmaster.paymentservice.infrastructure.messaging;

import com.eventmaster.paymentservice.infrastructure.persistence.outbox.OutboxEvento;
import com.eventmaster.paymentservice.infrastructure.persistence.outbox.OutboxEventoJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lê eventos pendentes do Outbox e os publica no Kafka.
 * Roda em loop com intervalo configurável via outbox.dispatch.delay-ms (padrão: 5 s).
 * Separado da transação de negócio — garante at-least-once delivery sem bloquear a requisição.
 */
@Slf4j
@Component
public class OutboxEventoDispatcher {

    private final OutboxEventoJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    public OutboxEventoDispatcher(OutboxEventoJpaRepository outboxRepository,
                                   KafkaTemplate<String, String> stringKafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.stringKafkaTemplate = stringKafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.dispatch.delay-ms:5000}")
    @Transactional
    public void despacharEventosPendentes() {
        List<OutboxEvento> pendentes = outboxRepository.findByProcessadoFalseOrderByCriadoEmAsc();
        if (pendentes.isEmpty()) {
            return;
        }
        log.info("Outbox: despachando {} evento(s) pendente(s)", pendentes.size());
        for (OutboxEvento evento : pendentes) {
            stringKafkaTemplate.send(evento.getTopico(), evento.getChave(), evento.getPayload());
            outboxRepository.marcarComoProcessado(evento.getId());
            log.debug("Outbox: evento {} enviado para tópico '{}'", evento.getId(), evento.getTopico());
        }
    }
}
