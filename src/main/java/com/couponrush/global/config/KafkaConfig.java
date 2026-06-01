package com.couponrush.global.config;

import com.couponrush.coupon.kafka.CouponProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic couponIssuedTopic() {
        return TopicBuilder.name(CouponProducer.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
