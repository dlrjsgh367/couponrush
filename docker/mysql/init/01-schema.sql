-- CouponRush 스키마 (source of truth: docs/handoff.md)
-- MySQL 컨테이너 최초 기동 시 docker-entrypoint-initdb.d 로 실행된다 (MYSQL_DATABASE 컨텍스트).

CREATE TABLE members (
    id          BIGINT       NOT NULL AUTO_INCREMENT            COMMENT '회원 PK',
    email       VARCHAR(255) NOT NULL                           COMMENT '이메일 (로그인 ID, 유니크)',
    password    VARCHAR(255) NOT NULL                           COMMENT '비밀번호 (BCrypt 해시)',
    nickname    VARCHAR(50)  NOT NULL                           COMMENT '닉네임',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT uk_members_email UNIQUE (email)
) COMMENT '회원';

CREATE TABLE coupon_events (
    id           BIGINT       NOT NULL AUTO_INCREMENT            COMMENT '쿠폰 이벤트 PK',
    name         VARCHAR(100) NOT NULL                           COMMENT '이벤트명',
    total_stock  INT          NOT NULL                           COMMENT '총 발급 수량 (불변 정의값, 실시간 재고는 Redis)',
    discount     INT          NOT NULL                           COMMENT '할인 금액',
    start_at     DATETIME     NOT NULL                           COMMENT '발급 시작 일시',
    end_at       DATETIME     NOT NULL                           COMMENT '발급 종료 일시',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT pk_coupon_events PRIMARY KEY (id),
    INDEX idx_coupon_events_start_end (start_at, end_at)
) COMMENT '쿠폰 이벤트 (마스터)';

CREATE TABLE issued_coupons (
    id          BIGINT   NOT NULL AUTO_INCREMENT                COMMENT '발급 쿠폰 PK',
    event_id    BIGINT   NOT NULL                               COMMENT '쿠폰 이벤트 FK',
    member_id   BIGINT   NOT NULL                               COMMENT '발급받은 회원 FK',
    issued_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '발급 일시',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '생성 일시',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT pk_issued_coupons PRIMARY KEY (id),
    CONSTRAINT fk_issued_coupons_event  FOREIGN KEY (event_id)  REFERENCES coupon_events(id),
    CONSTRAINT fk_issued_coupons_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_issued_coupons_event_member UNIQUE (event_id, member_id),
    INDEX idx_issued_coupons_member_id (member_id)
) COMMENT '발급된 쿠폰 (중복 발급 방지: event_id+member_id 유니크)';
