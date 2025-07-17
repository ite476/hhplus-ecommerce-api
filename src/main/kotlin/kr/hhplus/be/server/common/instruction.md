# 🗂️ common 개발 지침

## 네이밍 컨벤션

- **Configuration 클래스**: `{도메인}Config.kt`
- **Properties 클래스**: `{도메인}Properties.kt`
- **Bean 메서드**: camelCase, 역할 중심

```kotlin
// ✅ GOOD
@Configuration
class DatabaseConfig(
    private val databaseProperties: DatabaseProperties
) {
    @Bean
    fun dataSource(): HikariDataSource { ... }
}

// ❌ BAD
@Configuration 
class DbConfiguration { ... }  // 축약어 사용
class DatabaseConfigurationClass { ... }  // 불필요한 접미사
```

## 개발 원칙

### ✅ DO - 생성자 주입 방식

```kotlin
// ✅ GOOD
@Configuration
class DatabaseConfig(
    private val databaseProperties: DatabaseProperties
)

// ❌ BAD
@Configuration
class DatabaseConfig {
    @Autowired
    lateinit var databaseProperties: DatabaseProperties
}
```

### ✅ DO - ConfigurationProperties 활용

```kotlin
// ✅ GOOD
@ConfigurationProperties("app.database")
data class DatabaseProperties(
    val url: String,
    val username: String
)

// ❌ BAD
class DatabaseConfig {
    @Value("\${app.database.url}")
    lateinit var url: String  // Hard-coded property
}
```

### ❌ DON'T
- `@Autowired` 필드 주입 사용 금지
- Configuration에 비즈니스 로직 포함 금지
- Hard-coded 값 사용 금지
- 순환 의존성 생성 금지
