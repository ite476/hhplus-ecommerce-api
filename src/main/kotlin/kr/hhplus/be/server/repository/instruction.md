# 🗄️ repository 개발 지침

## 네이밍 컨벤션

- **인터페이스명**: `{도메인}Repository.kt`
- **구현체**: `{도메인}JpaRepository.kt`
- **메서드명**: Spring Data JPA 규칙 준수

```kotlin
// ✅ GOOD
interface UserRepository {
    fun save(user: User): User
    fun findById(userId: Long): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}

// ❌ BAD
interface UserRepo {  // 축약어 사용
    fun create(user: User): User  // CRUD 용어
    fun findUserById(id: Long): User?  // 불필요한 접두사
    fun getUserByEmail(email: String): User?  // get 접두사
}
```

## 커스텀 쿼리 작성

### ✅ DO - @Query 활용

```kotlin
// ✅ GOOD
interface UserRepository : JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    fun findActiveUserByEmail(@Param("email") email: String): User?
    
    @Query("SELECT u FROM User u WHERE u.createdAt >= :startDate")
    fun findUsersCreatedAfter(@Param("startDate") startDate: LocalDateTime): List<User>
}

// ❌ BAD
interface UserRepository : JpaRepository<User, Long> {
    @Query(value = "SELECT * FROM users WHERE email = ?", nativeQuery = true)
    fun findByEmailNative(email: String): User?  // 불필요한 Native Query
    
    fun findByEmailAndIsActiveTrue(email: String): User?  // 너무 긴 메서드명
}
```

## 트랜잭션 처리

### ✅ DO - Repository는 트랜잭션 경계 없음

```kotlin
// ✅ GOOD - Repository
interface UserRepository {
    fun save(user: User): User  // 트랜잭션 어노테이션 없음
    fun findById(userId: Long): User?
}

// ✅ GOOD - Service에서 트랜잭션 관리
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository
) {
    @Transactional
    fun createUser(command: CreateUserCommand): User {
        return userRepository.save(User.create(command))
    }
}

// ❌ BAD
interface UserRepository {
    @Transactional  // Repository에서 트랜잭션 관리 금지
    fun save(user: User): User
}
```

## 개발 원칙

### ✅ DO
- Spring Data JPA 기본 메서드 활용
- 복잡한 쿼리는 `@Query` 사용
- 도메인별 Repository 인터페이스 분리
- 메서드명으로 쿼리 의도 명확히 표현

### ❌ DON'T
- Repository에 비즈니스 로직 포함 금지
- Repository에서 트랜잭션 관리 금지
- 불필요한 Native Query 사용 금지
- 과도하게 긴 메서드명 사용 금지