Feature: Pagamento via Boleto Bancario
  Regras de negocio para pagamentos via Boleto

  Scenario: Boleto deve ser gerado com linha digitavel e status aguardando pagamento
    Given um pagamento Boleto no valor de 350.00 na moeda BRL
    When o cliente envia o pagamento
    Then o codigo HTTP de retorno deve ser 201
    And o status de retorno deve ser AGUARDANDO_PAGAMENTO
    And a linha digitavel deve estar presente na resposta

  Scenario: Boleto deve ser aprovado apos confirmacao do gateway via webhook
    Given um boleto no valor de 200.00 foi criado com sucesso
    When o gateway confirma o pagamento do boleto via webhook
    Then o status final do boleto deve ser APROVADO

  Scenario: Boleto nao pago deve ser expirado pelo job de vencimento
    Given um boleto no valor de 150.00 foi criado com sucesso
    When o vencimento e alterado para ontem e o job de expiracao e executado
    Then o status final do boleto deve ser REJEITADO
    And o motivo de rejeicao final deve ser TIMEOUT
