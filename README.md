# config-server

**함께하개의 설정 서버입니다.** `paw-trail/config` 저장소를 읽어 서비스들에게 설정을
내려 줍니다.

```
도메인 서비스 14개  ──▶  이 서버  ──▶  config 저장소
                            │                │
                            │                └──▶  GitHub 에서 yml 을 읽음  (paw-trail/config)
                            │                      기동할 때 한 번 clone, 요청마다 다시 읽음
                            │
                            ├──▶  4계층을 겹쳐 하나로 만들어 돌려줌
                            ├──▶  ${환경변수} 는 치환하지 않고 문자열 그대로
                            └──▶  유레카에 등록만 함 (아무도 안 찾음)

        자바 파일 1개 · 포트 8888 · DB · Redis · Kafka 안 씀
```

> ⚠ **`config` 와 `config-server` 는 다른 저장소입니다.**
>
> ```
> paw-trail/config          yml 23개.  설정값이 담긴 저장소
> paw-trail/config-server   이 저장소. 그것을 읽어 뿌리는 스프링 앱
> ```

<br><br>

---

## 0. 이 서비스가 하는 일

**우리 시스템에서 가장 먼저 뜨는 서비스입니다.**

```
config-server  ──▶  eureka-server  ──▶  gateway-server  ──▶  도메인 14개
      ▲
      └── 아무도 안 찾아도 되고 유레카가 늦게 떠도 안 죽음
```

---

**숫자로 보면 이렇습니다.**

| | 값 |
|---|---|
| 자바 파일 | **1개** — `ConfigServerApplication` |
| 포트 | 8888 |
| 의존성 | 5개 (Config Server · Eureka Client · Actuator · Prometheus · Loki) |
| DB · Redis · Kafka | **안 씀** |
| 공통 모듈 | **안 씀** |
| 읽는 저장소 | `https://github.com/paw-trail/config` (`main`) |

---

**코드가 이게 전부입니다.**

```java
@SpringBootApplication
@EnableConfigServer          // *Initializr 가 붙여 주지 않음. 없으면 그냥 빈 웹 앱이 뜸
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**나머지는 전부 `application.yml` 입니다.**

<br><br>

---

### 먼저 알아 두면 좋은 것 3가지

---

**① 설정 서버가 왜 있나**

```
설정 서버가 없으면                       있으면

  서비스 14개가 각자 application.yml       서비스 14개가 기동할 때 여기에 물어봄
  에 포트·DB 주소를 가짐                          │
        │                                       └── 값은 config 저장소 한 곳에
        └── DB 주소가 바뀌면                          바뀌면 그 한 곳만 고침
            14개를 고치고 다시 배포
```

**서비스 쪽 `application.yml` 은 세 줄뿐이고** 나머지를 여기서 받습니다.

---

**② "4계층" 이란**

`config` 저장소에는 파일이 네 부류 있고, **서비스 하나가 기동하면 그중 최대 4개가 겹쳐서
하나가 됩니다.** 이 서버가 그 겹치기를 합니다.

```
application.yml           모든 서비스 공통
place-service.yml         place 만
application-local.yml     local 환경만
place-service-local.yml   place · local 만     ← 있으면
        │
        └──▶  겹쳐서 하나로  →  place-service 에 내려 줌
```

어느 것이 이기는지는 [`config` README 2장](https://github.com/paw-trail/config) 에 있습니다.

---

**③ "유레카에 등록만 한다" 는 뜻**

```
등록     유레카 장부에 "config-server 는 8888 에 있다" 고 적음
         → 대시보드에 보임
조회     남의 주소를 장부에서 찾음
         → 이 서버는 남을 부를 일이 없어 안 함
