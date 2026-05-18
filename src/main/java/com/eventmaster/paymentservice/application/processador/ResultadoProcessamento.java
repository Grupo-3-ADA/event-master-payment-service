package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.model.Pagamento;

/**
 * Resultado de um processamento de pagamento.
 * Usa Command pattern: cada instância carrega a ação de finalização correspondente.
 * O PagamentoService chama apenas finalizar() — sem if/else, sem conhecer o tipo do resultado.
 * Adicionar novos métodos de pagamento (ex: Boleto) não requer alterar o serviço.
 */
public class ResultadoProcessamento {

    private final AcaoFinalizacao acao;

    private ResultadoProcessamento(AcaoFinalizacao acao) {
        this.acao = acao;
    }

    public static ResultadoProcessamento aprovado() {
        return new ResultadoProcessamento((pagamento, repositorio, eventos) -> {
            pagamento.aprovar();
            Pagamento salvo = repositorio.salvar(pagamento);
            eventos.publicarPagamentoAprovado(salvo);
            return salvo;
        });
    }

    public static ResultadoProcessamento rejeitado(MotivoRejeicao motivo) {
        return new ResultadoProcessamento((pagamento, repositorio, eventos) -> {
            pagamento.rejeitar(motivo);
            Pagamento salvo = repositorio.salvar(pagamento);
            eventos.publicarPagamentoRejeitado(salvo);
            return salvo;
        });
    }

    public static ResultadoProcessamento aguardando() {
        return new ResultadoProcessamento((pagamento, repositorio, eventos) -> {
            Pagamento salvo = repositorio.salvar(pagamento);
            eventos.publicarBoletoGerado(salvo);
            return salvo;
        });
    }

    public Pagamento finalizar(Pagamento pagamento, PagamentoRepositorioPort repositorio, PagamentoEventoPort eventos) {
        return acao.executar(pagamento, repositorio, eventos);
    }
}
