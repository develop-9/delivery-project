# 🚚 Sparta Logistics Platform

MSA 기반 B2B 물류 관리 및 배송 시스템

허브 기반 물류 네트워크를 통해 업체의 상품 관리부터 주문, 재고, 배송 경로 관리까지 수행하는 물류 플랫폼입니다.

Spring Boot Spring Cloud Spring Security PostgreSQL Docker Kafka Redis Gemini API

---

# 📌 프로젝트 소개

Sparta Logistics Platform은 기업 간 거래(B2B)를 위한 물류 관리 및 배송 시스템입니다.

전국 17개의 허브 센터를 기반으로 업체와 상품을 관리하고, 주문 발생 시 허브 간 이동 경로를 생성하여 최종 목적지까지 배송하는 전체 물류 흐름을 관리합니다.

MSA(Microservices Architecture)를 기반으로 서비스를 독립적으로 분리하고, 각 서비스별 데이터베이스 스키마를 구성하여 서비스 간 결합도를 낮추고 확장 가능한 구조를 설계했습니다.

Spring Cloud Gateway와 Eureka Discovery를 활용하여 마이크로서비스 환경을 구성하고, JWT 기반 인증 및 Role 기반 권한 관리를 통해 안전한 서비스 접근 제어를 제공합니다.

---

# 🎯 프로젝트 목표

✅ MSA 기반 마이크로서비스 설계 및 구현

✅ 서비스별 독립 Database 구성 및 데이터 관리

✅ Spring Cloud Gateway + Eureka 기반 서비스 관리

✅ 서비스 간 REST API 통신 구조 설계

✅ JWT 기반 인증 및 Role 기반 접근 제어 구현

✅ 물류 도메인 기반 주문 → 배송 프로세스 구현

✅ Redis 기반 허브 및 배송 경로 캐싱

✅ AI 기반 배송 예상 시간 생성 및 Slack 알림

✅ Kafka 기반 이벤트 처리 구조 경험

---

# ✨ 주요 기능

| Domain | 기능 |
|---|---|
| 👤 User | 회원가입, 로그인, JWT 인증, 권한 관리 |
| 🌐 Gateway | API Gateway, 인증 및 요청 라우팅 |
| 📍 Hub | 허브 CRUD, 허브 검색, 허브 캐싱 |
| 🚚 Hub Route | 허브 간 이동 경로 관리, 이동 거리 및 시간 관리 |
| 🏢 Company | 업체 등록 및 관리 |
| 📦 Product | 상품 등록 및 관리 |
| 🛒 Order | 주문 생성, 주문 취소, 주문 조회 |
| 📊 Inventory | 상품 재고 관리 및 재고 차감 |
| 🚛 Delivery | 배송 생성, 배송 상태 관리 |
| 🛣 Delivery Route | 배송 경로 및 이동 기록 관리 |
| 👷 Delivery Manager | 배송 담당자 관리 및 배정 |
| 🤖 AI | 배송 예상 시간 생성 |
| 💬 Slack | 배송 담당자 알림 메시지 관리 |

---

# 👥 역할

| Role | 권한 |
|---|---|
| MASTER | 전체 시스템 관리 |
| HUB_MANAGER | 담당 허브 및 업체 관리 |
| DELIVERY_MANAGER | 배송 수행 및 배송 정보 관리 |
| COMPANY_MANAGER | 업체 및 상품 관리 |

---

# 👨‍💻 팀원

| 이름 | 담당 | 패키지                       |                                          
| -- | -------------------------------------------- |---------------------------|
| 은택 | User Service / Gateway                       | api-gateway, user-service |
| 찬영 | Hub / Hub Route                              | hub-service |
| 경민 | Company / Product                            | company-service |
| 건우 | Order / Order Snapshot / Inventory           | order-service |
| 태윤 | Delivery / Delivery Route / Delivery Manager | delivery-service |
| 제희 | Slack Message / AI History                   | slack-service |

---

# 🏗 프로젝트 구조

