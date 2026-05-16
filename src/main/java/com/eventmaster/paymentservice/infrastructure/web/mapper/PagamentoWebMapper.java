package com.eventmaster.paymentservice.infrastructure.web.mapper;

import com.eventmaster.paymentservice.application.comando.CriarPagamentoComando;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import com.eventmaster.paymentservice.infrastructure.web.dto.CriarPagamentoDTO;
import com.eventmaster.paymentservice.infrastructure.web.dto.PagamentoRespostaDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PagamentoWebMapper {

    CriarPagamentoComando paraComando(CriarPagamentoDTO dto);

    PagamentoRespostaDTO paraRespostaDTO(Pagamento pagamento);
}
