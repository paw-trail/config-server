# config-server

함께하개(paw-trail)의 **설정 서버**입니다. `paw-trail/config` 저장소를 읽어 각 서비스에 설정을 내려줍니다.

이름이 비슷한 저장소가 하나 더 있으므로 혼동하지 않도록 주의합니다.

```
paw-trail/config          YAML 파일 모음. 실행되지 않습니다
paw-trail/config-server   이 저장소. 위 저장소를 읽어 서비스에 내려주는 스프링 애플리케이션
```

**설정을 바꾸는 일에는 이 서버가 관여하지 않습니다.** 요청을 받을 때마다 저장소를 다시 읽으므로, 값을 고치는 것은 `config` 저장소에 커밋하는 것으로 끝나며 이 서버를 다시 띄울 필요가 없습니다.

---

## 1. 이 서비스가 하는 일

### 1-1. 서비스가 설정을 받아 가는 경로

각 서비스는 기동할 때 자기 이름과 프로파일을 알리고 설정을 받아 갑니다. 유레카를 거치지 않고 이 서버의 주소로 바로 찾아옵니다(Config First).

```
auth-service 기동
   │
   ├─→ "나는 auth-service, 프로파일은 dev" 라고 요청
   │
   ├─→ config-server 가 config 저장소에서 아래 4개를 찾아 합침
   │      application.yml  <  auth-service.yml  <  application-dev.yml  <  auth-service-dev.yml
   │      (오른쪽이 이깁니다. 계층 번호로는 1 < 2 < 3 < 4 입니다)
   │
   └─→ 합쳐진 설정 한 벌을 응답
```

계층 구조와 값 배치 규칙은 `paw-trail/config` 의 README에 있습니다.

### 1-2. 자기 설정은 여기에 있습니다

이 서비스는 자기 설정을 `config` 저장소에서 받지 못합니다. 저장소 주소를 알아야 저장소를 읽을 수 있기 때문입니다. 따라서 **`config` 저장소에 `config-server.yml` 은 존재하지 않으며**, 이 서비스의 설정은 `src/main/resources/application.yml` 과 환경변수에 있습니다.

바꿀 때 무엇이 필요한지는 세 가지로 갈립니다.

| 바꾸는 것 | 필요한 작업 |
|---|---|
| `config` 저장소의 값 | 커밋만 합니다. 이 서버는 다시 띄우지 않아도 됩니다 |
| 이 서비스의 주소 설정 | 컨테이너 재시작. 환경변수로 빼 두었으므로 이미지는 그대로입니다 |
| 코드와 의존성 | 이미지 재빌드와 재배포 |

`application.yml` 을 최대한 비워 두고 환경마다 다른 값을 환경변수로 뺀 것은 세 번째로 갈 일을 만들지 않기 위해서입니다.

---

## 2. 로컬 실행

### 2-1. 준비

`config` 저장소가 공개되어 있으므로 **인증 설정이 필요 없습니다.** 환경변수도 기본값이 있어 그대로 실행하면 됩니다.

```powershell
git clone https://github.com/paw-trail/config-server.git
cd config-server
.\gradlew bootRun
```

프로파일을 지정하지 않으면 `local` 로 동작하며 Loki 전송이 꺼집니다.

### 2-2. 환경변수

| 이름 | 기본값 | 언제 지정하는가 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | (없음, `local` 로 동작) | 컨테이너에서 `dev` 또는 `prod` |
| `EUREKA_HOST` | `localhost` | 컨테이너에서 `eureka-server` 또는 노드 주소 |
| `LOKI_HOST` | `localhost` | 컨테이너에서 `loki` 또는 노드 주소 |

기본값을 붙인 것은 의도입니다. 로컬에서는 언제나 `localhost` 이므로 기본값이 정답이고, 없으면 개발자마다 실행 구성에 환경변수를 넣어야 합니다. 다만 **비밀 값에는 기본값을 붙이지 않습니다.** 이 서비스에는 비밀 값이 없습니다.

### 2-3. 컨테이너로 띄우기

