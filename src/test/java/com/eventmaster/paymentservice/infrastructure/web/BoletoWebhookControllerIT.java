package com.eventmaster.paymentservice.infrastructure.web;

import com.eventmaster.paymentservice.application.port.in.GerenciarPagamentoUseCase;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoletoWebhookController.class)
class BoletoWebhookControllerIT {

    @Autowired MockMvc mockMvc;
    @MockBean GerenciarPagamentoUseCase gerenciarPagamento;

    @Test
    void postConfirmacao_deveRetornar200_quandoBoletoConfirmado() throws Exception {
        UUID pagamentoId = UUID.randomUUID();
        Pagamento aprovado = boletoAprovado(pagamentoId);
        when(gerenciarPagamento.confirmarPagamentoBoleto(any())).thenReturn(aprovado);

        String corpo = """
                {
                  "pagamentoId": "%s",
                  "dataPagamento": "2026-05-20",
                  "valorPago": 350.00
                }
                """.formatted(pagamentoId);

        mockMvc.perform(post("/webhooks/boleto/confirmacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
    }

    private Pagamento boletoAprovado(UUID id) {
        return Pagamento.reconstituir(id, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("350.00"), "BRL", TipoMetodoPagamento.BOLETO,
                null, null, null, StatusPagamento.APROVADO, null,
                "34191...", LocalDate.now().minusDays(1),
                LocalDateTime.now(), LocalDateTime.now());
    }
}
