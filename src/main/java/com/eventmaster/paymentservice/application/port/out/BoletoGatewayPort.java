package com.eventmaster.paymentservice.application.port.out;

import com.eventmaster.paymentservice.application.processador.ResultadoBoleto;
import com.eventmaster.paymentservice.domain.model.Pagamento;

/**
 * Porta de saída para geração de boleto via gateway bancário.
 * Em produção: Bradesco, Itaú, Asaas, PagSeguro, etc.
 * No ambiente atual: satisfeita por BoletoGatewayStub.
 */
public interface BoletoGatewayPort {

    ResultadoBoleto gerarBoleto(Pagamento pagamento, int diasVencimento);
}
