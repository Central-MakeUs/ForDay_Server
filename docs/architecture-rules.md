# 아키텍처 규칙 (SOLID 기반)

이 문서는 `ForDay_Server`가 지키는 아키텍처 규칙과, 그중 **기계로 강제되는 규칙의 실행 코드**를 담는다.
배경과 결정 과정은 [ADR-0001](./adr/0001-incremental-hexagonal-architecture.md) 참고.

> **원칙** — 규칙은 문서가 아니라 테스트가 지킨다. 여기 적혔지만 ArchUnit으로 검증되지 않는 항목은 §7 리뷰 체크리스트로 따로 분리했다. 지켜지지 않을 규칙을 강제되는 규칙인 척 적지 않는다.

---

## 1. 도입 방법

`build.gradle`:

```gradle
testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'  // 버전은 최신 확인
```

테스트 위치: `src/test/java/com/example/ForDay/architecture/ArchitectureTest.java`

실행: `./gradlew test --tests "ArchitectureTest"`

**위반이 남아 있는 규칙은 `@ArchIgnore(reason = "Phase N 완료 후 활성화")`로 표시**하고, 해당 Phase가 끝나면 애노테이션을 제거한다.

> JUnit의 `@Disabled`는 `@ArchTest` 필드에 적용되지 않는다. ArchUnit 자체 애노테이션인 `@ArchIgnore`(`com.tngtech.archunit.junit.ArchIgnore`)를 써야 한다. 속성명은 `value`가 아니라 **`reason`** 이므로 `@ArchIgnore("...")`는 컴파일되지 않는다.

freeze(violation store)는 쓰지 않는다. 위반이 Phase 1·2에서 전부 제거할 대상이라 저장 파일 관리 비용이 이득보다 크다.

**현재 위반 규모** (규칙을 일시 활성화해 측정, 2026-08-30):

| 규칙 | 위반 엣지 | 해소 |
| --- | --- | --- |
| D1 도메인→인프라 | 68 | Phase 2 |
| D2 엔티티→DTO | 48 | Phase 1 |
| I2 record→hobby.dto | 9 | Phase 1 |
| D5 RestTemplate 직접 사용 | 2 | Phase 2 |
| D3 엔티티→Spring | 1 | Phase 1 |
| S4 순환 의존 | 94 | Phase 3 이후 |

파일 수로는 25개 남짓이지만 의존 엣지 기준으로는 위 표가 정확하다.

---

## 2. 규칙 목록

| ID | 원칙 | 규칙 | 현재 상태 |
| --- | --- | --- | --- |
| D1 | DIP | `domain`은 `infra` 구현체에 의존하지 않는다 | 🔴 68건 → Phase 2 |
| D2 | DIP | 엔티티는 `dto`에 의존하지 않는다 | 🔴 48건 → Phase 1 |
| D3 | DIP | 엔티티는 Spring에 의존하지 않는다 | 🔴 위반 1 → Phase 1 |
| D4 | DIP | `..port..`는 인터페이스만 포함한다 | 🟢 활성 (신규) |
| D5 | DIP | 도메인은 `RestTemplate`을 직접 쓰지 않는다 | 🔴 2건 → Phase 2 |
| S1 | SRP | 클래스당 주입 의존성 ≤ 8 | 🟡 레거시 7개 예외 |
| S2 | SRP | 컨트롤러는 리포지토리에 직접 의존하지 않는다 | 🟢 활성 |
| S3 | SRP | 컨트롤러는 대응 `Docs` 인터페이스를 구현한다 | 🟢 활성 |
| S4 | SRP | 도메인 간 순환 의존이 없다 | 🔴 94건 → Phase 3 이후 |
| O1 | OCP | 도메인은 어댑터 구현체를 직접 참조하지 않는다 | 🟢 활성 (신규) |
| L1 | LSP | 포트 구현체는 `UnsupportedOperationException`을 던지지 않는다 | 🟢 활성 (신규) |
| L2 | LSP | 서비스는 다른 서비스를 상속하지 않는다 | 🟢 활성 |
| I1 | ISP | 포트 인터페이스의 메서드는 5개 이하 | 🟢 활성 (신규) |
| I2 | ISP | 도메인 간 DTO 교차 참조 금지 | 🔴 9건 → Phase 1 |

🟢 즉시 활성 · 🟡 예외 목록과 함께 활성 · 🔴 `@ArchIgnore` 후 해당 Phase에서 활성

---

## 3. DIP — 의존 역전

