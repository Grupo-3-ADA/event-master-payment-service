package com.eventmaster.paymentservice.application.service;

import com.eventmaster.paymentservice.application.comando.CriarPagamentoComando;
import com.eventmaster.paymentservice.application.port.in.GerenciarPagamentoUseCase;
import com.eventmaster.paymentservice.application.port.out.PagamentoEventoPort;
import com.eventmaster.paymentservice.application.port.out.PagamentoRepositorioPort;
import com.eventmaster.paymentservice.application.processador.ProcessadorNaoSuportado;
import com.eventmaster.paymentservice.application.processador.ProcessadorPagamento;
import com.eventmaster.paymentservice.application.processador.ResultadoProcessamento;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import com.eventmaster.paymentservice.domain.exceptions.PagamentoNaoEncontradoException;
import com.eventmaster.paymentservice.domain.model.Pagamento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("Criando pagamento — pedidoId={} método={} valor={} moeda={}",
                comando.getPedidoId(), comando.getMetodoPagamento(),
                comando.getValor(), comando.getMoeda());
        Pagamento pagamento = Pagamento.novo(
                comando.getPedidoId(),
                comando.getClienteId(),
                comando.getValor(),
                comando.getMoeda(),
                comando.getMetodoPagamento(),
                comando.getNumeroCartao(),
                comando.getDataExpiracao(),
                comando.getCvv());
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
        log.info("Reprocessamento solicitado — pagamentoId={}", id);
        Pagamento pagamento = repositorio.buscarPorId(id)
                .orElseThrow(() -> new PagamentoNaoEncontradoException(id));

        if (pagamento.estaEmEstadoFinal()) {
            log.warn("Pagamento {} já está em estado final ({}), ignorando reprocessamento",
                    id, pagamento.getStatus());
            return pagamento;
        }

        return executarProcessamento(pagamento);
    }

    @Override
    @Transactional
    public Pagamento confirmarPagamentoBoleto(UUID pagamentoId) {
        log.info("Confirmação de boleto recebida — pagamentoId={}", pagamentoId);
        Pagamento pagamento = repositorio.buscarPorId(pagamentoId)
                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId));

        if (pagamento.getMetodoPagamento() != TipoMetodoPagamento.BOLETO) {
            throw new IllegalArgumentException("Pagamento não é do tipo boleto");
        }
        if (pagamento.getStatus() == StatusPagamento.APROVADO) {
            log.info("Boleto {} já estava aprovado, ignorando confirmação duplicada", pagamentoId);
            return pagamento;
        }
        if (!pagamento.estaAguardandoPagamento()) {
            throw new IllegalArgumentException(
                    "Pagamento não está aguardando confirmação (status atual: " + pagamento.getStatus() + ")");
        }

        pagamento.aprovar();
        Pagamento salvo = repositorio.salvar(pagamento);
        eventoPublicador.publicarPagamentoAprovado(salvo);
        log.info("Boleto {} confirmado e aprovado", pagamentoId);
        return salvo;
    }

    private Pagamento executarProcessamento(Pagamento pagamento) {
        pagamento.iniciarProcessamento();
        pagamento = repositorio.salvar(pagamento);

        log.debug("Processando pagamento {} via {}", pagamento.getId(), pagamento.getMetodoPagamento());

        ProcessadorPagamento processador = processadores.getOrDefault(
                pagamento.getMetodoPagamento(), ProcessadorNaoSuportado.INSTANCIA);

        ResultadoProcessamento resultado = processador.processar(pagamento);
        Pagamento finalizado = resultado.finalizar(pagamento, repositorio, eventoPublicador);

        log.info("Pagamento {} finalizado — status={} motivo={}",
                finalizado.getId(), finalizado.getStatus(), finalizado.getMotivoRejeicao());
        return finalizado;
    }
}
