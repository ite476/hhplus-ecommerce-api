package kr.hhplus.be.server.service.redis.port

import java.time.Duration

interface RedisPort {
    fun setValue(key: String, value: String, ttl: Duration? = null)
    fun getValue(key: String): String?
    fun delete(key: String)
}