```text
delivery-project/
├── .env.example                         # 환경 변수 예시 파일
│
├── docker/
│   ├── postgres/
│   │   └── init.sql                     # PostgreSQL 초기화 및 Schema/DB 설정 SQL
│   ├── docker-compose-infra.yaml        # PostgreSQL, Redis 등 공통 인프라 실행 설정
│   └── docker-compose-service.yaml      # 각 Spring Boot 서비스 실행 설정
│
├── eureka-server/
│   ├── Dockerfile                       # Eureka Server Docker 이미지 빌드 설정
│   ├── build.gradle                     # Eureka Server의 Gradle 빌드 및 의존성 설정
│   └── src/main/java/.../
│       └── EurekaServerApplication.java # Eureka Server 애플리케이션 진입점
│
├── api-gateway/
│   ├── Dockerfile                       # API Gateway Docker 이미지 빌드 설정
│   ├── build.gradle                     
│   └── src/main/
│       ├── java/.../gateway/
│       │   ├── config/                  # Gateway의 라우팅, CORS 등 전역 설정
│       │   ├── filter/                  # 요청/응답에 대한 Global Filter 및 JWT 검증
│       │   └── ApiGatewayApplication.java
│       └── resources/
│           └── application.yml          # API Gateway 실행 환경 및 라우팅 설정
│
├── user-service/
│   ├── Dockerfile                       # User Service Docker 이미지 빌드 설정
│   ├── build.gradle                     
│   └── src/main/
│       ├── java/.../user_service/
│       │   │
│       │   ├── user/                    # User 도메인의 전체 기능을 담당하는 Bounded Context
│       │   │
│       │   ├── application/             # 유스케이스 실행 및 도메인/외부 시스템 간 흐름 조정
│       │   │   │
│       │   │   ├── command/              # 상태 변경(Command)에 사용하는 입력 객체
│       │   │   ├── command_service/      # 생성/수정/삭제 등 상태 변경 유스케이스 구현
│       │   │   ├── query/                # 조회(Query)에 사용하는 입력 객체
│       │   │   ├── query_service/        # 단건/목록 조회 및 검색 등 조회 유스케이스 구현
│       │   │   ├── result/               # Application Service의 처리 결과를 표현하는 객체
│       │   │   ├── port/                 # 외부 시스템과 통신하기 위한 Application 계층의 추상화
│       │   │   └── support/              # Application 계층에서 공통적으로 사용하는 지원 기능
│       │   │
│       │   ├── domain/                   # 핵심 비즈니스 규칙과 도메인 모델을 담당
│       │   │   │
│       │   │   ├── entity/               # User 등 도메인의 핵심 상태와 행위를 표현하는 Entity
│       │   │   └── repository/           # Domain에서 필요한 Repository의 추상화
│       │   │
│       │   ├── infrastructure/           # 외부 기술 및 프레임워크와 실제로 연결되는 구현 영역
│       │   │   │
│       │   │   ├── client/               # Feign 등 외부/다른 Service와의 실제 통신 구현
│       │   │   ├── jwt/                  # JWT 생성/검증 등 JWT 기술 구현
│       │   │   ├── adapter/              # Application Port의 구체적인 구현체
│       │   │   └── persistence/          # JPA/QueryDSL 등 DB 접근 기술의 구체적인 구현
│       │   │
│       │   ├── presentation/             # 외부 요청을 받아 Application 계층으로 전달하는 진입점
│       │   │   │
│       │   │   ├── api_controller/       # 외부 Client가 사용하는 Public API Controller
│       │   │   ├── internal_controller/  # MSA 내부 Service 간 통신을 위한 Internal API Controller
│       │   │   ├── request/              # HTTP 요청을 표현하는 Request DTO
│       │   │   └── response/             # HTTP 응답을 표현하는 Response DTO
│       │   │
│       │   ├── global/                   # User 도메인에 한정되지 않고 Service 전체에서 사용하는 공통 기능
│       │   │   │
│       │   │   ├── common/                # 여러 계층에서 공통으로 사용하는 공통 객체 및 기능
│       │   │   ├── config/                # Spring 및 외부 기술에 대한 Service 전역 설정
│       │   │   ├── exception/             # 공통 예외, ErrorCode 및 예외 처리
│       │   │   ├── response/              # 공통 API 응답 형식 및 응답 관련 객체
│       │   │   ├── security/              # Spring Security 인증/인가 관련 공통 구현
│       │   │   └── util/                  # 특정 도메인에 종속되지 않는 공통 유틸리티
│       │   │
│       │   └── UserServiceApplication.java
│       │
│       └── resources/
│           └── application.yml            # User Service 실행 환경 및 Spring 설정
│
├── hub-service/                           # Hub 도메인을 담당하는 독립적인 MSA Service
│   └── (user-service와 동일한 DDD + Layered Architecture 구조)
│
├── company-service/                       # Company 도메인을 담당하는 독립적인 MSA Service
│   └── (user-service와 동일한 DDD + Layered Architecture 구조)
│
├── order-service/                         # Order 도메인을 담당하는 독립적인 MSA Service
│   └── (user-service와 동일한 DDD + Layered Architecture 구조)
│
├── delivery-service/                      # Delivery 도메인을 담당하는 독립적인 MSA Service
│   └── (user-service와 동일한 DDD + Layered Architecture 구조)
│
└── slack-service/                         # Slack 알림 및 메시지 연동을 담당하는 독립적인 MSA Service
    └── (user-service와 동일한 DDD + Layered Architecture 구조)
```

### 계층별 흐름도

```text
Client
  │
  ▼
presentation
  │
  │ Request DTO → Command / Query
  ▼
application
  │
  ├── command_service
  ├── query_service
  │
  ├── domain 호출
  │
  └── port 호출
          │
          ├───────────────┐
          ▼               ▼
      domain       infrastructure
          │               │
          │               ├── client
          │               ├── adapter
          │               └── persistence
          │
          ▼
       Entity
```

---

# 🛠 기술 스택

## Backend

- Java 21
- Spring Boot 4.1.0
- Spring Security
- Spring Data JPA
- Hibernate
- QueryDSL
- JWT
- Spring Boot Actuator

## Spring Cloud

- Spring Cloud 2025.1.2
- Spring Cloud OpenFeign
- Spring Cloud Netflix Eureka

## Observability

- Micrometer Tracing
- Zipkin

## Database

- PostgreSQL 16
- Redis 7

## AI

- Google Gemini

## External Services

- Slack

## Testing

- JUnit 5

## Build

- Gradle

## Infrastructure

- Docker
- Docker Compose
- AWS EC2

## CI/CD

- GitHub Actions

---

# 📌 향후 개선 사항

- Kafka 기반 Event Driven Architecture 확대
- Saga Pattern 적용
- Circuit Breaker 적용
- 배송 담당자 최적 배정 알고리즘
- AI 기반 배송 경로 최적화
- Kubernetes 운영 환경 구축
- Blue-Green Deployment
- Monitoring Dashboard 개선

---

<div align="center">

## ⭐ Sparta Logistics Platform

MSA 기반 물류 시스템을 구축하며

Spring Cloud, Spring Security, Docker, Kafka, Redis, AI API를 활용하여

실제 기업 환경에서 요구되는 분산 시스템 설계와 개발 경험을 목표로 한 프로젝트입니다.

</div>