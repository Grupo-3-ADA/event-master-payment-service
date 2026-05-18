package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.application.port.out.AutorizadorExternoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.application.processador.impl.ProcessadorCartaoCredito;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessadorCartaoCreditoTest {

    @Mock AutorizadorExternoPort autorizador;
    @Mock PagamentoRepositorioPort repositorio;
    @Mock PagamentoEventoPort eventos;

    ProcessadorCartaoCredito processador;

    @BeforeEach
    void setUp() {
        processador = new ProcessadorCartaoCredito(autorizador);
        when(repositorio.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void deveAprovarCartaoValido() {
        Pagamento pagamento = cartaoCom("4111111111111111", "12/29", "123", new BigDecimal("500.00"), "BRL");
        when(autorizador.autorizar(any())).thenReturn(ResultadoAutorizacao.autorizado());

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(eventos).publicarPagamentoAprovado(any());
    }

    @Test
    void deveRejeitarQuandoMoedaNaoForBRL() {
        Pagamento pagamento = cartaoCom("4111111111111111", "12/29", "123", new BigDecimal("500.00"), "USD");

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.NAO_AUTORIZADO);
        verifyNoInteractions(autorizador);
    }

    @Test
    void deveRejeitarQuandoNumeroCartaoEstiverAusente() {
        Pagamento pagamento = cartaoCom(null, "12/29", "123", new BigDecimal("500.00"), "BRL");

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_INVALIDO);
        verifyNoInteractions(autorizador);
    }

    @Test
    void deveRejeitarQuandoValorExcedeLimiteMaximo() {
        Pagamento pagamento = cartaoCom("4111111111111111", "12/29", "123", new BigDecimal("10001.00"), "BRL");

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.LIMITE_EXCEDIDO);
        verifyNoInteractions(autorizador);
    }

    private Pagamento cartaoCom(String numero, String expiracao, String cvv, BigDecimal valor, String moeda) {
        return Pagamento.novo(UUID.randomUUID(), UUID.randomUUID(),
                valor, moeda, TipoMetodoPagamento.CARTAO_CREDITO,
                numero, expiracao, cvv);
    }
}
