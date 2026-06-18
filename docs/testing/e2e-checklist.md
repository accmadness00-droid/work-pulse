# WorkPulse MVP E2E Checklist

Use this checklist for a local smoke test without adding or changing business logic.

## Prerequisites

- Docker is running.
- PostgreSQL is started with `docker compose up -d postgres`.
- App is started with `mvn spring-boot:run`.
- Postman collection `docs/postman/workpulse-mvp.postman_collection.json` and environment `docs/postman/workpulse-local.postman_environment.json` are imported.
- Active environment is `WorkPulse Local`.

## Flow

1. Health
   - Run `Health`.
   - Expect `status = UP`.

2. Auth
   - Run `A. Auth / Login`.
   - Expect `accessToken` and `refreshToken` to be saved in the environment.
   - Run `A. Auth / Me`.
   - Run `A. Auth / Refresh`.
   - Skip `Change Password (optional - skip for demo)` during normal demo runs.

3. Company
   - Run `Create Company`.
   - Confirm `companyId` is saved.
   - Run `Get Companies`, `Get Company by ID`, `Get Company Settings`.
   - Run `Update Company Settings`.

4. Branch
   - Run `Create Branch`.
   - Confirm `branchId` is saved.
   - Run `Get Branches by Company`, `Get Branch by ID`, `Get Branch Schedule`.
   - Run `Update Branch Schedule`.

5. Employee
   - Run `Create Employee`.
   - Confirm `employeeId` is saved.
   - Run `List Employees` and `Get Employee by ID`.

6. Device
   - Run `Create Device`.
   - Confirm `deviceId` and `deviceSerialNumber` are saved.
   - Run `List Devices`.
   - Run `Rotate Device API Key`.

7. Employee Credential
   - Run `Create EmployeeCredential`.
   - Confirm `credentialId` and `credentialExternalId` are saved.
   - Run `List EmployeeCredentials`.

8. Attendance Manual
   - Run `Check In`.
   - Run `Check Out`.
   - Run `Get Attendance List`.
   - Run `Get Employee Attendance History`.

9. DeviceEvent
   - Run `Ingest IN event`.
   - Run `Ingest OUT event`.
   - Run `Process batch`.
   - Run `Get DeviceEvents`.
   - Run `Get Unprocessed DeviceEvents`.

10. Reports
    - Run `Summary report`.
    - Run `Daily report`.
    - Run `Monthly report`.
    - Run `Employee report`.
    - Run `Branch report`.
    - Run `Excel export` and confirm a non-empty `.xlsx` response is returned.

## Expected Result

- All main requests return HTTP 200.
- IDs and tokens are stored in the Postman environment automatically.
- Manual attendance and device-event attendance both appear in report data.
- Excel export downloads an `.xlsx` file.

## Notes

- If a duplicate open attendance session error appears, run the matching check-out request or create a fresh employee for the demo.
- If login fails, make sure the app is running with the `dev` or `local` profile so the local `admin@workpulse.uz / admin123` seed user exists.
