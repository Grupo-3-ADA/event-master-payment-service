package com.eventmaster.paymentservice.application.service;

import com.eventmaster.paymentservice.application.comando.CriarPagamentoComando;
import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.application.processador.ProcessadorPagamento;
import com.eventmaster.paymentservice.application.processador.ResultadoProcessamento;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.excecao.PagamentoNaoEncontradoException;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagamentoService — orquestração do caso de uso")
class PagamentoServiceTest {

    @Mock
    private PagamentoRepositorioPort repositorio;

    @Mock
    private PagamentoEventoPort eventoPublicador;

    @Mock
    private ProcessadorPagamento processadorPix;

    private PagamentoService servico;

    @BeforeEach
    void configurar() {
        when(processadorPix.tipoPagamento()).thenReturn(TipoMetodoPagamento.PIX);
        lenient().when(repositorio.salvar(any(Pagamento.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        servico = new PagamentoService(repositorio, eventoPublicador, List.of(processadorPix));
    }

    // ── criar ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve criar pagamento PIX e aprovar quando o processador aprova")
    void deveCriarEAprovarPagamentoPix() {
        when(processadorPix.processar(any(Pagamento.class)))
                .thenReturn(ResultadoProcessamento.aprovado());

        Pagamento resultado = servico.criar(comando(TipoMetodoPagamento.PIX));

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(eventoPublicador).publicarPagamentoAprovado(any(Pagamento.class));
        verify(eventoPublicador, never()).publicarPagamentoRejeitado(any());
        verify(repositorio, times(3)).salvar(any(Pagamento.class));
    }

    @Test
    @DisplayName("deve criar pagamento PIX e rejeitar quando o processador rejeita")
    void deveCriarERejeitar() {
        when(processadorPix.processar(any(Pagamento.class)))
                .thenReturn(ResultadoProcessamento.rejeitado(MotivoRejeicao.LIMITE_EXCEDIDO));

        Pagamento resultado = servico.criar(comando(TipoMetodoPagamento.PIX));

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.LIMITE_EXCEDIDO);
        verify(eventoPublicador).publicarPagamentoRejeitado(any(Pagamento.class));
        verify(eventoPublicador, never()).publicarPagamentoAprovado(any());
    }

    @Test
    @DisplayName("deve rejeitar com ERRO_PROCESSAMENTO quando não há processador para o tipo")
    void deveRejeitarQuandoNaoHaProcessador() {
        Pagamento resultado = servico.criar(comando(TipoMetodoPagamento.BOLETO));

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.ERRO_PROCESSAMENTO);
        verify(eventoPublicador).publicarPagamentoRejeitado(any(Pagamento.class));
        verify(processadorPix, never()).processar(any());
    }

    // ── buscarPorId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve retornar pagamento existente por ID")
    void deveBuscarPagamentoPorId() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.of(pagamentoEm(StatusPagamento.APROVADO)));

        Pagamento resultado = servico.buscarPorId(id);

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    @DisplayName("deve lançar PagamentoNaoEncontradoException quando o ID não existe")
    void deveLancarExcecaoQuandoNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        when(repositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.buscarPorId(idInexistente))
                .isInstanceOf(PagamentoNaoEncontradoException.class);
    }

    // ── processar (reprocessamento externo via Kafka) ──────────────────────

    @Test
    @DisplayName("deve reprocessar pagamento pendente quando solicitado externamente")
    void deveReprocessarPagamentoPendente() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.of(pagamentoEm(StatusPagamento.PENDENTE)));
        when(processadorPix.processar(any(Pagamento.class)))
                .thenReturn(ResultadoProcessamento.aprovado());

        Pagamento resultado = servico.processar(id);

        assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(processadorPix).processar(any(Pagamento.class));
    }

    @Test
    @DisplayName("deve ignorar reprocessamento de pagamento já finalizado")
    void deveIgnorarReprocessamentoDeJaFinalizado() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.of(pagamentoEm(StatusPagamento.APROVADO)));

        servico.processar(id);

        verify(processadorPix, never()).processar(any());
        verify(eventoPublicador, never()).publicarPagamentoAprovado(any());
        verify(eventoPublicador, never()).publicarPagamentoRejeitado(any());
    }

    // ── Utilitários ────────────────────────────────────────────────────────

    private CriarPagamentoComando comando(TipoMetodoPagamento tipo) {
        return new CriarPagamentoComando(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                "BRL", tipo, null, null, null);
    }

    private Pagamento pagamentoEm(StatusPagamento status) {
        return Pagamento.reconstituir(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "BRL", TipoMetodoPagamento.PIX,
                null, null, null,
                status, null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
