package com.eventmaster.paymentservice.application.service;

import com.eventmaster.paymentservice.application.comando.CriarPagamentoComando;
import com.eventmaster.paymentservice.application.port.in.GerenciarPagamentoUseCase;
import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.application.processador.ProcessadorPagamento;
import com.eventmaster.paymentservice.application.processador.ResultadoProcessamento;
import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.excecao.PagamentoNaoEncontradoException;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço de aplicação: implementa o caso de uso orquestrando domínio e portas de saída.
 * Não conhece JPA, Kafka nem DTOs — depende apenas de abstrações (portas).
 */
@Service
public class PagamentoService implements GerenciarPagamentoUseCase {

    private final PagamentoRepositorioPort repositorio;
    private final PagamentoEventoPort eventoPublicador;
    private final Map<TipoMetodoPagamento, ProcessadorPagamento> processadores;

    public PagamentoService(
            PagamentoRepositorioPort repositorio,
            PagamentoEventoPort eventoPublicador,
            List<ProcessadorPagamento> listaProcessadores) {
        this.repositorio = repositorio;
        this.eventoPublicador = eventoPublicador;
        this.processadores = listaProcessadores.stream()
                .collect(Collectors.toMap(ProcessadorPagamento::tipoPagamento, p -> p));
    }

    @Override
    @Transactional
    public Pagamento criar(CriarPagamentoComando comando) {
        Pagamento pagamento = Pagamento.novo(
                comando.getPedidoId(),
                comando.getClienteId(),
                comando.getValor(),
                comando.getMoeda(),
                comando.getMetodoPagamento(),
                comando.getNumeroCartao(),
                comando.getDataExpiracao(),
                comando.getCvv());
        pagamento = repositorio.salvar(pagamento);
        return executarProcessamento(pagamento);
    }

    @Override
    public Pagamento buscarPorId(UUID id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new PagamentoNaoEncontradoException(id));
    }

    @Override
    @Transactional
    public Pagamento processar(UUID id) {
        Pagamento pagamento = repositorio.buscarPorId(id)
                .orElseThrow(() -> new PagamentoNaoEncontradoException(id));

        if (!pagamento.estaPendente()) {
            return pagamento;
        }

        return executarProcessamento(pagamento);
    }

    private Pagamento executarProcessamento(Pagamento pagamento) {
        pagamento.iniciarProcessamento();
        pagamento = repositorio.salvar(pagamento);

        ProcessadorPagamento processador = processadores.get(pagamento.getMetodoPagamento());

        if (processador == null) {
            pagamento.rejeitar(MotivoRejeicao.ERRO_PROCESSAMENTO);
            pagamento = repositorio.salvar(pagamento);
            eventoPublicador.publicarPagamentoRejeitado(pagamento);
            return pagamento;
        }

        ResultadoProcessamento resultado = processador.processar(pagamento);

        if (resultado.isFoiAprovado()) {
            pagamento.aprovar();
        } else {
            pagamento.rejeitar(resultado.getMotivoRejeicao());
        }
        pagamento = repositorio.salvar(pagamento);

        if (resultado.isFoiAprovado()) {
            eventoPublicador.publicarPagamentoAprovado(pagamento);
        } else {
            eventoPublicador.publicarPagamentoRejeitado(pagamento);
        }

        return pagamento;
    }
}
