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

| 이름 | 담당                                           |
| -- | -------------------------------------------- |
| 은택 | User Service / Gateway                       |
| 찬영 | Hub / Hub Route                              |
| 경민 | Company / Product                            |
| 건우 | Order / Order Snapshot / Inventory           |
| 태윤 | Delivery / Delivery Route / Delivery Manager |
| 제희 | Slack Message / AI History                   |

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