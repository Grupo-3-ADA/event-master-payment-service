package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import lombok.Getter;

@Getter
public class ResultadoAutorizacao {

    private final boolean autorizado;
    private final MotivoRejeicao motivoRejeicao;

    private ResultadoAutorizacao(boolean autorizado, MotivoRejeicao motivoRejeicao) {
        this.autorizado = autorizado;
        this.motivoRejeicao = motivoRejeicao;
    }

    public static ResultadoAutorizacao autorizado() {
        return new ResultadoAutorizacao(true, null);
    }

    public static ResultadoAutorizacao negado(MotivoRejeicao motivo) {
        return new ResultadoAutorizacao(false, motivo);
    }
}
