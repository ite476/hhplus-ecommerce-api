# 🗄️ Repository 레이어 개발 지침

## 🎯 목적
- **Clean Architecture** 구조에서 Domain과 Infrastructure 분리
- **Domain Entity의 순수성 보장** (Infrastructure 코드로부터 격리)
- **Repository 레이어에서 데이터 정합성 및 Null Safety 보장**
- **일관된 에러 처리 및 변환 로직 제공**

---

## 📋 전체 구조

### **레이어 구조**
```
src/main/kotlin/kr/hhplus/be/server/repository/
├── {domain}/
│   └── {Domain}Adapter.kt        # Domain Port 구현체 (Clean Architecture)
└── jpa/
    ├── entity/{domain}/          # JPA Entity (Infrastructure)
    └── repository/{domain}/      # JPA Repository Interface (Spring Data JPA)
```

### **역할 분담**
| 컴포넌트 | 역할 | 예시 |
|----------|------|------|
| **Domain Port** | Service ↔ Repository 계약 | `UserPort` |
| **JPA Repository** | Spring Data JPA 인터페이스 | `UserJpaRepository` |
| **Repository Adapter** | Port 구현체 + Entity 변환 | `UserAdapter` |
| **JPA Entity** | 데이터베이스 매핑 | `UserEntity` |

---

## 🏗️ JPA Repository 개발 가이드

### **네이밍 컨벤션**

```kotlin
// ✅ GOOD
interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByName(name: String): UserEntity?
    fun findByEmail(email: String): UserEntity?
    fun existsByEmail(email: String): Boolean
}

// ❌ BAD
interface UserRepo {  // 축약어 사용
    fun getUserById(id: Long): UserEntity?  // get 접두사
    fun findUserByEmail(email: String): UserEntity?  // 불필요한 접두사
}
```

### **커스텀 쿼리 작성**

#### **✅ DO - @Query 활용**
```kotlin
interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email AND u.deletedAt IS NULL")
    fun findActiveUserByEmail(@Param("email") email: String): UserEntity?
    
    @Query("SELECT u FROM UserEntity u WHERE u.createdAt >= :startDate")
    fun findUsersCreatedAfter(@Param("startDate") startDate: ZonedDateTime): List<UserEntity>
}
```

#### **❌ DON'T**
```kotlin
// ❌ 불필요한 Native Query
@Query(value = "SELECT * FROM users WHERE email = ?", nativeQuery = true)
fun findByEmailNative(email: String): UserEntity?

// ❌ 너무 긴 메서드명
fun findByEmailAndIsActiveTrue(email: String): UserEntity?
```

---

## 🔧 Repository Adapter 구현 컨벤션

### **기본 클래스 구조**
```kotlin
@Component
class {Domain}Adapter(
    private val {domain}Repository: {Domain}JpaRepository
) : {Domain}Port {
    
    // Port 메서드 구현
}
```

### **1. Null Safety & Data Validation**

#### **✅ Optional 처리 (단일 조회)**
```kotlin
override fun findUserById(userId: Long): User {
    val userEntity = userRepository.findById(userId)
        .unwrapOrThrow { UserNotFoundException() }
    
    return userEntity.toDomain()
}
```

#### **✅ RequireNotNull (필수 필드 검증)**
```kotlin
override fun findAllProducts(): List<Product> {
    return productRepository.findAll().map { entity ->
        Product(
            id = entity.id,
            name = entity.name,
            createdAt = requireNotNull(entity.createdAt) {
                "ProductEntity.createdAt is null (ID=${entity.id}). 데이터 정합성 확인 필요"
            }
        )
    }
}
```

#### **✅ Collection 처리**
```kotlin
override fun findAllUserCoupons(userId: Long): List<UserCoupon> {
    val entities = userCouponRepository.findByUserId(userId)
    
    require(entities.isNotEmpty()) {
        "User $userId has no coupons. 사용자 쿠폰 데이터 확인 필요"
    }
    
    return entities.map { it.toDomain() }
}
```

