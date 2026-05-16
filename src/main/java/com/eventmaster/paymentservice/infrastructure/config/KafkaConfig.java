package com.eventmaster.paymentservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic topicoPagamentoReprocessar() {
        return TopicBuilder.name("pagamento.reprocessar")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicoPagamentoAprovado() {
        return TopicBuilder.name("pagamento.aprovado")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicoPagamentoRejeitado() {
        return TopicBuilder.name("pagamento.rejeitado")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
