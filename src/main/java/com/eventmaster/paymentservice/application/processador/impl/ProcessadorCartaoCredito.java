package com.eventmaster.paymentservice.application.processador.impl;

import com.eventmaster.paymentservice.application.processador.ProcessadorPagamento;
import com.eventmaster.paymentservice.application.processador.ResultadoProcessamento;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class ProcessadorCartaoCredito implements ProcessadorPagamento {

    private static final BigDecimal LIMITE_MINIMO = new BigDecimal("1.00");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("10000.00");
    private static final String MOEDA_ACEITA = "BRL";
    private static final DateTimeFormatter FORMATO_EXPIRACAO = DateTimeFormatter.ofPattern("MM/yy");

    @Override
    public TipoMetodoPagamento tipoPagamento() {
        return TipoMetodoPagamento.CARTAO_CREDITO;
    }

    @Override
    public ResultadoProcessamento processar(Pagamento pagamento) {
        if (!moedaValida(pagamento.getMoeda())) {
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.NAO_AUTORIZADO);
        }

        if (!limiteValido(pagamento.getValor())) {
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.LIMITE_EXCEDIDO);
        }

        if (!numeroCartaoValido(pagamento.getNumeroCartao())) {
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.CARTAO_INVALIDO);
        }

        if (!cartaoNaoExpirado(pagamento.getDataExpiracao())) {
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.CARTAO_EXPIRADO);
        }

        if (!cvvValido(pagamento.getCvv())) {
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.CARTAO_INVALIDO);
        }

        return ResultadoProcessamento.aprovado();
    }

    private boolean moedaValida(String moeda) {
        return MOEDA_ACEITA.equals(moeda);
    }

    private boolean limiteValido(BigDecimal valor) {
        return valor.compareTo(LIMITE_MINIMO) >= 0 && valor.compareTo(LIMITE_MAXIMO) <= 0;
    }

    private boolean numeroCartaoValido(String numero) {
        if (numero == null || numero.isBlank()) return false;

        String digitos = numero.replaceAll("[\\s-]", "");

        if (!digitos.matches("\\d{13,19}")) return false;

        return passaLuhn(digitos);
    }

    private boolean passaLuhn(String digitos) {
        int soma = 0;
        boolean dobrar = false;

        for (int i = digitos.length() - 1; i >= 0; i--) {
            int digito = digitos.charAt(i) - '0';

            if (dobrar) {
                digito *= 2;
                if (digito > 9) digito -= 9;
            }

            soma += digito;
            dobrar = !dobrar;
        }

        return soma % 10 == 0;
    }

    private boolean cartaoNaoExpirado(String dataExpiracao) {
        if (dataExpiracao == null || dataExpiracao.isBlank()) return false;

        try {
            YearMonth vencimento = YearMonth.parse(dataExpiracao, FORMATO_EXPIRACAO);
            return !vencimento.isBefore(YearMonth.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean cvvValido(String cvv) {
        return cvv != null && cvv.matches("\\d{3,4}");
    }
}
