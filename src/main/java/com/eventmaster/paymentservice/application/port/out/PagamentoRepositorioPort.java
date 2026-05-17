package com.eventmaster.paymentservice.application.port.out;

import com.eventmaster.paymentservice.domain.model.Pagamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída (driven port) para persistência.
 * A aplicação fala em termos de domínio; o adaptador na infraestrutura traduz para JPA.
 */
public interface PagamentoRepositorioPort {

    Pagamento salvar(Pagamento pagamento);

    Optional<Pagamento> buscarPorId(UUID id);

    List<Pagamento> buscarBoletosVencidos(LocalDate dataReferencia);
}
