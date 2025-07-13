# 개요

- 전체 도메인 각 클래스 다이어그램

## 클래스 다이어그램

```mermaid
classDiagram
    class User {
        + String userId
        + OffsetDateTime joinedAt
    }
    
    class PointBalance {
        + String userId # 보유자
        + long amount        
    }
```