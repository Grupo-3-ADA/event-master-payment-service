package com.eventmaster.paymentservice.infrastructure.autorizador;

import com.eventmaster.paymentservice.application.port.out.AutorizadorExternoPort;
import com.eventmaster.paymentservice.application.processador.ResultadoAutorizacao;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.stereotype.Component;

/**
 * Stub que simula a resposta de um gateway externo (adquirente/DICT).
 * Usa "magic numbers" no número do cartão para forçar cenários de teste:
 *   - termina em "0002" → CARTAO_INVALIDO  (ex: 4111111111110002)
 *   - termina em "0069" → CARTAO_EXPIRADO  (ex: 4111111111110069)
 *   - qualquer outro    → autorizado
 * PIX: sempre autorizado pelo stub (rejeições vêm das regras de negócio do processador).
 */
@Component
public class AutorizadorExternoStub implements AutorizadorExternoPort {

    @Override
    public ResultadoAutorizacao autorizar(Pagamento pagamento) {
        if (pagamento.getMetodoPagamento() == TipoMetodoPagamento.CARTAO_CREDITO) {
            return autorizarCartao(pagamento.getNumeroCartao());
        }
        return ResultadoAutorizacao.autorizado();
    }

    private ResultadoAutorizacao autorizarCartao(String numeroCartao) {
        String digitos = numeroCartao.replaceAll("[\\s-]", "");
        if (digitos.endsWith("0002")) {
            return ResultadoAutorizacao.negado(MotivoRejeicao.CARTAO_INVALIDO);
        }
        if (digitos.endsWith("0069")) {
            return ResultadoAutorizacao.negado(MotivoRejeicao.CARTAO_EXPIRADO);
        }
        return ResultadoAutorizacao.autorizado();
    }
}
