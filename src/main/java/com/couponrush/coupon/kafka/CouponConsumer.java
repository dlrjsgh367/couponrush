package com.couponrush.coupon.kafka;

import com.couponrush.coupon.kafka.dto.CouponIssuedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CouponConsumer {

    @KafkaListener(topics = CouponProducer.TOPIC, groupId = "coupon-notification-group")
    public void consume(CouponIssuedEvent event, Acknowledgment ack) {
        // MVP: 실제 알림 발송 대신 로그로 대체
        log.info("[coupon-issued] 알림 발송(로그 대체) eventId={} memberId={} issuedAt={}",
                event.eventId(), event.memberId(), event.issuedAt());
        ack.acknowledge();
    }
}