> 상위 정책은 하위 세부사항에 의존하지 않는다. 둘 다 추상에 의존한다.

이 프로젝트에서 가장 크게 깨져 있는 원칙이다. 도메인 서비스가 S3·Lambda·FCM·FastAPI를 **구체 클래스로** 직접 붙잡고 있어(의존 엣지 68건), 도메인 로직만 떼어 테스트할 수 없다.

**D1. `domain`은 `infra` 구현체에 의존하지 않는다.** 외부 자원 접근은 포트 인터페이스를 통한다.

**D2. 엔티티는 DTO에 의존하지 않는다.** 안쪽(도메인)이 바깥쪽(웹)을 알면 API 스펙 변경이 도메인을 흔든다.

**D3. 엔티티는 Spring에 의존하지 않는다.** JPA/Hibernate 애노테이션은 허용한다 — ADR-0001에서 JPA 엔티티와 도메인 모델을 분리하지 않기로 했으므로, 여기까지가 이 프로젝트가 지킬 수 있는 선이다.

현재 위반 2건이고 처리 방식이 다르다.

| 위반 | 처리 |
| --- | --- |
| `User.java:10` — `org.springframework.util.StringUtils.hasText` (147행 1회 사용) | **Phase 1에서 제거.** `nickname != null && !nickname.isBlank()`로 바꾸면 끝난다 |
| `RefreshToken.java` — `@RedisHash`, `@Id`(Spring Data) | **규칙 예외.** Redis 해시 엔티티는 Spring Data Redis 없이 표현할 수 없다. `@RedisHash`가 붙은 클래스는 규칙 대상에서 제외한다 |

**D4. `..port..` 패키지에는 인터페이스만 둔다.** 구현체가 섞이면 포트가 아니다.

**D5. 도메인은 `RestTemplate`을 직접 쓰지 않는다.** 현재 `AppleService:116`이 메서드 안에서 `new RestTemplate()`을 생성해 스텁 주입 지점 자체가 없다.

**포트 위치 규칙** — 2개 이상 도메인이 쓰면 `global/port`, 단일 도메인 전용이면 `domain/<name>/port`.

---

## 4. SRP — 단일 책임

> 클래스가 변경되는 이유는 하나여야 한다.

기계로 완벽히 검증할 수 없어 **프록시 지표**를 쓴다. 주입 의존성 개수는 그중 가장 신뢰할 만한 신호다.

**S1. 클래스당 주입 의존성 ≤ 8.** 현재 분포:

| 클래스 | 주입 수 | 비고 |
| --- | --- | --- |
| `ActivityRecordServiceV2` | 19 | 예외 등록 |
| `HobbyService` (v1) | 18 | 예외 등록 (528줄) |
| `ActivityRecordService` (v1) | 17 | 예외 등록 |
| `ReactionService` | 12 | 예외 등록 |
| `ActivityService` | 12 | 예외 등록 |
| `AuthService` | 11 | 예외 등록 |
| `UserService` | 10 | 예외 등록 |
| `ActivityRecordServiceV3` | 8 | 통과 |
| `HobbyServiceV2` | 6 | 통과 |

임계값 8은 임의로 정한 값이 아니라 **최근에 작성된 클래스(V3=8, HobbyServiceV2=6)가 통과하는 선**이다. 레거시 7개는 이름으로 명시 예외 처리하고, 예외 목록에 새 클래스를 추가하는 것은 금지한다. 목록은 줄어들기만 해야 한다.

**선택자 주의** — S3와 같은 이유로 `haveSimpleNameEndingWith("Service")`는 쓰지 않는다. `ActivityRecordServiceV2`(19개, 최악의 위반)와 `HobbyServiceV2`가 통째로 빠진다. `haveSimpleNameContaining("Service")`를 쓴다.

**S2. 컨트롤러는 리포지토리에 직접 의존하지 않는다.** 컨트롤러는 매핑과 서비스 위임만 한다 (CLAUDE.md 컨벤션).

**S3. 컨트롤러는 대응 `Docs` 인터페이스를 구현한다.** Swagger 애노테이션이 컨트롤러 본문으로 새는 것을 막는다.

**선택자 주의** — 클래스 이름이 `Controller`로 끝나는지로 고르면 안 된다. 버전 컨트롤러는 `HobbyControllerV2`, `ActivityRecordControllerV2`, `ActivityRecordControllerV3`처럼 **버전 접미사로 끝나서 전부 누락된다.** `@RestController` 애노테이션으로 고른다.

