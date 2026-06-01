package com.couponrush.coupon.kafka;

import com.couponrush.coupon.kafka.dto.CouponIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponProducer {

    public static final String TOPIC = "coupon-issued";

    private final KafkaTemplate<String, CouponIssuedEvent> kafkaTemplate;

    public void produce(CouponIssuedEvent event) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(event.memberId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[coupon-issued] produce 실패 eventId={} memberId={}",
                                    event.eventId(), event.memberId(), ex);
                        }
                    });
        } catch (RuntimeException e) {
            // 발급 트랜잭션을 깨지 않기 위해 동기 예외도 삼킨다 (MVP, 개선안: Transactional Outbox)
            log.error("[coupon-issued] produce 동기 실패 eventId={} memberId={}",
                    event.eventId(), event.memberId(), e);
        }
    }
}