```

**서비스들은 유레카가 아니라 `spring.config.import` 에 적힌 주소로 직접 찾아옵니다.**
그래서 등록은 동작에 필요해서가 아니라 **대시보드에 보이기 위해서** 합니다.

<br><br>

---

### 이 문서를 읽는 순서

| 지금 하려는 일 | 볼 곳 |
|---|---|
| 띄워서 확인하고 싶다 | [1장](#1-로컬에서-띄우기) |
| 왜 자기 설정을 저장소에서 안 받는지 | [2장](#2-닭-달걀-문제) |
| 설정을 바꾸려는데 무엇이 필요한지 | [3장](#3-무엇을-바꾸면-무엇이-필요한가) |
| `application.yml` 을 고쳐야 한다 | [4장](#4-applicationyml) |
| 이미지를 굽거나 배포해야 한다 | [5장](#5-컨테이너와-배포) |
| 뭔가 안 된다 | [6장](#6-막히기-쉬운-자리) |
| "왜 이렇게 만들었지" | [7장](#7-왜-이렇게-만들었나) |
| 모르는 말이 나온다 | [8장](#8-용어) |

> **설정값 자체는 이 문서에 없습니다.** 어떤 값이 어느 계층에 있는지는
> [`config` README](https://github.com/paw-trail/config) 를 봅니다.

<br><br>

---

## 1. 로컬에서 띄우기

**준비물이 없습니다.** DB 도 환경변수도 필요 없습니다.

```
① ConfigServerApplication 실행          IntelliJ
        │
        ├──▶  GitHub 에서 config 저장소를 clone         clone-on-start: true
        └──▶  유레카에 등록 시도                        없어도 안 죽음
        │
        ▼
② curl :8888/actuator/health           {"status":"UP"}
        │
        ▼
③ curl :8888/place-service/local       실제로 내려가는 값
```

<br><br>

---

### 1-1. 기동 로그에서 볼 것

```
The following 1 profile is active: "local"
Tomcat started on port 8888              *웹 스타터가 전이로 들어온 증거
Started ConfigServerApplication in 6.2 seconds
```

> **`spring-boot-starter-webmvc` 를 따로 선언하지 않았습니다.**
> `spring-cloud-config-server` 가 전이로 끌고 옵니다. **8888 로 HTTP 응답이 오면
> 그것이 확인된 것입니다.**

---

**유레카 연결 거부 스택트레이스가 잔뜩 나오는 것은 정상입니다.**

```
Connection refused: http://localhost:8761/eureka/
```

**유레카가 아직 안 떠 있어서이고 등록만 재시도할 뿐 이 서버는 죽지 않습니다.**
유레카를 띄우면 사라집니다.

<br><br>

---

### 1-2. 헬스 응답 읽는 법

```bash
curl http://localhost:8888/actuator/health
```

```powershell
curl.exe http://localhost:8888/actuator/health
```

| 항목 | 정상 | 뜻 |
|---|---|---|
| `configServer` | **`UP`** | 저장소를 읽었음. `repositories` 에 URL 이 보임 |
| `discoveryClient` | `UP` | 유레카 클라이언트가 떠 있음 |
| `eureka` | `UNKNOWN` 이어도 됨 | 유레카가 아직 없을 때 |
| **`clientConfigServer`** | **`UNKNOWN`** | ✅ **정상입니다** — 아래 참고 |
| 전체 `status` | `UP` | |

---

**`clientConfigServer: UNKNOWN` 은 정상입니다.**

```
spring-cloud-config-server 가 config 클라이언트를 전이로 가져옴
        │
        └── 그런데 이 서버는 자기 설정을 저장소에서 안 받음
              → "no property sources located"
                  스프링이 UNKNOWN 을 DOWN 으로 치지 않아 전체 판정에 영향 없음
```

> 거슬리면 `management.health.config.enabled: false` 로 끌 수 있지만,
> 나중에 *"왜 UNKNOWN 이지"* 로 헷갈릴 여지만 남으므로 **그대로 둡니다.**

---

**`configServer` 의 `repositories` 가 1계층만 잡는 것도 정상입니다.**

헬스 인디케이터가 기본 프로브(`name: app` · `profiles: [default]`)로 조회하는데
**`app` 이라는 서비스 파일이 없어 `application.yml` 만 매칭됩니다.**

<br><br>

---

### 1-3. 값이 실제로 내려가는지 보기

```
http://localhost:8888/{서비스명}/{환경}
```

```bash
curl http://localhost:8888/place-service/local
```

```json
{
  "name": "place-service",
  "profiles": ["local"],
  "label": null,
  "version": "2da1336...",                  ← 어느 커밋을 읽었는지
  "propertySources": [
    { "name": "...config/application-local.yml", "source": { } },
    { "name": "...config/place-service.yml",     "source": { } },
    { "name": "...config/application.yml",       "source": { } }
  ]
}
```

**`propertySources` 배열은 앞이 우선순위가 높습니다.**

---

**⛔ 확장자 주소는 쓸 수 없습니다.**

```
http://localhost:8888/place-service-local.yml       →  400 Bad Request
```

```
설정 서버가 그 주소에서 서비스명과 환경을 하이픈으로 가름
        │
        └── 우리 서비스명이 전부 하이픈을 포함함
              auth-service · gateway-server ...
                    → 경계를 못 찾아 예외를 던짐
