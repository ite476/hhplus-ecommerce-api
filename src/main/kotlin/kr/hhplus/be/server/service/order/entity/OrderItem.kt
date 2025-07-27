package kr.hhplus.be.server.service.order.entity

class OrderItem (
    val id: Long? = null,
    val productId: Long,
    val productName: String,
    val unitPrice: Long,
    val quantity: Int
) {
  val totalPrice: Long
      get()  = unitPrice * quantity
}