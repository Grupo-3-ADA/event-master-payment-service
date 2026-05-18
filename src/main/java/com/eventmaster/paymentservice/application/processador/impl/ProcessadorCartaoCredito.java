package com.eventmaster.paymentservice.application.processador.impl;

import com.eventmaster.paymentservice.application.port.out.AutorizadorExternoPort;
import com.eventmaster.paymentservice.application.processador.ProcessadorPagamentoBase;
import com.eventmaster.paymentservice.application.processador.ResultadoProcessamento;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class ProcessadorCartaoCredito extends ProcessadorPagamentoBase {

    private static final BigDecimal LIMITE_MINIMO = new BigDecimal("1.00");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("10000.00");

    public ProcessadorCartaoCredito(AutorizadorExternoPort autorizador) {
        super(autorizador);
    }

    @Override
    public TipoMetodoPagamento tipoPagamento() {
        return TipoMetodoPagamento.CARTAO_CREDITO;
    }

    @Override
    protected BigDecimal limiteMinimo() {
        return LIMITE_MINIMO;
    }

    @Override
    protected BigDecimal limiteMaximo() {
        return LIMITE_MAXIMO;
    }

    @Override
    protected Optional<ResultadoProcessamento> validacoesEspecificas(Pagamento pagamento) {
        if (pagamento.getNumeroCartao() == null || pagamento.getNumeroCartao().isBlank()
                || pagamento.getCvv() == null || pagamento.getCvv().isBlank()
                || pagamento.getDataExpiracao() == null || pagamento.getDataExpiracao().isBlank()) {
            return Optional.of(ResultadoProcessamento.rejeitado(MotivoRejeicao.CARTAO_INVALIDO));
        }
        return Optional.empty();
    }
}
