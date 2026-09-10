# 5기 체크인

`POST /events/active/check-ins`는 인증 없이 이름과 전화번호를 받는다.

```json
{"name":"테스트","phoneNumber":"01000000000"}
```

이름 앞뒤 공백, 전화번호 하이픈·공백을 제거한 뒤 검증한다. 서버 KST 시각을
노션 조회 전에 확보한다. 노션 `이름`/`연락처`가 일치하는 `활동 중` 인원을 조회한 뒤
MySQL에 체크인 결과와 노션 반영 대상 정보를 함께 저장하고 `200 OK`를 반환한다.
이 응답은 노션 업데이트 완료를 의미하지 않는다.

## 시간과 상태

- `eventDateTime`: 체크인 시작 시각. 정각부터 허용.
- `lateFrom`: 지각 시작 시각. 정각부터 지각, 그 전은 참석.
- 온보딩: 2026-09-19 12:30 시작, 13:15부터 지각.
- 세미나: 2026-10-10 13:30 시작, 14:15부터 지각.
- 그 밖의 행사는 지정 날짜 12:00을 임시 시작으로 저장한다. 지각 시각은 미정(null)이며
  확정 후 설정해야 제출할 수 있다. 조회 API에서는 시작 시각을 제공한다.
- 13:10/14:10은 제출 차단 시각이 아니다. 종료는 관리자가 INACTIVE로 전환한다.
- `UNCHECKED`는 노션 `select: null`이며 불참으로 자동 변환하지 않는다.
- 불참 3종은 운영자가 노션에서 관리한다. 체크인 API는 참석·지각만 기록한다.

## 오류

| 상황 | HTTP | 코드 |
| --- | --- | --- |
| 등록된 행사 없음 | 404 | EVENT-003 |
| 체크인 시작 전 | 409 | EVENT-004 |
| ACTIVE 없음 또는 조회 중 원래 행사가 INACTIVE 전환 | 409 | CHECKIN-001 |
| 중복 체크인 | 409 | CHECKIN-002 |
| 지각 시각/노션 컬럼 설정 미완료 | 409 | CHECKIN-003 |
| DB 접수 실패 | 409 | CHECKIN-004 |
| 기존 멤버 데이터 충돌 | 409 | CHECKIN-005 |
| 노션 설정/조회 오류 | 503 / 502 | NOTION-001 / NOTION-002 |
| 명단 불일치 / 복수 일치 | 404 / 409 | NOTION-003 / NOTION-004 |

## 비동기 반영

`EventParticipant`의 `PENDING` 기록을 1초 간격으로 하나씩 처리한다. 행 잠금과
트랜잭션으로 작업을 처리하며 서버 재시작 후에도 PENDING은 남는다.
원래 행사 컬럼·노션 페이지·판정 결과를 저장하므로 행사 전환이 작업 대상을 바꾸지 않는다.
성공은 SUCCESS, 실패는 FAILED로 남기고 내부 참석 ID/행사 ID/오류 코드 로그를 기록한다.
FAILED는 자동 재시도하지 않는다. 운영자가 원인 해결 후 해당 기록을 PENDING으로
전환하면 재처리할 수 있다. 재처리 시 노션 값이 이미 동일하면 성공으로 처리한다.

노션에 다른 값(관리자 불참 포함)이 있으면 덮어쓰지 않고 NOTION-005로 실패 처리한다.
단, Notion에는 이 읽기/쓰기 사이를 원자적으로 비교·갱신하는 기능이 없으므로,
운영자가 동기화 중 같은 컬럼을 동시에 편집하지 않도록 한다.
요청 제한·타임아웃·서버 오류도 실패 로그와 FAILED 상태로 확인한다.

`NOTION_JECT_5TH_DB_TOKEN`은 환경변수로 주입한다. 운영 compose는 이미 `.env`를
env_file로 읽는다. 토큰이나 이름·전화번호 원문은 로그에 남기지 않는다.
`app.notion.sync.enabled=false`로 실제 노션 쓰기를 비활성화할 수 있다.

## 일회성 행사 등록

`seed-events` 프로필은 JPA `repository.save()`로 7개 행사를 등록하고 종료한다.
동일 이름·날짜를 중복 등록하지 않으며, 초기 설정 이후의 관리자 변경을 보존한다.
`--app.seed.activate-onboarding=true`를 명시한 경우에만 온보딩을 ACTIVE로,
나머지 모든 행사를 INACTIVE로 설정한다. 일반 실행에서는 시드가 실행되지 않는다.

```sh
java -jar build/libs/official-qr-checkin-server-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=seed-events --app.seed.activate-onboarding=true
```

대상 DB_URL/DB_USERNAME/DB_PASSWORD를 비공개 환경변수로 설정한다.
DDL은 `JPA_DDL_AUTO=update`로 추가 필드·인덱스를 반영한다. 이 프로필은
노션 작업을 끄고 HTTP 서버를 로컬 임시 포트에만 바인딩한다.
