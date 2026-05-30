# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 확정 스펙(스키마, 엔티티, 발급 흐름, API, 작업 순서)의 source of truth는 [`docs/handoff.md`](docs/handoff.md)다.
> handoff.md의 **확정 스펙과 완성 코드는 임의로 변경하지 말 것.** 다른 방향이 더 낫다고 판단되면 먼저 제안하고 확인받는다.

## Hard Rules

이 규칙은 협상 대상이 아니다. 모든 작업에 우선 적용한다.

1. **검증 없이 완료 선언 금지.** "완료", "됐다", "수정함"이라고 말하기 전에 검증 명령을 실제로 실행하고 그 출력을 보여준다. (superpowers `verification-before-completion`)
   - 코드 수정 직후: `./gradlew compileJava` (빠른 컴파일 확인)
   - 단계 완료 전: `./gradlew build` 또는 관련 테스트 실행 + 애플리케이션 기동 확인
2. **원자적 커밋.** 한 커밋에는 하나의 논리적 변경만. handoff 8번 작업 순서의 단계 단위로 의미 있게 분리한다. (커밋 포맷은 아래 Commit Message Convention)
3. **승인 없이 건드리지 말 것 (Protected).** 아래는 변경 전 반드시 제안하고 확인받는다.
   - `docs/handoff.md`의 확정 스펙: DB 스키마, 완성 엔티티(Member/CouponEvent/IssuedCoupon/BaseTimeEntity), 발급 흐름 순서, 패키지 구조
   - 보안/정합성 경로: SecurityConfig, JwtProvider/JwtAuthenticationFilter, 발급 트랜잭션(`CouponIssueService`), `CouponRedisRepository`
4. **MVP 범위 엄수.** 범위 밖(관리자 페이지, 쿠폰 사용 처리, 알림 실제 발송, 결제, 미사용 컬럼)은 구현 금지.
5. **버그 수정은 최소 변경.** 한 번에 한 가지 원인만 고친다. 무관한 리팩터링을 끼워넣지 않는다.

<!-- ===== 상: 프로젝트 분석 ===== -->

## Project Overview

CouponRush는 배달앱 선착순 쿠폰 발급 백엔드다. Java 17 / Spring Boot 3.x / Gradle 기반이며, 핵심 챌린지는 10만 건 동시 요청 처리와 데이터 정합성이다. Redis로 1차 동시성 제어(재고/중복/대기열), MySQL을 source of truth로, Kafka로 발급 이벤트를 비동기 영속화한다. 백엔드 포트폴리오 프로젝트.

## Architecture

도메인 우선(package-by-feature) 구조. 계층 우선 패키징 금지.

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

### Layer Dependencies

- controller -> service -> repository(JPA) / redis / kafka 방향으로만 의존한다.
- Redis 접근은 반드시 `CouponRedisRepository`로 캡슐화한다. 서비스에 `RedisTemplate`을 직접 노출하지 않는다.
- 재고/중복 등 모든 if 분기 판단은 Java 서비스 계층에서 한다. Redis는 연산과 반환값만 담당한다.
- 발급 INSERT는 `getReferenceById`로 SELECT 없이 프록시 참조만 사용한다.

### 발급 흐름 (순서 엄수)

```
POST /api/coupons/{eventId}/issue
 -> Redis ZADD      coupon:queue:{eventId}   (대기열 진입)
 -> Redis SISMEMBER coupon:issued:{eventId}  (1이면 409 중복 거절)
 -> Redis DECR      coupon:stock:{eventId}   (반환값 < 0 이면 품절 거절)
 -> Redis SADD      coupon:issued:{eventId}  (발급자 등록)
 -> DB INSERT       issued_coupons (getReferenceById)   <- source of truth
 -> Kafka produce   topic=coupon-issued key=memberId
 -> 즉시 200 응답
        (비동기) CouponConsumer 수동 커밋 -> 알림 발송(로그 대체)
```

불변 규칙:
- **DB INSERT 성공 후에 Kafka produce** (역순 금지, 정합성 핵심)
- 정합성 2중 방어: Redis(1차) + DB `uk_issued_coupons_event_member`(최종)
- Kafka 토픽 `coupon-issued`, 파티션 3, key=memberId (파티션 내 순서 보장)
- consumer 수동 커밋 = at-least-once. produce 실패는 MVP에선 로그만 (README에 Transactional Outbox 개선안 명시)

### Custom Annotations

- `@Comment`(org.hibernate.annotations.Comment): 모든 엔티티 컬럼/테이블에 부착한다.
- `@CreatedDate` / `@LastModifiedDate`: `BaseTimeEntity`에서 사용. `JpaConfig`의 `@EnableJpaAuditing`이 있어야 동작한다.

## Development Commands

### Build and Run

```bash
# 로컬 인프라 기동 (MySQL + Redis + Kafka + Kafka UI)
docker-compose up -d

# 빌드
./gradlew build

# 실행 (Spring Boot는 IDE/도커 밖에서 실행)
./gradlew bootRun
```

### Testing

```bash
./gradlew test                       # 전체 테스트
./gradlew test --tests "ClassName"   # 단일 클래스
```

### Code Generation / Load Test

```bash
./gradlew compileJava   # QueryDSL Q-class 생성(선택 의존성)
k6 run <scenario>.js    # 부하테스트 (개선 전/후 TPS, 중복건수 비교)
```

## Environment

로컬 개발 중 Spring Boot는 IDE(도커 밖)에서 실행하고, 인프라만 docker-compose로 띄운다.