```

**슬래시 형태를 씁니다.** 실사용에는 영향이 없습니다 — **config 클라이언트는
`/{name}/{profile}` 로 요청**하므로 서비스가 설정을 받아 가는 경로는 정상입니다.

> ⚠ **프로파일 이름에도 하이픈을 쓰지 않습니다.** 쓰면 슬래시 형태마저 404 가 됩니다.
> 우리 환경 축이 `local` · `dev` · `prod` 라 안전합니다.

<br><br>

---

## 2. 닭-달걀 문제

**이 서버만 자기 설정을 `config` 저장소에서 받지 못합니다.**

```
다른 서비스                          이 서버

application.yml 3줄                 application.yml 전부
  name                                name · profiles
  config.import  ← 저장소 주소          git.uri     *저장소 주소
  profiles.default                     port 8888
        │                              eureka 주소
        │                              loki 주소
        ▼                                    │
  나머지를 config 에서 받음                    │
                                             ▼
                                       ⛔ config 에서 못 받음
                                            저장소 주소를 알아야
                                            저장소를 읽을 수 있음


그래서 config 저장소에 config-server.yml 이 없음
플랫폼 파일은 eureka-server.yml · gateway-server.yml 둘뿐
```

<br><br>

---

### 2-1. 자기 저장소에서 자기 설정을 읽는 기능(self-referential)을 쓰지 않습니다

**스프링에는 설정 서버가 자기 저장소에서 자기 설정을 읽는 기능이 있는데 안 씁니다.**

| 이유 | |
|---|---|
| git url 은 어차피 못 받음 | 그것을 알아야 저장소를 읽음 |
| 부트스트랩 순서가 꼬임 | Boot 4 의 bootstrap 제거 흐름과도 안 맞음 |
| 얻는 것이 적음 | 관측 주소·로깅 패턴이 소량 중복되는 정도 |

**소량 중복은 감수합니다.**

<br><br>

---

## 3. 무엇을 바꾸면 무엇이 필요한가

```
무엇을 바꾸나                        무엇이 필요한가

config 저장소의 값        ──▶  git push 만
  포트 · DB 주소 · 라우트          이 서버는 다시 띄우지 않아도 됨
                                요청마다 저장소를 다시 읽기 때문

이 서버의 주소값          ──▶  컨테이너 재시작
  EUREKA_HOST · LOKI_HOST        이미지는 그대로
  CONFIG_HOST

코드 · 의존성             ──▶  이미지 재빌드 + 재배포
  application.yml 포함            *드물어야 함


그래서 application.yml 을 최대한 비우고
환경마다 다른 값은 전부 환경변수로 뺐음
```

<br><br>

---

### 3-1. "저장소 값은 push 만" 이 실물로 확인됐습니다

```
config 저장소를 고치고 push
        │
        └── 이 서버를 전혀 건드리지 않음
              서비스만 재기동하거나 /actuator/refresh 하면 새 값이 내려옴
```

**이 서버가 요청 때마다 저장소를 다시 읽기 때문입니다.**

> **그래서 이 서버에는 `/actuator/refresh` 가 없습니다.** 자기 설정을 갱신할
> 대상이 없습니다.

<br><br>

---

### 3-2. 서비스 쪽에서 반영하기

```bash
curl -X POST http://localhost:8084/actuator/refresh
```

**바뀐 키 목록이 배열로 돌아옵니다.**

```json
["app.datasource.host", "server.port"]
```

> ⚠ **refresh 응답이 200 이고 바뀐 키가 나오는 것은 아무것도 증명하지 않습니다.**
> DataSource 는 프로퍼티만 다시 바인딩되고 **커넥션 풀은 옛 주소를 물고 있을 수
> 있습니다.** 그래서 1계층에 두 줄을 넣어 두었습니다 — `config` README 6-2 참고.

<br><br>

---

## 4. application.yml

**이 파일이 이 서비스의 전부입니다.** 블록마다 왜 그 값인지가 붙어 있습니다.

<br><br>

---

### 4-1. 저장소를 읽는 설정

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/paw-trail/config
          default-label: main
          clone-on-start: true
```

