package com.eventmaster.paymentservice.domain.enums;

public enum MotivoRejeicao {
    SALDO_INSUFICIENTE,
    CARTAO_INVALIDO,
    CARTAO_EXPIRADO,
    LIMITE_EXCEDIDO,
    SUSPEITA_FRAUDE,
    NAO_AUTORIZADO,
    ERRO_PROCESSAMENTO,
    TIMEOUT
}
