package com.transaction.ingestion.service.config;

import com.riskplatform.common.event.TransactionEvent;
import com.transaction.ingestion.service.dto.*;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.transaction-received}")
    private String transactionReceivedTopic;

    @Value("${kafka.topics.transaction-validated}")
    private String transactionValidatedTopic;

    @Value("${kafka.topics.transaction-rejected}")
    private String transactionRejectedTopic;

    @Value("${kafka.topics.transaction-validation-failed:transaction-validation-failed}")
    private String transactionValidationFailedTopic;

    @Value("${kafka.partition-count}")
    private int partitionCount;

    @Value("${kafka.replication-factor}")
    private short replicationFactor;

    @Value("${kafka.retention-days}")
    private int retentionDays;

    // Producer Configuration
    @Bean
    public ProducerFactory<String, TransactionEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, TransactionEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, TransactionEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "transaction-ingestion-group");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES,
                "com.riskplatform.common.event,com.transaction.ingestion.service.model");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    // Topic Configuration
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic transactionReceivedTopic() {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionDays * 24 * 60 * 60 * 1000L));
        return new NewTopic(transactionReceivedTopic, partitionCount, replicationFactor).configs(configs);
    }

    @Bean
    public NewTopic transactionValidatedTopic() {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionDays * 24 * 60 * 60 * 1000L));
        return new NewTopic(transactionValidatedTopic, partitionCount, replicationFactor).configs(configs);
    }

    @Bean
    public NewTopic transactionRejectedTopic() {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionDays * 24 * 60 * 60 * 1000L));
        return new NewTopic(transactionRejectedTopic, partitionCount / 2, replicationFactor).configs(configs); // Half
                                                                                                               // the
                                                                                                               // partitions
                                                                                                               // for
                                                                                                               // rejected
                                                                                                               // topic
    }

    @Bean
    public NewTopic transactionValidationFailedTopic() {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionDays * 24 * 60 * 60 * 1000L));
        return new NewTopic(transactionValidationFailedTopic, partitionCount, replicationFactor).configs(configs);
    }
}