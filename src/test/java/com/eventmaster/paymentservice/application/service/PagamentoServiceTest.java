package com.eventmaster.paymentservice.application.service;

import com.eventmaster.paymentservice.application.comando.CriarPagamentoComando;
import com.eventmaster.paymentservice.application.port.out.AutorizadorExternoPort;
import com.eventmaster.paymentservice.application.port.out.BoletoGatewayPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.application.processador.ResultadoAutorizacao;
import com.eventmaster.paymentservice.application.processador.ResultadoBoleto;
import com.eventmaster.paymentservice.application.processador.impl.ProcessadorBoleto;
import com.eventmaster.paymentservice.application.processador.impl.ProcessadorCartaoCredito;
import com.eventmaster.paymentservice.application.processador.impl.ProcessadorPix;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.exceptions.PagamentoDuplicadoException;
import com.eventmaster.paymentservice.domain.exceptions.PagamentoNaoEncontradoException;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagamentoServiceTest {

    @Mock PagamentoRepositorioPort repositorio;
    @Mock PagamentoEventoPort eventoPublicador;
    @Mock AutorizadorExternoPort autorizador;
    @Mock BoletoGatewayPort boletoGateway;

    PagamentoService service;

    private static final UUID PEDIDO_ID = UUID.randomUUID();
    private static final UUID CLIENTE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(repositorio.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcessadorPix processadorPix = new ProcessadorPix(autorizador);
        ProcessadorCartaoCredito processadorCartao = new ProcessadorCartaoCredito(autorizador);
        ProcessadorBoleto processadorBoleto = new ProcessadorBoleto(boletoGateway);

        service = new PagamentoService(repositorio, eventoPublicador,
                List.of(processadorPix, processadorCartao, processadorBoleto));
    }

    @Test
    void deveCriarEAprovarPagamentoPix() {
        when(autorizador.autorizar(any())).thenReturn(ResultadoAutorizacao.autorizado());

        Pagamento resultado = service.criar(comandoPix(new BigDecimal("100.00")));

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(repositorio, times(2)).salvar(any());
        verify(eventoPublicador).publicarPagamentoAprovado(any());
    }

    @Test
    void deveCriarBoletoEFicarAguardandoPagamento() {
        when(boletoGateway.gerarBoleto(any(), anyInt()))
                .thenReturn(ResultadoBoleto.gerado("34191...", LocalDate.now().plusDays(3)));

        Pagamento resultado = service.criar(comandoBoleto(new BigDecimal("250.00")));

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.AGUARDANDO_PAGAMENTO);
        assertThat(resultado.getLinhaDigitavel()).isNotBlank();
        verify(eventoPublicador).publicarBoletoGerado(any());
    }

    @Test
    void deveCriarPagamentoRejeitadoQuandoGatewayNegar() {
        when(autorizador.autorizar(any()))
                .thenReturn(ResultadoAutorizacao.negado(MotivoRejeicao.NAO_AUTORIZADO));

        Pagamento resultado = service.criar(comandoPix(new BigDecimal("100.00")));

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.NAO_AUTORIZADO);
        verify(eventoPublicador).publicarPagamentoRejeitado(any());
    }

    @Test
    void deveLancarExcecaoQuandoPagamentoDuplicado() {
        Pagamento existente = pagamentoAprovado();
        when(repositorio.salvar(any())).thenThrow(new PagamentoDuplicadoException(existente));

        assertThatThrownBy(() -> service.criar(comandoPix(new BigDecimal("100.00"))))
                .isInstanceOf(PagamentoDuplicadoException.class);

        verifyNoInteractions(eventoPublicador);
    }

    @Test
    void deveBuscarPagamentoPorId() {
        UUID id = UUID.randomUUID();
        Pagamento pagamento = pagamentoAprovado();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.of(pagamento));

        Pagamento resultado = service.buscarPorId(id);

        assertThat(resultado).isEqualTo(pagamento);
    }

    @Test
    void deveLancarExcecaoQuandoPagamentoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(PagamentoNaoEncontradoException.class);
    }

    @Test
    void deveConfirmarPagamentoDeBoleto() {
        UUID id = UUID.randomUUID();
        Pagamento boleto = pagamentoBoletoAguardando(id);
        when(repositorio.buscarPorId(id)).thenReturn(Optional.of(boleto));

        Pagamento resultado = service.confirmarPagamentoBoleto(id);

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(eventoPublicador).publicarPagamentoAprovado(any());
    }

    @Test
    void naoDeveReprocessarPagamentoEmEstadoFinal() {
        UUID id = UUID.randomUUID();
        Pagamento aprovado = pagamentoAprovado();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.of(aprovado));

        Pagamento resultado = service.processar(id);

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(repositorio, never()).salvar(any());
        verifyNoInteractions(eventoPublicador);
    }

    private CriarPagamentoComando comandoPix(BigDecimal valor) {
        return CriarPagamentoComando.builder()
                .pedidoId(PEDIDO_ID).clienteId(CLIENTE_ID)
                .valor(valor).moeda("BRL")
                .metodoPagamento(TipoMetodoPagamento.PIX)
                .build();
    }

    private CriarPagamentoComando comandoBoleto(BigDecimal valor) {
        return CriarPagamentoComando.builder()
                .pedidoId(PEDIDO_ID).clienteId(CLIENTE_ID)
                .valor(valor).moeda("BRL")
                .metodoPagamento(TipoMetodoPagamento.BOLETO)
                .build();
    }

    private Pagamento pagamentoAprovado() {
        Pagamento p = Pagamento.novo(PEDIDO_ID, CLIENTE_ID,
                new BigDecimal("100.00"), "BRL", TipoMetodoPagamento.PIX, null, null, null);
        p.iniciarProcessamento();
        p.aprovar();
        return p;
    }

    private Pagamento pagamentoBoletoAguardando(UUID id) {
        return Pagamento.reconstituir(id, PEDIDO_ID, CLIENTE_ID,
                new BigDecimal("200.00"), "BRL", TipoMetodoPagamento.BOLETO,
                null, null, null,
                StatusPagamento.AGUARDANDO_PAGAMENTO, null,
                "34191...", LocalDate.now().plusDays(2),
                LocalDateTime.now(), LocalDateTime.now());
    }
}
