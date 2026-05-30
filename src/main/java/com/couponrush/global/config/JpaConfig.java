package com.couponrush.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @CreatedDate / @LastModifiedDate 동작에 필수
@Configuration
@EnableJpaAuditing
public class JpaConfig {

}
