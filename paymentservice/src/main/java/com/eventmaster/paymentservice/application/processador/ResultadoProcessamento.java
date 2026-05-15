package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import lombok.Getter;

@Getter
public class ResultadoProcessamento {

    private final boolean foiAprovado;
    private final MotivoRejeicao motivoRejeicao;

    private ResultadoProcessamento(boolean foiAprovado, MotivoRejeicao motivoRejeicao) {
        this.foiAprovado = foiAprovado;
        this.motivoRejeicao = motivoRejeicao;
    }

    public static ResultadoProcessamento aprovado() {
        return new ResultadoProcessamento(true, null);
    }

    public static ResultadoProcessamento rejeitado(MotivoRejeicao motivo) {
        return new ResultadoProcessamento(false, motivo);
    }
}
