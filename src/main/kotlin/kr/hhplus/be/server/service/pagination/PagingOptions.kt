package kr.hhplus.be.server.service.pagination

data class PagingOptions(
    val page: Int,
    val size: Int,
) {
    init {
        require(page >= 0) { "Page index must be non-negative" }
        require(size in 1..100) { "Page size must be between 1 and 100" }
    }
}

