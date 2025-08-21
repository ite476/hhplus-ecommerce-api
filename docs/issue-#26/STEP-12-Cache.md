# ⚡ 캐시 도입 절차 및 결과 보고서

> 버전: 2025-08-15 · 담당: BE · 범위: 인기상품 조회(읽기 성능 최적화)

---

## 📌 개요

* **목표**: 인기상품 조회 API의 응답 시간을 캐시(Hit 기준)로 **< 200ms** 수준으로 단축
* **접근**: Redisson 인프라 유지 + `CachePort` 기반 **Cache-Aside 패턴**(직접 구현)
* **비범위(Out of Scope)**: 전 엔드포인트 일괄 캐시, 분산 캐시 일관성 강제, 백그라운드 리프레시 워커 운영

---

## 🧭 배경 & 의사결정 요약

* 분산 락 도입 시 구축한 **Redisson** 스택을 **재사용**
* AOP + SpEL 기반 캐시 키 추출은, 락과 동일하게 **학습 곡선/가시성 이슈**로 제외
* 인터페이스 중심 설계: `CachePort`를 정의하고 **Adapter**(Redis)로 구현 → 테스트/교체 용이성 확보
* 캐시 정책: **Cache-Aside** (미스 시 원본 조회 → 캐시 쓰기 → 반환)

---

## 🎯 도입 대상 선정 근거

1. **인기상품 조회** 평균 응답 **\~10s** (집계/조인 비용 큼)
2. **시간 축**: "현재 시점 기준 n일 전 \~ 현재" 조회 → **완전 실시간 일관성 비요구**
3. **TTL 제안**: 운영 가정 하에 **10분 \~ 1시간**(±지터) → 과도한 스탬피드 방지 & 충분한 신선도 균형

---

## 🏗️ 설계 개요

### 🔑 키 스킴

* 네임스페이스: `hhplus:{env}:cache:popular:v1:{dateBucket}:{page}:{size}`

    * `env` = `local|dev|stg|prod`
    * `dateBucket` = `yyyy-MM-dd` (일 단위 버킷; 필요 시 `yyyy-MM-dd:HH`로 확장)
    * \*\*버전 필드(v1)\*\*로 스키마 변경/정책 변경 시 대량 무효화 용이
* 사용 예: `hhplus:prod:cache:popular:v1:2025-08-15:0:20`

> **각주**: 현재 구현에서는 키를 간소화하여 `"popularProducts:${pagingOptions.page}:${pagingOptions.size}"` 형식을 사용함. 기능 안정화/검증 이후 본 문서의 체계적 키 발급 스킴(네임스페이스/버전/버킷)을 단계적으로 적용 예정.

### 🧠 캐시 전략

* **패턴**: Cache-Aside
* **TTL**: 기본 10m, 인기 구간/트래픽 상황에 따라 30m\~60m까지 조정. 만료 지점 집중을 피하기 위해 **±10% 지터** 적용 권장
* **Stampede 방지(설계)**: 미스 시 **SingleFlight**(단일 로더) 또는 **소프트 TTL(logical TTL + 백그라운드 리프레시)**
* **Invalidation**: 주문/재고 이벤트에 의한 즉시성 요구 낮음 → 기본은 TTL 기반. 추후 이벤트 기반 **선제 무효화** 확장 포인트 노출

### 🧾 직렬화/역직렬화

* **문제**: `ZonedDateTime` 역직렬화 오류
* **대응 옵션**

    1. **DTO 경량화**: 시간 필드를 **`epochMillis: Long`** 또는 `Instant`로 저장 (가장 견고, 가독성 낮음)
    2. **Jackson 설정(우선 선택)**: `JavaTimeModule` 등록 + `WRITE_DATES_AS_TIMESTAMPS=false` + `KotlinModule` + 필요 시 `@JsonFormat`으로 패턴 고정 → **ISO 8601 가독성 유지**를 목표
    3. **포맷 고정(Fallback)**: 시간 필드를 **`ISO_OFFSET_DATE_TIME` 문자열**로 직렬화/역직렬화. 타임존 표기(오프셋)가 일부 유실될 수 있으나, **ISO 8601-ish 문자열 가독성 유지**를 전제
* **프로젝트 선택 사유**: **ISO 8601 가독성을 포기하지 않기 위함**. 2안을 먼저 재시도하고, 이슈 지속 시 3안으로 전환.

---

## 🔧 구현 요약

* Redisson 인프라 **재사용**
* `CachePort` 인터페이스 설계 및 Redis Adapter 개발 진행
* **Write 저장** 및 Redis에 직렬화된 데이터 확인 완료
* **Read 시 `ZonedDateTime` 파싱 오류**로 역직렬화 실패 → 수정 필요

```kotlin
// CachePort (예시)
interface CachePort {
  suspend fun <T: Any> get(key: String, type: KClass<T>): T?
  suspend fun <T: Any> set(key: String, value: T, ttl: Duration)
  suspend fun evict(key: String)

  // 미스 시 단일 로딩까지 포함하는 편의 API (추가 제안)
  suspend fun <T: Any> getOrLoad(
    key: String,
    type: KClass<T>,
    ttl: Duration,
    loader: suspend () -> T
  ): T
}
```

