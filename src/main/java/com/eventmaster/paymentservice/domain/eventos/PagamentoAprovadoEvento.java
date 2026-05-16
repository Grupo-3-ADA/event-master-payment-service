package com.eventmaster.paymentservice.domain.eventos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoAprovadoEvento {

    private UUID pagamentoId;
    private UUID pedidoId;
    private UUID clienteId;
    private BigDecimal valor;
    private LocalDateTime ocorridoEm;
}
