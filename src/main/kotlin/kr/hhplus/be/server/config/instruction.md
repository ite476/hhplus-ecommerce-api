# config/instruction.md

## ✅ 작성 규칙

1. **구체적인 목적 단위로 서브 디렉토리 분리**
    - ex: `config/jpa/JpaConfig.kt`, `config/swagger/SwaggerConfig.kt`

2. **공통 설정은 모두 `@Configuration` 클래스에 작성**
    - 빈 주입에 의존하지 않는 단순 상수/Enum 설정도 여기 포함

3. **불필요한 `@ComponentScan`, `@Enable*` 남발 금지**
    - 기본 AutoConfiguration 흐름을 우선 따르고, 꼭 필요한 경우만 사용

4. **Configuration 클래스 네이밍은 반드시 `~Config`로 통일**
    - `JpaConfig`, `SwaggerConfig`, `WebMvcConfig` 등

5. **테스트 환경 설정은 `test/resources` 또는 Profile 분기 처리**
    - `@Profile("test")` 또는 `application-test.yml` 활용

---

## 🧪 테스트 규칙

- Configuration 단위 테스트는 최소화하고,
- 실제 적용 여부는 통합 테스트(`@SpringBootTest`)로 검증

---

## 📎 커밋 메시지 템플릿

```markdown
chore(config): JPA 설정 분리 및 JpaConfig 도입
chore(config): Swagger 설정 개선 (v3 기준)
```
