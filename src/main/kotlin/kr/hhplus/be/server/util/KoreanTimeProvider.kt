package kr.hhplus.be.server.util

import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

interface TimeProvider {
    fun now(): ZonedDateTime
    fun toZonedDateTime(instant: Instant): ZonedDateTime
}

@Service
class KoreanTimeProvider : TimeProvider {
    private val zoneId: ZoneId = ZoneId.of("Asia/Seoul")

    override fun now(): ZonedDateTime = ZonedDateTime.now(zoneId)

    override fun toZonedDateTime(instant: Instant): ZonedDateTime {
        return instant.atZone(zoneId)
    }
}
