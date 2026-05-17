package com.eventmaster.paymentservice.application.processador;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ResultadoBoleto {

    private final String linhaDigitavel;
    private final LocalDate dataVencimento;

    private ResultadoBoleto(String linhaDigitavel, LocalDate dataVencimento) {
        this.linhaDigitavel = linhaDigitavel;
        this.dataVencimento = dataVencimento;
    }

    public static ResultadoBoleto gerado(String linhaDigitavel, LocalDate dataVencimento) {
        return new ResultadoBoleto(linhaDigitavel, dataVencimento);
    }
}
