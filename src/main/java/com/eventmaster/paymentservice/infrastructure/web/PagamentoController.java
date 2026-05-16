package com.eventmaster.paymentservice.infrastructure.web;

import com.eventmaster.paymentservice.application.port.in.GerenciarPagamentoUseCase;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import com.eventmaster.paymentservice.infrastructure.web.dto.CriarPagamentoDTO;
import com.eventmaster.paymentservice.infrastructure.web.dto.PagamentoRespostaDTO;
import com.eventmaster.paymentservice.infrastructure.web.mapper.PagamentoWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/pagamentos")
@Tag(name = "Pagamentos", description = "Gerenciamento de pagamentos")
public class PagamentoController {

    private final GerenciarPagamentoUseCase gerenciarPagamento;
    private final PagamentoWebMapper mapeador;

    public PagamentoController(GerenciarPagamentoUseCase gerenciarPagamento,
                               PagamentoWebMapper mapeador) {
        this.gerenciarPagamento = gerenciarPagamento;
        this.mapeador = mapeador;
    }

    @PostMapping
    @Operation(summary = "Criar pagamento",
               description = "Cria e processa o pagamento. 201 = aprovado, 200 = rejeitado (consulte motivoRejeicao).")
    public ResponseEntity<PagamentoRespostaDTO> criarPagamento(@Valid @RequestBody CriarPagamentoDTO dto) {
        Pagamento pagamento = gerenciarPagamento.criar(mapeador.paraComando(dto));
        PagamentoRespostaDTO resposta = mapeador.paraRespostaDTO(pagamento);
        URI localizacao = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.getId())
                .toUri();
        if (StatusPagamento.APROVADO == resposta.getStatus()) {
            return ResponseEntity.created(localizacao).body(resposta);
        }
        return ResponseEntity.ok().header("Location", localizacao.toString()).body(resposta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar pagamento", description = "Retorna o status atual de um pagamento")
    public ResponseEntity<PagamentoRespostaDTO> buscarPorId(@PathVariable UUID id) {
        Pagamento pagamento = gerenciarPagamento.buscarPorId(id);
        return ResponseEntity.ok(mapeador.paraRespostaDTO(pagamento));
    }
}
