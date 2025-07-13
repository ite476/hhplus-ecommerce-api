# controller/instruction.md

## ✅ 작성 규칙

### 🧱 구조
- 도메인 단위로 하위 디렉토리를 구분
- `dto/`는 각 도메인의 `controller/` 하위에 위치
### 📐 클래스 네이밍
- 컨트롤러 클래스는 반드시 `~Controller` 접미사 사용
- DTO 클래스는 요청/응답 목적에 따라 `~Request`, `~Response` 접미사 사용
### 📌 어노테이션 원칙
- `@RestController` + `@RequestMapping("/v1/{domain}")` 사용
- 엔드포인트 메서드에는 `@PostMapping`, `@GetMapping` 등 명시
- 요청 DTO에는 `@Valid` + 제약 조건 명시
### 🔐 인증/인가 처리
- 인증 정보는 `@RequestHeader`, `@AuthenticationPrincipal`, `@RequestAttribute` 등으로 주입
- 인가 로직은 서비스 레이어 또는 별도 필터/어드바이저로 분리 권장
### 🧪 테스트
- 컨트롤러 단위 테스트는 `@WebMvcTest`를 사용
- 통합 테스트에서는 `TestRestTemplate` 또는 `MockMvc` 사용

---

## 💬 커밋 템플릿 예시

```markdown
feat(user-controller): 사용자 등록 API 추가
refactor(product-controller): 상품 목록 조회 응답 DTO 개선
```