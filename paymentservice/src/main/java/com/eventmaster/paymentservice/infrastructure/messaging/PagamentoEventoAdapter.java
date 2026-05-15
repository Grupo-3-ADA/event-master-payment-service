package com.eventmaster.paymentservice.infrastructure.messaging;

import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.domain.evento.PagamentoAprovadoEvento;
import com.eventmaster.paymentservice.domain.evento.PagamentoRejeitadoEvento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de saída: implementa a porta de eventos publicando no Kafka.
 * Saída da coreografia SAGA — outros serviços consomem estes eventos.
 */
@Component
public class PagamentoEventoAdapter implements PagamentoEventoPort {

    private static final String TOPICO_APROVADO = "pagamento.aprovado";
    private static final String TOPICO_REJEITADO = "pagamento.rejeitado";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PagamentoEventoAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publicarPagamentoAprovado(Pagamento pagamento) {
        PagamentoAprovadoEvento evento = new PagamentoAprovadoEvento(
                pagamento.getId(),
                pagamento.getPedidoId(),
                pagamento.getClienteId(),
                pagamento.getValor());
        kafkaTemplate.send(TOPICO_APROVADO, pagamento.getId().toString(), evento);
    }

    @Override
    public void publicarPagamentoRejeitado(Pagamento pagamento) {
        PagamentoRejeitadoEvento evento = new PagamentoRejeitadoEvento(
                pagamento.getId(),
                pagamento.getPedidoId(),
                pagamento.getClienteId(),
                pagamento.getMotivoRejeicao());
        kafkaTemplate.send(TOPICO_REJEITADO, pagamento.getId().toString(), evento);
    }
}
