Feature: Pagamento via PIX
  Regras de negocio para pagamentos via PIX

  Scenario: PIX com dados validos deve ser aprovado
    Given um pagamento PIX no valor de 100.00 na moeda BRL
    When o cliente envia o pagamento
    Then o codigo HTTP de retorno deve ser 201
    And o status de retorno deve ser APROVADO

  Scenario: PIX com moeda invalida deve ser rejeitado
    Given um pagamento PIX no valor de 100.00 na moeda USD
    When o cliente envia o pagamento
    Then o codigo HTTP de retorno deve ser 200
    And o status de retorno deve ser REJEITADO
    And o motivo de rejeicao deve ser NAO_AUTORIZADO

  Scenario: PIX com valor acima do limite deve ser rejeitado
    Given um pagamento PIX no valor de 6000.00 na moeda BRL
    When o cliente envia o pagamento
    Then o codigo HTTP de retorno deve ser 200
    And o status de retorno deve ser REJEITADO
    And o motivo de rejeicao deve ser LIMITE_EXCEDIDO
