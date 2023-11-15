package com.paymentic.infra.kafka;

import com.sun.source.doctree.SeeTree;
import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.CloudEventDeserializer;
import io.cloudevents.kafka.CloudEventSerializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.protocol.types.Field.Str;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@EnableKafka
public class CloudEventsKafkaProducer {
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, CloudEvent> kafkaListenerContainerFactory(@Value("${spring.kafka.bootstrap-servers}") String bootstrapService,
                                                                                                   @Value("${spring.kafka.consumer.group-id}")String consumerGroupId,
                                                                                                   @Value("${spring.kafka.properties.sasl.jaas.config}") String jaasCfg,
                                                                                                   @Value("${spring.kafka.properties.security.protocol}") String securityProtocol,
                                                                                                   @Value("${spring.kafka.properties.sasl.mechanism}") String saslMechanism) {
    ConcurrentKafkaListenerContainerFactory<String, CloudEvent> factory =  new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory(bootstrapService,consumerGroupId,jaasCfg,securityProtocol,saslMechanism));
    return factory;
  }
  public Map<String, Object> consumerConfigs(String bootstrapService, String consumerGroupId,String jaasCfg,String securityProtocol,String saslMechanism) {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapService);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CloudEventDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put("security.protocol", securityProtocol);
    props.put("sasl.mechanism", saslMechanism);
    props.put("sasl.jaas.config", jaasCfg);
    return props;
  }
  @Bean(name = "kafkaProducer")
  public ProducerFactory<String, CloudEvent> producerFactory(@Value("${spring.kafka.bootstrap-servers}") String bootstrapService,
                                                             @Value("${spring.kafka.properties.sasl.jaas.config}") String jaasCfg,
                                                             @Value("${spring.kafka.properties.security.protocol}") String securityProtocol,
                                                             @Value("${spring.kafka.properties.sasl.mechanism}") String saslMechanism) {
    return new DefaultKafkaProducerFactory<>(producerConfigs(bootstrapService,jaasCfg,securityProtocol,saslMechanism));
  }
  @Bean
  public ConsumerFactory<String, CloudEvent> consumerFactory(@Value("${spring.kafka.bootstrap-servers}") String bootstrapService,
                                                             @Value("${spring.kafka.consumer.group-id}")String consumerGroupId,
                                                             @Value("${spring.kafka.properties.sasl.jaas.config}") String jaasCfg,
                                                             @Value("${spring.kafka.properties.security.protocol}") String securityProtocol,
                                                             @Value("${spring.kafka.properties.sasl.mechanism}") String saslMechanism) {
    return new DefaultKafkaConsumerFactory<>(consumerConfigs(bootstrapService,consumerGroupId,jaasCfg,securityProtocol,saslMechanism));
  }
  @Bean
  public KafkaTemplate<String, CloudEvent> kafkaTemplate(@Value("${spring.kafka.bootstrap-servers}") String bootstrapService,
                                                         @Value("${spring.kafka.properties.sasl.jaas.config}") String jaasCfg,
                                                         @Value("${spring.kafka.properties.security.protocol}") String securityProtocol,
                                                         @Value("${spring.kafka.properties.sasl.mechanism}") String saslMechanism) {
    var template = new KafkaTemplate<>(producerFactory(bootstrapService,jaasCfg,securityProtocol,saslMechanism));
    template.setObservationEnabled(true);
    return template;
  }
  public Map<String, Object> producerConfigs(String bootstrapService,String jaasCfg,String securityProtocol,String saslMechanism) {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapService);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class);
    props.put("security.protocol", securityProtocol);
    props.put("sasl.mechanism", saslMechanism);
    props.put("sasl.jaas.config", jaasCfg);
    return props;
  }

}
