package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.application.port.out.BoletoGatewayPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.application.processador.impl.ProcessadorBoleto;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessadorBoletoTest {

    @Mock BoletoGatewayPort boletoGateway;
    @Mock PagamentoRepositorioPort repositorio;
    @Mock PagamentoEventoPort eventos;

    ProcessadorBoleto processador;

    private static final String LINHA_DIGITAVEL = "34191.09008 63521.980006 61980.069008 1 00000020000";
    private static final LocalDate VENCIMENTO = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        processador = new ProcessadorBoleto(boletoGateway);
        when(repositorio.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(boletoGateway.gerarBoleto(any(), anyInt()))
                .thenReturn(ResultadoBoleto.gerado(LINHA_DIGITAVEL, VENCIMENTO));
    }

    @Test
    void deveGerarBoletoETransicionarParaAguardando() {
        Pagamento pagamento = boletoCom(new BigDecimal("350.00"), "BRL");

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.AGUARDANDO_PAGAMENTO);
        assertThat(pagamento.getLinhaDigitavel()).isEqualTo(LINHA_DIGITAVEL);
        assertThat(pagamento.getDataVencimento()).isEqualTo(VENCIMENTO);
        verify(eventos).publicarBoletoGerado(any());
    }

    @Test
    void deveRejeitarBoletoQuandoMoedaNaoForBRL() {
        Pagamento pagamento = boletoCom(new BigDecimal("350.00"), "USD");

        ResultadoProcessamento resultado = processador.processar(pagamento);
        resultado.finalizar(pagamento, repositorio, eventos);

        assertThat(pagamento.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.NAO_AUTORIZADO);
        verifyNoInteractions(boletoGateway);
    }

    @Test
    void tipoPagamentoDeveSerBoleto() {
        assertThat(processador.tipoPagamento()).isEqualTo(TipoMetodoPagamento.BOLETO);
    }

    private Pagamento boletoCom(BigDecimal valor, String moeda) {
        return Pagamento.novo(UUID.randomUUID(), UUID.randomUUID(),
                valor, moeda, TipoMetodoPagamento.BOLETO, null, null, null);
    }
}
