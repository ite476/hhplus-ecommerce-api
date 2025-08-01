package kr.hhplus.be.server.controller.v1.product

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.dto.request.PagingOptionsRequestParam
import kr.hhplus.be.server.controller.v1.product.response.GetProductsPopularResponse
import kr.hhplus.be.server.controller.v1.product.response.GetProductsResponse
import kr.hhplus.be.server.service.pagination.PagedList
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.usecase.FindPagedPopularProductsUsecase
import kr.hhplus.be.server.service.product.usecase.FindPagedProductsUsecase
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product API", description = "상품 API")
class ProductController (
    val findAllProductsUsecase: FindPagedProductsUsecase,
    val findAllPopularProductsUsecase: FindPagedPopularProductsUsecase
    ) : ProductApiSepc {

    @GetMapping("")
    override fun getProducts(
        @ParameterObject pagingOptions: PagingOptionsRequestParam
    ): ResponseEntity<GetProductsResponse> {
        val products: PagedList<Product> = findAllProductsUsecase.findPagedProducts(pagingOptions.toPagingOptions());

        val productsResposne: GetProductsResponse = GetProductsResponse.fromEntity(products)

        return ResponseEntity.ok(productsResposne)
    }

    @GetMapping("/popular")
    override fun getPopularProducts(
        @ParameterObject pagingOptions: PagingOptionsRequestParam
    ): ResponseEntity<GetProductsPopularResponse> {
        val popularProducts: PagedList<ProductSaleSummary> = findAllPopularProductsUsecase.findPagedPopularProducts(pagingOptions.toPagingOptions())

        val popularResponse: GetProductsPopularResponse = GetProductsPopularResponse.fromEntity(popularProducts)

        return  ResponseEntity.ok(popularResponse)
    }
}