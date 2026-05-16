package com.eventmaster.paymentservice.application.port.out;

import com.eventmaster.paymentservice.domain.model.Pagamento;

/**
 * Porta de saída (driven port) para publicação de eventos de domínio.
 * A aplicação não conhece Kafka — apenas que existe um meio de publicar eventos.
 */
public interface PagamentoEventoPort {

    void publicarPagamentoAprovado(Pagamento pagamento);

    void publicarPagamentoRejeitado(Pagamento pagamento);
}
