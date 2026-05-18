package com.eventmaster.paymentservice.application.comando;

import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objeto de entrada do caso de uso de criação de pagamento.
 * Isola a camada de aplicação dos DTOs da camada web.
 */
@Getter
@Builder
@AllArgsConstructor
public class CriarPagamentoComando {

    private final UUID pedidoId;
    private final UUID clienteId;
    private final BigDecimal valor;
    private final String moeda;
    private final TipoMetodoPagamento metodoPagamento;
    private final String numeroCartao;
    private final String dataExpiracao;
    private final String cvv;
}
