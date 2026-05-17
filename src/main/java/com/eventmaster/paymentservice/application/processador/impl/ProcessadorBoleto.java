package com.eventmaster.paymentservice.application.processador.impl;

import com.eventmaster.paymentservice.application.port.out.BoletoGatewayPort;
import com.eventmaster.paymentservice.application.processador.ProcessadorPagamento;
import com.eventmaster.paymentservice.application.processador.ResultadoBoleto;
import com.eventmaster.paymentservice.application.processador.ResultadoProcessamento;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProcessadorBoleto implements ProcessadorPagamento {

    private static final String MOEDA_ACEITA = "BRL";
    private static final int DIAS_VENCIMENTO = 3;

    private final BoletoGatewayPort boletoGateway;

    public ProcessadorBoleto(BoletoGatewayPort boletoGateway) {
        this.boletoGateway = boletoGateway;
    }

    @Override
    public TipoMetodoPagamento tipoPagamento() {
        return TipoMetodoPagamento.BOLETO;
    }

    @Override
    public ResultadoProcessamento processar(Pagamento pagamento) {
        if (!MOEDA_ACEITA.equals(pagamento.getMoeda())) {
            log.warn("Boleto {} rejeitado — moeda {} não aceita", pagamento.getId(), pagamento.getMoeda());
            return ResultadoProcessamento.rejeitado(MotivoRejeicao.NAO_AUTORIZADO);
        }
        ResultadoBoleto boleto = boletoGateway.gerarBoleto(pagamento, DIAS_VENCIMENTO);
        pagamento.registrarBoleto(boleto.getLinhaDigitavel(), boleto.getDataVencimento());
        log.info("Boleto gerado para pagamento {} — vencimento={}", pagamento.getId(), boleto.getDataVencimento());
        return ResultadoProcessamento.aguardando();
    }
}
