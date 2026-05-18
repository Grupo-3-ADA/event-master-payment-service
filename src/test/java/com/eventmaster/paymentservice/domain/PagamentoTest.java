package com.eventmaster.paymentservice.domain;

import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoTest {

    private Pagamento pagamentoPix() {
        return Pagamento.novo(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "BRL", TipoMetodoPagamento.PIX,
                null, null, null);
    }

    @Test
    void deveCriarPagamentoComStatusPendente() {
        Pagamento pagamento = pagamentoPix();

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(pagamento.getId()).isNull();
        assertThat(pagamento.getMotivoRejeicao()).isNull();
    }

    @Test
    void deveTransicionarParaProcessando() {
        Pagamento pagamento = pagamentoPix();
        pagamento.iniciarProcessamento();

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PROCESSANDO);
    }

    @Test
    void deveAprovarPagamento() {
        Pagamento pagamento = pagamentoPix();
        pagamento.iniciarProcessamento();
        pagamento.aprovar();

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(pagamento.estaEmEstadoFinal()).isTrue();
    }

    @Test
    void deveRejeitarPagamentoComMotivo() {
        Pagamento pagamento = pagamentoPix();
        pagamento.iniciarProcessamento();
        pagamento.rejeitar(MotivoRejeicao.LIMITE_EXCEDIDO);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(pagamento.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.LIMITE_EXCEDIDO);
        assertThat(pagamento.estaEmEstadoFinal()).isTrue();
    }

    @Test
    void deveRegistrarBoletoETransicionarParaAguardando() {
        Pagamento pagamento = Pagamento.novo(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("200.00"), "BRL", TipoMetodoPagamento.BOLETO,
                null, null, null);
        pagamento.iniciarProcessamento();

        String linhaDigitavel = "34191.09008 63521.980006 61980.069008 1 00000020000";
        LocalDate vencimento = LocalDate.now().plusDays(3);
        pagamento.registrarBoleto(linhaDigitavel, vencimento);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.AGUARDANDO_PAGAMENTO);
        assertThat(pagamento.getLinhaDigitavel()).isEqualTo(linhaDigitavel);
        assertThat(pagamento.getDataVencimento()).isEqualTo(vencimento);
        assertThat(pagamento.estaAguardandoPagamento()).isTrue();
    }

    @Test
    void estadoFinalDeveSerVerdadeiroParaAprovadoRejeitadoCancelado() {
        assertThat(StatusPagamento.APROVADO.isEstadoFinal()).isTrue();
        assertThat(StatusPagamento.REJEITADO.isEstadoFinal()).isTrue();
        assertThat(StatusPagamento.CANCELADO.isEstadoFinal()).isTrue();
    }

    @Test
    void estadoFinalDeveSerFalsoParaEstadosTransitorios() {
        assertThat(StatusPagamento.PENDENTE.isEstadoFinal()).isFalse();
        assertThat(StatusPagamento.PROCESSANDO.isEstadoFinal()).isFalse();
        assertThat(StatusPagamento.AGUARDANDO_PAGAMENTO.isEstadoFinal()).isFalse();
    }

    @Test
    void deveLancarExcecaoQuandoPedidoIdForNulo() {
        assertThatThrownBy(() ->
                Pagamento.novo(null, UUID.randomUUID(), new BigDecimal("10.00"),
                        "BRL", TipoMetodoPagamento.PIX, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
