# CouponRush — Claude Code 핸드오프

배달앱 선착순 쿠폰 발급 서비스. 백엔드 포트폴리오.
핵심 챌린지: 10만 건 동시 요청 처리 + 데이터 정합성.

이 문서는 설계가 끝난 상태에서 구현을 위임하기 위한 것이다.
아래 **확정 스펙과 완성 코드는 변경하지 말 것.** 변경이 필요하면 먼저 물어볼 것.

---

## 1. 기술 스택 / 환경

- Java 17, Spring Boot 3.x, Gradle
- MySQL 8.0 (source of truth)
- Redis 7.2 (재고/중복/대기열/캐시)
- Kafka (KRaft 모드, 발급 이벤트 비동기)
- 로컬 인프라는 `docker-compose.yml`로 기동 (MySQL+Redis+Kafka+Kafka UI, 별도 제공됨)
- 개발 중 Spring Boot는 IDE(도커 밖)에서 실행 → 접속 주소:
  - MySQL `localhost:3306`, DB `couponrush`, user `coupon` / `coupon1234`
  - Redis `localhost:6379`
  - Kafka `localhost:9092`

## 2. 코드 컨벤션 (필수 준수)

- 패키지: 도메인 우선(package-by-feature). 계층 우선 금지.
- 네이밍: snake_case 컬럼, 테이블 복수형, 제약조건 접두사 `pk_/fk_/uk_/idx_`
- 엔티티는 단수(`Member`), 테이블은 복수(`members`) + `@Table(name=...)` 명시
- 회사 prefix 금지. status 류 미사용 컬럼 추가 금지 (MVP 범위 엄수)
- 연관관계: 객체 참조 + `@ManyToOne(LAZY)`. 발급 INSERT는 `getReferenceById`로 SELECT 없이.
- 읽기 경로 N+1 회피: fetch join 또는 QueryDSL projection
- 엔티티 컬럼/테이블에 `@Comment`(org.hibernate.annotations.Comment) 부착
- Lombok 사용. 엔티티 기본생성자는 `@NoArgsConstructor(access = PROTECTED)`, 생성은 `@Builder`

## 3. 패키지 구조 (확정)

```
com.couponrush
├── global
│   ├── config        RedisConfig, KafkaConfig, SecurityConfig, JpaConfig
│   ├── common        ApiResponse, BaseTimeEntity
│   ├── error         ErrorCode, BusinessException, GlobalExceptionHandler
│   └── jwt           JwtProvider, JwtAuthenticationFilter
├── member
│   ├── controller    AuthController
│   ├── service       AuthService
│   ├── domain        Member
│   ├── repository    MemberRepository
│   └── dto           SignUpRequest, LoginRequest, TokenResponse
└── coupon
    ├── controller    CouponEventController, CouponIssueController
    ├── service       CouponEventService, CouponIssueService, CouponStockService
    ├── domain        CouponEvent, IssuedCoupon
    ├── repository    CouponEventRepository, IssuedCouponRepository
    ├── redis         CouponRedisRepository
    ├── kafka         CouponProducer, CouponConsumer, dto/CouponIssuedEvent
    └── dto           CouponEventResponse, IssueRequest, MyCouponResponse
```

## 4. DB 스키마 (확정 DDL)

JPA `ddl-auto`는 개발 중 `validate` 또는 `none` 권장. 아래 DDL을 source of truth로 둔다.

