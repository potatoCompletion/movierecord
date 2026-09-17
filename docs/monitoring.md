# 모니터링 — CloudWatch

> [← README](../README.md)

## 설계 목표

초기에는 Prometheus와 Grafana를 같은 EC2 인스턴스의 컨테이너로 운영했습니다. 이 구성에는 구조적 한계가 있었습니다. **관측 대상과 관측 도구가 같은 장애 도메인에 있어**, 인스턴스에 문제가 생기면 원인을 확인할 수단도 함께 사라집니다. 2GB 환경에서 모니터링 스택이 약 200MB를 점유하는 것도 부담이었습니다.

관측 데이터를 인스턴스 외부로 분리하는 것을 목표로 CloudWatch 기반으로 전환했습니다.

## 지표 수집

EC2 기본 지표에는 메모리와 디스크 사용률이 포함되지 않습니다. CloudWatch Agent를 설치해 커스텀 네임스페이스로 전송합니다.

| 지표 | 네임스페이스 | 수집 방식 |
|------|-------------|----------|
| `mem_used_percent` | `MovieRecord/Host` | CloudWatch Agent |
| `swap_used_percent` | `MovieRecord/Host` | CloudWatch Agent |
| `disk_used_percent` | `MovieRecord/Host` | CloudWatch Agent |
| `ErrorCount` | `MovieRecord/App` | 로그 지표 필터 |
| `HealthStatus` | `MovieRecord/App` | systemd timer (1분 주기) |

Docker 호스트는 컨테이너마다 overlay 마운트가 생성되므로, 디스크 지표는 루트 파일시스템만 수집하도록 제한했습니다. 제한하지 않으면 마운트 하나당 지표 시리즈가 생성되어 커스텀 지표 비용이 불필요하게 늘어납니다.

```json
"disk": {
  "measurement": ["used_percent"],
  "resources": ["/"],
  "ignore_file_system_types": ["overlay", "tmpfs", "devtmpfs", "squashfs"]
}
```

## 로그 수집

Docker의 `awslogs` 로그 드라이버로 컨테이너 표준 출력을 CloudWatch Logs에 직접 전송합니다. 별도 에이전트로 로그 파일을 tail하는 방식보다 구성이 단순합니다.

```yaml
logging:
  driver: awslogs
  options:
    awslogs-region: ap-northeast-2
    awslogs-group: /movierecord/app
    awslogs-multiline-pattern: '^\d{4}-\d{2}-\d{2}'
    mode: non-blocking
    max-buffer-size: 4m
```

`mode: non-blocking`이 핵심입니다. 로그 드라이버의 기본값은 blocking이라, CloudWatch API가 지연되면 컨테이너의 stdout 쓰기 자체가 막혀 로그 수집 문제가 애플리케이션 장애로 번집니다. non-blocking으로 설정하면 버퍼가 가득 찰 때 로그를 버리고 애플리케이션은 계속 동작합니다. 관측 도구 때문에 서비스가 멈추지 않도록 로그 유실을 감수한 선택입니다.

`awslogs-multiline-pattern`은 Java 스택트레이스가 줄 단위로 분리되는 것을 막습니다. 타임스탬프로 시작하는 줄을 새 이벤트의 경계로 인식해, 예외 하나가 하나의 로그 이벤트로 묶입니다.

로그 그룹은 컨테이너별로 분리하고 보존 기간을 지정했습니다. 기본값이 무기한이라 명시하지 않으면 저장 비용이 계속 누적됩니다.

| 로그 그룹 | 보존 기간 |
|----------|----------|
| `/movierecord/app` | 14일 |
| `/movierecord/mysql` | 14일 |
| `/movierecord/redis` | 14일 |
| `/movierecord/nginx` | 7일 (액세스 로그 비중이 커 짧게 설정) |

## 애플리케이션 헬스 체크

호스트 지표만으로는 **애플리케이션 프로세스는 살아 있지만 요청을 처리하지 못하는 상태**를 감지할 수 없습니다. Actuator의 `/actuator/health`를 1분 주기로 호출해 결과를 지표로 전송합니다. Actuator는 서비스 포트(8080)가 아닌 관리 포트 9090에만 노출되며, 9090은 호스트에 publish하지 않아 같은 Docker 네트워크의 컨테이너에서만 접근할 수 있습니다.

```bash
CODE=$(docker exec movierecord-app \
  curl -s -o /dev/null -w "%{http_code}" --max-time 5 \
  http://localhost:9090/actuator/health)
[ "$CODE" = "200" ] && VALUE=1 || VALUE=0
aws cloudwatch put-metric-data --namespace MovieRecord/App \
  --metric-name HealthStatus --value $VALUE ...
```

`--max-time`으로 타임아웃을 두는 것이 중요합니다. 애플리케이션이 응답하지 못하는 상황에서 타임아웃이 없으면 체크 스크립트 자체가 대기 상태에 빠져 지표가 전송되지 않습니다.

실행은 cron 대신 systemd timer를 사용합니다. Amazon Linux 2023은 cron이 기본 설치되어 있지 않고, timer는 실행 결과가 journald에 남아 실패 원인을 추적하기 쉽습니다.

## 알람

임계값 초과 시 SNS를 통해 이메일로 발송합니다.

| 알람 | 조건 | 누락 데이터 처리 |
|------|------|-----------------|
| `movierecord-app-down` | `HealthStatus` 최소값 < 1 (5분) | **불량** — 지표가 끊긴 것 자체가 장애 신호 |
| `movierecord-app-errors` | `ErrorCount` 합계 > 3 (5분) | 양호 — 로그가 없는 것은 정상 |
| `movierecord-mem-high` | `mem_used_percent` > 85% (5분) | 누락 |
| `movierecord-swap-high` | `swap_used_percent` > 60% (10분, 2회 연속) | 누락 |
| `movierecord-disk-high` | `disk_used_percent` > 80% (5분) | 누락 |
| `movierecord-cpucredit-low` | `CPUCreditBalance` < 50 | 정상 |

두 애플리케이션 알람의 누락 데이터 처리를 정반대로 둔 것이 핵심입니다. `HealthStatus`는 데이터가 없다는 것이 곧 서버나 체크 프로세스가 죽었다는 뜻이므로 장애로 판정하고, `ErrorCount`는 에러 로그가 없는 조용한 시간대가 정상이므로 양호로 판정합니다.

`CPUCreditBalance`는 버스터블 인스턴스의 CPU 크레딧이 소진되는 상황을 감지합니다. 크레딧이 바닥나면 CPU 사용률은 낮은데 응답만 느려지는, 일반 지표로는 원인을 찾기 어려운 현상이 발생합니다.

## 대시보드

CloudWatch 대시보드에 헬스 상태, 호스트 자원, 에러 카운트, 애플리케이션 에러 로그를 배치했습니다. 서비스 가용 여부를 가장 먼저 확인할 수 있도록 `HealthStatus`를 최상단에 두었습니다.
