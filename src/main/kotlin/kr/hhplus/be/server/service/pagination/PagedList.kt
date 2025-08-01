package kr.hhplus.be.server.service.pagination

data class PagedList<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalCount: Long
) {
    companion object {
        fun <T> empty(): PagedList<T> {
            return PagedList(items = listOf(), page = 0, size = 0, totalCount = 0)
        }
    }
}