### **2. 에러 메시지 컨벤션**

#### **✅ 메시지 형식**
```kotlin
"{Entity}.{Field} is null (ID=${entity.id}). {한글 설명}"

// 예시들
"UserEntity.name is null (ID=${entity.id}). 사용자명 데이터 정합성 확인 필요"
"CouponEntity.expiredAt is null (ID=${entity.id}). 쿠폰 만료일 설정 확인 필요"
"OrderEntity.orderedAt is null (ID=${entity.id}). 주문일시 데이터 무결성 확인 필요"
```

#### **✅ 비즈니스 로직 검증**
```kotlin
require(coupon.issuedQuantity <= coupon.totalQuantity) {
    "Coupon ${coupon.id}: 발급량(${coupon.issuedQuantity}) > 총량(${coupon.totalQuantity}). 쿠폰 발급 로직 검토 필요"
}
```

### **3. Entity 변환 로직**

#### **✅ Extension Function 방식**
```kotlin
// JPA Entity → Domain Entity
private fun UserEntity.toDomain(): User = User(
    id = this.id,
    name = this.name,
    point = this.point
)

// Domain Entity → JPA Entity (저장용)
private fun User.toEntity(): UserEntity = UserEntity(
    name = this.name,
    point = this.point
).apply {
    // ID는 JPA에서 자동 생성
    if (this@toEntity.id != 0L) {
        // 기존 Entity 업데이트의 경우 ID 설정 로직
    }
}
```

#### **✅ 복잡한 변환 로직**
```kotlin
private fun OrderEntity.toDomain(): Order {
    val orderItems = this.orderItems.map { item ->
        OrderItem(
            productId = item.product.id,
            unitPrice = requireNotNull(item.unitPrice) {
                "OrderItem.unitPrice is null (OrderItem.ID=${item.id}). 주문 상품 가격 정합성 확인 필요"
            },
            quantity = item.quantity.takeIf { it > 0 } 
                ?: error("OrderItem quantity must be positive (OrderItem.ID=${item.id})")
        )
    }
    
    return Order(
        id = this.id,
        userId = this.user.id,
        orderItems = orderItems,
        orderedAt = requireNotNull(this.orderedAt) {
            "OrderEntity.orderedAt is null (ID=${this.id}). 주문일시 데이터 무결성 확인 필요"
        }
    )
}
```

---

## 🚫 금지 사항

### **❌ Domain Entity 오염**
```kotlin
// BAD: Domain Entity에 JPA 어노테이션 추가
@Entity  // ← 금지!
data class User(val id: Long, val name: String)

// GOOD: Domain Entity는 순수하게 유지
data class User(val id: Long, val name: String)
```

### **❌ Infrastructure 예외 노출**
```kotlin
// BAD: JPA 예외를 그대로 던지기
fun findUser(id: Long): User {
    return userRepository.findById(id).get() // ← NoSuchElementException 발생 가능
}

// GOOD: Domain 예외로 변환
fun findUser(id: Long): User {
    return userRepository.findById(id)
        .unwrapOrThrow { UserNotFoundException() }
}
```

### **❌ Repository에 비즈니스 로직 포함**
```kotlin
// BAD: Repository에서 비즈니스 로직 처리
class UserAdapter {
    fun createUser(userData: UserData): User {
        if (userData.age < 18) {  // ← 비즈니스 로직
            throw IllegalArgumentException("미성년자는 가입할 수 없습니다")
        }
        return userRepository.save(userData.toEntity())
    }
}

// GOOD: Service에서 비즈니스 로직 처리
class UserService {
    fun createUser(userData: UserData): User {
        require(userData.age >= 18) { "미성년자는 가입할 수 없습니다" }
        return userPort.save(userData.toDomain())
    }
}
```

---

## 🛠 헬퍼 함수 활용

