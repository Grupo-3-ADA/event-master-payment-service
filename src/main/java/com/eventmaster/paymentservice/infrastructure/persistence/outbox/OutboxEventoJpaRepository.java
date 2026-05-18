package com.eventmaster.paymentservice.infrastructure.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventoJpaRepository extends JpaRepository<OutboxEvento, UUID> {

    List<OutboxEvento> findByProcessadoFalseOrderByCriadoEmAsc();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OutboxEvento o SET o.processado = true WHERE o.id = :id")
    void marcarComoProcessado(@Param("id") UUID id);
}
