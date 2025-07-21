# 🎯 controller

## 폴더 구조

```text
controller/
├── api/v1/            # API v1 컨트롤러
├── dto/               # Request/Response DTO
│   ├── request/       # 요청 DTO
│   └── response/      # 응답 DTO
└── advice/            # 컨트롤러 공통 처리
    └── GlobalExceptionHandler.kt
```

## 역할

**REST API 엔드포인트 제공**: HTTP 요청/응답 처리 및 검증

## 🧬 구성 및 설계

```mermaid
---
    title: API Controller 구성
---
classDiagram    
    %% 🔧 Core Services
    note for SampleApiSpec "
    책임 범위:
    - API 행위 목록의 나열, 정의
    - API 문서화 메타데이터
    "
    class SampleApiSpec {
        <<🧩 Controller Interface>>
        +ReadSamples(query: ReadSamplesRequest): ReadSamplesResponse
        +CreateSample(body: CreateSampleRequest): SampleResponse
    }
    
    note for SampleController "
    책임 범위:
    - API의 실제 구현
    - 엔드포인트 Routing
    "
    class SampleController {
        <<🔧 Controller Implementation>>
        +ReadSamples(query: ReadSamplesRequest): ReadSamplesResponse
        +CreateSample(body: CreateSampleRequest): ReadSamplesResponse
    }    

    %% ❓📦 Controller DTO
    class ReadSamplesRequest {
        <<❓ API Request>>
        +string keyword
    }

    class ReadSamplesResponse {
        <<📦 API Response>>
        +List~Sample~ samples        
    }
    
    class CreateSampleRequest {
        <<❓ API Request>>
        +string entityName
    }
    
    class SampleResponse {
        <<📦 API Response>>
        +long id
        +string entityName
    }

    %% 📊 Relations

    SampleApiSpec --> ReadSamplesRequest: "consumes"
    SampleApiSpec --> ReadSamplesResponse: "produces"    
    SampleApiSpec --> CreateSampleRequest : "consumes"
    SampleApiSpec --> SampleResponse : "produces"
    
    SampleController --> SampleApiSpec : "implements"
    
    ReadSamplesResponse --> SampleResponse: "Contains"
```

---

> 상세한 개발 지침은 [instruction.md](./instruction.md) 참조
