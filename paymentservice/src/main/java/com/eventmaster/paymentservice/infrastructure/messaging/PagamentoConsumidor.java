package com.eventmaster.paymentservice.infrastructure.messaging;

import com.eventmaster.paymentservice.application.port.in.GerenciarPagamentoUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador de entrada (driving): consome solicitações de reprocessamento de serviços externos.
 * Exemplo: um serviço de monitoramento identifica pagamentos travados e solicita nova tentativa.
 */
@Component
public class PagamentoConsumidor {

    private final GerenciarPagamentoUseCase gerenciarPagamento;

    public PagamentoConsumidor(GerenciarPagamentoUseCase gerenciarPagamento) {
        this.gerenciarPagamento = gerenciarPagamento;
    }

    @KafkaListener(topics = "pagamento.reprocessar", groupId = "${spring.kafka.consumer.group-id}")
    public void consumirReprocessamento(String pagamentoId) {
        gerenciarPagamento.processar(UUID.fromString(pagamentoId));
    }
}
