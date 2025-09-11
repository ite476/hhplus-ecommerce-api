# Kafka Basics

## 목적
로컬 환경에서 단일 브로커(KRaft) Kafka를 기동하고, 애플리케이션이 주문 생성 이벤트를 Kafka 토픽으로 발행할 수 있도록 최소 구성을 갖춘다.

## 개념 요약
- Topic / Partition / Offset: 이벤트 스트림 저장 단위, 병렬 처리 단위, 메시지 위치 지표
- Producer / Consumer / Consumer Group: 발행 / 구독 / 확장성 단위
- KRaft: Zookeeper 없이 동작하는 Kafka 내부 컨트롤러/브로커 모드

## 로컬 실행
- `docker compose up -d`
- Kafka UI: http://localhost:28080
- 브로커: `localhost:9092`

## 애플리케이션 연동 (최소)
- 의존성: `org.springframework.kafka:spring-kafka`
- 설정:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
```
- 이벤트 발행: `OrderCreated` → 토픽 `order.events`

## 확인 방법
1) Kafka UI에서 `order.events` 토픽 확인
2) 주문 생성 플로우 실행
3) UI에서 메시지 수신 확인 (키: `orderId`)

## 주의
- 개발 편의상 자동 토픽 생성, PLAINTEXT가 활성화되어 있음(로컬 한정)


