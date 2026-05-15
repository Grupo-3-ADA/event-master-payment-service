package com.eventmaster.paymentservice.application.processador.impl;

import com.eventmaster.paymentservice.application.processador.ProcessadorPagamento;
import com.eventmaster.paymentservice.application.processador.ResultadoProcessamento;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProcessadorPix implements ProcessadorPagamento {

    private static final BigDecimal LIMITE_MAXIMO_PIX = new BigDecimal("5000.00");
    private static final BigDecimal LIMITE_MINIMO_PIX = new BigDecimal("0.01");
    private static final String MOEDA_ACEITA = "BRL";

    @Override
    public TipoMetodoPagamento tipoPagamento() {
        return TipoMetodoPagamento.PIX;
    }

    @Override
    public ResultadoProcessamento processar(Pagamento pagamento) {
        if (!MOEDA_ACEITA.equals(pagamento.getMoeda())) {
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.NAO_AUTORIZADO);
        }

        if (pagamento.getValor().compareTo(LIMITE_MINIMO_PIX) < 0) {
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.NAO_AUTORIZADO);
        }

        if (pagamento.getValor().compareTo(LIMITE_MAXIMO_PIX) > 0) {
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.LIMITE_EXCEDIDO);
        }

        return ResultadoProcessamento.aprovado();
    }
}
