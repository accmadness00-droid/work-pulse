# Attendance Management System — Architecture

> **Stack:** Java 21 · Spring Boot 3.x · PostgreSQL 16 · React 18 · Docker
> **Pattern:** Modular Monolith
> **Version:** MVP 1.4 — Hikvision integration modes, event hash, open session race protection

---

## Table of Contents

1. [Overview](#1-overview)
2. [Project Structure](#2-project-structure)
3. [Module Descriptions](#3-module-descriptions)
   - [Auth Module](#31-auth-module)
   - [Company Module](#32-company-module)
   - [Branch Module](#33-branch-module)
   - [Employee Module](#34-employee-module)
   - [Attendance Module](#35-attendance-module)
   - [Report Module](#36-report-module)
   - [Device Module](#37-device-module)
4. [Database Schema](#4-database-schema)
5. [API Overview](#5-api-overview)
6. [Inter-Module Communication](#6-inter-module-communication)
7. [Security](#7-security)
8. [Infrastructure & Docker](#8-infrastructure--docker)
9. [Frontend Structure](#9-frontend-structure)
10. [ADRs — Architecture Decision Records](#10-adrs--architecture-decision-records)
11. [Changelog](#11-changelog)

---

## 1. Overview

Attendance Management System — xodimlarning kelish-ketishini qayd etuvchi, GPS geofence, Face ID, PIN va fizik qurilmalar (Hikvision, ZKTeco va boshqalar) orqali check-in imkoniyatiga ega, ko'p kompaniyali (multi-tenant) platforma.

```
┌─────────────────────────────────────────────────┐
│                  React Frontend                 │
│        (Vite + TypeScript + TanStack Query)     │
└─────────────────────┬───────────────────────────┘
                      │ HTTPS / REST
┌─────────────────────▼───────────────────────────┐
│              Spring Boot Application            │
│  ┌───────┐ ┌─────────┐ ┌────────┐ ┌──────────┐ │
│  │ Auth  │ │ Company │ │ Branch │ │ Employee │ │
│  └───────┘ └─────────┘ └────────┘ └──────────┘ │
│  ┌──────────────┐ ┌──────────┐ ┌─────────────┐ │
│  │  Attendance  │ │  Report  │ │   Device    │ │
│  └──────────────┘ └──────────┘ └─────────────┘ │
│  ┌───────────────────────────────────────────┐  │
│  │              Shared Kernel                │  │
│  │  ApiResponse · BaseEntity(+createdBy/     │  │
│  │  updatedBy) · Security · AuditConfig      │  │
│  └───────────────────────────────────────────┘  │
└───────────────┬────────────────┬────────────────┘
                │                │
         ┌──────▼──────┐  ┌──────▼──────┐
         │ PostgreSQL  │  │    Redis     │
         │   (main)    │  │  (cache +   │
         └─────────────┘  │  blacklist) │
                          └─────────────┘

[Hikvision / ZKTeco Device]
      │
      ├─ PUSH         → POST /api/v1/device-events
      │                 (X-Device-Api-Key bilan autentifikatsiya)
      │
      ├─ POLLING      ← Backend Hikvision ISAPI'dan eventlarni tortadi
      │                 (device credentials/vault orqali)
      │
      └─ ALERT_STREAM ← Backend Hikvision alert stream connection ochadi
                        (real-time event stream)
      ▼
[DeviceEventService]  →  device_events (raw, o'zgartirilmaydi)
      │                 event_hash bilan idempotency/deduplication
      │  SELECT FOR UPDATE SKIP LOCKED
      ▼
[DeviceEventProcessor]  →  EmployeeFacade (cross-module)
      │  AttendanceFacade.processDeviceEvent()
      ▼
[AttendanceService]  →  attendance_sessions yoziladi
      │                 partial UNIQUE index: bitta employee = bitta open session
      ▼
[ReportSnapshotScheduler]  →  report_snapshots (kunlik @Scheduled)
```

**Asosiy prinsiplar:**
- Device event = raw fakt. Attendance = hisoblangan natija.
- Attendance logic hech qachon device'ga to'g'ridan-to'g'ri bog'lanmaydi.
- Modullar faqat Facade interface orqali muloqot qiladi — hech qachon boshqa modulning Repository'sini inject qilmaydi.
- Ish vaqti va kechikish chegarasi faqat `BranchSchedule`da — `CompanySettings`da emas.

**Modular Monolith tanlash sabablari:**
- MVP tez yetkazib berish — bitta deploy, minimal ops xarajati
- Modullar chegarasi aniq — kelajakda microservicega aylantirish oson
- Bitta database transaction ichida bir nechta modul ishlashi mumkin
- Jamoaning kichikligi uchun mos (2–5 developer)

---

## 2. Project Structure

```
attendance-system/
├── backend/
│   └── src/main/java/uz/attendance/
│       ├── AttendanceApplication.java
│       │
│       ├── shared/                              # Shared Kernel
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── JwtConfig.java
│       │   │   ├── RedisConfig.java
│       │   │   └── OpenApiConfig.java
│       │   ├── domain/
│       │   │   └── BaseEntity.java              # id, createdAt, updatedAt,
│       │   │                                    # createdBy, updatedBy (UUID)
│       │   ├── response/
│       │   │   └── ApiResponse.java
│       │   ├── exception/
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   ├── BusinessException.java
│       │   │   └── ErrorCode.java
│       │   └── audit/
│       │       └── AuditAwareImpl.java          # @CreatedBy/@LastModifiedBy uchun
│       │
│       ├── auth/                                # Auth Module
│       │   ├── api/AuthController.java
│       │   ├── application/
│       │   │   ├── AuthService.java
│       │   │   ├── JwtService.java
│       │   │   └── PasswordService.java
│       │   ├── domain/
│       │   │   ├── User.java
│       │   │   └── RefreshToken.java
│       │   ├── infrastructure/
│       │   │   ├── UserRepository.java
│       │   │   └── TokenRepository.java
│       │   └── dto/
│       │       ├── LoginRequest.java
│       │       ├── RefreshTokenRequest.java
│       │       └── AuthResponse.java
│       │
│       ├── company/                             # Company Module
│       │   ├── api/CompanyController.java
│       │   ├── application/CompanyService.java
│       │   ├── domain/
│       │   │   ├── Company.java
│       │   │   └── CompanySettings.java         # faqat: timezone, locale, plan
│       │   ├── infrastructure/CompanyRepository.java
│       │   └── dto/
│       │       ├── CreateCompanyRequest.java
│       │       ├── UpdateCompanyRequest.java
│       │       └── CompanyResponse.java
│       │
│       ├── branch/                              # Branch Module
│       │   ├── api/BranchController.java
│       │   ├── application/
│       │   │   ├── BranchService.java
│       │   │   └── BranchFacade.java            # cross-module interface
│       │   ├── domain/
│       │   │   ├── Branch.java
│       │   │   └── BranchSchedule.java          # start_time, end_time,
│       │   │                                    # late_threshold_min PER DAY
│       │   ├── infrastructure/BranchRepository.java
│       │   └── dto/
│       │       ├── CreateBranchRequest.java
│       │       ├── BranchResponse.java
│       │       └── BranchScheduleInfo.java      # Facade uchun read DTO
│       │
│       ├── employee/                            # Employee Module
│       │   ├── api/EmployeeController.java
│       │   ├── application/
│       │   │   ├── EmployeeService.java
│       │   │   ├── EmployeeQueryService.java
│       │   │   └── EmployeeFacade.java          # cross-module interface
│       │   ├── domain/Employee.java
│       │   ├── infrastructure/EmployeeRepository.java
│       │   └── dto/
│       │       ├── CreateEmployeeRequest.java
│       │       ├── UpdateEmployeeRequest.java
│       │       └── EmployeeResponse.java
│       │
│       ├── attendance/                          # Attendance Module
│       │   ├── api/
│       │   │   ├── AttendanceController.java
│       │   │   └── LeaveController.java
│       │   ├── application/
│       │   │   ├── AttendanceService.java
│       │   │   ├── AttendanceFacade.java        # cross-module interface
│       │   │   ├── GeofenceService.java
│       │   │   └── LeaveService.java
│       │   ├── domain/
│       │   │   ├── AttendanceSession.java       # bir IN/OUT jufti (UNIQUE olib tashlandi)
│       │   │   └── Leave.java
│       │   ├── infrastructure/
│       │   │   ├── AttendanceSessionRepository.java
│       │   │   └── LeaveRepository.java
│       │   └── dto/
│       │       ├── CheckInRequest.java
│       │       ├── CheckOutRequest.java
│       │       └── AttendanceResponse.java
│       │
│       ├── report/                              # Report Module
│       │   ├── api/ReportController.java
│       │   ├── application/
│       │   │   ├── ReportService.java
│       │   │   ├── ReportSnapshotScheduler.java # @Scheduled kunlik snapshot
│       │   │   ├── ExcelExportService.java
│       │   │   └── PdfExportService.java
│       │   ├── domain/ReportSnapshot.java
│       │   ├── infrastructure/ReportRepository.java
│       │   └── dto/
│       │       ├── ReportRequest.java
│       │       └── AttendanceSummary.java
│       │
│       └── device/                              # Device Module
│           ├── api/
│           │   ├── DeviceController.java
│           │   └── DeviceEventController.java
│           ├── application/
│           │   ├── DeviceService.java
│           │   ├── DeviceEventService.java
│           │   └── DeviceEventProcessor.java    # SELECT FOR UPDATE SKIP LOCKED
│           ├── adapter/
│           │   ├── DeviceAdapter.java           # interface
│           │   ├── HikvisionAdapter.java        # mock impl (TODO: ISAPI)
│           │   └── dto/HikvisionEventPayload.java
│           ├── domain/
│           │   ├── Device.java
│           │   ├── DeviceEvent.java
│           │   └── EmployeeCredential.java      # CARD/FACE/FINGERPRINT/QR
│           ├── infrastructure/
│           │   ├── DeviceRepository.java
│           │   ├── DeviceEventRepository.java
│           │   └── EmployeeCredentialRepository.java
│           └── dto/
│               ├── CreateDeviceRequest.java
│               ├── DeviceResponse.java
│               ├── DeviceEventResponse.java
│               └── IngestEventRequest.java
│
├── frontend/
│   └── src/modules/
│       ├── auth/ · company/ · branch/
│       ├── employee/ · attendance/ · report/
│       └── device/
│           ├── pages/
│           │   ├── DevicesPage.tsx
│           │   ├── DeviceDetailPage.tsx
│           │   └── DeviceEventsPage.tsx
│           ├── components/
│           │   ├── DeviceStatusBadge.tsx
│           │   ├── DeviceEventTable.tsx
│           │   └── EmployeeCredentialForm.tsx
│           └── api/deviceApi.ts
│
├── src/main/resources/db/migration/
│   ├── V1__init_auth.sql             # users, refresh_tokens
│   ├── V2__init_company_branch.sql   # companies, company_settings, branches, branch_schedules
│   ├── V3__init_employee.sql         # employees
│   ├── V4__init_device.sql           # devices, device_events, employee_credentials
│   ├── V5__init_attendance.sql       # attendance_sessions (→ devices ✓), leaves
│   └── V6__init_report.sql           # report_snapshots
│
├── docker-compose.yml
└── ARCHITECTURE.md
```

---

## 3. Module Descriptions

### 3.1 Auth Module

**Mas'uliyat:** Foydalanuvchi autentifikatsiyasi va avtorizatsiyasi. JWT access/refresh token, BCrypt parol, role-based kirish nazorati. Device'lar uchun alohida API Key mexanizmi (7-bo'lim).

**Database tables:** `users`, `refresh_tokens`

| Field | Type | Note |
|---|---|---|
| id | UUID | PK |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | BCrypt (strength=12) |
| role | ENUM | SUPER_ADMIN, COMPANY_ADMIN, BRANCH_MANAGER, EMPLOYEE |
| is_active | BOOLEAN | default true |
| created_at | TIMESTAMPTZ | NOT NULL |
| created_by | UUID | FK → users (Spring @CreatedBy) |
| updated_at | TIMESTAMPTZ | |
| updated_by | UUID | FK → users (Spring @LastModifiedBy) |

**API Endpoints:**

| Method | Path | Tavsif |
|---|---|---|
| POST | `/api/v1/auth/login` | Login, JWT qaytaradi |
| POST | `/api/v1/auth/refresh` | Access token yangilash |
| POST | `/api/v1/auth/logout` | Refresh tokenni revoke (Redis blacklist) |
| GET | `/api/v1/auth/me` | Joriy foydalanuvchi ma'lumotlari |
| PATCH | `/api/v1/auth/password` | Parol o'zgartirish |

---

### 3.2 Company Module

**Mas'uliyat:** Multi-tenant kompaniyalarni boshqarish. `CompanySettings` faqat tenant-level global parametrlarni saqlaydi: `timezone`, `locale`, obuna rejasi. Ish vaqti va kechikish chegarasi — faqat `BranchSchedule`da.

**Database tables:** `companies`, `company_settings`

`company_settings` jadvali:

| Field | Type | Note |
|---|---|---|
| id | UUID | PK |
| company_id | UUID | FK UNIQUE → companies |
| timezone | VARCHAR(50) | default 'Asia/Tashkent' |
| locale | VARCHAR(10) | default 'uz-UZ' |
| ~~work_start~~ | ~~TIME~~ | **O'CHIRILDI** → BranchSchedule ga ko'chirildi |
| ~~work_end~~ | ~~TIME~~ | **O'CHIRILDI** → BranchSchedule ga ko'chirildi |
| ~~late_threshold_min~~ | ~~INT~~ | **O'CHIRILDI** → BranchSchedule ga ko'chirildi |

**API Endpoints:**

| Method | Path | Tavsif |
|---|---|---|
| GET | `/api/v1/companies` | Barcha kompaniyalar (SuperAdmin) |
| POST | `/api/v1/companies` | Kompaniya yaratish |
| GET | `/api/v1/companies/{id}` | Bitta kompaniya |
| PUT | `/api/v1/companies/{id}` | Yangilash |
| DELETE | `/api/v1/companies/{id}` | Soft delete |
| GET | `/api/v1/companies/{id}/settings` | Sozlamalar (timezone, locale, plan) |
| PUT | `/api/v1/companies/{id}/settings` | Sozlamalarni yangilash |

---

### 3.3 Branch Module

**Mas'uliyat:** Kompaniya filiallari. GPS geofence. Ish jadvali. `BranchSchedule` — ish vaqti va kechikish chegarasining yagona manbai (Single Source of Truth). `AttendanceService` faqat shu jadvalga qaraydi.

**Database tables:** `branches`, `branch_schedules`

`branch_schedules` jadvali (kengaytirildi):

| Field | Type | Note |
|---|---|---|
| id | UUID | PK |
| branch_id | UUID | FK → branches |
| day_of_week | SMALLINT | 1=Dush … 7=Yak |
| start_time | TIME | NOT NULL — ish boshlanish vaqti |
| end_time | TIME | NOT NULL — ish tugash vaqti |
| late_threshold_min | INT | NOT NULL default 15 — kechikish chegarasi (daqiqa) |
| is_workday | BOOLEAN | default true |
| UNIQUE | (branch_id, day_of_week) | |

**BranchFacade** — cross-module interfeysi:

```java
// BranchFacade.java (branch module ichida, boshqa modullar shu interfeysi inject qiladi)
public interface BranchFacade {
    BranchScheduleInfo getScheduleForDate(UUID branchId, LocalDate date);
    boolean isWithinGeofence(UUID branchId, double lat, double lng);
}

record BranchScheduleInfo(
    LocalTime startTime,
    LocalTime endTime,
    int       lateThresholdMin,
    boolean   isWorkday
) {}
```

**API Endpoints:**

| Method | Path | Tavsif |
|---|---|---|
| GET | `/api/v1/companies/{cId}/branches` | Filiallar ro'yxati |
| POST | `/api/v1/companies/{cId}/branches` | Filial yaratish |
| GET | `/api/v1/branches/{id}` | Bitta filial |
| PUT | `/api/v1/branches/{id}` | Yangilash |
| DELETE | `/api/v1/branches/{id}` | Soft delete |
| GET | `/api/v1/branches/{id}/schedule` | Haftalik jadval |
| PUT | `/api/v1/branches/{id}/schedule` | Jadvalni yangilash |

---

### 3.4 Employee Module

**Mas'uliyat:** Xodimlar profili, lavozimi, ish shartnomasi. `EmployeeFacade` — boshqa modullar xodim ma'lumotini faqat shu interfeys orqali oladi, hech qachon `EmployeeRepository`ni to'g'ridan-to'g'ri inject qilmaydi.

**Database tables:** `employees`

`employees` jadvaliga qo'shimcha:

| Field | Type | Note |
|---|---|---|
| ... | ... | (avvalgi barcha ustunlar) |
| employee_code | VARCHAR(50) | UNIQUE — device bilan bog'lash uchun |

**EmployeeFacade** — cross-module interfeysi:

```java
// EmployeeFacade.java
public interface EmployeeFacade {
    Optional<UUID> findEmployeeIdByCode(String employeeCode);
    boolean existsAndActive(UUID employeeId);
}
```

**API Endpoints:**

| Method | Path | Tavsif |
|---|---|---|
| GET | `/api/v1/employees` | Ro'yxat (filter: branch, position, status) |
| POST | `/api/v1/employees` | Xodim yaratish (User ham yaratiladi) |
| GET | `/api/v1/employees/{id}` | Profil |
| PUT | `/api/v1/employees/{id}` | Yangilash |
| DELETE | `/api/v1/employees/{id}` | Soft delete |
| POST | `/api/v1/employees/{id}/photo` | Foto yuklash |

---

### 3.5 Attendance Module

**Mas'uliyat:** Check-in/check-out sessiyalarini qayd etish. Kechikish hisoblash (faqat `BranchFacade` orqali). GPS geofence. Ta'til so'rovlari. `AttendanceFacade` — Device moduli shu interfeys orqali chaqiradi.

**Database tables:** `attendance_sessions`, `leaves`

#### attendance_sessions jadvali

Avvalgi `attendances` jadvalidagi `UNIQUE(employee_id, date)` constraint **olib tashlandi**. Uning o'rniga bir kunda bir nechta sessiya (smena, overtime, tushlik) qo'llab-quvvatlanadi. "Bir kunda bitta active session" mantiq application darajasida boshqariladi.

| Field | Type | Note |
|---|---|---|
| id | UUID | PK |
| employee_id | UUID | FK → employees, NOT NULL |
| branch_id | UUID | FK → branches |
| date | DATE | NOT NULL — sessiya kuni |
| check_in_time | TIMESTAMPTZ | |
| check_out_time | TIMESTAMPTZ | |
| check_in_lat | DECIMAL(9,6) | |
| check_in_lng | DECIMAL(9,6) | |
| check_out_lat | DECIMAL(9,6) | |
| check_out_lng | DECIMAL(9,6) | |
| status | ENUM | PRESENT, LATE, ABSENT, LEAVE, HOLIDAY |
| late_minutes | INT | default 0 — BranchSchedule asosida |
| work_minutes | INT | default 0 — check_out - check_in |
| method | ENUM | GPS, FACE_ID, MANUAL, PIN, DEVICE |
| source_device_id | UUID | FK → devices (agar device orqali) |
| session_type | ENUM | REGULAR, OVERTIME, BREAK — kelajak uchun |
| note | TEXT | admin izoh |
| created_at | TIMESTAMPTZ | NOT NULL |
| created_by | UUID | FK → users |
| updated_at | TIMESTAMPTZ | |
| updated_by | UUID | FK → users — kim o'zgartirdi (audit) |

> `UNIQUE(employee_id, date)` **yo'q**. Bir kunda bir nechta sessiya mumkin.
> Application qoidasi: yangi check-in avval ochiq sessiya yo'qligini tekshiradi.

**AttendanceFacade** — Device moduli shu interfeys orqali chaqiradi:

```java
// AttendanceFacade.java
public interface AttendanceFacade {
    void processDeviceEvent(
        UUID      employeeId,
        Instant   eventTime,
        Direction direction,   // IN | OUT
        UUID      deviceId
    );
}

// AttendanceService.java — implements AttendanceFacade
// Kechikishni hisoblash: BranchFacade.getScheduleForDate() orqali
// Hech qachon BranchRepository yoki BranchSchedule entitysini inject qilmaydi
```

**processDeviceEvent ichki logikasi:**

```
1. employeeId + date bo'yicha ochiq sessiya (check_out=null) qidiriladi
2. direction=IN:
   a. Ochiq sessiya yo'q → yangi AttendanceSession yaratiladi
   b. Ochiq sessiya bor → DUPLICATE log, ignore (idempotent)
3. direction=OUT:
   a. Ochiq sessiya topiladi → check_out_time set, work_minutes hisoblash
   b. Ochiq sessiya yo'q → orphan OUT log, processingError qaytariladi
4. late_minutes: BranchFacade.getScheduleForDate() → startTime bilan taqqoslash
5. status: PRESENT yoki LATE
```

**API Endpoints:**

| Method | Path | Tavsif |
|---|---|---|
| POST | `/api/v1/attendance/check-in` | Kelish qaydlash |
| POST | `/api/v1/attendance/check-out` | Ketish qaydlash |
| GET | `/api/v1/attendance` | Ro'yxat (filter: date, employee, branch) |
| GET | `/api/v1/attendance/today` | Bugungi barcha sessiyalar |
| PATCH | `/api/v1/attendance/{id}` | Admin tomonidan to'g'irlash (updated_by yoziladi) |
| GET | `/api/v1/attendance/employee/{empId}` | Xodim tarixi |
| POST | `/api/v1/leaves` | Ta'til so'rovi |
| PATCH | `/api/v1/leaves/{id}/approve` | Tasdiqlash / rad etish |

---

### 3.6 Report Module

**Mas'uliyat:** Davomat statistikasi, kunlik/oylik hisobotlar, Excel/PDF eksport. `ReportSnapshotScheduler` — har kuni yarim tunda `report_snapshots` jadvalini to'ldiradi. Bu yerga qadar `report_snapshots`ning qachon to'ldirilishi noaniq edi — hozir aniq belgilandi.

**Database tables:** `report_snapshots`

**ReportSnapshotScheduler:**

```
@Scheduled(cron = "0 0 1 * * *")  — har kuni soat 01:00 da
→ Kechagi kun uchun barcha kompaniya/filiallar bo'yicha snapshot hisoblaydi
→ attendance_sessions dan aggregate qiladi
→ report_snapshots ga yozadi
```

**API Endpoints:**

| Method | Path | Tavsif |
|---|---|---|
| GET | `/api/v1/reports/summary` | Dashboard uchun umumiy statistika |
| GET | `/api/v1/reports/daily` | Kunlik hisobot |
| GET | `/api/v1/reports/monthly` | Oylik hisobot |
| GET | `/api/v1/reports/employee/{id}` | Xodim bo'yicha |
| GET | `/api/v1/reports/branch/{id}` | Filial bo'yicha |
| GET | `/api/v1/reports/export/excel` | Excel (.xlsx) |
| GET | `/api/v1/reports/export/pdf` | PDF |

---

### 3.7 Device Module

**Mas'uliyat:** Fizik qurilmalardan keladigan eventlarni qabul qilish, raw holda saqlash va `AttendanceFacade` orqali qayta ishlash.

**Database tables:** `devices`, `device_events`, `employee_credentials`

#### Entities

**Device:**

| Field | Type | Note |
|---|---|---|
| id | UUID | PK |
| name | VARCHAR(100) | NOT NULL |
| serial_number | VARCHAR(100) | UNIQUE |
| ip_address | VARCHAR(45) | Hikvision/ZKTeco device IP |
| port | INT | default 80 — polling/alertStream uchun |
| username | VARCHAR(100) | polling/alertStream login; nullable |
| credentials_secret | TEXT | encrypted password yoki Vault secret reference; plain text emas |
| api_key_hash | VARCHAR(255) | faqat PUSH endpoint uchun BCrypt hash |
| connection_type | ENUM | PUSH, POLLING, ALERT_STREAM |
| last_sync_time | TIMESTAMPTZ | polling/alertStream oxirgi sync nuqtasi |
| branch_id | UUID | FK → branches |
| type | ENUM | HIKVISION, ZKTECO, SUPREMA, QR, MOBILE |
| status | ENUM | ACTIVE, INACTIVE |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

> `api_key_hash` faqat device → backend PUSH holati uchun ishlatiladi. `POLLING` yoki `ALERT_STREAM` holatida backend device'ga ulanadi, shuning uchun credential plain text emas, encrypted secret yoki Vault reference sifatida saqlanadi.

**DeviceEvent:**

| Field | Type | Note |
|---|---|---|
| id | UUID | PK |
| device_id | UUID | FK → devices |
| external_event_id | VARCHAR(255) | device tomonidan berilgan ID; nullable bo'lishi mumkin |
| event_hash | VARCHAR(64) | NOT NULL — backend generate qiladigan stable dedup hash |
| employee_code | VARCHAR(100) | employeeCode orqali employee topish |
| credential_value | VARCHAR(255) | cardNo, faceId va h.k. — EmployeeCredential bilan moslashtirish |
| event_time | TIMESTAMPTZ | NOT NULL |
| direction | ENUM | IN, OUT, UNKNOWN |
| auth_type | ENUM | CARD, FACE, FINGERPRINT, QR |
| raw_payload | JSONB | original JSON — o'zgartirilmaydi |
| processed | BOOLEAN | default false |
| processing_error | TEXT | oxirgi xato xabari |
| retry_count | INT | default 0 — necha marta retry qilindi |
| created_at | TIMESTAMPTZ | |
| UNIQUE | (device_id, event_hash) | duplicate oldini olish |

> Ba'zi device loglarida stable `external_event_id` bo'lmasligi mumkin. Shuning uchun asosiy idempotency kaliti `event_hash`: `device_id + event_time + credential_value + direction + auth_type + normalized raw_payload` asosida generate qilinadi. Agar `external_event_id` mavjud bo'lsa, hash ichida ham ishlatilishi mumkin.

**EmployeeCredential** (avvalgi `EmployeeCard` o'rniga):

| Field | Type | Note |
|---|---|---|
| id | UUID | PK |
| employee_id | UUID | FK → employees |
| credential_type | ENUM | CARD, FACE, FINGERPRINT, QR |
| external_id | VARCHAR(255) | cardNo, faceTemplateId, fingerprintId va h.k. |
| active | BOOLEAN | default true |
| created_at | TIMESTAMPTZ | |
| UNIQUE | (credential_type, external_id) | bir xil credential ikkita xodimda bo'lmasin |

> `EmployeeCard` → `EmployeeCredential` sababi: Face va Fingerprint auth qo'shilganda yangi jadval ochish o'rniga bitta universal model. Lookup: `findByCredentialTypeAndExternalIdAndActiveTrue()` — barcha auth turlari uchun ishlaydi.

#### DeviceEventProcessor — Cluster-safe

```java
// SELECT FOR UPDATE SKIP LOCKED — cluster da parallel instance bo'lsa
// ikkita instance bir xil eventni olmaydi
@Transactional
public void processUnprocessedBatch() {
    List<DeviceEvent> events = eventRepository
        .findUnprocessedWithLock(batchSize);   // SKIP LOCKED
    events.forEach(e -> processEvent(e.getId()));
}

// resolveEmployee — faqat EmployeeCredentialRepository va EmployeeFacade
private UUID resolveEmployee(DeviceEvent event) {
    // 1. credential_value + auth_type → EmployeeCredential
    if (event.getCredentialValue() != null && event.getAuthType() != null) {
        return credentialRepository
            .findByCredentialTypeAndExternalIdAndActiveTrue(
                event.getAuthType().toCredentialType(),
                event.getCredentialValue())
            .map(EmployeeCredential::getEmployeeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CREDENTIAL_NOT_FOUND));
    }
    // 2. employeeCode → EmployeeFacade (cross-module, Repository inject EMAS)
    if (event.getEmployeeCode() != null) {
        return employeeFacade.findEmployeeIdByCode(event.getEmployeeCode())
            .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }
    throw new BusinessException(ErrorCode.EMPLOYEE_IDENTIFIER_MISSING);
}
```

#### Business Rules

| Qoida | Amalga oshirish |
|---|---|
| Event duplicate bo'lmasin | `event_hash` generate qilinadi va `UNIQUE(device_id, event_hash)` bilan himoyalanadi |
| `external_event_id` bo'lmasa ham event saqlansin | `external_event_id` nullable, lekin `event_hash` NOT NULL |
| Device `INACTIVE` bo'lsa event rejected | `DeviceEventService.ingest()` status check |
| PUSH device autentifikatsiyasi | `X-Device-Api-Key` header → BCrypt verify |
| POLLING/ALERT_STREAM autentifikatsiyasi | Backend device'ga ulanadi; password encrypted secret yoki Vault orqali olinadi |
| Employee credential orqali topilsin | `EmployeeCredentialRepository` — device module ichida |
| Employee code orqali topilsin | `EmployeeFacade` — cross-module, Repository inject yo'q |
| Attendance logic faqat `AttendanceFacade` orqali | Interface dependency, implementation hidden |
| Cluster-safe processing | `SELECT FOR UPDATE SKIP LOCKED` |
| Open session race condition | DB partial unique index: `uq_open_attendance_session` |
| Retry count tracking | `retry_count` ustuni — monitoring uchun |
| Xato bo'lsa `processingError` ga | `event.markFailed(msg)` + `retry_count++` |

#### API Endpoints

| Method | Path | Tavsif |
|---|---|---|
| POST | `/api/v1/devices` | Yangi device qo'shish |
| GET | `/api/v1/devices` | Device ro'yxati |
| GET | `/api/v1/devices/{id}` | Bitta device |
| PATCH | `/api/v1/devices/{id}/status` | ACTIVE / INACTIVE toggle |
| POST | `/api/v1/device-events` | Event yuklash (device push yoki manual) |
| GET | `/api/v1/device-events` | Ro'yxat (filter: deviceId, date, processed) |
| GET | `/api/v1/device-events/unprocessed` | Retry kutayotganlar |
| POST | `/api/v1/device-events/{id}/process` | Bitta eventni qayta ishlash |
| GET | `/api/v1/employee-credentials` | Credential ro'yxati |
| POST | `/api/v1/employee-credentials` | Credential biriktirish (CARD/FACE/FINGERPRINT/QR) |
| DELETE | `/api/v1/employee-credentials/{id}` | Bekor qilish |

---

## 4. Database Schema

```sql
-- ============================================================
-- AUTH
-- ============================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL
                  CHECK (role IN ('SUPER_ADMIN','COMPANY_ADMIN','BRANCH_MANAGER','EMPLOYEE')),
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    UUID         REFERENCES users(id),
    updated_at    TIMESTAMPTZ,
    updated_by    UUID         REFERENCES users(id)
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(512) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- COMPANY
-- company_settings: faqat timezone, locale — ish vaqti YO'Q
-- ============================================================
CREATE TABLE companies (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    legal_name   VARCHAR(255),
    inn          VARCHAR(20)  UNIQUE,
    phone        VARCHAR(20),
    email        VARCHAR(255),
    logo_url     TEXT,
    plan         VARCHAR(20)  NOT NULL DEFAULT 'FREE'
                 CHECK (plan IN ('FREE','BASIC','PRO')),
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    owner_id     UUID         REFERENCES users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   UUID         REFERENCES users(id),
    updated_at   TIMESTAMPTZ,
    updated_by   UUID         REFERENCES users(id)
);

CREATE TABLE company_settings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID        NOT NULL UNIQUE REFERENCES companies(id) ON DELETE CASCADE,
    timezone    VARCHAR(50) NOT NULL DEFAULT 'Asia/Tashkent',
    locale      VARCHAR(10) NOT NULL DEFAULT 'uz-UZ'
    -- work_start, work_end, late_threshold_min — BU YERDA YO'Q
    -- Yagona manba: branch_schedules jadvali
);

-- ============================================================
-- BRANCH
-- branch_schedules: start_time, end_time, late_threshold_min
-- Kechikish hisoblashning YAGONA manbai
-- ============================================================
CREATE TABLE branches (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID         NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    address        TEXT,
    latitude       DECIMAL(9,6),
    longitude      DECIMAL(9,6),
    radius_meters  INT          NOT NULL DEFAULT 100,
    is_active      BOOLEAN      NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     UUID         REFERENCES users(id),
    updated_at     TIMESTAMPTZ,
    updated_by     UUID         REFERENCES users(id)
);

CREATE TABLE branch_schedules (
    id                  UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id           UUID     NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    day_of_week         SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time          TIME     NOT NULL,
    end_time            TIME     NOT NULL,
    late_threshold_min  INT      NOT NULL DEFAULT 15,  -- kechikish chegarasi
    is_workday          BOOLEAN  NOT NULL DEFAULT true,
    UNIQUE (branch_id, day_of_week)
);

-- ============================================================
-- EMPLOYEE
-- ============================================================
CREATE TABLE employees (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         UNIQUE REFERENCES users(id),
    company_id       UUID         NOT NULL REFERENCES companies(id),
    branch_id        UUID         REFERENCES branches(id),
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    middle_name      VARCHAR(100),
    phone            VARCHAR(20),
    photo_url        TEXT,
    position         VARCHAR(100),
    employee_code    VARCHAR(50)  UNIQUE,
    hired_date       DATE,
    birth_date       DATE,
    employment_type  VARCHAR(20)  NOT NULL DEFAULT 'FULL_TIME'
                     CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT')),
    salary           DECIMAL(15,2),
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       UUID         REFERENCES users(id),
    updated_at       TIMESTAMPTZ,
    updated_by       UUID         REFERENCES users(id)
);

-- ============================================================
-- DEVICE  (V4__init_device.sql)
-- devices oldin yaratilishi kerak — attendance_sessions.source_device_id
-- devices(id) ga FK qiladi. Shuning uchun V4 (device) V5 (attendance) dan oldin.
-- ============================================================
CREATE TABLE devices (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL,
    serial_number       VARCHAR(100) UNIQUE,
    ip_address          VARCHAR(45),
    port                INT          NOT NULL DEFAULT 80,
    username            VARCHAR(100),          -- polling/alertStream login
    credentials_secret  TEXT,                  -- encrypted password yoki Vault secret reference
    api_key_hash        VARCHAR(255),          -- faqat PUSH uchun X-Device-Api-Key BCrypt hash
    connection_type     VARCHAR(20)  NOT NULL DEFAULT 'PUSH'
                        CHECK (connection_type IN ('PUSH','POLLING','ALERT_STREAM')),
    last_sync_time      TIMESTAMPTZ,
    branch_id           UUID         REFERENCES branches(id),
    type                VARCHAR(20)  NOT NULL
                        CHECK (type IN ('HIKVISION','ZKTECO','SUPREMA','QR','MOBILE')),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          UUID         REFERENCES users(id),
    updated_at          TIMESTAMPTZ,
    updated_by          UUID         REFERENCES users(id),
    CONSTRAINT chk_device_push_key CHECK (
        connection_type <> 'PUSH' OR api_key_hash IS NOT NULL
    )
);

CREATE TABLE device_events (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id           UUID         NOT NULL REFERENCES devices(id),
    external_event_id   VARCHAR(255),          -- nullable: hamma device stable ID bermaydi
    event_hash          VARCHAR(64)  NOT NULL, -- backend generate qiladigan stable SHA-256 hash
    employee_code       VARCHAR(100),
    credential_value    VARCHAR(255),          -- cardNo, faceId va h.k.
    event_time          TIMESTAMPTZ  NOT NULL,
    direction           VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN'
                        CHECK (direction IN ('IN','OUT','UNKNOWN')),
    auth_type           VARCHAR(20)
                        CHECK (auth_type IN ('CARD','FACE','FINGERPRINT','QR')),
    raw_payload         JSONB,                 -- original, o'zgartirilmaydi
    processed           BOOLEAN      NOT NULL DEFAULT false,
    processing_error    TEXT,
    retry_count         INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_device_event_hash UNIQUE (device_id, event_hash)
);

CREATE TABLE employee_credentials (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id      UUID         NOT NULL REFERENCES employees(id),
    credential_type  VARCHAR(20)  NOT NULL
                     CHECK (credential_type IN ('CARD','FACE','FINGERPRINT','QR')),
    external_id      VARCHAR(255) NOT NULL,  -- cardNo, faceTemplateId va h.k.
    active           BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       UUID         REFERENCES users(id),
    CONSTRAINT uq_credential_type_id UNIQUE (credential_type, external_id)
);

-- ============================================================
-- ATTENDANCE  (V5__init_attendance.sql)
-- devices V4 da yaratilgan, shuning uchun source_device_id FK ishlaydi ✓
-- UNIQUE(employee_id, date) YO'Q — smena, overtime, night shift uchun
-- updated_by — kim o'zgartirdi (audit minimum)
-- ============================================================
CREATE TABLE attendance_sessions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id      UUID        NOT NULL REFERENCES employees(id),
    branch_id        UUID        REFERENCES branches(id),
    date             DATE        NOT NULL,
    check_in_time    TIMESTAMPTZ,
    check_out_time   TIMESTAMPTZ,
    check_in_lat     DECIMAL(9,6),
    check_in_lng     DECIMAL(9,6),
    check_out_lat    DECIMAL(9,6),
    check_out_lng    DECIMAL(9,6),
    status           VARCHAR(20) NOT NULL DEFAULT 'ABSENT'
                     CHECK (status IN ('PRESENT','LATE','ABSENT','LEAVE','HOLIDAY')),
    late_minutes     INT         NOT NULL DEFAULT 0,
    work_minutes     INT         NOT NULL DEFAULT 0,
    method           VARCHAR(20) CHECK (method IN ('GPS','FACE_ID','MANUAL','PIN','DEVICE')),
    source_device_id UUID        REFERENCES devices(id),
    session_type     VARCHAR(20) NOT NULL DEFAULT 'REGULAR'
                     CHECK (session_type IN ('REGULAR','OVERTIME','BREAK')),
    note             TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       UUID        REFERENCES users(id),
    updated_at       TIMESTAMPTZ,
    updated_by       UUID        REFERENCES users(id)
    -- UNIQUE(employee_id, date) YO'Q: application logic boshqaradi
);

CREATE TABLE leaves (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id  UUID        NOT NULL REFERENCES employees(id),
    type         VARCHAR(20) NOT NULL
                 CHECK (type IN ('SICK','ANNUAL','UNPAID','OTHER')),
    start_date   DATE        NOT NULL,
    end_date     DATE        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    reason       TEXT,
    approved_by  UUID        REFERENCES users(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID        REFERENCES users(id)
);

-- ============================================================
-- REPORT  (V6__init_report.sql)
-- ReportSnapshotScheduler: @Scheduled(cron="0 0 1 * * *")
-- Har kuni soat 01:00 da kechagi kun uchun hisoblaydi
-- ============================================================
CREATE TABLE report_snapshots (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID        NOT NULL REFERENCES companies(id),
    branch_id         UUID        REFERENCES branches(id),
    period_type       VARCHAR(20) NOT NULL
                      CHECK (period_type IN ('DAILY','WEEKLY','MONTHLY')),
    period_start      DATE        NOT NULL,
    period_end        DATE        NOT NULL,
    total_employees   INT         NOT NULL DEFAULT 0,
    present_count     INT         NOT NULL DEFAULT 0,
    late_count        INT         NOT NULL DEFAULT 0,
    absent_count      INT         NOT NULL DEFAULT 0,
    avg_work_minutes  INT         NOT NULL DEFAULT 0,
    generated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    generated_by      VARCHAR(50) NOT NULL DEFAULT 'SCHEDULER'
                      CHECK (generated_by IN ('SCHEDULER','MANUAL'))
);

-- ============================================================
-- INDEXES
-- ============================================================
-- Attendance (sessions)
CREATE INDEX idx_att_sessions_emp_date    ON attendance_sessions(employee_id, date);
CREATE INDEX idx_att_sessions_branch_date ON attendance_sessions(branch_id, date);
CREATE INDEX idx_att_sessions_date        ON attendance_sessions(date);
CREATE UNIQUE INDEX uq_open_attendance_session
    ON attendance_sessions(employee_id)
    WHERE check_out_time IS NULL;            -- race condition: bitta employee = bitta open session

-- Employee
CREATE INDEX idx_employees_company        ON employees(company_id);
CREATE INDEX idx_employees_branch         ON employees(branch_id);
CREATE INDEX idx_employees_code           ON employees(employee_code)
    WHERE employee_code IS NOT NULL;

-- Leaves
CREATE INDEX idx_leaves_employee          ON leaves(employee_id);

-- Auth
CREATE INDEX idx_refresh_tokens_user      ON refresh_tokens(user_id);

-- Device
CREATE INDEX idx_device_events_device     ON device_events(device_id);
CREATE INDEX idx_device_events_time       ON device_events(event_time);
CREATE INDEX idx_device_events_external   ON device_events(device_id, external_event_id)
    WHERE external_event_id IS NOT NULL;
CREATE INDEX idx_device_events_unproc     ON device_events(processed, retry_count)
    WHERE processed = false;               -- SKIP LOCKED query uchun
CREATE INDEX idx_credentials_lookup       ON employee_credentials(credential_type, external_id)
    WHERE active = true;
```

---

## 5. API Overview

Barcha endpointlar `/api/v1` prefiksi bilan boshlanadi.

**Standart response format:**

```json
{
  "success": true,
  "data": {},
  "message": "OK",
  "errors": null,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

**Xato formati:**

```json
{
  "success": false,
  "data": null,
  "message": "Validation failed",
  "errors": [
    { "field": "credentialType", "message": "To'ldirilishi shart" }
  ],
  "timestamp": "2025-01-15T10:30:00Z"
}
```

**Sahifalash:**

```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

---

## 6. Inter-Module Communication

**Qoida: hech bir modul boshqa modulning `@Repository` beanini inject qilmaydi. Faqat Facade interface yoki ApplicationEvent.**

```
┌──────────────┐      BranchFacade        ┌──────────────┐
│  Attendance  │ ────────────────────────► │    Branch    │
│   Module     │                           │    Module    │
└──────┬───────┘                           └──────────────┘
       │
       │  AttendanceFacade
       ▼
┌──────────────┐      EmployeeFacade       ┌──────────────┐
│    Device    │ ────────────────────────► │   Employee   │
│    Module    │                           │    Module    │
└──────────────┘      AttendanceFacade     └──────────────┘
       │ ─────────────────────────────────►┌──────────────┐
       │                                   │  Attendance  │
       │                                   │    Module    │
       │                                   └──────────────┘
       │
       └── EmployeeCredentialRepository (device module ichida, cross yo'q)
```

**Facade pattern:**

```java
// Har modul o'z Facade interface'ini e'lon qiladi
// Implement qilish — o'sha modulning application/ ichida
// Boshqa modul faqat interface'ni import qiladi, implementation'ni emas

// Misol: Device → Employee
@Autowired EmployeeFacade employeeFacade;  // TO'G'RI
@Autowired EmployeeRepository empRepo;      // XATO — modul chegarasi buzildi
```

**ApplicationEvent (write-side uchun):**

```java
// Attendance yozilganda Report moduli xabardor bo'ladi
public record AttendanceSessionCreatedEvent(UUID employeeId, LocalDate date) {}

// Report moduli tinglab real-time snapshot invalidate qiladi
@EventListener
public void onAttendanceCreated(AttendanceSessionCreatedEvent event) { ... }
```

---

## 7. Security

**JWT konfiguratsiyasi:**

| Parametr | Qiymat |
|---|---|
| Access token TTL | 15 daqiqa |
| Refresh token TTL | 7 kun |
| Algoritm | HS256 |
| Parol xeshlash | BCrypt (strength=12) |
| Logout | Redis blacklist (TTL = access token TTL) |

**Role va ruxsatlar:**

| Role | Ruxsatlar |
|---|---|
| SUPER_ADMIN | Barcha kompaniyalar, devicelar, foydalanuvchilar |
| COMPANY_ADMIN | O'z kompaniyasi: filiallar, xodimlar, devicelar, hisobotlar |
| BRANCH_MANAGER | O'z filiali: xodimlar, device eventlari ko'rish, davomat to'g'irlash |
| EMPLOYEE | Faqat o'z sessiyalari, check-in/out |

**Device autentifikatsiyasi:**

Device'lar foydalanuvchi JWT bilan emas, integratsiya turiga qarab alohida mexanizm bilan ishlaydi.

**PUSH mode:** device backendga event yuboradi.

```
Device → POST /api/v1/device-events
         Header: X-Device-Serial: HIK-001
         Header: X-Device-Api-Key: <plain-text-key>

Server:  devices jadvalidan serial_number bo'yicha topadi
         BCrypt.verify(plain, device.apiKeyHash)
         device.status == ACTIVE tekshiradi
         device.connectionType == PUSH tekshiradi
```

API Key admin tomonidan `POST /api/v1/devices/{id}/rotate-key` orqali yaratiladi va bir marta plain-text ko'rsatiladi, keyin hash saqlanadi (xuddi parol kabi).

**POLLING / ALERT_STREAM mode:** backend device'ga ulanadi.

```
Backend Scheduler/Listener → Hikvision ISAPI / alertStream
Device config: ip_address, port, username, credentials_secret
```

`credentials_secret` plain password emas. U encrypted value yoki Vault/secret-manager reference bo'lishi kerak. Production'da secret rotation va audit log alohida nazorat qilinadi.

**Endpoint himoya misollari:**

```java
// Faqat JWT bilan
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','BRANCH_MANAGER')")
GET /api/v1/device-events

// Device API Key bilan (DeviceApiKeyFilter orqali)
POST /api/v1/device-events   ← X-Device-Api-Key header

// Ikkisi ham qabul qilinadi (manual test uchun)
@PreAuthorize("hasAnyRole('COMPANY_ADMIN') or hasAuthority('DEVICE_PUSH')")
POST /api/v1/device-events
```

`DEVICE_PUSH` authority — device muvaffaqiyatli API Key bilan autentifikatsiya qilganda `SecurityContext`ga qo'shiladi. Alohida User yaratilmaydi.

---

## 8. Infrastructure & Docker

**docker-compose.yml:**

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    container_name: attendance_db
    environment:
      POSTGRES_DB: attendance_db
      POSTGRES_USER: attendance_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U attendance_user -d attendance_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: attendance_redis
    command: redis-server --requirepass ${REDIS_PASSWORD}
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: attendance_backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/attendance_db
      SPRING_DATASOURCE_USERNAME: attendance_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      DEVICE_PROCESSOR_INTERVAL_MS: 60000
      DEVICE_PROCESSOR_BATCH_SIZE: 100
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: attendance_frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  postgres_data:
  redis_data:
```

**application.yml:**

```yaml
spring:
  application:
    name: attendance-system
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  data:
    redis:
      host: ${SPRING_REDIS_HOST:localhost}
      port: 6379
      password: ${SPRING_REDIS_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  access-token-ttl: 900        # 15 daqiqa (seconds)
  refresh-token-ttl: 604800    # 7 kun (seconds)

device:
  processor:
    interval-ms: 60000         # batch scheduler interval
    batch-size: 100            # bir marta nechta event
  hikvision:
    connection-timeout: 5000
    read-timeout: 10000

report:
  snapshot:
    cron: "0 0 1 * * *"        # har kuni soat 01:00
```

---

## 9. Frontend Structure

```
frontend/src/
├── modules/
│   ├── auth/
│   │   ├── pages/LoginPage.tsx
│   │   └── hooks/useAuth.ts
│   ├── dashboard/
│   │   ├── pages/DashboardPage.tsx
│   │   └── components/StatsCard.tsx, AttendanceChart.tsx
│   ├── company/
│   │   ├── pages/CompaniesPage.tsx, CompanyDetailPage.tsx
│   │   └── api/companyApi.ts
│   ├── branch/
│   │   ├── pages/BranchesPage.tsx
│   │   └── components/BranchMap.tsx, ScheduleEditor.tsx
│   ├── employee/
│   │   ├── pages/EmployeesPage.tsx, EmployeeProfilePage.tsx
│   │   └── components/EmployeeTable.tsx, EmployeeForm.tsx
│   ├── attendance/
│   │   ├── pages/AttendancePage.tsx, CheckInPage.tsx
│   │   └── components/SessionList.tsx, StatusBadge.tsx
│   ├── report/
│   │   ├── pages/ReportsPage.tsx
│   │   └── components/ReportTable.tsx, ExportButtons.tsx
│   └── device/
│       ├── pages/
│       │   ├── DevicesPage.tsx
│       │   ├── DeviceDetailPage.tsx
│       │   └── DeviceEventsPage.tsx
│       ├── components/
│       │   ├── DeviceStatusBadge.tsx
│       │   ├── DeviceEventTable.tsx
│       │   └── EmployeeCredentialForm.tsx  # CARD/FACE/FINGERPRINT/QR
│       └── api/deviceApi.ts
│
└── shared/
    ├── components/    Button, Input, Modal, Table, Badge, Pagination
    ├── hooks/         useDebounce, usePagination, usePermission
    ├── api/           axiosInstance.ts, queryClient.ts
    ├── types/         index.ts
    └── utils/         formatDate, formatMinutes, formatDuration
```

---

## 10. ADRs — Architecture Decision Records

### ADR-001: Modular Monolith vs Microservices

**Qaror:** Modular Monolith
**Sabab:** MVP bosqichida xarajat va murakkablikni minimallashtirish. Modullar chegarasi Facade interface orqali aniq bo'lgani uchun kelajakda microservicega migration oson — har bir modul o'z Facade'ini API'ga aylantirish yetarli.

---

### ADR-002: UUID birlamchi kalit

**Qaror:** UUID (`gen_random_uuid()`)
**Sabab:** Multi-tenant izolyatsiya uchun qulay. Tashqi tizimlar (device, HR) bilan integratsiyada ID to'qnashuvi yo'q. BIGSERIAL bilan taqqoslaganda insert performance minimal farq qiladi (PostgreSQL 16 da).

---

### ADR-003: Soft Delete

**Qaror:** Barcha asosiy entitylarda `is_active = false`
**Sabab:** Davomat tarixi, audit, huquqiy talablar — ma'lumot o'chirilmasligi shart. Cascade delete faqat `ON DELETE CASCADE` bilan belgilangan bog'liqlik uchun (masalan, `refresh_tokens`).

---

### ADR-004: Flyway migratsiyasi

**Qaror:** Flyway, `ddl-auto: validate`
**Sabab:** Production'da JPA avtomatik DDL o'zgartirmasligi kerak. Barcha schema o'zgarishlari versiyalanadi, rollback mumkin, CI/CD ga integratsiya oson.

---

### ADR-005: Redis token blacklist

**Qaror:** Logout qilingan access tokenlar Redis'da saqlanadi (TTL = token TTL)
**Sabab:** JWT stateless, lekin logout va kompromiss holatida tokenni revoke qilish kerak. Redis'da key = `token_hash`, TTL = qolgan umri — avtomatik tozalanadi.

---

### ADR-006: Device Event raw storage

**Qaror:** Device eventlari avval `device_events` jadvaliga raw holda saqlanadi, keyin alohida processor qayta ishlaydi.
**Sabab:** Device va attendance logic mustaqil. Xato bo'lsa event yo'qolmaydi. Retry imkoni bor. Original payload audit uchun har doim saqlanadi. `retry_count` monitoring uchun qo'shildi.

---

### ADR-007: DeviceAdapter interface

**Qaror:** Har bir device turi `DeviceAdapter` interfeysi orqali implement qilinadi.
**Sabab:** Open/Closed principle — Hikvision bugun, ZKTeco ertaga, kod o'zgarmaydi. Adapter ro'yxati Spring'da `Map<DeviceType, DeviceAdapter>` sifatida inject qilinadi.

---

### ADR-008: JSONB for rawPayload

**Qaror:** `raw_payload` ustuni `JSONB`
**Sabab:** Har xil device'lar turli formatda yuboradi. JSONB PostgreSQL'da GIN indeks va `->` operator bilan query imkonini beradi. Kelajakda payload structurasini o'zgartirmasdan yangi maydonlar qo'shish mumkin.

---

### ADR-009: Scheduler vs Message Queue

**Qaror:** MVP uchun `@Scheduled` + `SELECT FOR UPDATE SKIP LOCKED`
**Sabab:** Kafka/RabbitMQ MVP ga nomutanosib infra va ops xarajati. `SKIP LOCKED` cluster deployment uchun race condition'ni hal qiladi — ikkita instance parallel ishlaganda bir event bir marta olinadi.

**Migration trigger (qachon Message Queue kerak bo'ladi):**
- Sub-10-second latency talabi yuzaga kelsa
- 3 yoki undan ortiq backend instance deploy qilinsa
- Event hajmi kuniga 1 million dan oshsa

Shu belgilar ko'ringanda ADR-009 qayta ko'rib chiqiladi va Outbox pattern yoki dedicated message broker tanlovi amalga oshiriladi.

---

### ADR-010: BranchSchedule — Single Source of Truth

**Qaror:** Ish vaqti (`start_time`, `end_time`) va kechikish chegarasi (`late_threshold_min`) faqat `branch_schedules` jadvalida saqlanadi.
**Sabab:** `CompanySettings`da ham saqlanishi ikkita manba — qaysi biri ustunlik qilishi noaniq va latent bug. `AttendanceService` faqat `BranchFacade.getScheduleForDate()` ni chaqiradi, hech qachon `CompanySettings`ga qaramaydi.

---

### ADR-011: attendance_sessions — UNIQUE constraint yo'q

**Qaror:** `attendance_sessions` jadvalida `UNIQUE(employee_id, date)` constraint yo'q.
**Sabab:** Smena ishi, overtime, tushlik tanaffus, night shift — barchasi bir kunda bir nechta sessiya talab qiladi. "Bir kunda bitta ochiq sessiya" qoidasi application darajasida (`check_out IS NULL` query) boshqariladi, DB constraint darajasida emas. Bu kelajakdagi use-case'larni bloklamaydi.

---

### ADR-012: EmployeeCredential — Universal credential model

**Qaror:** `EmployeeCard` o'rniga `EmployeeCredential(credential_type, external_id)`
**Sabab:** CARD faqat emas, FACE va FINGERPRINT ham keladi. Bitta jadval, bitta `findByCredentialTypeAndExternalId()` query — barcha auth turlari uchun ishlaydi. `EmployeeCard` nomi misleading bo'lib qoladi. `UNIQUE(credential_type, external_id)` — bir credential ikkita xodimda bo'lmaydi.

---

### ADR-013: Facade pattern — modul chegarasi

**Qaror:** Modullar araro muloqot faqat Facade interface va ApplicationEvent orqali.
**Sabab:** Bir modul boshqa modulning `@Repository`ini inject qilsa — modul chegarasi buziladi, coupling oshadi, kelajakda microservice ajratish qiyinlashadi. `BranchFacade`, `EmployeeFacade`, `AttendanceFacade` — har biri o'z modulida declare qilinadi, implement qilinadi va boshqa modullar faqat interface'ni ko'radi.

---

### ADR-014: BaseEntity — created_by / updated_by

**Qaror:** Barcha asosiy entitylarda `created_by` va `updated_by` (UUID) ustunlari. Spring `@CreatedBy` / `@LastModifiedBy` annotatsiyalari `AuditAwareImpl` orqali to'ldiriladi.
**Sabab:** Attendance correction, manual override, device qo'shish — kim qildi? `updatedAt` bor lekin `updatedBy` yo'q edi. Minimal xarajat (Spring Auditing allaqachon konfiguratsiyalangan), maksimal foyda — audit trail uchun asosiy talabni qoplash.

---

### ADR-015: Flyway Migration Order — Device before Attendance

**Qaror:** Migration tartibi: V1=auth → V2=company_branch → V3=employee → **V4=device** → **V5=attendance** → V6=report.

**Sabab:** `attendance_sessions.source_device_id UUID REFERENCES devices(id)` FK mavjud. PostgreSQL Flyway migration paytida FK'ni darhol tekshiradi — `devices` jadvali yaratilmagan bo'lsa `ERROR: relation "devices" does not exist` beradi. Variant A tanlandi: migration tartibini o'zgartirish — toza, alohida patch migration kerak emas.

**Tekshirilgan barcha FK zanjirlar:**

| Jadval | FK | Migration | Holat |
|---|---|---|---|
| `users.created_by` | `users(id)` self-ref | V1 | ✓ NULL allowed, PostgreSQL self-ref qo'llab-quvvatlaydi |
| `refresh_tokens.user_id` | `users(id)` | V1 | ✓ |
| `companies.*` | `users(id)` | V2 → V1 | ✓ |
| `branches.*` | `companies(id)`, `users(id)` | V2 → V1,V2 | ✓ |
| `employees.*` | `users(id)`, `companies(id)`, `branches(id)` | V3 → V1,V2 | ✓ |
| `devices.*` | `branches(id)`, `users(id)` | V4 → V2,V1 | ✓ |
| `device_events.device_id` | `devices(id)` | V4 → V4 (same migration) | ✓ |
| `employee_credentials.employee_id` | `employees(id)` | V4 → V3 | ✓ |
| `attendance_sessions.employee_id` | `employees(id)` | V5 → V3 | ✓ |
| `attendance_sessions.source_device_id` | `devices(id)` | V5 → V4 | ✓ **FIXED** |
| `report_snapshots.company_id` | `companies(id)` | V6 → V2 | ✓ |

---

### ADR-016: Device Connection Modes — PUSH, POLLING, ALERT_STREAM

**Qaror:** `devices.connection_type` uchta rejimni qo'llab-quvvatlaydi: `PUSH`, `POLLING`, `ALERT_STREAM`.

**Sabab:** Hikvision va boshqa access control qurilmalari har doim backend endpointiga push qila olmaydi. Ba'zi holatda backend ISAPI orqali loglarni polling qiladi, ba'zi holatda alert stream connection ochib real-time event oladi. Shu sababli device model faqat `api_key_hash` bilan cheklanmaydi.

**Security:** `api_key_hash` faqat `PUSH` uchun. `POLLING` va `ALERT_STREAM` uchun `username` va `credentials_secret` ishlatiladi. `credentials_secret` plain text emas — encrypted value yoki Vault/secret-manager reference.

---

### ADR-017: Device Event Deduplication — event_hash

**Qaror:** `external_event_id` nullable, asosiy idempotency kaliti esa `event_hash`. DB constraint: `UNIQUE(device_id, event_hash)`.

**Sabab:** Hamma device loglarida stable external event ID bo'lmasligi mumkin. Backend `device_id + event_time + credential_value + direction + auth_type + normalized raw_payload` asosida stable SHA-256 hash generate qiladi. Bu push, polling va alert stream rejimlarida duplicate eventlarni bir xil usulda aniqlashga imkon beradi.

---

### ADR-018: Open Attendance Session Race Protection

**Qaror:** `attendance_sessions` uchun partial unique index qo'shildi:

```sql
CREATE UNIQUE INDEX uq_open_attendance_session
ON attendance_sessions(employee_id)
WHERE check_out_time IS NULL;
```

**Sabab:** `UNIQUE(employee_id, date)` olib tashlangan, chunki bir kunda bir nechta sessiya bo'lishi mumkin. Lekin bir xodimda bir vaqtning o'zida faqat bitta open session bo'lishi kerak. Parallel `IN` eventlar kelganda application check yetarli emas; DB darajasidagi partial unique index race condition'ni bloklaydi.

---

## 11. Changelog

| Versiya | O'zgarish |
|---|---|
| v1.0 | MVP: Auth, Company, Branch, Employee, Attendance, Report |
| v1.1 | Device Integration: Device, DeviceEvent, EmployeeCard, HikvisionAdapter |
| v1.2 | Architecture review fixes: (1) CompanySettings dan work_start/work_end/late_threshold_min olib tashlandi → BranchSchedule; (2) UNIQUE(employee_id,date) olib tashlandi → attendance_sessions; (3) EmployeeCard → EmployeeCredential; (4) Facade pattern — modul chegaralari aniq belgilandi; (5) Device API Key autentifikatsiyasi aniqlashtirildi; (6) ReportSnapshotScheduler qachon ishlashi belgilandi; (7) SELECT FOR UPDATE SKIP LOCKED qo'shildi; (8) BaseEntity → created_by/updated_by; (9) retry_count qo'shildi; (10) ADR-009..014 qo'shildi |
| v1.3 | Migration order fix (Variant A): V4=device, V5=attendance, V6=report. `attendance_sessions.source_device_id REFERENCES devices(id)` endi to'g'ri ishlaydi — devices V4 da yaratiladi, attendance V5 da. Database schema bo'limidagi jadval tartibi ham migration tartibiga mos keltirildi. Barcha FK muammolari tekshirildi: users self-reference (NULL allowed, muammo yo'q ✓), employee_credentials→employees (V3 dan keyin V4 ✓), device_events→devices (V4 ichida ✓). ADR-015 qo'shildi. |
| v1.4 | Production risk fixes: (1) Device `connection_type` qo'shildi: PUSH/POLLING/ALERT_STREAM; Hikvision polling/alertStream uchun `port`, `username`, `credentials_secret`, `last_sync_time` qo'shildi; (2) `external_event_id` nullable qilindi va `event_hash` asosiy dedup key bo'ldi; (3) `UNIQUE(device_id,event_hash)` qo'shildi; (4) open attendance session race condition uchun `uq_open_attendance_session` partial unique index qo'shildi; (5) ADR-016..018 qo'shildi. |

---

*Hujjat oxiri · v1.4 · Muallif: Development Team*
