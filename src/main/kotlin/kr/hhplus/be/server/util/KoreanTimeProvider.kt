package kr.hhplus.be.server.util

import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

interface TimeProvider {
    fun now(): ZonedDateTime
}

@Service
class KoreanTimeProvider : TimeProvider {
    override fun now(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
}
