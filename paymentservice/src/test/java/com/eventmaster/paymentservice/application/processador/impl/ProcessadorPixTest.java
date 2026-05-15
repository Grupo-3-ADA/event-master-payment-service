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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProcessadorPix — regras de negócio")
class ProcessadorPixTest {

    private ProcessadorPix processador;

    @BeforeEach
    void configurar() {
        processador = new ProcessadorPix();
    }

    @Test
    @DisplayName("deve retornar PIX como tipo de pagamento")
    void deveRetornarTipoCorreto() {
        assertThat(processador.tipoPagamento()).isEqualTo(TipoMetodoPagamento.PIX);
    }

    // ── Cenários de aprovação ──────────────────────────────────────────────

    @Test
    @DisplayName("deve aprovar pagamento PIX dentro dos limites")
    void deveAprovarPagamentoDentroDoLimite() {
        Pagamento pagamento = construirPagamento(new BigDecimal("100.00"), "BRL");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isTrue();
        assertThat(resultado.getMotivoRejeicao()).isNull();
    }

    @Test
    @DisplayName("deve aprovar pagamento no valor mínimo exato (R$ 0,01)")
    void deveAprovarPagamentoNoValorMinimo() {
        Pagamento pagamento = construirPagamento(new BigDecimal("0.01"), "BRL");

        assertThat(processador.processar(pagamento).isFoiAprovado()).isTrue();
    }

    @Test
    @DisplayName("deve aprovar pagamento no valor máximo exato (R$ 5.000,00)")
    void deveAprovarPagamentoNoValorMaximo() {
        Pagamento pagamento = construirPagamento(new BigDecimal("5000.00"), "BRL");

        assertThat(processador.processar(pagamento).isFoiAprovado()).isTrue();
    }

    // ── Cenários de rejeição ───────────────────────────────────────────────

    @Test
    @DisplayName("deve rejeitar quando moeda não é BRL")
    void deveRejeitarMoedaInvalida() {
        Pagamento pagamento = construirPagamento(new BigDecimal("100.00"), "USD");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.NAO_AUTORIZADO);
    }

    @Test
    @DisplayName("deve rejeitar quando valor é inferior ao mínimo (R$ 0,01)")
    void deveRejeitarValorAbaixoDoMinimo() {
        Pagamento pagamento = construirPagamento(new BigDecimal("0.00"), "BRL");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.NAO_AUTORIZADO);
    }

    @Test
    @DisplayName("deve rejeitar quando valor excede o limite (R$ 5.000,00)")
    void deveRejeitarValorAcimaDoMaximo() {
        Pagamento pagamento = construirPagamento(new BigDecimal("5000.01"), "BRL");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.LIMITE_EXCEDIDO);
    }

    @Test
    @DisplayName("deve rejeitar valor negativo")
    void deveRejeitarValorNegativo() {
        Pagamento pagamento = construirPagamento(new BigDecimal("-1.00"), "BRL");

        ResultadoProcessamento resultado = processador.processar(pagamento);

        assertThat(resultado.isFoiAprovado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.NAO_AUTORIZADO);
    }

    // ── Utilitário ─────────────────────────────────────────────────────────

    private Pagamento construirPagamento(BigDecimal valor, String moeda) {
        return Pagamento.reconstituir(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                valor, moeda, TipoMetodoPagamento.PIX,
                null, null, null,
                StatusPagamento.PENDENTE, null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
