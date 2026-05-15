package com.eventmaster.paymentservice.infrastructure.persistence;

import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.stereotype.Component;

/**
 * Tradutor entre o modelo de domínio (Pagamento) e a entidade de persistência (PagamentoEntity).
 * Mantém o domínio livre de qualquer dependência de JPA.
 */
@Component
public class PagamentoEntityMapper {

    public PagamentoEntity paraEntity(Pagamento pagamento) {
        return PagamentoEntity.builder()
                .id(pagamento.getId())
                .pedidoId(pagamento.getPedidoId())
                .clienteId(pagamento.getClienteId())
                .valor(pagamento.getValor())
                .moeda(pagamento.getMoeda())
                .numeroCartao(pagamento.getNumeroCartao())
                .dataExpiracao(pagamento.getDataExpiracao())
                .cvv(pagamento.getCvv())
                .status(pagamento.getStatus())
                .metodoPagamento(pagamento.getMetodoPagamento())
                .motivoRejeicao(pagamento.getMotivoRejeicao())
                .criadoEm(pagamento.getCriadoEm())
                .atualizadoEm(pagamento.getAtualizadoEm())
                .build();
    }

    public Pagamento paraDominio(PagamentoEntity entity) {
        return Pagamento.reconstituir(
                entity.getId(),
                entity.getPedidoId(),
                entity.getClienteId(),
                entity.getValor(),
                entity.getMoeda(),
                entity.getMetodoPagamento(),
                entity.getNumeroCartao(),
                entity.getDataExpiracao(),
                entity.getCvv(),
                entity.getStatus(),
                entity.getMotivoRejeicao(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm());
    }
}