| 값 | 왜 |
|---|---|
| `uri` | **공개 저장소라 인증이 없습니다.** 비공개로 바꾸면 `username`·`password` 에 fine-grained PAT 를 환경변수로 |
| `default-label` | 라벨은 **깃 브랜치**이고 우리 기본 브랜치가 `main` |
| `clone-on-start` | **기동할 때 미리 clone** |

---

**`clone-on-start: true` 인 이유입니다.**

```
기본값(false)
        │
        ├── 첫 요청 때 clone  →  처음 뜨는 서비스만 느려짐
        └── 저장소 주소가 틀려도 그때까지 안 드러남

true
        │
        └── 기동 시점에 실패하므로 바로 알 수 있음
              헬스의 configServer 가 UP 인 것으로 확인됨
```

<br><br>

---

### 4-2. 프로파일

```yaml
spring:
  profiles:
    default: local
```

**`active` 가 아니라 `default` 인 것이 중요합니다.**

```
active    강제. 컨테이너에서 덮어쓸 때 헷갈림
default   안 정해주면 local
            → 컨테이너의 SPRING_PROFILES_ACTIVE=dev 가 이김
            → IntelliJ 는 그냥 local 로 돎
```

<br><br>

---

### 4-3. 유레카 — 등록만 합니다

```yaml
eureka:
  client:
    fetch-registry: false
    register-with-eureka: true
    service-url:
      defaultZone: http://${EUREKA_HOST:localhost}:8761/eureka/
```

**"찾는 것" 과 "찾아지는 것" 이 다릅니다.**

| | 이 서버 | 왜 |
|---|---|---|
| `fetch-registry` | **`false`** | 다른 서비스를 호출할 일이 없음 |
| `register-with-eureka` | `true` | 동작에 필요해서가 아님 — 아래 |

---

**등록만 하는 이유입니다.**

```
유레카 대시보드가 "지금 무엇이 떠 있나" 를 보는 단일 화면인데
        │
        └── 플랫폼 3개 중 하나만 빠지면 확인 경로가 갈라짐
              "config-server 는 유레카 말고 다른 데서 확인하세요" 가 됨
```

**Config First 라 등록조차 필수가 아닙니다.** 서비스들은 유레카가 아니라
`spring.config.import` 의 주소로 직접 찾아옵니다.

---

**⚠ 등록됐다고 게이트웨이 라우팅 대상이 되면 안 됩니다.**

```
GET /place-service/prod  한 방이면
        │
        └── DB 이름 · 계정명 · 노드 주소 · 포트 배치가 통째로 나감
```

**`/internal` 과 같은 취급으로 게이트웨이가 라우트를 안 만들고 네트워크로도
격리합니다.** `gateway-server` README 3-1 에 *"라우트를 만들지 않는 것"* 으로
명시돼 있습니다.

<br><br>

---

### 4-4. 액추에이터 — `refresh` 를 넣지 않습니다

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
```

**다른 서비스는 여기에 `refresh` 가 있습니다.**

```
도메인 서비스   health, info, prometheus, refresh
이 서버        health, info, prometheus
                                     ▲
                                     └── 요청마다 저장소를 다시 읽으므로
                                         자기 설정을 갱신할 대상이 없음
```

<br><br>

---

### 4-5. 로그 — 공통 모듈을 안 쓰므로 직접 씁니다

```yaml
app:
  logging:
    loki:
      url: http://${LOKI_HOST:localhost}:3100/loki/api/v1/push
```

**`local` 에서는 이 값이 쓰이지 않습니다.** 프로파일만 바꿔 확인할 수 있도록
채워 두었습니다.

---

**`logback-spring.xml` 의 구조가 중요합니다.**

```xml
<springProfile name="local">
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</springProfile>

