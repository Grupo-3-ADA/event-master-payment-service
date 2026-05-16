package com.eventmaster.paymentservice.domain.excecao;

import java.util.UUID;

public class PagamentoNaoEncontradoException extends RuntimeException {

    public PagamentoNaoEncontradoException(UUID id) {
        super("Pagamento não encontrado com id: " + id);
    }
}
