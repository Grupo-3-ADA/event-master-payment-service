package com.eventmaster.paymentservice.application.processador.impl;

import com.eventmaster.paymentservice.application.processador.ResultadoProcessamento;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProcessadorCartaoCredito — regras de negócio")
class ProcessadorCartaoCreditoTest {

    private ProcessadorCartaoCredito processador;

    // Número de cartão válido pelo algoritmo de Luhn (Visa de teste)
    private static final String CARTAO_VALIDO = "4111111111111111";
    private static final String CARTAO_INVALIDO = "1234567890123456";

    @BeforeEach
    void configurar() {
        processador = new ProcessadorCartaoCredito();
    }

    @Test
    @DisplayName("deve retornar CARTAO_CREDITO como tipo de pagamento")
    void deveRetornarTipoCorreto() {
        assertThat(processador.tipoPagamento()).isEqualTo(TipoMetodoPagamento.CARTAO_CREDITO);
    }

    // ── Aprovação ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve aprovar pagamento com todos os dados válidos")
    void deveAprovarPagamentoValido() {
        Pagamento pagamento = construirPagamento("500.00", "BRL", CARTAO_VALIDO, expiraProximoMes(), "123");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isTrue();
        assertThat(resultado.getMotivoRejeicao()).isNull();
    }

    @Test
    @DisplayName("deve aprovar no valor mínimo (R$ 1,00)")
    void deveAprovarNoValorMinimo() {
        Pagamento pagamento = construirPagamento("1.00", "BRL", CARTAO_VALIDO, expiraProximoMes(), "123");

        assertThat(processador.processar(pagamento).isFoiAprovado()).isTrue();
    }

    @Test
    @DisplayName("deve aprovar no valor máximo (R$ 10.000,00)")
    void deveAprovarNoValorMaximo() {
        Pagamento pagamento = construirPagamento("10000.00", "BRL", CARTAO_VALIDO, expiraProximoMes(), "123");

        assertThat(processador.processar(pagamento).isFoiAprovado()).isTrue();
    }

    // ── Moeda ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve rejeitar quando moeda não é BRL")
    void deveRejeitarMoedaInvalida() {
        Pagamento pagamento = construirPagamento("500.00", "USD", CARTAO_VALIDO, expiraProximoMes(), "123");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.NAO_AUTORIZADO);
    }

    // ── Limites ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve rejeitar valor abaixo do mínimo (R$ 0,99)")
    void deveRejeitarValorAbaixoDoMinimo() {
        Pagamento pagamento = construirPagamento("0.99", "BRL", CARTAO_VALIDO, expiraProximoMes(), "123");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.LIMITE_EXCEDIDO);
    }

    @Test
    @DisplayName("deve rejeitar valor acima do máximo (R$ 10.000,01)")
    void deveRejeitarValorAcimaDoMaximo() {
        Pagamento pagamento = construirPagamento("10000.01", "BRL", CARTAO_VALIDO, expiraProximoMes(), "123");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.LIMITE_EXCEDIDO);
    }

    // ── Número do cartão ───────────────────────────────────────────────────

    @Test
    @DisplayName("deve rejeitar número de cartão inválido (falha no Luhn)")
    void deveRejeitarCartaoInvalido() {
        Pagamento pagamento = construirPagamento("500.00", "BRL", CARTAO_INVALIDO, expiraProximoMes(), "123");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_INVALIDO);
    }

    @Test
    @DisplayName("deve rejeitar quando número do cartão está em branco")
    void deveRejeitarCartaoEmBranco() {
        Pagamento pagamento = construirPagamento("500.00", "BRL", "", expiraProximoMes(), "123");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_INVALIDO);
    }

    @Test
    @DisplayName("deve rejeitar quando número do cartão é nulo")
    void deveRejeitarCartaoNulo() {
        Pagamento pagamento = construirPagamento("500.00", "BRL", null, expiraProximoMes(), "123");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_INVALIDO);
    }

    // ── Expiração ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve rejeitar cartão expirado")
    void deveRejeitarCartaoExpirado() {
        Pagamento pagamento = construirPagamento("500.00", "BRL", CARTAO_VALIDO, "01/20", "123");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_EXPIRADO);
    }

    @Test
    @DisplayName("deve rejeitar data de expiração com formato inválido")
    void deveRejeitarDataExpiracaoFormatoInvalido() {
        Pagamento pagamento = construirPagamento("500.00", "BRL", CARTAO_VALIDO, "13/2025", "123");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_EXPIRADO);
    }

    // ── CVV ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve rejeitar CVV com menos de 3 dígitos")
    void deveRejeitarCvvCurto() {
        Pagamento pagamento = construirPagamento("500.00", "BRL", CARTAO_VALIDO, expiraProximoMes(), "12");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_INVALIDO);
    }

    @Test
    @DisplayName("deve aceitar CVV com 4 dígitos (Amex)")
    void deveAceitarCvvComQuatroDigitos() {
        Pagamento pagamento = construirPagamento("500.00", "BRL", CARTAO_VALIDO, expiraProximoMes(), "1234");

        assertThat(processador.processar(pagamento).isFoiAprovado()).isTrue();
    }

    @Test
    @DisplayName("deve rejeitar CVV nulo")
    void deveRejeitarCvvNulo() {
        Pagamento pagamento = construirPagamento("500.00", "BRL", CARTAO_VALIDO, expiraProximoMes(), null);

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_INVALIDO);
    }

    // ── Utilitários ────────────────────────────────────────────────────────

    private Pagamento construirPagamento(String valor, String moeda,
                                         String numeroCartao, String dataExpiracao, String cvv) {
        return Pagamento.reconstituir(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal(valor), moeda, TipoMetodoPagamento.CARTAO_CREDITO,
                numeroCartao, dataExpiracao, cvv,
                StatusPagamento.PENDENTE, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private String expiraProximoMes() {
        return YearMonth.now().plusMonths(1).format(DateTimeFormatter.ofPattern("MM/yy"));
    }
}
