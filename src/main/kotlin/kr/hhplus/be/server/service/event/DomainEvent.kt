package kr.hhplus.be.server.service.event

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * 애플리케이션 내부(in-process) 도메인 이벤트의 공통 베이스.
 * - eventId: 관찰성(추적) 및 중복 방지를 위한 고유 식별자
 * - occurredAt: 시스템 시간 정책(Asia/Seoul)에 맞춘 발생 시각
 */
open class DomainEvent(
    val eventId: UUID = UUID.randomUUID(),
    val occurredAt: ZonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
)