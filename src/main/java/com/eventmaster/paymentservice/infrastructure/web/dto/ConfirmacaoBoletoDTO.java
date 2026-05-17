package com.eventmaster.paymentservice.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmacaoBoletoDTO {

    @NotNull
    private UUID pagamentoId;

    private LocalDate dataPagamento;

    private BigDecimal valorPago;
}
