package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;

/**
 * Contrato do padrão Strategy: cada método de pagamento implementa suas próprias regras.
 */
public interface ProcessadorPagamento {

    TipoMetodoPagamento tipoPagamento();

    ResultadoProcessamento processar(Pagamento pagamento);
}
