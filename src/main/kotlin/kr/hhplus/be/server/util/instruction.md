# 🛠️ util 개발 지침

## 네이밍 컨벤션

- **유틸리티 클래스**: `{기능}Utils.kt` (object 사용)
- **확장 함수 파일**: `{타입}Extensions.kt`

```kotlin
// ✅ GOOD
object DateUtils {
    fun formatToIso(dateTime: LocalDateTime): String {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

// String 확장 함수
fun String.toSnakeCase(): String {
    return this.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}

// ❌ BAD
class DateUtility {  // class 사용, 이상한 접미사
    fun formatDate(dateTime: LocalDateTime?): String? {
        return dateTime?.toString()  // null 안전하지 않음
    }
}

fun stringToSnakeCase(str: String): String {  // 확장 함수 사용하지 않음
    return str.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}
```

## 순수 함수 설계

### ✅ DO - null 안전성과 불변성

```kotlin
// ✅ GOOD
object ValidationUtils {
    fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrBlank() && 
               email.contains("@") && 
               email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
    
    fun isValidPhoneNumber(phone: String?): Boolean {
        return !phone.isNullOrBlank() && 
               phone.matches(Regex("^01[016789]-\\d{3,4}-\\d{4}$"))
    }
}

// ❌ BAD
object ValidationUtils {
    private var lastValidatedEmail: String? = null  // 상태 보유
    
    fun validateEmail(email: String): Boolean {
        lastValidatedEmail = email  // 사이드 이펙트
        if (email.isEmpty()) throw Exception("이메일이 비어있습니다")  // 예외 발생
        return email.contains("@")  // 간단한 검증
    }
}
```

## 확장 함수 활용

### ✅ DO - 기존 타입 확장

```kotlin
// ✅ GOOD
fun List<String>.joinWithCommaAndAnd(): String {
    return when (size) {
        0 -> ""
        1 -> first()
        2 -> "${first()} 그리고 ${last()}"
        else -> "${dropLast(1).joinToString(", ")} 그리고 ${last()}"
    }
}

fun LocalDateTime.toKoreanFormat(): String {
    return this.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분"))
}

// ❌ BAD
object ListUtils {
    fun joinStringListWithCommaAndAnd(list: List<String>): String {  // 확장 함수 사용하지 않음
        // 동일한 로직
    }
}
```

## 개발 원칙

### ✅ DO
- object 키워드로 싱글톤 유틸리티 클래스 생성
- 확장 함수로 기존 타입 확장
- null 안전성 고려한 안전한 함수 작성
- 순수 함수로 사이드 이펙트 없이 작성

### ❌ DON'T
- 유틸리티에 비즈니스 로직 포함 금지
- 상태를 가지는 유틸리티 클래스 생성 금지
- 예외를 던지는 유틸리티 함수 지양
- 도메인 종속적인 유틸리티 생성 금지