`infra` 저장소의 Compose에 `platform` 프로파일이 있습니다. 도메인 서비스를 개발할 때는 **플랫폼을 컨테이너로 두고 작업 중인 서비스만 개발 도구에서 실행하는 조합**이 편합니다.

```powershell
cd ..\infra
docker compose --profile infra --profile platform up -d
docker compose ps
```

`STATUS` 가 `(healthy)` 로 바뀌면 준비된 것입니다. 이 서비스가 가장 먼저 뜨고 나머지 둘이 그것을 기다립니다.

이미지는 ghcr에서 받아 오며, 코드를 고쳤다면 먼저 다시 만들어 올려야 합니다. 그 절차는 `infra` 저장소 README에 있습니다.

```powershell
.\gradlew clean build
docker build -t ghcr.io/paw-trail/config-server:latest .
docker push ghcr.io/paw-trail/config-server:latest
```

**다만 대부분은 이미지를 다시 만들 필요가 없습니다.** 이 서비스가 내려주는 값은 `config` 저장소에 있으므로 그쪽을 고치면 됩니다. 이미지를 다시 만들어야 하는 것은 이 저장소의 코드와 의존성이 바뀐 경우뿐입니다.

**이 서비스만 주소를 환경변수로 직접 받아야 합니다.** 나머지 둘은 프로파일에 따라 이 서비스가 주소를 내려주지만, 이 서비스는 자기 설정을 그곳에서 받지 않기 때문입니다. 저장소 주소를 알아야 저장소를 읽을 수 있어 그렇게 두었습니다.

```yaml
environment:
  SPRING_PROFILES_ACTIVE: dev
  EUREKA_HOST: eureka-server
  LOKI_HOST: loki
```

빠뜨리면 `localhost` 로 남는데 컨테이너 안에서 그것은 자기 자신이라 **유레카 등록이 영영 실패합니다.** 다만 등록 실패가 기동을 막지는 않아 30초마다 재시도만 반복되므로, **유레카 화면에 이 서비스가 안 보이는 것으로 알아차리게 됩니다.**

### 2-4. 기동 순서

Config First 방식이라 기동 순서가 선형입니다.

```
config-server  →  eureka-server  →  gateway-server  →  도메인 서비스 14개
```

이 서비스는 아무도 호출하지 않아도 되고 유레카에는 등록만 하므로 **가장 먼저 떠도 되고, 유레카가 늦게 떠도 죽지 않습니다.** 등록만 조용히 재시도합니다.

---

## 3. 제대로 도는지 확인하기

### 3-1. 합쳐진 설정 보기

```powershell
curl.exe http://localhost:8888/auth-service/dev
```

브라우저로 같은 주소를 열어도 됩니다. PowerShell 의 `curl` 은 `Invoke-WebRequest` 의 별칭이라 응답이 객체로 감싸지므로, 원문을 보려면 `curl.exe` 라고 확장자까지 적습니다.

**`.yml` · `.properties` · `.json` 주소는 쓸 수 없습니다.** 설정 서버가 그 주소에서 서비스명과 프로파일을 하이픈으로 가르는데, 우리 서비스명은 모두 `auth-service` 처럼 하이픈을 포함하고 있어 400 이 납니다. 서비스가 설정을 받아 가는 경로도 위 슬래시 주소이므로 실사용에는 영향이 없습니다.

### 3-2. 확인할 것

- 계층이 모두 응답에 들어 있는지. `propertySources` 배열은 **앞이 우선순위가 높은 쪽**이며, 4계층이 없으면 `application-dev.yml`, `auth-service.yml`, `application.yml` 순으로 세 개가 나옵니다
- **`${SERVICE_DB_PASSWORD}` 같은 플레이스홀더가 문자열 그대로 내려오는지.** 이 서버는 비밀 값을 알지 못하며, 치환은 설정을 받아 간 서비스가 자기 환경변수로 수행합니다
- `${app.datasource.host}` 참조가 담긴 `spring.datasource.url` 이 그대로 내려오는지

