# ⚙️ core 개발 지침

## 네이밍 컨벤션

- **Service 클래스**: `{도메인}Service.kt`
- **Entity 클래스**: `{도메인}.kt`
- **Command 객체**: `{도메인}{동작}Command.kt`
- **Port 인터페이스**: `{도메인}Port.kt`

```kotlin
// ✅ GOOD
@Service
class UserService(
    private val userPort: UserPort,
    private val notificationService: NotificationService
) {
    @Transactional
    fun createUser(command: CreateUserCommand): User {
        val user = User.create(command.name, command.email)
        return userPort.save(user)
    }
}

// ❌ BAD
@Service
class UserManager {  // Manager 접미사
    @Autowired
    lateinit var userRepository: UserRepository  // 필드 주입
    
    fun addUser(name: String, email: String): User {  // 원시 타입 파라미터
        return userRepository.save(User(name, email))
    }
}
```

## 트랜잭션 관리

### ✅ DO - 클래스/메서드 레벨 트랜잭션

```kotlin
// ✅ GOOD
@Service
@Transactional(readOnly = true)  // 기본값: 읽기 전용
class UserService {
    
    @Transactional  // 쓰기 작업: 읽기-쓰기 트랜잭션
    fun createUser(command: CreateUserCommand): User {
        // 여러 Repository 호출이 하나의 트랜잭션으로 처리
    }
    
    fun getUser(userId: Long): User {  // 읽기 전용 트랜잭션
        return userPort.findById(userId) ?: throw UserNotFoundException()
    }
}

// ❌ BAD
@Service
class UserService {
    fun createUser(command: CreateUserCommand): User {
        // 트랜잭션 경계 없음
    }
}
```

## Command 객체 설계

### ✅ DO - 불변 객체와 검증

```kotlin
// ✅ GOOD
data class CreateUserCommand(
    val name: String,
    val email: String
) {
    init {
        require(name.isNotBlank()) { "이름은 필수입니다" }
        require(email.isNotBlank()) { "이메일은 필수입니다" }
    }
}

// ❌ BAD
class CreateUserCommand {
    var name: String? = null  // 가변, nullable
    var email: String? = null
    // 검증 로직 없음
}
```

## Port 인터페이스 설계

### ✅ DO - 도메인 중심 인터페이스

```kotlin
// ✅ GOOD
interface UserPort {
    fun save(user: User): User
    fun findById(userId: Long): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}

// ❌ BAD
interface UserPort {
    fun create(user: User): User
    fun read(id: Long): User?  // CRUD 용어 사용
    fun update(user: User): User
    fun delete(id: Long)
}
```

## 개발 원칙

### ✅ DO
- 생성자 주입 방식 사용
- `@Transactional`로 트랜잭션 경계 명시
- Command 객체로 파라미터 그룹화
- Port 인터페이스로 Repository 추상화

### ❌ DON'T
- Service에서 HTTP 관련 처리 금지
- Entity를 Controller로 직접 반환 금지
- `@Autowired` 필드 주입 금지
- 서비스 간 순환 의존성 금지