```sql
CREATE TABLE members (
    id          BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '회원 PK',
    email       VARCHAR(255) NOT NULL                         COMMENT '이메일 (로그인 ID, 유니크)',
    password    VARCHAR(255) NOT NULL                         COMMENT '비밀번호 (BCrypt 해시)',
    nickname    VARCHAR(50)  NOT NULL                         COMMENT '닉네임',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT uk_members_email UNIQUE (email)
) COMMENT '회원';

CREATE TABLE coupon_events (
    id           BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '쿠폰 이벤트 PK',
    name         VARCHAR(100) NOT NULL                         COMMENT '이벤트명',
    total_stock  INT          NOT NULL                         COMMENT '총 발급 수량 (불변 정의값, 실시간 재고는 Redis)',
    discount     INT          NOT NULL                         COMMENT '할인 금액',
    start_at     DATETIME     NOT NULL                         COMMENT '발급 시작 일시',
    end_at       DATETIME     NOT NULL                         COMMENT '발급 종료 일시',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT pk_coupon_events PRIMARY KEY (id),
    INDEX idx_coupon_events_start_end (start_at, end_at)
) COMMENT '쿠폰 이벤트 (마스터)';

CREATE TABLE issued_coupons (
    id          BIGINT   NOT NULL AUTO_INCREMENT              COMMENT '발급 쿠폰 PK',
    event_id    BIGINT   NOT NULL                             COMMENT '쿠폰 이벤트 FK',
    member_id   BIGINT   NOT NULL                             COMMENT '발급받은 회원 FK',
    issued_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '발급 일시',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '생성 일시',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT pk_issued_coupons PRIMARY KEY (id),
    CONSTRAINT fk_issued_coupons_event  FOREIGN KEY (event_id)  REFERENCES coupon_events(id),
    CONSTRAINT fk_issued_coupons_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_issued_coupons_event_member UNIQUE (event_id, member_id),
    INDEX idx_issued_coupons_member_id (member_id)
) COMMENT '발급된 쿠폰 (중복 발급 방지: event_id+member_id 유니크)';
```

## 5. 완성된 엔티티 (그대로 사용)

```java
// global/common/BaseTimeEntity.java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate @Column(updatable = false) @Comment("생성 일시")
    private LocalDateTime createdAt;
    @LastModifiedDate @Comment("수정 일시")
    private LocalDateTime updatedAt;
}
```

```java
// global/config/JpaConfig.java  — @CreatedDate/@LastModifiedDate 동작에 필수
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
```

```java
// member/domain/Member.java
@Entity @Getter @Table(name = "members") @Comment("회원")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Comment("회원 PK")
    private Long id;
    @Column(nullable = false, unique = true) @Comment("이메일 (로그인 ID, 유니크)")
    private String email;
    @Column(nullable = false) @Comment("비밀번호 (BCrypt 해시)")
    private String password;
    @Column(nullable = false, length = 50) @Comment("닉네임")
    private String nickname;

    @Builder
    public Member(String email, String password, String nickname) {
        this.email = email; this.password = password; this.nickname = nickname;
    }
}
```

```java
// coupon/domain/CouponEvent.java
@Entity @Getter @Table(name = "coupon_events") @Comment("쿠폰 이벤트 (마스터)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEvent extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Comment("쿠폰 이벤트 PK")
    private Long id;
    @Column(nullable = false, length = 100) @Comment("이벤트명")
    private String name;
    @Column(nullable = false) @Comment("총 발급 수량 (불변 정의값, 실시간 재고는 Redis)")
    private int totalStock;
    @Column(nullable = false) @Comment("할인 금액")
    private int discount;
    @Column(nullable = false) @Comment("발급 시작 일시")
    private LocalDateTime startAt;
    @Column(nullable = false) @Comment("발급 종료 일시")
    private LocalDateTime endAt;

    @Builder
    public CouponEvent(String name, int totalStock, int discount,
                       LocalDateTime startAt, LocalDateTime endAt) {
        this.name = name; this.totalStock = totalStock; this.discount = discount;
        this.startAt = startAt; this.endAt = endAt;
    }
}
```

```java
// coupon/domain/IssuedCoupon.java
@Entity @Getter @Table(name = "issued_coupons")
@Comment("발급된 쿠폰 (중복 발급 방지: event_id+member_id 유니크)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Comment("발급 쿠폰 PK")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false) @Comment("쿠폰 이벤트 FK")
    private CouponEvent event;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false) @Comment("발급받은 회원 FK")
    private Member member;
    @Comment("발급 일시")
    private LocalDateTime issuedAt;

    @Builder
    public IssuedCoupon(CouponEvent event, Member member, LocalDateTime issuedAt) {
        this.event = event; this.member = member; this.issuedAt = issuedAt;
    }
}
```

## 6. 발급 흐름 (핵심 로직 — 순서 엄수)

