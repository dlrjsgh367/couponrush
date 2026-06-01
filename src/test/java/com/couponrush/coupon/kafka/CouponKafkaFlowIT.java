package com.couponrush.coupon.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.couponrush.coupon.kafka.dto.CouponIssuedEvent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.mockito.Mockito;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

class CouponKafkaFlowIT {

    private static final String TOPIC = "coupon-issued";

    private EmbeddedKafkaBroker broker;
    private ConcurrentMessageListenerContainer<String, CouponIssuedEvent> container;

    @BeforeEach
    void setUp() {
        broker = new EmbeddedKafkaKraftBroker(1, 3, TOPIC);
        broker.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        broker.destroy();
    }

    @Test
    void produce하면_같은_파티션_컨슈머가_수신하고_수동커밋한다() throws Exception {
        KafkaTemplate<String, CouponIssuedEvent> kafkaTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerProps()));
        CouponProducer couponProducer = new CouponProducer(kafkaTemplate);

        AtomicReference<CouponIssuedEvent> received = new AtomicReference<>();
        AtomicReference<String> receivedKey = new AtomicReference<>();
        CountDownLatch ackLatch = new CountDownLatch(1);

        ContainerProperties props = new ContainerProperties(TOPIC);
        props.setGroupId("coupon-notification-group-it");
        props.setAckMode(AckMode.MANUAL);
        props.setMessageListener((AcknowledgingMessageListener<String, CouponIssuedEvent>) (record, ack) -> {
            received.set(record.value());
            receivedKey.set(record.key());
            ack.acknowledge();
            ackLatch.countDown();
        });
        container = new ConcurrentMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(consumerProps()), props);
        container.start();

        CouponIssuedEvent event = new CouponIssuedEvent(1L, 100L, LocalDateTime.of(2026, 6, 1, 10, 0));
        couponProducer.produce(event);

        assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(received.get()).isEqualTo(event);
        assertThat(receivedKey.get()).isEqualTo("100");
    }

    @Test
    void CouponConsumer가_브로커에서_수신해서_ack을_호출한다() throws Exception {
        KafkaTemplate<String, CouponIssuedEvent> kafkaTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerProps()));
        CouponProducer couponProducer = new CouponProducer(kafkaTemplate);

        CouponConsumer realConsumer = new CouponConsumer();
        CountDownLatch ackLatch = new CountDownLatch(1);

        ContainerProperties props = new ContainerProperties(TOPIC);
        props.setGroupId("coupon-notification-group-it-2");
        props.setAckMode(AckMode.MANUAL);
        props.setMessageListener((AcknowledgingMessageListener<String, CouponIssuedEvent>) (record, ack) -> {
            Acknowledgment spyAck = Mockito.spy(ack);
            Mockito.doAnswer(invocation -> {
                ack.acknowledge();
                ackLatch.countDown();
                return null;
            }).when(spyAck).acknowledge();
            realConsumer.consume(record.value(), spyAck);
        });
        container = new ConcurrentMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(consumerProps()), props);
        container.start();

        couponProducer.produce(new CouponIssuedEvent(2L, 200L, LocalDateTime.of(2026, 6, 1, 11, 0)));

        assertThat(ackLatch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }

    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.couponrush.*");
        return props;
    }
}
