package com.eventmaster.paymentservice.infrastructure.persistence;

import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PagamentoEntityMapper {

    PagamentoEntity paraEntity(Pagamento pagamento);

    default Pagamento paraDominio(PagamentoEntity entity) {
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
