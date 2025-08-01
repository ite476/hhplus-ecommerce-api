package kr.hhplus.be.server.repository.pagination

import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.pagination.PagingOptions
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

fun PagingOptions.toPageable() : Pageable {
    return PageRequest.of(page, size)
}

fun <T> Page<T>.toPagedList() : PagedList<T> {
    return PagedList(
        items = content,
        page = number,
        size = size,
        totalCount = totalElements
    )
}

fun <From, To> Page<From>.toPagedList(convert: From.() -> To) : PagedList<To> {
    return PagedList(
        items = content.map { convert(it) },
        page = number,
        size = size,
        totalCount = totalElements
    )
}