디버그용 컨트롤러 2개(`TestNotificationController`, `global/common/test/controller/TestController`)는 Docs 인터페이스가 없고 만들 이유도 없으므로, 이름이 `Test`로 시작하는 클래스를 제외한다. (S1의 레거시 예외 목록과 달리 이건 영구 제외다.)

**S4. 도메인 간 순환 의존이 없다.** 측정 결과 순환 위반 94건으로, 도메인 간 사이클이 이미 존재한다.
활성화하려면 사이클을 먼저 끊어야 하므로 Phase 3 이후로 미룬다.

---

## 5. OCP / LSP

> OCP: 확장에는 열리고 수정에는 닫힌다. LSP: 하위 타입은 상위 타입을 대체할 수 있어야 한다.

**O1. 도메인은 어댑터 구현체를 직접 참조하지 않는다.** 어댑터는 `config`에서만 조립한다. 새 저장소·새 푸시 채널을 붙일 때 도메인 코드를 열지 않기 위함이다.

**L1. 포트 구현체는 `UnsupportedOperationException`을 던지지 않는다.** "이 어댑터는 이 메서드를 지원하지 않는다"는 순간 그 포트는 대체 가능하지 않다. 이게 발생하면 포트를 쪼개야 한다는 신호다(→ I1).

**L2. 서비스는 다른 서비스를 상속하지 않는다.** 현재 상속 사용처는 `Notification` 엔티티 계층과 `JwtTokenFilter`, `MyKeyLocator`뿐이다. 버전 간 코드 공유를 상속(`ActivityRecordServiceV3 extends ActivityRecordServiceV2`)으로 해결하려는 유혹을 차단한다 — 그 경우 조합(공유 UseCase 주입)을 쓴다.

---

## 6. ISP — 인터페이스 분리

> 클라이언트가 쓰지 않는 메서드에 의존하게 만들지 않는다.

**I1. 포트 인터페이스의 메서드는 5개 이하.**

Phase 2에서 가장 실수하기 쉬운 지점이다. `S3Service`는 public 메서드가 8개(`generateKey`, `createPresignedPutRequest`, `createFileUrl`, `existsByKey`, `deleteByKey`, `extractKeyFromFileUrl`, `copyObject`, …)인데, 이걸 그대로 `ImageStoragePort` 하나에 옮기면 ISP 위반이다. 이미지 URL만 필요한 `FriendService`가 presigned URL 생성과 삭제까지 의존하게 된다.

역할별로 쪼갠다:

```
ImageUrlPort      # createFileUrl, extractKeyFromFileUrl  — 조회 계열이 사용
ImageUploadPort   # generateKey, createPresignedPutRequest — 업로드 경로만
ImageLifecyclePort# existsByKey, deleteByKey, copyObject   — 생성/삭제 경로만
```

`Docs` 인터페이스와 Spring Data `Repository`는 이 규칙 대상에서 제외한다 (`HobbyControllerDocs`는 1401줄이지만 문서 전용이고, Repository 메서드 수는 Spring Data 관용구다).

**I2. 도메인 간 DTO 교차 참조 금지.** 현재 `record` 컨텍스트의 `ActivityRecord`가 `hobby.dto.request.RecordActivityReqDto`를 참조한다. 컨텍스트 간 통신은 DTO가 아니라 도메인 타입이나 커맨드 객체로 한다.

---

## 7. 기계 검증이 불가능한 규칙 (코드 리뷰 체크리스트)

ArchUnit으로 표현할 수 없어 사람이 봐야 하는 항목이다. **위 규칙과 같은 무게로 취급하지 않는다** — 어겼다고 CI가 막지 않는다.

- **[SRP]** `utils` 클래스에 순수 변환과 외부 I/O를 섞지 않는다.
  현재 `S3Util`이 URL 문자열 변환(`toXxxResizedUrl`) + S3 존재 확인(I/O) + 트랜잭션 동기화 삭제를 한 클래스에 담고 있다. 성격이 다른 셋이다.
