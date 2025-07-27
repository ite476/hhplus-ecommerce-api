package kr.hhplus.be.server.controller.v1.product

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.product.response.GetProductsPopularResponse
import kr.hhplus.be.server.controller.v1.product.response.GetProductsResponse
import kr.hhplus.be.server.service.product.entity.Product
import kr.hhplus.be.server.service.product.entity.ProductSaleSummary
import kr.hhplus.be.server.service.product.usecase.FindAllPopularProductsUsecase
import kr.hhplus.be.server.service.product.usecase.FindAllProductsUsecase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product API", description = "상품 API")
class ProductController (
    val findAllProductsUsecase: FindAllProductsUsecase,
    val findAllPopularProductsUsecase: FindAllPopularProductsUsecase
    ) : ProductApiSepc {

    @GetMapping("")
    override fun getProducts(): ResponseEntity<List<GetProductsResponse>> {
        val products: List<Product> = findAllProductsUsecase.findAllProducts();

        val productsResposne: List<GetProductsResponse> = products.map { product ->
            val productId: Long = product.requiresId()

            GetProductsResponse(
                id = productId,
                name = product.name,
                price = product.price,
                stock = product.stock,
            )
        }

        return ResponseEntity.ok(productsResposne)
    }

    @GetMapping("/popular")
    override fun getPopularProducts(): ResponseEntity<List<GetProductsPopularResponse>> {
        val popularProducts: List<ProductSaleSummary> = findAllPopularProductsUsecase.findAllPopularProducts()

        val popularResponse: List<GetProductsPopularResponse> = popularProducts.map { it ->
            val productId: Long = it.product.requiresId()

            GetProductsPopularResponse(
                id = productId,
                name = it.product.name,
                price = it.product.price,
                stock = it.product.stock,
                rank = it.rank,
                sold = it.soldCount,
            )
        }

        return  ResponseEntity.ok(popularResponse)
    }
}