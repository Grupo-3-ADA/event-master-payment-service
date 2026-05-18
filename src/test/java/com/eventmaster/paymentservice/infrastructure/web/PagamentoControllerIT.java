package com.eventmaster.paymentservice.infrastructure.web;

import com.eventmaster.paymentservice.application.port.in.GerenciarPagamentoUseCase;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.exceptions.PagamentoDuplicadoException;
import com.eventmaster.paymentservice.domain.exceptions.PagamentoNaoEncontradoException;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import com.eventmaster.paymentservice.infrastructure.web.dto.PagamentoRespostaDTO;
import com.eventmaster.paymentservice.infrastructure.web.mapper.PagamentoWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagamentoController.class)
class PagamentoControllerIT {

    @Autowired MockMvc mockMvc;
    @MockBean GerenciarPagamentoUseCase gerenciarPagamento;
    @MockBean PagamentoWebMapper mapeador;

    private static final UUID PAGAMENTO_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final String CORPO_PIX = """
            {
              "pedidoId": "11111111-1111-1111-1111-111111111111",
              "clienteId": "22222222-2222-2222-2222-222222222222",
              "valor": 100.00,
              "moeda": "BRL",
              "metodoPagamento": "PIX"
            }
            """;

    @BeforeEach
    void setUp() {
        when(mapeador.paraComando(any())).thenReturn(null);
    }

    @Test
    void postPagamentos_deveRetornar201_quandoAprovado() throws Exception {
        PagamentoRespostaDTO dto = respostaDto(StatusPagamento.APROVADO, null, null);
        when(gerenciarPagamento.criar(any())).thenReturn(pagamentoDummy());
        when(mapeador.paraRespostaDTO(any())).thenReturn(dto);

        mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_PIX))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(header().exists("Location"));
    }

    @Test
    void postPagamentos_deveRetornar201_quandoBoletoAguardando() throws Exception {
        PagamentoRespostaDTO dto = respostaDto(StatusPagamento.AGUARDANDO_PAGAMENTO, null,
                "34191.09008 63521.980006 61980.069008 1 00000010000");
        when(gerenciarPagamento.criar(any())).thenReturn(pagamentoDummy());
        when(mapeador.paraRespostaDTO(any())).thenReturn(dto);

        String corpoboleto = CORPO_PIX.replace("\"PIX\"", "\"BOLETO\"");

        mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoboleto))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.linhaDigitavel").exists());
    }

    @Test
    void postPagamentos_deveRetornar200_quandoRejeitado() throws Exception {
        PagamentoRespostaDTO dto = respostaDto(StatusPagamento.REJEITADO, MotivoRejeicao.LIMITE_EXCEDIDO, null);
        when(gerenciarPagamento.criar(any())).thenReturn(pagamentoDummy());
        when(mapeador.paraRespostaDTO(any())).thenReturn(dto);

        mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_PIX))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"))
                .andExpect(jsonPath("$.motivoRejeicao").value("LIMITE_EXCEDIDO"));
    }

    @Test
    void postPagamentos_deveRetornar409_quandoPedidoDuplicado() throws Exception {
        Pagamento existente = Pagamento.reconstituir(PAGAMENTO_ID, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "BRL", TipoMetodoPagamento.PIX,
                null, null, null, StatusPagamento.APROVADO, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(gerenciarPagamento.criar(any())).thenThrow(new PagamentoDuplicadoException(existente));

        mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_PIX))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.pagamentoId").value(PAGAMENTO_ID.toString()));
    }

    @Test
    void getPagamentoPorId_deveRetornar200_quandoEncontrado() throws Exception {
        PagamentoRespostaDTO dto = respostaDto(StatusPagamento.APROVADO, null, null);
        when(gerenciarPagamento.buscarPorId(PAGAMENTO_ID)).thenReturn(pagamentoDummy());
        when(mapeador.paraRespostaDTO(any())).thenReturn(dto);

        mockMvc.perform(get("/pagamentos/{id}", PAGAMENTO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void getPagamentoPorId_deveRetornar404_quandoNaoEncontrado() throws Exception {
        when(gerenciarPagamento.buscarPorId(PAGAMENTO_ID))
                .thenThrow(new PagamentoNaoEncontradoException(PAGAMENTO_ID));

        mockMvc.perform(get("/pagamentos/{id}", PAGAMENTO_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    private PagamentoRespostaDTO respostaDto(StatusPagamento status, MotivoRejeicao motivo, String linhaDigitavel) {
        PagamentoRespostaDTO dto = new PagamentoRespostaDTO();
        dto.setId(PAGAMENTO_ID);
        dto.setStatus(status);
        dto.setMotivoRejeicao(motivo);
        dto.setLinhaDigitavel(linhaDigitavel);
        return dto;
    }

    private Pagamento pagamentoDummy() {
        return Pagamento.novo(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "BRL", TipoMetodoPagamento.PIX, null, null, null);
    }
}
