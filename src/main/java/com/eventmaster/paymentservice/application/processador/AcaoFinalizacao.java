package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.domain.model.Pagamento;

/**
 * Command que encapsula a ação de finalização de um processamento.
 * Cada ResultadoProcessamento carrega sua própria implementação,
 * eliminando if/else no serviço de aplicação (Command + Strategy).
 */
@FunctionalInterface
public interface AcaoFinalizacao {

    Pagamento executar(Pagamento pagamento, PagamentoRepositorioPort repositorio, PagamentoEventoPort eventos);
}
