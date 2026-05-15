package com.eventmaster.paymentservice.infrastructure.persistence;

import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de saída: implementa a porta de persistência usando JPA.
 * Traduz domínio ↔ entidade e isola o resto do sistema do banco de dados.
 */
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
        PagamentoEntity entity = entityMapper.paraEntity(pagamento);
        PagamentoEntity salvo = jpaRepository.save(entity);
        return entityMapper.paraDominio(salvo);
    }

    @Override
    public Optional<Pagamento> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(entityMapper::paraDominio);
    }
}
