package com.eventmaster.paymentservice.infrastructure.messaging;

import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class BoletoExpiracaoJob {

    private final PagamentoRepositorioPort repositorio;
    private final PagamentoEventoPort eventoPublicador;

    public BoletoExpiracaoJob(PagamentoRepositorioPort repositorio,
                              PagamentoEventoPort eventoPublicador) {
        this.repositorio = repositorio;
        this.eventoPublicador = eventoPublicador;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void expirarBoletosVencidos() {
        LocalDate hoje = LocalDate.now();
        List<Pagamento> vencidos = repositorio.buscarBoletosVencidos(hoje.plusDays(1));
        if (vencidos.isEmpty()) {
            log.debug("Nenhum boleto vencido para expirar em {}", hoje);
            return;
        }
        log.info("Expirando {} boleto(s) vencido(s)", vencidos.size());
        for (Pagamento pagamento : vencidos) {
            pagamento.rejeitar(MotivoRejeicao.TIMEOUT);
            Pagamento salvo = repositorio.salvar(pagamento);
            eventoPublicador.publicarPagamentoRejeitado(salvo);
            log.info("Boleto {} expirado — pedidoId={}", salvo.getId(), salvo.getPedidoId());
        }
    }
}
