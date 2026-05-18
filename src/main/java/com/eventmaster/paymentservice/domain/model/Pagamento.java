package com.eventmaster.paymentservice.domain.model;

import com.eventmaster.paymentservice.domain.enums.MotivoRejeicao;
import com.eventmaster.paymentservice.domain.enums.StatusPagamento;
import com.eventmaster.paymentservice.domain.enums.TipoMetodoPagamento;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Agregado raiz do domínio de pagamento.
 * Domínio puro: não conhece JPA, Spring ou qualquer framework de infraestrutura.
 * O estado só pode ser alterado pelos métodos de negócio, que protegem os invariantes.
 */
@Getter
public class Pagamento {

    private final UUID id;
    private final UUID pedidoId;
    private final UUID clienteId;
    private final BigDecimal valor;
    private final String moeda;
    private final TipoMetodoPagamento metodoPagamento;
    private final String numeroCartao;
    private final String dataExpiracao;
    private final String cvv;
    private StatusPagamento status;
    private MotivoRejeicao motivoRejeicao;
    private String linhaDigitavel;
    private LocalDate dataVencimento;
    private final LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    private Pagamento(UUID id, UUID pedidoId, UUID clienteId, BigDecimal valor, String moeda,
                      TipoMetodoPagamento metodoPagamento, String numeroCartao, String dataExpiracao,
                      String cvv, StatusPagamento status, MotivoRejeicao motivoRejeicao,
                      String linhaDigitavel, LocalDate dataVencimento,
                      LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.valor = valor;
        this.moeda = moeda;
        this.metodoPagamento = metodoPagamento;
        this.numeroCartao = numeroCartao;
        this.dataExpiracao = dataExpiracao;
        this.cvv = cvv;
        this.status = status;
        this.motivoRejeicao = motivoRejeicao;
        this.linhaDigitavel = linhaDigitavel;
        this.dataVencimento = dataVencimento;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    /**
     * Cria um novo pagamento entrando no sistema. Sempre nasce PENDENTE.
     * Valida os invariantes universais — o domínio não confia que o chamador já validou.
     */
    public static Pagamento novo(UUID pedidoId, UUID clienteId, BigDecimal valor, String moeda,
                                 TipoMetodoPagamento metodoPagamento, String numeroCartao,
                                 String dataExpiracao, String cvv) {
        if (pedidoId == null || clienteId == null) {
            throw new IllegalArgumentException("Pedido e cliente são obrigatórios");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero");
        }
        if (moeda == null || moeda.isBlank()) {
            throw new IllegalArgumentException("A moeda é obrigatória");
        }
        if (metodoPagamento == null) {
            throw new IllegalArgumentException("O método de pagamento é obrigatório");
        }
        LocalDateTime agora = LocalDateTime.now();
        return new Pagamento(null, pedidoId, clienteId, valor, moeda, metodoPagamento,
                numeroCartao, dataExpiracao, cvv, StatusPagamento.PENDENTE, null, null, null, agora, agora);
    }

    /**
     * Reconstrói um pagamento a partir dos dados persistidos.
     * Usado exclusivamente pela camada de infraestrutura ao ler do banco.
     */
    public static Pagamento reconstituir(UUID id, UUID pedidoId, UUID clienteId, BigDecimal valor,
                                         String moeda, TipoMetodoPagamento metodoPagamento,
                                         String numeroCartao, String dataExpiracao, String cvv,
                                         StatusPagamento status, MotivoRejeicao motivoRejeicao,
                                         String linhaDigitavel, LocalDate dataVencimento,
                                         LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        return new Pagamento(id, pedidoId, clienteId, valor, moeda, metodoPagamento, numeroCartao,
                dataExpiracao, cvv, status, motivoRejeicao, linhaDigitavel, dataVencimento,
                criadoEm, atualizadoEm);
    }

    public void iniciarProcessamento() {
        this.status = StatusPagamento.PROCESSANDO;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void aprovar() {
        this.status = StatusPagamento.APROVADO;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void rejeitar(MotivoRejeicao motivo) {
        this.status = StatusPagamento.REJEITADO;
        this.motivoRejeicao = motivo;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void cancelar() {
        this.status = StatusPagamento.CANCELADO;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void registrarBoleto(String linhaDigitavel, LocalDate dataVencimento) {
        this.linhaDigitavel = linhaDigitavel;
        this.dataVencimento = dataVencimento;
        this.status = StatusPagamento.AGUARDANDO_PAGAMENTO;
        this.atualizadoEm = LocalDateTime.now();
    }

    public boolean estaPendente() {
        return StatusPagamento.PENDENTE == this.status;
    }

    public boolean estaProcessando() {
        return StatusPagamento.PROCESSANDO == this.status;
    }

    public boolean estaAguardandoPagamento() {
        return StatusPagamento.AGUARDANDO_PAGAMENTO == this.status;
    }

    public boolean estaEmEstadoFinal() {
        return this.status.isEstadoFinal();
    }
}
