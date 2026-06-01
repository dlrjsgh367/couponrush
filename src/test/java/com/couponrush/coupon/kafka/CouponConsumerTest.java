package com.couponrush.coupon.kafka;

import static org.mockito.Mockito.verify;

import com.couponrush.coupon.kafka.dto.CouponIssuedEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class CouponConsumerTest {

    @Mock
    private Acknowledgment acknowledgment;
    @InjectMocks
    private CouponConsumer couponConsumer;

    @Test
    void 알림_처리_후_수동으로_오프셋을_커밋한다() {
        CouponIssuedEvent event = new CouponIssuedEvent(1L, 100L, LocalDateTime.now());

        couponConsumer.consume(event, acknowledgment);

        verify(acknowledgment).acknowledge();
    }
}
