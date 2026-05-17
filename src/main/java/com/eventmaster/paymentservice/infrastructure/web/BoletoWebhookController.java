package com.eventmaster.paymentservice.infrastructure.web;

import com.eventmaster.paymentservice.application.port.in.GerenciarPagamentoUseCase;
import com.eventmaster.paymentservice.infrastructure.web.dto.ConfirmacaoBoletoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/boleto")
@Tag(name = "Webhooks Boleto", description = "Callbacks do gateway bancário para confirmação de pagamento")
public class BoletoWebhookController {

    private final GerenciarPagamentoUseCase gerenciarPagamento;

    public BoletoWebhookController(GerenciarPagamentoUseCase gerenciarPagamento) {
        this.gerenciarPagamento = gerenciarPagamento;
    }

    @PostMapping("/confirmacao")
    @Operation(summary = "Confirmar pagamento de boleto",
               description = "Chamado pelo gateway quando o banco confirma o pagamento do boleto")
    public ResponseEntity<Void> confirmarPagamento(@Valid @RequestBody ConfirmacaoBoletoDTO dto) {
        gerenciarPagamento.confirmarPagamentoBoleto(dto.getPagamentoId());
        return ResponseEntity.ok().build();
    }
}