```kotlin
// Redis Adapter Jackson 설정 (예시)
val mapper = ObjectMapper()
  .registerModule(JavaTimeModule())
  .registerModule(kotlinModule())
  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)

val codec = JsonJacksonCodec(mapper)
val redisson = Redisson.create(Config().apply {
  useSingleServer().address = "redis://$host:$port"
})

// 사용: RBucket<String> or RMapCache<String, String> + codec
```

```kotlin
// 서비스 사용 패턴 (Cache-Aside)
suspend fun getPopularProducts(q: Query): PopularProductsResponse {
  val key = keyOf(q) // hhplus:prod:cache:popular:v1:2025-08-15:${q.page}:${q.size}
  return cache.getOrLoad(key, PopularProductsCached::class, ttl = 10.minutes) {
    // MISS → 원본 조회
    val raw = repository.findPopularProducts(q)
    PopularProductsCached.from(raw) // 시간 → epochMillis 변환 등
  }.toResponse()
}
```

---

## 🧪 테스트 전략 (현황 & 계획)

### 현황

* **Write 경로**: Redis 저장 확인(OK)
* **Read 경로**: `ZonedDateTime` 역직렬화 실패(수정 필요)

### 계획

1. **단위 테스트(Port/Adapter)**

    * DTO ↔ JSON 직렬화/역직렬화 (시간 필드)
    * TTL 만료 동작, 키 생성 규칙, 네임스페이스 분리
2. **통합 테스트(Testcontainers + Redis)**

    * `@Testcontainers`로 Redis 부트스트랩, `JsonJacksonCodec` 구성 확인
    * **Hit/Miss** 비율 시나리오, 만료 후 재적재
3. **경합/Stampede**

    * 동시 다건 MISS 상황 재현 → SingleFlight/락 미적용 시 문제 관찰
    * 후속으로 **락 or 작업 큐** 도입 시 회귀 테스트 추가
4. **충돌 방지**

    * 락 로직과 동일 Redis 인스턴스 사용 시, **키 프리픽스/DB index**로 **충돌 방지** 확인

```kotlin
@Testcontainers
class PopularCacheIT {
  companion object {
    @Container @JvmField val redis = GenericContainer("redis:7.2").withExposedPorts(6379)
  }
  @DynamicPropertySource
  fun props(reg: DynamicPropertyRegistry) {
    reg.add("spring.data.redis.host") { redis.host }
    reg.add("spring.data.redis.port") { redis.getMappedPort(6379) }
  }

  @Test fun `hit-miss 시나리오`() = runBlocking {
    // 1) miss → load → set → hit 확인
    // 2) TTL 경과 후 miss → 재적재 확인
  }
}
```

---

## 📊 모니터링 & KPI

* **Hit Ratio**: 목표 80%+
* **응답시간**: Hit P95 < 100ms, Miss P95 개선 추이
* **Stampede 지표**: 동일 키 동시 로딩 수
* **Redis 메모리/키스페이스**: 메모리 사용량, eviction count
* **직렬화 오류율**: 캐시 역직렬화 실패 비율

> Micrometer 커스텀 메트릭 예: `counter(cache.popular.hit)`, `counter(cache.popular.miss)`, `timer(cache.popular.load)`

---

## ⚠️ 리스크 & 대응

* **ZonedDateTime 역직렬화 실패** → **ISO 8601 문자열 직렬화(옵션 3)** 또는 **JavaTimeModule 설정(옵션 2)** 재시도로 해결. 타임존 표기 유실 가능 시에도 ISO 8601-ish 포맷 유지
* **Cache Stampede** → SingleFlight(분산 락 or in-progress 플래그 키), TTL 지터, 소프트 TTL
* **Stale 데이터** → TTL/버킷 전략 + 이벤트 기반 무효화(후속)
* **키 충돌** → 고정 프리픽스 + env + 버전 + 파라미터 정규화
* **메모리 폭주** → 상위 N 페이지 캐시, LZ4 압축(선택), TTL 축소

---

## 📈 결과 요약

1. **캐시 Write/저장 확인 완료**
2. **Read 시 역직렬화 오류로 반환 실패** → 시간 필드 전략 변경 필요
3. **미구현 과제**: Write 경합 제어(락/큐), Testcontainers 통합 테스트, Port/Adapter 단위·통합 테스트, 키 충돌 방지 테스트

---

## 🗺️ 향후 작업(Backlog)

* [ ] **JavaTimeModule 설정 재시도(옵션 2)** 및 실패 시 **ISO 8601 문자열 포맷 고정(옵션 3)**
* [ ] `getOrLoad`에 **SingleFlight**(키별 락) 도입
* [ ] Testcontainers + Redis 통합 테스트 구성
* [ ] 히트율/지연시간 **메트릭 수집 & 대시보드**
* [ ] 키 버전/네임스페이스 정책 문서화
* [ ] 이벤트 기반 **선제 무효화** PoC (주문량 급증 시)
