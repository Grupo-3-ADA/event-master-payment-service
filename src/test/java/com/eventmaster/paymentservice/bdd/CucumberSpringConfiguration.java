package com.eventmaster.paymentservice.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {
        "pagamento.reprocessar", "pagamento.aprovado", "pagamento.rejeitado", "boleto.gerado"
})
public class CucumberSpringConfiguration {}
