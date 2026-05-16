package com.eventmaster.paymentservice.domain.eventos;

import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoRejeitadoEvento {

    private UUID pagamentoId;
    private UUID pedidoId;
    private UUID clienteId;
    private MotivoRejeicao motivo;
    private LocalDateTime ocorridoEm;
}
