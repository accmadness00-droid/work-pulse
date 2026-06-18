# WorkPulse Screenshot Capture Checklist

Bu checklist `docs/frontend-visual-guide.md` ichidagi screenshotlarni real frontend sahifalari bilan to'ldirish uchun.

## Talablar

Backend ishlashi kerak:

```bash
docker compose up -d postgres
mvn spring-boot:run
```

Frontend ishlashi kerak:

```bash
cd frontend
npm run dev
```

Default URL:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

Login:

- Email: `admin@workpulse.uz`
- Password: `admin123`

## Screenshot Ro'yxati

| Fayl | Sahifa | URL / amal | Status |
|---|---|---|---|
| `01-login.png` | Login | `/login` | Done |
| `02-dashboard.png` | Dashboard | `/dashboard` | Done |
| `03-companies.png` | Companies list | `/companies` | Done |
| `04-company-form.png` | Company form | `/companies/new` | Done |
| `05-branches.png` | Branches list | `/branches` | Done |
| `06-branch-form.png` | Branch form | `/branches/new` | Done |
| `07-branch-schedule.png` | Branch schedule | `/branches/{id}/schedule` | Done |
| `08-employees.png` | Employees list | `/employees` | Done |
| `09-employee-form.png` | Employee form | `/employees/new` | Done |
| `10-employee-detail.png` | Employee detail | `/employees/{id}` | Done |
| `11-employee-photo-upload.png` | Employee photo upload | `/employees/{id}/edit` | Done |
| `12-credentials.png` | Credentials list | `/credentials` | Done |
| `13-credential-form.png` | Credential form | `/credentials/new?employeeId={id}` | Done |
| `14-devices.png` | Devices list | `/devices` | Done |
| `15-device-form.png` | Device form | `/devices/new` | Done |
| `16-device-detail.png` | Device detail | `/devices/{id}` | Pending demo device |
| `17-attendance.png` | Attendance list | `/attendance` | Done |
| `18-manual-check.png` | Manual check | `/attendance/manual` | Done |
| `19-attendance-detail.png` | Attendance detail | `/attendance/{id}` | Pending attendance record |
| `20-device-events.png` | Device events list | `/device-events` | Done |
| `21-device-event-ingest.png` | Device event ingest | `/device-events/new` | Done |
| `22-device-events-unprocessed.png` | Unprocessed events | `/device-events/unprocessed` | Done |
| `23-reports-summary.png` | Report summary | `/reports` | Done |
| `24-report-daily.png` | Daily report | `/reports/daily` | Done |
| `25-report-monthly.png` | Monthly report | `/reports/monthly` | Done |
| `26-report-employee.png` | Employee report | `/reports/employee` | Done |
| `27-report-branch.png` | Branch report | `/reports/branch` | Done |

## Tavsiya Qilingan Demo Flow

Screenshot olishdan oldin quyidagi demo data yaratilsa, rasmlar chiroyli va tushunarli chiqadi:

1. Company: `WorkPulse Demo LLC`
2. Branch: `Main Office`
3. Employee: `Ali Valiyev`, code `EMP001`
4. Employee photo yuklang
5. Credential: `CARD`, external ID `EMP001-CARD`
6. Device: `HIK-001`
7. Manual check-in/check-out qiling
8. DeviceEvent ingest/process qiling
9. Reports sahifalarini oching

## Saqlash Joyi

Screenshotlar shu papkaga saqlanadi:

```text
docs/assets/screenshots/
```
