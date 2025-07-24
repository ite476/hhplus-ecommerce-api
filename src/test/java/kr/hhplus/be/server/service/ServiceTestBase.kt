package kr.hhplus.be.server.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kr.hhplus.be.server.util.KoreanTimeProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZoneId
import java.time.ZonedDateTime

@ExtendWith(MockKExtension::class)
abstract class ServiceTestBase {

    @MockK
    protected lateinit var timeProvider: KoreanTimeProvider

    protected val fixedTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Seoul"))

    @BeforeEach

    open fun setUp() {
        every { timeProvider.now() } returns fixedTime
    }
}