- **[OCP]** 새 API 버전은 **기존 서비스를 고치지 않고 새 클래스로** 만든다. 구버전 앱이 v1을 계속 사용한다 (CLAUDE.md).
- **[OCP]** 서비스 안에서 `if (version == 2)` 같은 버전 분기를 두지 않는다. 버전 차이는 클래스 경계로 표현한다.
- **[SRP]** 트랜잭션 서비스 메서드 안에서 FCM을 직접 발송하지 않는다. `@TransactionalEventListener(AFTER_COMMIT)` → RabbitMQ 경로를 쓴다 (CLAUDE.md).
- **[DIP]** 비즈니스 판단을 `global/`에 두지 않는다. 현재 `AiUserSummaryService.determine(User, Hobby)`가 어댑터 자리에서 도메인 판단을 한다.
- **[ISP]** 포트 메서드가 5개를 넘어가면 개수를 줄이지 말고 **포트를 쪼갠다.** 한 인터페이스에 억지로 욱여넣기 위한 파라미터 플래그(`boolean isThumbnail` 같은)를 만들지 않는다.

---

## 8. ArchUnit 실행 코드

**구현 위치: `src/test/java/com/example/ForDay/architecture/ArchitectureTest.java`**

규칙 코드의 정본은 그 파일이다. 이 문서에 Java 코드를 복제해두면 반드시 어긋나므로 두지 않는다.
이 문서는 **왜 그 규칙인지**를, 테스트 파일은 **어떻게 검사하는지**를 담당한다.

실행:

```
./gradlew test --tests "ArchitectureTest"
```

### 구현 시 주의할 점

작성 과정에서 실제로 걸렸던 함정들이다.

**1. `@ArchIgnore`의 속성명은 `reason`이다.**
`@ArchIgnore("...")`는 `cannot find symbol: method value()`로 컴파일 실패한다.

**2. 이름 기반 선택자는 버전 접미사 클래스를 누락시킨다.**
`haveSimpleNameEndingWith("Service")`는 `ActivityRecordServiceV2`(주입 19개, 최악의 위반)를,
`haveSimpleNameEndingWith("Controller")`는 `HobbyControllerV2`·`ActivityRecordControllerV2/V3`를 통째로 건너뛴다.
→ `haveSimpleNameContaining` 또는 애노테이션 기반(`areAnnotatedWith(RestController.class)`) 선택자를 쓴다.

**3. 이름만으로 어댑터를 거르면 서드파티가 걸린다.**
`haveSimpleNameEndingWith("Adapter")`는 `io.jsonwebtoken.LocatorAdapter`(JJWT)를 잡아
`MyKeyLocator`가 O1 위반으로 뜬다. 패키지 조건을 AND로 걸어 우리 코드로 한정한다.

```java
private static final DescribedPredicate<JavaClass> 우리가_만든_어댑터 =
        resideInAPackage("com.example.ForDay..")
                .and(simpleNameEndingWith("Adapter"))
                .as("우리 코드베이스의 어댑터 구현체");
```

**4. 아직 존재하지 않는 패키지를 대상으로 하는 규칙은 `allowEmptyShould(true)`가 필요하다.**
`..port..`는 Phase 2에서 생기므로 D4·I1·L1이 여기 해당한다.

**5. 컴파일·테스트 인코딩을 UTF-8로 고정해야 한다.**
지정하지 않으면 Windows에서 플랫폼 기본(MS949)을 써 한글 규칙명과 실패 메시지가 깨지고,
CI(Linux)와 로컬의 빌드 결과가 갈린다. `build.gradle`에 반영되어 있다.

```gradle
tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
tasks.named('test') { defaultCharacterEncoding = 'UTF-8' }
```

### 커스텀 조건

ArchUnit 기본 문법으로 표현되지 않아 `ArchCondition`을 직접 구현한 규칙이다.

| 조건 | 쓰이는 규칙 | 판정 기준 |
| --- | --- | --- |
| `주입_의존성이_제한_이하` | S1 | non-static final 필드 개수 |
| `메서드가_제한_이하` | I1 | 인터페이스의 메서드 개수 |
| `Docs_인터페이스를_구현한다` | S3 | 구현 인터페이스 중 이름이 `Docs`로 끝나는 것 존재 |
| `다른_서비스를_상속하지_않는다` | L2 | 상위 클래스 이름이 `Service`로 끝나는지 |

## 9. 규칙 변경 절차

- 규칙 **추가**: PR에 근거(어떤 사고/중복을 막는지)를 적는다.
- 규칙 **완화·예외 추가**: ADR에 기록한다. S1 예외 목록에 새 클래스를 넣는 것은 규칙 완화에 해당한다.
- `@ArchIgnore` 제거는 해당 Phase의 완료 조건이다. Phase가 끝났는데 애노테이션이 남아 있으면 그 Phase는 끝난 것이 아니다.
