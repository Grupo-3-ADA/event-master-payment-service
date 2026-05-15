package com.eventmaster.paymentservice.application.port.in;

import com.eventmaster.paymentservice.application.comando.CriarPagamentoComando;
import com.eventmaster.paymentservice.domain.model.Pagamento;

import java.util.UUID;

/**
 * Porta de entrada (driving port) do hexágono.
 * Define o que o sistema sabe fazer com pagamentos, sem expor detalhes de implementação.
 */
public interface GerenciarPagamentoUseCase {

    Pagamento criar(CriarPagamentoComando comando);

    Pagamento buscarPorId(UUID id);

    Pagamento processar(UUID id);
}
