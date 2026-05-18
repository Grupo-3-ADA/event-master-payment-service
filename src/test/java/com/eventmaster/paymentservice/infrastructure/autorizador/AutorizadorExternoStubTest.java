package com.eventmaster.paymentservice.infrastructure.autorizador;

import com.eventmaster.paymentservice.application.processador.ResultadoAutorizacao;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AutorizadorExternoStubTest {

    AutorizadorExternoStub autorizador;

    @BeforeEach
    void setUp() {
        autorizador = new AutorizadorExternoStub();
    }

    @Test
    void deveAutorizarCartaoValido() {
        Pagamento pagamento = cartaoCom("4111111111111111");

        ResultadoAutorizacao resultado = autorizador.autorizar(pagamento);

        assertThat(resultado.isAutorizado()).isTrue();
    }

    @Test
    void deveNegarCartaoComSufixo0002() {
        Pagamento pagamento = cartaoCom("4111111111110002");

        ResultadoAutorizacao resultado = autorizador.autorizar(pagamento);

        assertThat(resultado.isAutorizado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_INVALIDO);
    }

    @Test
    void deveNegarCartaoComSufixo0069() {
        Pagamento pagamento = cartaoCom("4111111111110069");

        ResultadoAutorizacao resultado = autorizador.autorizar(pagamento);

        assertThat(resultado.isAutorizado()).isFalse();
        assertThat(resultado.getMotivoRejeicao()).isEqualTo(MotivoRejeicao.CARTAO_EXPIRADO);
    }

    private Pagamento cartaoCom(String numeroCartao) {
        return Pagamento.novo(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "BRL",
                TipoMetodoPagamento.CARTAO_CREDITO,
                numeroCartao, "12/29", "123");
    }
}
