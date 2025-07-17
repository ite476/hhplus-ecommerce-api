# 🎯 controller 개발 지침

## 네이밍 컨벤션

- **Controller 클래스**: `{도메인}Controller.kt`
- **메서드명**: CRUD 중심 (`createUser`, `getUser`)
- **DTO 클래스**: `{메서드명}Request/Response.kt`

```kotlin
// ✅ GOOD
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {
    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse>
}

// ❌ BAD
@RestController
class User_Controller {  // 스네이크 케이스
    @Autowired
    lateinit var userService: UserService  // 필드 주입
    
    @PostMapping("/api/v1/user")  // 단수형 URI
    fun addUser(request: UserDto): UserDto  // 검증 없음, Entity 반환
}
```

## URI 설계 원칙

### ✅ DO - RESTful URI

```kotlin
// ✅ GOOD - 개선된 방식
@RequestMapping("/api/v1/user")
@GetMapping("/{userId}")          // 단일 조회
@GetMapping("/list")              // 목록 조회  
@PostMapping                      // 생성

// 기존 방식도 함께 언급
@RequestMapping("/api/v1/users")  // 복수형도 일관성 있게 사용한다면 OK
@GetMapping("/{userId}")          // 파라미터는 명확하게
```

## HTTP 상태코드

### ✅ DO - 의미적 상태코드 사용

```kotlin
// ✅ GOOD
@PostMapping
fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
    val user = userService.createUser(request.toCommand())
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(UserResponse.from(user))
}

@GetMapping("/{userId}")
fun getUser(@PathVariable userId: Long): ResponseEntity<UserResponse> {
    val user = userService.getUser(userId) ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(UserResponse.from(user))
}

// ❌ BAD
@PostMapping
fun createUser(request: CreateUserRequest): UserResponse {
    return userService.createUser(request)  // 항상 200 OK
}
```

## DTO 설계

### ✅ DO - 검증과 변환 분리

```kotlin
// ✅ GOOD
data class CreateUserRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(min = 2, max = 50)
    val name: String,
    
    @field:Email(message = "올바른 이메일 형식이어야 합니다")
    val email: String
) {
    fun toCommand(): CreateUserCommand {
        return CreateUserCommand(
            name = name.trim(),
            email = email.lowercase()
        )
    }
}

// ❌ BAD
data class UserDto(  // Request와 Response 구분 없음
    val name: String?,  // 검증 없음
    val email: String
)
```

## 개발 원칙

### ✅ DO
- 생성자 주입 방식 사용
- `@Valid` + Bean Validation 활용
- DTO ↔ Command/Entity 변환 로직 포함
- ResponseEntity로 상태코드 명시

### ❌ DON'T
- Controller에 비즈니스 로직 포함 금지
- Entity 객체 직접 반환 금지
- `@Autowired` 필드 주입 금지
- 검증 없는 요청 처리 금지
