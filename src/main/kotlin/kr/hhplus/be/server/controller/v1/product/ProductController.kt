package kr.hhplus.be.server.controller.v1.product

import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.controller.v1.product.response.GetProductsPopularResponse
import kr.hhplus.be.server.controller.v1.product.response.GetProductsResponse
import kr.hhplus.be.server.service.product.service.ProductService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product API", description = "상품 API")
class ProductController (
    val productService: ProductService
    ) : ProductApiSepc {

    @GetMapping("")
    override fun getProducts(): ResponseEntity<List<GetProductsResponse>> {
        val products = productService.readProducts();

        val productsResposne = products.map {
            GetProductsResponse(
                it.id,
                it.name,
                it.price,
                it.stock,
            )
        }

        return ResponseEntity.ok(productsResposne)
    }

    @GetMapping("/popular")
    override fun getPopularProducts(): ResponseEntity<List<GetProductsPopularResponse>> {
        val popularProducts = productService.readPopularProducts()

        val popularResponse = popularProducts.map {
            GetProductsPopularResponse(
                it.product.id,
                it.product.name,
                it.product.price,
                it.product.stock,
                it.rank,
                it.soldCount,
            )
        }

        return  ResponseEntity.ok(popularResponse)
    }
}