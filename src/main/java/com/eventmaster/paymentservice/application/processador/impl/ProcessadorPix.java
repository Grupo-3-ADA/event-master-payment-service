package com.eventmaster.paymentservice.application.processador.impl;

import com.eventmaster.paymentservice.application.port.out.AutorizadorExternoPort;
import com.eventmaster.paymentservice.application.processador.ProcessadorPagamentoBase;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProcessadorPix extends ProcessadorPagamentoBase {

    private static final BigDecimal LIMITE_MINIMO = new BigDecimal("0.01");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("5000.00");

    public ProcessadorPix(AutorizadorExternoPort autorizador) {
        super(autorizador);
    }

    @Override
    public TipoMetodoPagamento tipoPagamento() {
        return TipoMetodoPagamento.PIX;
    }

    @Override
    protected BigDecimal limiteMinimo() {
        return LIMITE_MINIMO;
    }

    @Override
    protected BigDecimal limiteMaximo() {
        return LIMITE_MAXIMO;
    }
}
