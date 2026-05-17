package com.eventmaster.paymentservice.infrastructure.autorizador;

import com.eventmaster.paymentservice.application.port.out.BoletoGatewayPort;
import com.eventmaster.paymentservice.application.processador.ResultadoBoleto;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class BoletoGatewayStub implements BoletoGatewayPort {

    @Override
    public ResultadoBoleto gerarBoleto(Pagamento pagamento, int diasVencimento) {
        String linhaDigitavel = "34191.09008 63521.980006 61980.069008 1 "
                + pagamento.getValor().toPlainString().replace(".", "");
        LocalDate vencimento = LocalDate.now().plusDays(diasVencimento);
        return ResultadoBoleto.gerado(linhaDigitavel, vencimento);
    }
}
