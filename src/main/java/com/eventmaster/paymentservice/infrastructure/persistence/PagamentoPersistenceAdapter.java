package com.eventmaster.paymentservice.infrastructure.persistence;

import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.domain.exceptions.PagamentoDuplicadoException;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de saída: implementa a porta de persistência usando JPA.
 * Traduz domínio ↔ entidade e isola o resto do sistema do banco de dados.
 */
@Slf4j
@Component
public class PagamentoPersistenceAdapter implements PagamentoRepositorioPort {

    private final PagamentoJpaRepository jpaRepository;
    private final PagamentoEntityMapper entityMapper;

    public PagamentoPersistenceAdapter(PagamentoJpaRepository jpaRepository,
                                       PagamentoEntityMapper entityMapper) {
        this.jpaRepository = jpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    public Pagamento salvar(Pagamento pagamento) {
        if (pagamento.getId() == null) {
            jpaRepository.findByPedidoId(pagamento.getPedidoId())
                    .map(entityMapper::paraDominio)
                    .ifPresent(existente -> {
                        log.warn("Pagamento duplicado detectado — pedidoId={} pagamentoExistenteId={}",
                                pagamento.getPedidoId(), existente.getId());
                        throw new PagamentoDuplicadoException(existente);
                    });
        }
        PagamentoEntity entity = entityMapper.paraEntity(pagamento);
        PagamentoEntity salvo = jpaRepository.save(entity);
        return entityMapper.paraDominio(salvo);
    }

    @Override
    public Optional<Pagamento> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(entityMapper::paraDominio);
    }

    @Override
    public List<Pagamento> buscarBoletosVencidos(LocalDate dataReferencia) {
        return jpaRepository
                .findByMetodoPagamentoAndStatusAndDataVencimentoBefore(
                        TipoMetodoPagamento.BOLETO,
                        StatusPagamento.AGUARDANDO_PAGAMENTO,
                        dataReferencia)
                .stream()
                .map(entityMapper::paraDominio)
                .toList();
    }
}
