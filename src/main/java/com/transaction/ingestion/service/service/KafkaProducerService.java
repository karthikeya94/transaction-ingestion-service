package com.transaction.ingestion.service.service;

import com.riskplatform.common.event.TransactionEvent;
import com.riskplatform.common.event.TransactionValidatedEvent;
import com.transaction.ingestion.service.dto.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, TransactionValidatedEvent> kafkaTemplate;

    public void sendMessage(String topic, TransactionValidatedEvent message) {
        try {
            kafkaTemplate.send(topic, message.getCustomerId(), message);
            log.info("Message sent to topic {}: {}", topic, message);
        } catch (Exception e) {
            log.error("Error sending message to topic {}: {}", topic, e.getMessage(), e);
        }
    }
}
