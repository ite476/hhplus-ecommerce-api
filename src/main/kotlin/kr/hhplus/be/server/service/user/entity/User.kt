package kr.hhplus.be.server.service.user.entity

class User (
    val id: Long,
    name: String,
    point: Long
) {
    var name: String = name
        private set

    var point: Long = point
        private set
}