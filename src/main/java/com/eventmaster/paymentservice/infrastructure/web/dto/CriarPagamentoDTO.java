package com.eventmaster.paymentservice.infrastructure.web.dto;

import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarPagamentoDTO {

    @NotNull(message = "O id do pedido é obrigatório")
    private UUID pedidoId;

    @NotNull(message = "O id do cliente é obrigatório")
    private UUID clienteId;

    @NotNull(message = "O valor é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    private BigDecimal valor;

    @NotNull(message = "A moeda é obrigatória")
    @Size(min = 3, max = 3, message = "A moeda deve ter 3 caracteres (ex: BRL)")
    private String moeda;

    @NotNull(message = "O método de pagamento é obrigatório")
    private TipoMetodoPagamento metodoPagamento;

    private String numeroCartao;
    private String dataExpiracao;
    private String cvv;
}
