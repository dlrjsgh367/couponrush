package com.couponrush.coupon.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.couponrush.coupon.kafka.dto.CouponIssuedEvent;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class CouponProducerTest {

    private static final Long EVENT_ID = 1L;
    private static final Long MEMBER_ID = 100L;

    @Mock
    private KafkaTemplate<String, CouponIssuedEvent> kafkaTemplate;
    @InjectMocks
    private CouponProducer couponProducer;

    @Test
    void coupon_issued_토픽에_memberId를_키로_이벤트를_발행한다() {
        CouponIssuedEvent event = new CouponIssuedEvent(EVENT_ID, MEMBER_ID, LocalDateTime.now());
        given(kafkaTemplate.send(eq("coupon-issued"), eq(String.valueOf(MEMBER_ID)), any(CouponIssuedEvent.class)))
                .willReturn(CompletableFuture.completedFuture((SendResult<String, CouponIssuedEvent>) null));

        couponProducer.produce(event);

        verify(kafkaTemplate).send("coupon-issued", String.valueOf(MEMBER_ID), event);
    }

    @Test
    void produce가_동기적으로_예외를_던지면_삼키고_예외를_전파하지_않는다() {
        CouponIssuedEvent event = new CouponIssuedEvent(EVENT_ID, MEMBER_ID, LocalDateTime.now());
        given(kafkaTemplate.send(eq("coupon-issued"), eq(String.valueOf(MEMBER_ID)), any(CouponIssuedEvent.class)))
                .willThrow(new RuntimeException("broker down"));

        couponProducer.produce(event);
    }
}
