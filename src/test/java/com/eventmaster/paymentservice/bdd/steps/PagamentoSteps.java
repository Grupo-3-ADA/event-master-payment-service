package com.eventmaster.paymentservice.bdd.steps;

import com.eventmaster.paymentservice.infrastructure.messaging.BoletoExpiracaoJob;
import com.eventmaster.paymentservice.infrastructure.persistence.PagamentoJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
@ScenarioScope
public class PagamentoSteps {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BoletoExpiracaoJob boletoExpiracaoJob;
    @Autowired PagamentoJpaRepository pagamentoJpaRepository;

    private String metodoPagamento;
    private String valor;
    private String moeda;
    private MvcResult ultimoResultado;
    private String ultimoResponseBody;
    private UUID ultimoPagamentoId;
    private String ultimoResponseBodyFinal;

    @Before
    public void limparEstado() {
        metodoPagamento = null;
        valor = null;
        moeda = null;
        ultimoResultado = null;
        ultimoResponseBody = null;
        ultimoPagamentoId = null;
        ultimoResponseBodyFinal = null;
    }

    // ─── Given ───────────────────────────────────────────────────────────────

    @Given("um pagamento {word} no valor de {double} na moeda {word}")
    public void umPagamentoNoValorDe(String metodo, double valorDouble, String moedaParam) {
        this.metodoPagamento = metodo.toUpperCase();
        this.valor = String.valueOf(valorDouble);
        this.moeda = moedaParam;
    }

    @Given("um boleto no valor de {double} foi criado com sucesso")
    public void umBoletoFoiCriadoComSucesso(double valorDouble) throws Exception {
        String corpo = corpoRequisicao(UUID.randomUUID(), String.valueOf(valorDouble), "BRL", "BOLETO");

        MvcResult resultado = mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> resposta = objectMapper.readValue(resultado.getResponse().getContentAsString(), Map.class);
        ultimoPagamentoId = UUID.fromString((String) resposta.get("id"));
    }

    // ─── When ────────────────────────────────────────────────────────────────

    @When("o cliente envia o pagamento")
    public void oClienteEnviaOPagamento() throws Exception {
        String corpo = corpoRequisicao(UUID.randomUUID(), valor, moeda, metodoPagamento);

        ultimoResultado = mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andReturn();

        ultimoResponseBody = ultimoResultado.getResponse().getContentAsString();
    }

    @When("o gateway confirma o pagamento do boleto via webhook")
    public void oGatewayConfirmaOBoleto() throws Exception {
        String corpo = """
                {"pagamentoId": "%s"}
                """.formatted(ultimoPagamentoId);

        mockMvc.perform(post("/webhooks/boleto/confirmacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
    }

    @When("o vencimento e alterado para ontem e o job de expiracao e executado")
    public void oVencimentoEAlteradoEJobExecutado() {
        pagamentoJpaRepository.findById(ultimoPagamentoId).ifPresent(entity -> {
            entity.setDataVencimento(LocalDate.now().minusDays(1));
            pagamentoJpaRepository.save(entity);
        });
        boletoExpiracaoJob.expirarBoletosVencidos();
    }

    // ─── Then ────────────────────────────────────────────────────────────────

    @Then("o codigo HTTP de retorno deve ser {int}")
    public void oCodigoHttpDeveSerRetornado(int codigoEsperado) {
        assertThat(ultimoResultado.getResponse().getStatus()).isEqualTo(codigoEsperado);
    }

    @Then("o status de retorno deve ser {word}")
    public void oStatusDeRetornoDeveSer(String statusEsperado) {
        assertThat(ultimoResponseBody).contains(statusEsperado);
    }

    @Then("o motivo de rejeicao deve ser {word}")
    public void oMotivoDeRejeicaoDeveSer(String motivoEsperado) {
        assertThat(ultimoResponseBody).contains(motivoEsperado);
    }

    @Then("a linha digitavel deve estar presente na resposta")
    public void aLinhaDigitavelDeveEstarPresente() {
        assertThat(ultimoResponseBody).contains("linhaDigitavel");
    }

    @Then("o status final do boleto deve ser {word}")
    public void oStatusFinalDoBoletoDeve(String statusEsperado) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/pagamentos/{id}", ultimoPagamentoId))
                .andReturn();
        ultimoResponseBodyFinal = resultado.getResponse().getContentAsString();
        assertThat(ultimoResponseBodyFinal).contains(statusEsperado);
    }

    @Then("o motivo de rejeicao final deve ser {word}")
    public void oMotivoDeRejeicaoFinalDeveSer(String motivoEsperado) {
        assertThat(ultimoResponseBodyFinal).contains(motivoEsperado);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String corpoRequisicao(UUID pedidoId, String valor, String moeda, String metodo) {
        return """
                {
                  "pedidoId": "%s",
                  "clienteId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                  "valor": %s,
                  "moeda": "%s",
                  "metodoPagamento": "%s"
                }
                """.formatted(pedidoId, valor, moeda, metodo);
    }
}