```
POST /api/coupons/{eventId}/issue
 → Redis ZADD   coupon:queue:{eventId}   score=timestamp member=memberId   (대기열 진입)
 → Redis SISMEMBER coupon:issued:{eventId} memberId   (1이면 409 중복 거절)
 → Redis DECR   coupon:stock:{eventId}                 (반환값 < 0 이면 품절 거절)
 → Redis SADD   coupon:issued:{eventId} memberId       (발급자 등록)
 → DB INSERT    issued_coupons (getReferenceById 사용)  ← source of truth
 → Kafka produce  topic=coupon-issued  key=memberId  value={eventId,memberId,issuedAt}
 → 즉시 200 응답
        ┄┄ 비동기 ┄┄
 → CouponConsumer (group=coupon-notification-group, 수동 커밋) → 알림 발송(로그 대체)
```

불변 규칙
- **DB INSERT 성공 후에 Kafka produce** (역순 금지 — 정합성)
- 재고 판단/중복 판단 등 모든 if 분기는 Java 서비스에서. Redis는 연산/반환만.
- 이벤트 시작 시 DB `total_stock`을 Redis `SET coupon:stock:{eventId}`로 적재 (CouponStockService)
- Redis 접근은 `CouponRedisRepository`로 캡슐화 (서비스에 redisTemplate 직접 노출 금지)
- Kafka 토픽 `coupon-issued`, 파티션 3, key=memberId(파티션 내 순서 보장)
- consumer 수동 커밋 = at-least-once. produce 실패는 MVP에선 로그만 남김
  (README에 Transactional Outbox 패턴 개선안 명시)
- 정합성 2중 방어: Redis(1차) + DB `uk_issued_coupons_event_member`(최종)

## 7. API 목록 (MVP)

- `POST /api/auth/signup` — 회원가입
- `POST /api/auth/login` — 로그인 (JWT 발급)
- `POST /api/auth/logout` — 로그아웃 (JWT Redis 블랙리스트 등록)
- `GET  /api/coupons/events` — 발급 가능 이벤트 목록 (Redis 캐시)
- `POST /api/coupons/{eventId}/issue` — 선착순 발급
- `GET  /api/coupons/my` — 내 쿠폰 조회 (fetch join)

## 8. 작업 순서 (이 순서로 진행)

- [x] 1. Spring Boot 프로젝트 셋업 — 의존성: web, validation, data-jpa, data-redis,
      kafka, security, lombok, mysql-connector, (querydsl 선택), actuator + micrometer-prometheus
- [x] 2. `application.yml` — datasource/redis/kafka/jpa(ddl-auto=validate) 설정
- [x] 3. global 공통 — BaseTimeEntity, JpaConfig, ApiResponse, ErrorCode/BusinessException/GlobalExceptionHandler
- [x] 4. 엔티티(위 5번) 배치 + Repository 3종
- [x] 5. 회원가입/로그인 — SecurityConfig(BCrypt), JwtProvider, JwtAuthenticationFilter, AuthService/Controller
- [x] 6. JWT 로그아웃 블랙리스트 (Redis, TTL=토큰 만료까지)
- [x] 7. 쿠폰 이벤트 조회 API + Redis 캐시
- [x] 8. CouponRedisRepository (ZADD/SISMEMBER/DECR/SADD 캡슐화) + CouponStockService(재고 적재)
- [x] 9. 선착순 발급 API (6번 흐름) — getReferenceById로 INSERT
- [x] 10. Kafka producer/consumer (수동 커밋)
- [x] 11. 내 쿠폰 조회 (fetch join)
- [x] 12. k6 부하테스트 시나리오 (개선 전/후 TPS·중복건수 비교)
- [x] 13. Prometheus + Grafana 연동 (actuator /prometheus 노출)
- [ ] 14. GitHub Actions CI (빌드/테스트)
- [ ] 15. OCI 배포
- [ ] 16. README (아키텍처 다이어그램 + 부하테스트 수치 + 트러블슈팅)

## 9. 범위 밖 (구현 금지)

관리자 페이지 / 쿠폰 사용 처리 / 알림 실제 발송(로그로 대체) / 결제 연동 / 미사용 컬럼 추가

## 10. 작업 시 지켜야 할 것

- 위 확정 스펙(스키마/엔티티/패키지/흐름)과 다른 방향이 더 낫다고 판단되면, 임의 변경 말고 먼저 제안할 것
- 각 단계 완료 후 컴파일 + 기동 확인하고 다음으로
- 커밋은 단계 단위로 의미 있게 분리
