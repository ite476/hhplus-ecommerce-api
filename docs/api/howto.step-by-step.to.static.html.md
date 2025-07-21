# Swagger를 케이크처럼 쉽게 정적 html로 추출하는 법

## 1. application.yml 설정

application.yml에 아래 내용을 추가한다.

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs.yaml
  swagger-ui:
    enabled: true
```

## 2. api-doc json 획득

`{web-host-url}/v3/api-docs.yaml` 에 접근하여 api-doc json 데이터를 획득한다.
본 예제에서는 `/docs/api/` 폴더에 저장하는 것으로 가정한다.

## 3. index.html 추출

>  Windows 기준입니다.
> 
프로젝트 루트 폴더에서 `PowerShell`을 켜고 아래 명령어를 실행한다.
그러면 `/docs/api/html/` 폴더 내에 api-doc `index.html`이 생성된다.

```powershell
docker run --rm -v ${PWD}:/local openapitools/openapi-generator-cli generate `
  -i /local/docs/api/api-docs.v1.json `
  -g html2 `
  -o /local/docs/api/html
```

끝.

---