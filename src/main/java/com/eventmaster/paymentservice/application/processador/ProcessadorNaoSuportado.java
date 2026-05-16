package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;

/**
 * Null Object para ProcessadorPagamento.
 * Usado como fallback quando nenhum processador está registrado para o método de pagamento.
 * Elimina o null check no serviço de aplicação (Open/Closed Principle).
 */
public final class ProcessadorNaoSuportado implements ProcessadorPagamento {

    public static final ProcessadorNaoSuportado INSTANCIA = new ProcessadorNaoSuportado();

    private ProcessadorNaoSuportado() {}

    @Override
    public TipoMetodoPagamento tipoPagamento() {
        throw new UnsupportedOperationException("Null Object não possui tipo definido");
    }

    @Override
    public ResultadoProcessamento processar(Pagamento pagamento) {
        return ResultadoProcessamento.rejeitado(MotivoRejeicao.ERRO_PROCESSAMENTO);
    }
}
