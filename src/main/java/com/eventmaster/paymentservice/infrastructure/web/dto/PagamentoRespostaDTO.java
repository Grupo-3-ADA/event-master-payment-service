package com.eventmaster.paymentservice.infrastructure.web.dto;

import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoRespostaDTO {

    private UUID id;
    private UUID pedidoId;
    private UUID clienteId;
    private BigDecimal valor;
    private String moeda;
    private StatusPagamento status;
    private TipoMetodoPagamento metodoPagamento;
    private MotivoRejeicao motivoRejeicao;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
