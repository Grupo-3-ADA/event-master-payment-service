package com.eventmaster.paymentservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositório Spring Data sobre a entidade JPA.
 * Detalhe de infraestrutura — é consumido pelo PagamentoPersistenceAdapter.
 */
@Repository
public interface PagamentoJpaRepository extends JpaRepository<PagamentoEntity, UUID> {
}