<springProfile name="!local">
    <!-- appender 정의도 이 안에 -->
    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">...</appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOKI"/>
    </root>
</springProfile>
```

> ⛔ **`<springProfile>` 을 `<root>` 안에 넣으면 안 됩니다.**
>
> ```
> <springProfile> elements cannot be nested within an <appender>, <logger> or <root> element
> ```
>
> logback 이 `<springProfile>` 을 먼저 처리하는데 `<root>` 안쪽은 나중에 처리해
> **평가 시점이 어긋납니다.** 경고만 뜨고 넘어가지만 **동작이 보장되지 않습니다.**
>
> **프로파일마다 `<root>` 를 따로 두는 것이 지원되는 형태입니다.**
> appender 정의를 `!local` 안에 넣으면 **local 에서는 만들어지지도 않습니다.**

<br><br>

---

### 4-6. 넣지 않은 것

| | 왜 |
|---|---|
| **공통 모듈** | 플랫폼 3개 공통 규칙. [7-1](#7-1-왜-공통-모듈을-안-쓰나) |
| `spring-cloud-starter-config` | **자기가 서버라 클라이언트가 불필요** |
| Lombok | 코드가 클래스 하나뿐 |
| springdoc | 도메인 API 가 없음 |
| zipkin · tracing | **요청 체인의 일부가 아님** — 기동 시 한 번과 refresh 때만 호출됨 |
| GitHub Packages 저장소 블록 | 공통 모듈을 안 쓰므로 |

---

**직접 넣은 것 셋입니다.**

```groovy
runtimeOnly    'io.micrometer:micrometer-registry-prometheus'   // actuator 만으로는 /actuator/prometheus 가 안 생김
implementation "com.github.loki4j:loki-logback-appender:${lokiVersion}"   // 공통 모듈 미의존이라 직접
```

```properties
# gradle.properties
lokiVersion=2.0.3
```

> **loki4j 는 `2.1.0` 을 못 씁니다.** Boot 4.1.1 이 물고 오는 Logback 이 1.5.38 이라
> 버전이 안 맞습니다.

<br><br>

---

## 5. 컨테이너와 배포

<br><br>

---

### 5-1. 이 서버만 주소 환경변수를 직접 받습니다

```yaml
# infra/docker-compose.yml
  config-server:
    environment:
      SPRING_PROFILES_ACTIVE: dev
      EUREKA_HOST: eureka-server        # *이 서버만
      LOKI_HOST: loki                   # *이 서버만

  eureka-server:
    environment:
      SPRING_PROFILES_ACTIVE: dev
      CONFIG_HOST: config-server        # 나머지는 이것만

  gateway-server:
    environment:
      SPRING_PROFILES_ACTIVE: dev
      CONFIG_HOST: config-server
```

**왜 이 서버만 다른가**

```
다른 서비스   dev 프로파일 → config 의 application-dev.yml 이 주소를 바꿔 줌
                              eureka: eureka-server:8761
                              loki:   loki:3100

이 서버      ⛔ config 에서 설정을 안 받음
                  → dev 프로파일이 주소를 바꿔 주는 경로가 없음
                  → 환경변수로 직접 줘야 함
```

---

**빠뜨리면 이렇게 됩니다.**

```
POST http://localhost:8761/eureka/apps/CONFIG-SERVER : Connection refused
        │
        └── 30초마다 반복
              ⚠ 등록 실패가 기동을 막지 않아 서버는 정상으로 보임
                  유레카 화면에 config-server 가 안 보이는 것으로 알아차림
```

> **실제로 겪었습니다.** `CONFIG_HOST` 만 챙기고 이 둘을 빠뜨렸습니다.

<br><br>

---

### 5-2. 이미지 굽고 올리기

```powershell
cd C:\Tour_Prj\config-server
.\gradlew clean build