### 3-3. 상태 확인

```powershell
curl http://localhost:8888/actuator/health
```

`refresh` 엔드포인트는 열어 두지 않았습니다. 요청마다 저장소를 다시 읽으므로 이 서비스가 갱신할 자기 설정이 없습니다.

---

## 4. 공통 모듈을 의존하지 않습니다

플랫폼 3종(`config-server`, `eureka-server`, `gateway-server`)은 공통 모듈(`com.pawtrail.common`)을 사용하지 않습니다. 공통 모듈은 도메인 서비스가 공유하는 것들을 담고 있고, 플랫폼은 성격이 다릅니다.

**특히 이 서비스에는 넣으면 안 되는 것이 있습니다.** 공통 모듈의 `TraceIdResponseAdvice` 는 `ResponseBodyAdvice` 이므로 응답을 감쌉니다. 설정 서버가 내려주는 응답까지 감싸이면 **설정을 받아 가는 서비스가 그것을 읽지 못합니다.** 오류가 아니라 파싱 실패로 나타나므로 원인을 찾기 어렵습니다.

나머지도 쓰이지 않습니다. `BaseEntity` 와 감사 컬럼은 데이터베이스가 있어야 하고, 에러 코드와 예외 처리기는 도메인 API가 있어야 하며, 헤더 인증 필터는 게이트웨이 뒤에 있는 서비스를 위한 것이고, Outbox와 Inbox는 데이터베이스와 Kafka가 필요합니다.

공통 모듈에서 유일하게 필요했던 Loki 전송 설정은 `logback-spring.xml` 에 직접 적었습니다.

---

## 5. 트러블슈팅

### 기동은 됐는데 웹 서버가 뜨지 않습니다

로그에 `Tomcat started on port 8888` 이 없다면 웹 스타터가 클래스패스에 없는 것입니다. `spring-cloud-config-server` 가 전이로 끌어오지만, 확인하려면 다음을 봅니다.

```powershell
.\gradlew dependencies --configuration runtimeClasspath | findstr webmvc
```

### 설정을 요청했는데 값이 비어 있습니다

파일명이 그 서비스의 `spring.application.name` 과 정확히 같은지 확인합니다. 다르면 **오류 없이 상위 계층 값만 내려갑니다.**

`config` 저장소에 커밋했는지도 확인합니다. 이 서버는 작업 디렉터리가 아니라 저장소를 읽으므로 커밋하지 않은 변경은 보이지 않습니다.

### 저장소를 못 읽습니다

`clone-on-start: true` 로 두었으므로 저장소 주소가 틀리면 **기동할 때 바로 드러납니다.** 이 값이 없으면 첫 요청이 올 때까지 문제가 감춰집니다.

### 서비스에 반영되지 않습니다

이 서버가 내려주는 값과 서비스가 실제로 쓰는 값은 다를 수 있습니다. 먼저 3-1로 이 서버의 응답을 확인하고, 값이 맞다면 서비스 쪽에서 `POST /actuator/refresh` 를 호출하거나 재기동합니다.

서비스 저장소의 `application.yml` 에 같은 키가 남아 있으면 그쪽이 이길 수 있습니다. **config 저장소로 옮긴 값은 서비스 저장소에서 지웁니다.**

---

## 6. 디렉터리 구조

```
config-server/
├── src/main/java/com/pawtrail/configserver/
│   └── ConfigServerApplication.java     @EnableConfigServer
├── src/main/resources/
│   ├── application.yml                  저장소 주소, 포트, 유레카
│   └── logback-spring.xml               콘솔과 Loki appender
├── src/test/java/com/pawtrail/configserver/
│   └── ConfigServerApplicationTests.java
├── build.gradle
├── gradle.properties
├── settings.gradle
├── Dockerfile
├── Jenkinsfile
├── .gitattributes
├── .editorconfig
├── .gitignore
├── .coderabbit.yaml
└── .github/
    ├── ISSUE_TEMPLATE/issue_template.md
    └── pull_request_template.md
```
