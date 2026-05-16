package com.eventmaster.paymentservice.domain.enums;

public enum StatusPagamento {

    PENDENTE(false),
    PROCESSANDO(false),
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
