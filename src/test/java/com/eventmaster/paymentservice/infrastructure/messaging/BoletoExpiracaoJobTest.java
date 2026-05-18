package com.eventmaster.paymentservice.infrastructure.messaging;

import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoletoExpiracaoJobTest {

    @Mock PagamentoRepositorioPort repositorio;
    @Mock PagamentoEventoPort eventoPublicador;

    BoletoExpiracaoJob job;

    @BeforeEach
    void setUp() {
        job = new BoletoExpiracaoJob(repositorio, eventoPublicador);
        when(repositorio.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void deveExpirarBoletosVencidos() {
        Pagamento boletoVencido = boletoVencidoOntem();
        when(repositorio.buscarBoletosVencidos(any())).thenReturn(List.of(boletoVencido));

        job.expirarBoletosVencidos();

        assertThat(boletoVencido.getStatus()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(boletoVencido.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.TIMEOUT);
        verify(repositorio).salvar(boletoVencido);
        verify(eventoPublicador).publicarPagamentoRejeitado(any());
    }

    @Test
    void naoDeveProcessarNadaQuandoNaoHaBoletosVencidos() {
        when(repositorio.buscarBoletosVencidos(any())).thenReturn(List.of());

        job.expirarBoletosVencidos();

        verify(repositorio, never()).salvar(any());
        verifyNoInteractions(eventoPublicador);
    }

    private Pagamento boletoVencidoOntem() {
        return Pagamento.reconstituir(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("150.00"), "BRL", TipoMetodoPagamento.BOLETO,
                null, null, null,
                StatusPagamento.AGUARDANDO_PAGAMENTO, null,
                "34191...", LocalDate.now().minusDays(1),
                LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(1));
    }
}
