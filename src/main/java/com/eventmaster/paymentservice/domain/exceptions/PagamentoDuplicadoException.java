package com.eventmaster.paymentservice.domain.exceptions;

import com.eventmaster.paymentservice.domain.model.Pagamento;

public class PagamentoDuplicadoException extends RuntimeException {

    private final Pagamento pagamentoExistente;

    public PagamentoDuplicadoException(Pagamento pagamentoExistente) {
        super("Pagamento já existe para o pedido " + pagamentoExistente.getPedidoId());
        this.pagamentoExistente = pagamentoExistente;
    }

    public Pagamento getPagamentoExistente() {
        return pagamentoExistente;
    }
}
