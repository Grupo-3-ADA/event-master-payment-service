package com.eventmaster.paymentservice.application.port.out;

import com.eventmaster.paymentservice.application.processador.ResultadoAutorizacao;
import com.eventmaster.paymentservice.domain.model.Pagamento;

/**
 * Porta de saída para autorização externa.
 * Em produção seria implementada chamando um gateway (Cielo, Stone, DICT/PIX).
 * No ambiente atual é satisfeita por AutorizadorExternoStub.
 */
public interface AutorizadorExternoPort {

    ResultadoAutorizacao autorizar(Pagamento pagamento);
}
