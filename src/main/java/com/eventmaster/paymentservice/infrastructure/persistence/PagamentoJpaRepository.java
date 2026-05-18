package com.eventmaster.paymentservice.infrastructure.persistence;

import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data sobre a entidade JPA.
 * Detalhe de infraestrutura — é consumido pelo PagamentoPersistenceAdapter.
 */
@Repository
public interface PagamentoJpaRepository extends JpaRepository<PagamentoEntity, UUID> {

    Optional<PagamentoEntity> findByPedidoId(UUID pedidoId);

    List<PagamentoEntity> findByMetodoPagamentoAndStatusAndDataVencimentoBefore(
            TipoMetodoPagamento metodoPagamento, StatusPagamento status, LocalDate dataReferencia);
}