| 컴포넌트 | 주소 | 비고 |
|----------|------|------|
| MySQL | localhost:3306 | DB `couponrush`, user `coupon` / `coupon1234`, source of truth |
| Redis | localhost:6379 | 재고/중복/대기열/캐시 |
| Kafka | localhost:9092 | KRaft 모드, 발급 이벤트 비동기 |

JPA `ddl-auto`는 개발 중 `validate` 또는 `none` 권장. DDL은 `docs/handoff.md`를 따른다.

## Key Technologies

- **Core**: Java 17, Spring Boot 3.x, Gradle
- **Persistence**: Spring Data JPA, MySQL 8.0, QueryDSL(선택)
- **Cache / Concurrency**: Redis 7.2 (Lettuce)
- **Messaging**: Kafka (KRaft)
- **Security**: Spring Security, JWT (BCrypt 해시, 로그아웃 블랙리스트는 Redis TTL)
- **Observability**: Actuator + micrometer-prometheus, Prometheus, Grafana

## Domain Contexts

- **member**: 회원가입, 로그인(JWT 발급), 로그아웃(JWT Redis 블랙리스트)
- **coupon**: 쿠폰 이벤트 조회(Redis 캐시), 선착순 발급, 내 쿠폰 조회(fetch join)

## API Conventions

URL 패턴: `/api/{domain}/...`, 공통 응답은 `ApiResponse`로 감싼다.

- `POST /api/auth/signup` 회원가입
- `POST /api/auth/login` 로그인 (JWT 발급)
- `POST /api/auth/logout` 로그아웃 (JWT 블랙리스트 등록)
- `GET  /api/coupons/events` 발급 가능 이벤트 목록 (Redis 캐시)
- `POST /api/coupons/{eventId}/issue` 선착순 발급
- `GET  /api/coupons/my` 내 쿠폰 조회 (fetch join)

## Code Quality Standards

- 들여쓰기 4칸(스페이스), K&R braces, 줄 길이 150자.
- 패키지는 도메인 우선. 회사 prefix 금지, MVP 범위 밖 컬럼(status 류 등) 추가 금지.
- 네이밍: snake_case 컬럼, 테이블 복수형, 엔티티 단수. 제약조건 접두사 `pk_/fk_/uk_/idx_`. `@Table(name=...)` 명시.
- 연관관계: 객체 참조 + `@ManyToOne(fetch = LAZY)`. 읽기 경로 N+1 회피는 fetch join 또는 QueryDSL projection.
- Lombok 사용. 엔티티 기본생성자 `@NoArgsConstructor(access = PROTECTED)`, 생성은 `@Builder`.

> 검증/커밋/범위/Protected 규율은 상단 [Hard Rules](#hard-rules) 참조.

<!-- ===== 중: Serena 행동 강령 ===== -->

## Serena 사용 강령

### 검색 원칙
- 검색 순서: find_symbol(후보 탐색) -> get_symbols_overview(파일 구조) -> find_symbol+include_body=true(본문)
- relative_path를 항상 지정하여 검색 범위를 폴더/파일로 제한
- 파일 전체 읽기 금지, 심볼 단위로만 접근
- list_dir recursive=true는 src/ 등 대폴더에서 금지

### include_body 규칙
- include_body=true는 확정된 심볼 1개에만, 단독 실행
- 먼저 include_body=false로 후보를 좁힌 후에만 본문 로딩
- 동시에 여러 include_body=true 요청 금지 (JDT LS 큐 블로킹 발생)

### name_path_pattern 규칙
- 클래스+메서드 조합으로 구체적으로 작성 (예: "CouponIssueService.issue")
- 짧은 패턴("findBy", "get" 등)은 프로젝트 전수 검사 유발, 금지

### search_for_pattern 규칙
- restrict_search_to_code_files=true 항상 사용
- paths_include_glob로 특정 폴더만 대상으로 지정
- context_lines_before / context_lines_after 기본 0 유지

<!-- ===== 하: Git 규칙 ===== -->

## Commit Message Convention

포맷: `Type: subject`

- Type은 PascalCase, 비어있으면 안 됨. (개인 GitHub 포트폴리오라 JIRA-ID 없음)
- subject는 한국어 50자 이내, 마침표/느낌표로 끝내지 않음.
- Merge / Revert / Rebase / Tag 류는 위 규칙 예외.

| Type | 사용 상황 |
|------|-----------|
| Feat | 새 기능 추가 |
| Fix | 버그 수정 |
| Style | 코드 포맷 변경 |
| Refactor | 로직 개선 |
| File | 파일(이미지 등) 추가 |
| Design | 퍼블리싱 변경 |
| Comment | 주석 추가 |
| Test | 테스트 코드 추가 |
| Docs | 문서 업데이트 |
| Remove | 파일/코드 제거 |
| Ci | CI/CD 스크립트 변경 |
| Chore | 빌드/설정 등 잡무 |

예시:
```
Feat: 선착순 쿠폰 발급 API 구현
Fix: 재고 DECR 음수 분기 누락 수정
Docs: README 부하테스트 수치 추가
```

### Branch 전략

- `main`: 배포 가능한 안정 브랜치
- `features/<slug>`: 신규 기능 (단수형 `feature/` 금지)
- `fix/<slug>`: 버그 수정
- 솔로 포트폴리오라 MR 강제 규칙은 없음. 작업 단위로 브랜치를 나누고 의미 있게 커밋한다.
