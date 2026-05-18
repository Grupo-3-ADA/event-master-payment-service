package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.application.port.out.AutorizadorExternoPort;
import com.eventmaster.paymentservice.application.processador.impl.ProcessadorPix;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
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
class ProcessadorPixTest {

    @Mock AutorizadorExternoPort autorizador;
    @Mock PagamentoRepositorioPort repositorio;
    @Mock PagamentoEventoPort eventos;

    ProcessadorPix processador;

    @BeforeEach
    void setUp() {
        processador = new ProcessadorPix(autorizador);
        when(repositorio.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void deveAprovarPixValido() {
        Pagamento pagamento = pixCom(new BigDecimal("100.00"), "BRL");
        when(autorizador.autorizar(any())).thenReturn(ResultadoAutorizacao.autorizado());

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(eventos).publicarPagamentoAprovado(any());
    }

    @Test
    void deveRejeitarQuandoMoedaNaoForBRL() {
        Pagamento pagamento = pixCom(new BigDecimal("100.00"), "USD");

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.NAO_AUTORIZADO);
        verifyNoInteractions(autorizador);
    }

    @Test
    void deveRejeitarQuandoValorAcimaDoLimite() {
        Pagamento pagamento = pixCom(new BigDecimal("5001.00"), "BRL");

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.LIMITE_EXCEDIDO);
        verifyNoInteractions(autorizador);
    }

    @Test
    void deveRejeitarQuandoValorAbaixoDoMinimo() {
        Pagamento pagamento = pixCom(new BigDecimal("0.005"), "BRL");

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.LIMITE_EXCEDIDO);
        verifyNoInteractions(autorizador);
    }

    private Pagamento pixCom(BigDecimal valor, String moeda) {
        return Pagamento.novo(UUID.randomUUID(), UUID.randomUUID(),
                valor, moeda, TipoMetodoPagamento.PIX, null, null, null);
    }
}