$env:GPR_TOKEN | docker login ghcr.io -u <GitHub 아이디> --password-stdin
docker build -t ghcr.io/paw-trail/config-server:latest .
docker push ghcr.io/paw-trail/config-server:latest
```

**확인**

```powershell
docker run --rm --entrypoint sh ghcr.io/paw-trail/config-server:latest -c "ls -lh /app"
```

`app.jar` 가 수십 MB 면 정상입니다.

> `build.gradle` 에 `tasks.named('jar') { enabled = false }` 가 있어
> **`-plain.jar` 가 안 생깁니다.** 없으면 Dockerfile 의 `COPY build/libs/*.jar` 가
> 둘 다 잡아 **어느 쪽이 담길지가 파일 정렬 순서에 달립니다.**

---

**컨테이너로 띄우기**

```bash
cd <infra 경로>
docker compose pull config-server
docker compose up -d
```

> `up -d` 만으로는 **이미지를 다시 받지 않습니다.** `pull` 이 먼저입니다.

<br><br>

---

### 5-3. 배포 방식이 플랫폼 3개 중 다릅니다

| 서비스 | 배포 방식 | 왜 |
|---|---|---|
| gateway-server | **blue-green** | nginx 가 upstream 으로 지목 |
| eureka-server | 단독 교체 | |
| **config-server** | **재시작** | 아무도 안 찾고, 잠깐 없어도 이미 뜬 서비스는 안 죽음 |

```
config-server 가 잠깐 내려가면
        │
        ├── 이미 뜬 서비스   설정을 이미 받아 뒀으므로 그대로 돎
        └── 새로 뜨는 서비스  기동이 실패함
                              → 배포 순서상 이 서버를 먼저 올림
```

<br><br>

---

### 5-4. 기동 순서

```
config-server  ──▶  eureka-server  ──▶  gateway-server  ──▶  도메인 14개
```

**Config First 라 순서가 선형입니다.**

| | |
|---|---|
| 이 서버가 먼저 떠야 하는 이유 | 나머지가 전부 설정을 여기서 받음 |
| 유레카가 늦게 떠도 되는 이유 | **등록만 재시도할 뿐 이 서버는 안 죽음** |
| 이 서버가 유레카를 안 찾아도 되는 이유 | `fetch-registry: false` |

<br><br>

---

## 6. 막히기 쉬운 자리

<br><br>

---

### 6-1. 이 서버가 안 뜰 때

| 로그 | 원인 |
|---|---|
| `Tomcat started on port 8080` | **포트를 못 읽음.** 이 서버는 config 를 안 받으므로 `application.yml` 을 볼 것 |
| 저장소 clone 실패 | `git.uri` 오타. 또는 저장소가 비공개로 바뀜 |
| 그냥 빈 웹 앱이 뜸 | **`@EnableConfigServer` 가 없음** |
| `Connection refused: :8761` 이 반복 | **정상.** 유레카가 아직 없음 |
| `clientConfigServer: UNKNOWN` | **정상.** [1-2](#1-2-헬스-응답-읽는-법) |

<br><br>

---

### 6-2. 서비스가 설정을 못 받을 때

| 증상 | 원인 |
|---|---|
| 포트가 8080 으로 뜸 | **2계층 파일을 못 찾음.** 파일명이 `spring.application.name` 과 같은지 |
| `propertySources` 가 비어 있음 | 같음 |
| 게이트웨이 라우트가 0개 | 같음. **게이트웨이는 정상 포트가 8080 이라 포트로 판별 불가** |
| 서비스가 이 서버를 못 찾음 | `CONFIG_HOST` — 컨테이너 안에서 `localhost` 는 자기 자신 |
| `${DB_HOST}` 가 그대로 | **환경변수를 안 넣음.** 이 서버는 치환하지 않음 |

**대조하는 것이 요령입니다.**

```
:8888/place-service/local        이 서버가 내려주는 것
:8084/actuator/env               서비스가 실제로 들고 있는 것
```

<br><br>

---

### 6-3. 저장소 값을 고쳤는데 반영이 안 될 때

```
① config 저장소에 push 했나              git log 로 확인
        │
        ▼
② 이 서버가 그것을 읽었나
        curl :8888/place-service/local 의 version 이 새 커밋 해시인지
        │
        ▼
③ 서비스에 반영했나
        POST :8084/actuator/refresh   또는 재기동
```

**②에서 이미 새 값이 보이면 이 서버는 할 일을 다 한 것입니다.**
**이 서버를 재시작할 필요는 없습니다.**

<br><br>

---

### 6-4. 환경

| | 주의 |
|---|---|
| PowerShell `curl` | `Invoke-WebRequest` 별칭이라 JSON 원문이 안 보임 → **`curl.exe`** |
| 설정 응답 보기 | **브라우저가 제일 쉬움** — `http://localhost:8888/place-service/local` |
| `bootRun` | **80% 에서 멈춘 것처럼 보이는 것이 정상** — 앱이 떠 있는 동안 안 끝나는 태스크 |
| | 구분법: 아래 줄이 `> :bootRun` 이면 실행 중 |
| 첫 빌드가 느림 | **`eureka-client` 의 전이 의존성 트리가 큼**(jersey · archaius · xstream) |
| IntelliJ 실행 | **Run 버튼이 나음** — 콘솔이 평범하고 중지가 쉬움 |

---

**IntelliJ 가 Gradle 프로젝트로 인식 못 할 때**

```
지문
  ① com · pawtrail · configserver 가 폴더 여러 개로 나뉘어 보임
  ② External Libraries 노드가 없음
  ③ main 메서드 옆에 실행 표시가 없음

해결
  ① build.gradle 우클릭 → Link Gradle Project
  ② Gradle 툴 창의 + 로 build.gradle 지정
  ③ *확실한 방법
       File → Close Project → File → Open
       폴더가 아니라 build.gradle 파일 자체를 선택 → Open as Project
```

붙은 뒤 **`Settings → Build Tools → Gradle` 의 `Gradle JVM` 이 21인지** 확인합니다.
PowerShell 의 `JAVA_HOME` 과 별개라 **`gradlew` 가 잘 돌았다고 IntelliJ 도 되는 것이
아닙니다.**

<br><br>

---

## 7. 왜 이렇게 만들었나

<br><br>

---

### 7-1. 왜 공통 모듈을 안 쓰나

**플랫폼 3개(config-server · eureka-server · gateway-server) 공통 규칙입니다.**

```
공통 모듈의 존재 이유   "도메인 서비스가 전부 쓰는 것"
플랫폼 3개            인프라 성격이라 그 기준 밖
```

---

**이 서버에는 결정적인 위험이 하나 더 있습니다.**

```
공통 모듈의 TraceIdResponseAdvice 는 ResponseBodyAdvice 임
        │
        └── 모든 응답을 {code, message, data, traceId} 로 감쌈
                  ▲
                  └── ⛔ 이 서버가 내려주는 것은 Environment JSON 임
                        거기에 래퍼가 씌워지면
                        config 클라이언트가 설정을 못 읽음
                              → config-server 자체가 무용지물
                              → 파싱 실패로 조용히 나타날 수 있음
```

---

**나머지도 쓸 자리가 없습니다.**

| 공통 모듈이 주는 것 | 이 서버에서 |
|---|---|
| `BaseEntity` · JPA Auditing | JPA 가 없음 |
| `ErrorCode` · `GlobalExceptionHandler` | 도메인 API 가 없음 |
| `HeaderAuthenticationFilter` · 보안 체인 | **게이트웨이 뒤가 아님** |
| Outbox · Inbox | DB · Kafka 가 없음 |

**유일하게 쓸모 있던 것이 Loki appender 하나**였고, 그것 때문에 위 전부를 들여오는
것은 비대칭이라 **loki4j 를 직접 선언하고 `logback-spring.xml` 을 자체 작성**했습니다.

<br><br>

---

### 7-2. 왜 Initializr 로 만들었나

**`service-template` 에서 복제하지 않았습니다.**

| | 걷어낼 것 | 남는 것 |
|---|---|---|
| `build.gradle` | JPA · QueryDSL · hibernate-spatial · Flyway · PostgreSQL · Kafka · Redis · springdoc | |
| 골격 | 4계층 패키지 · `V20__template.sql` | |
| `application.yml` | DB · Kafka · Redis · JPA · Flyway 블록 | |
| README | 전부 | |
| | | Dockerfile · Jenkinsfile · `.github/` · `.coderabbit.yaml` **넷뿐** |

**넷은 복사하면 됩니다.**

---

**결정적인 이유는 따로 있습니다.**

```
Initializr 는 Boot 4 의 정확한 아티팩트 이름을 알려 줌
        │
        └── service-template 을 만들 때 그렇게 발견했음
              spring-boot-starter-web  →  -webmvc
              Flyway · Kafka · Zipkin 이 각각 별도 스타터가 됨
```

**걷어내기는 "빠뜨리면 조용히 남는" 종류입니다.** QueryDSL APT 가 남으면 빌드만
느려지고, **JPA 가 남으면 DataSource 자동 설정이 돌아 기동이 실패합니다.**

> ⚠ **Initializr 가 `@EnableConfigServer` 를 붙여 주지 않습니다.**
> 직접 추가해야 하고 **안 하면 그냥 빈 웹 앱이 뜹니다.**

<br><br>

---

### 7-3. 왜 `application.yml` 을 최대한 비우나

```
여기 적힌 값을 고치면  →  이미지 재빌드 + 재배포
환경변수로 빼 두면    →  컨테이너 재시작만
```

**그래서 환경마다 다른 값은 전부 환경변수입니다.**

```yaml
defaultZone: http://${EUREKA_HOST:localhost}:8761/eureka/
url:         http://${LOKI_HOST:localhost}:3100/loki/api/v1/push
```

> **여기만 기본값을 붙였습니다.** 다른 저장소에서는 `${...:기본값}` 을 금지하는데,
> 이 둘은 **로컬에서 언제나 `localhost` 가 정답**이고 **컨테이너·AWS 에서만
> 덮어쓰면 되기 때문**입니다.

<br><br>

---

### 7-4. 왜 공개 저장소를 읽나

```
전제 — 이 서버는 ${...} 를 치환하지 않고 문자열 그대로 내려보냄
        │
        └── 비밀값이 애초에 config 저장소에 존재하지 않음
              각 서비스가 자기 환경변수로 해석함
                    │
                    └── 이 서버가 비밀을 몰라도 됨
```

**만약 이 서버가 치환을 시도했다면** 컨테이너에 **전 서비스의 비밀번호를 다 넣어야
하는 구조**가 되어 공개 저장소 결정 자체가 뒤집혔을 것입니다.

> **실물로 확인했습니다.** `GET /auth-service/dev` 응답에
> `"spring.datasource.password": "${SERVICE_DB_PASSWORD}"` 가 원문 그대로 있었습니다.

---

**비공개로 바꾸려면**

```
HTTPS + fine-grained PAT (config 레포 하나에 Contents read-only)
        │
        └── git.username · git.password 에 환경변수로 주입
```

<br><br>

---

## 8. 용어

설정 용어는 `config` README 10장에 있습니다. **여기는 이 서버에서만 쓰는 말**입니다.

| 용어 | 뜻 |
|---|---|
| **설정 서버** | 이 서비스. 저장소를 읽어 설정을 내려 줌 |
| **config 클라이언트** | 설정을 받아 가는 쪽. 다른 서비스 전부 |
| **`spring.config.import`** | 클라이언트가 설정 서버 주소를 적는 자리. `optional:configserver:...` |
| **`optional:`** | 설정 서버가 없어도 기동은 하라는 표시. 대신 포트가 8080 으로 뜸 |
| **닭-달걀 문제** | 저장소 주소를 알아야 저장소를 읽음 → 이 서버만 자기 설정을 못 받음 |
| **self-referential** | 설정 서버가 자기 저장소에서 자기 설정을 읽는 기능. 우리는 안 씀 |
| **`clone-on-start`** | 기동할 때 저장소를 미리 받아 둠. 주소가 틀리면 바로 드러남 |
| **라벨 (label)** | 읽을 깃 브랜치. 우리는 `main` |
| **`propertySources`** | 응답에서 어느 파일에서 온 값인지 보여 주는 배열. 앞이 우선 |
| **Config First** | 서비스가 설정 서버를 **주소로 직접** 찾는 방식. 유레카를 안 거침 |
| **`fetch-registry`** | 유레카에서 남의 주소를 받아 올지. 이 서버는 `false` |
| **`register-with-eureka`** | 유레카에 자기를 올릴지. 이 서버는 `true` — 대시보드용 |
| **`EUREKA_HOST` · `LOKI_HOST`** | 이 서버만 직접 받는 주소 환경변수. config 를 안 받아서 |
| **loki4j** | 로그를 Loki 로 보내는 logback 부품. 공통 모듈 대신 직접 넣음 |
