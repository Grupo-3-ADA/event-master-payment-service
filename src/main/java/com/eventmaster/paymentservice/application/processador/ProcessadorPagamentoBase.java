package com.eventmaster.paymentservice.application.processador;

import com.eventmaster.paymentservice.application.port.out.AutorizadorExternoPort;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Template Method para processadores de pagamento.
 * Define o algoritmo fixo: validar moeda → validar limite → validações específicas → autorizar.
 * Subclasses fornecem os limites e podem sobrescrever o hook validacoesEspecificas().
 */
@Slf4j
public abstract class ProcessadorPagamentoBase implements ProcessadorPagamento {

    private static final String MOEDA_ACEITA = "BRL";

    protected final AutorizadorExternoPort autorizador;

    protected ProcessadorPagamentoBase(AutorizadorExternoPort autorizador) {
        this.autorizador = autorizador;
    }

    protected abstract BigDecimal limiteMinimo();

    protected abstract BigDecimal limiteMaximo();

    protected Optional<ResultadoProcessamento> validacoesEspecificas(Pagamento pagamento) {
        return Optional.empty();
    }

    @Override
    public final ResultadoProcessamento processar(Pagamento pagamento) {
        if (!MOEDA_ACEITA.equals(pagamento.getMoeda())) {
            log.warn("Pagamento {} rejeitado — moeda {} não aceita", pagamento.getId(), pagamento.getMoeda());
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.NAO_AUTORIZADO);
        }
        if (pagamento.getValor().compareTo(limiteMinimo()) < 0
                || pagamento.getValor().compareTo(limiteMaximo()) > 0) {
            log.warn("Pagamento {} rejeitado — valor {} fora do limite [{}, {}]",
                    pagamento.getId(), pagamento.getValor(), limiteMinimo(), limiteMaximo());
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.LIMITE_EXCEDIDO);
        }
        Optional<ResultadoProcessamento> especifico = validacoesEspecificas(pagamento);
        if (especifico.isPresent()) {
            return especifico.get();
        }
        ResultadoAutorizacao autorizacao = autorizador.autorizar(pagamento);
        if (!autorizacao.isAutorizado()) {
            log.warn("Pagamento {} negado pelo autorizador externo — motivo={}", pagamento.getId(), autorizacao.getMotivoRejeicao());
            return ResultadoProcessamento.rejeitado(autorizacao.getMotivoRejeicao());
        }
        return ResultadoProcessamento.aprovado();
    }
}