### **unwrapOrThrow (Optional 처리)**
```kotlin
// src/main/kotlin/kr/hhplus/be/server/util/Unwrapper.kt
fun <T> Optional<T?>.unwrapOrThrow(exception: () -> RuntimeException): T {
    return this.orElse(null) ?: throw exception()
}
```

### **추가 헬퍼 함수 제안**
```kotlin
// 향후 추가 고려사항
inline fun <T> T?.requireNotNullWithContext(
    entityName: String,
    fieldName: String,
    entityId: Long,
    lazyMessage: () -> String = { "데이터 정합성 확인 필요" }
): T {
    return this ?: error("$entityName.$fieldName is null (ID=$entityId). ${lazyMessage()}")
}

// 사용 예시
val createdAt = entity.createdAt.requireNotNullWithContext(
    "ProductEntity", "createdAt", entity.id
) { "상품 생성일시 설정 확인 필요" }
```

---

## 🔄 트랜잭션 처리

### **✅ DO - Service에서 트랜잭션 관리**
```kotlin
// ✅ GOOD - Repository Adapter
@Component
class UserAdapter : UserPort {
    fun save(user: User): User  // 트랜잭션 어노테이션 없음
    fun findById(userId: Long): User?
}

// ✅ GOOD - Service에서 트랜잭션 관리
@Service
@Transactional(readOnly = true)
class UserService(
    private val userPort: UserPort
) {
    @Transactional
    fun createUser(command: CreateUserCommand): User {
        return userPort.save(User.create(command))
    }
}
```

### **❌ DON'T - Repository에서 트랜잭션 관리**
```kotlin
// ❌ BAD
@Component
class UserAdapter : UserPort {
    @Transactional  // Repository에서 트랜잭션 관리 금지
    fun save(user: User): User
}
```

---

## ✅ 체크리스트

### **JPA Repository 구현 체크리스트**
- [ ] Spring Data JPA 메서드 네이밍 규칙 준수
- [ ] 복잡한 쿼리에 @Query 적절히 사용
- [ ] Native Query 남용 금지
- [ ] 트랜잭션 어노테이션 사용 금지

### **Repository Adapter 구현 체크리스트**
- [ ] Domain Port 인터페이스 메서드 전체 구현
- [ ] 모든 필수 필드에 `requireNotNull` 적용
- [ ] Optional 반환 메서드에 `unwrapOrThrow` 적용
- [ ] 의미있는 에러 메시지 작성
- [ ] Entity 변환 로직 완전성
- [ ] Domain Entity 순수성 유지
- [ ] JPA Repository 의존성 주입 설정
- [ ] 필요한 커스텀 예외 클래스 존재 여부

---

## 🎯 예외 상황 처리

### **데이터 부재 시**
```kotlin
// 단일 조회: 커스텀 예외
.unwrapOrThrow { UserNotFoundException() }

// 다중 조회: 빈 리스트 반환 (비즈니스 로직에 따라)
return entities.map { it.toDomain() } // 빈 리스트 가능

// 필수 데이터: require 사용
require(entities.isNotEmpty()) { "Required data not found" }
```

### **데이터 정합성 오류 시**
```kotlin
require(order.totalAmount >= 0) {
    "Order ${order.id}: 총액이 음수입니다. 주문 데이터 검증 필요"
}
```

---

## 📚 개발 원칙 요약

### **✅ DO**
- Clean Architecture 구조 준수 (Domain ↔ Port ↔ Adapter ↔ JPA)
- Spring Data JPA 기본 메서드 활용
- Extension Function으로 Entity 변환
- Repository 레이어에서 데이터 정합성 검증
- 도메인별 Repository 분리
- 의미있는 메서드명과 에러 메시지

### **❌ DON'T**
- Domain Entity에 Infrastructure 코드 포함
- Repository에 비즈니스 로직 포함
- Repository에서 트랜잭션 관리
- Infrastructure 예외를 Service로 전파
- 불필요한 Native Query 사용
- 과도하게 긴 메서드명 사용

**이 가이드를 따라 일관성 있고 안전한 Repository 레이어를 구현해주세요.** 🎯