# WorkPulse Sales Demo Script

Bu script WorkPulse'ni mijozga ko'rsatishda ishlatiladi. Maqsad: mijozga mahsulot nima muammoni yechishini va admin panel qanday ishlashini oddiy tilda tushuntirish.

## 1. Opening

Assalomu alaykum. WorkPulse bu kompaniyalarda xodimlar davomatini nazorat qilish uchun web admin panel va backend tizim. Tizim kompaniya, filial, xodim, qurilma va hisobotlarni bir joyga jamlaydi.

Bugun sizga bitta oddiy flow ko'rsataman:

1. Kompaniya yaratamiz.
2. Filial qo'shamiz.
3. Xodim qo'shamiz.
4. Xodimga rasm va credential qo'shamiz.
5. Qurilma yaratamiz.
6. Xodim check-in/check-out qiladi.
7. Hisobotni ko'ramiz.

## 2. Login

Demo credential:

- Email: `admin@workpulse.uz`
- Password: `admin123`

Gapirish:

> "Tizimga faqat ruxsat berilgan admin yoki manager kira oladi. Login qilingandan keyin barcha so'rovlar token bilan himoyalanadi."

## 3. Dashboard

Gapirish:

> "Dashboard rahbar uchun asosiy ekran. Bu yerda bugungi kelganlar, kechikkanlar, xodimlar soni, aktiv qurilmalar va qayta ishlanmagan device eventlar ko'rinadi."

Mijozga savol:

> "Sizga kunlik nazoratda eng muhim metrika qaysi: kechikishmi, kelmagan xodimlarmi yoki umumiy ishlangan vaqtmi?"

## 4. Company

Demo:

- Name: `WorkPulse Demo LLC`
- INN: `123456789`
- Phone: `+998901234567`
- Email: `info@workpulse.uz`
- Address: `Tashkent, Uzbekistan`

Gapirish:

> "Har bir mijoz tizimda Company sifatida ochiladi. Company ichida filiallar, xodimlar, qurilmalar va hisobotlar yuritiladi."

## 5. Branch

Demo:

- Company: `WorkPulse Demo LLC`
- Name: `Main Office`
- Address: `Tashkent, Chilanzar`
- Latitude: `41.2995`
- Longitude: `69.2401`
- Radius meters: `100`

Gapirish:

> "Branch bu ofis yoki filial. Filialga GPS radius, manzil va ish jadvali beriladi. Keyin xodimlar shu filialga biriktiriladi."

Schedule demo:

- Monday-Friday: `09:00-18:00`, workday
- Saturday-Sunday: not workday
- Late threshold: `15 min`

Gapirish:

> "Kechikish filial jadvali bo'yicha hisoblanadi. Masalan ish 09:00 da boshlansa va threshold 15 daqiqa bo'lsa, 09:16 dan keyin tizim kechikish deb belgilaydi."

## 6. Employee

Demo:

- Company: `WorkPulse Demo LLC`
- Branch: `Main Office`
- First name: `Ali`
- Last name: `Valiyev`
- Employee code: `EMP001`
- Position: `Operator`
- Phone: `+998901234567`
- Employment type: `FULL_TIME`

Gapirish:

> "Employee code juda muhim. Qurilmadan event kelganda tizim xodimni shu kod yoki credential orqali topadi."

Photo upload:

> "Bu yerda employee rasmi yuklanadi. Hozir u profil rasmi sifatida ishlaydi. Keyingi bosqichda Face ID enrollment uchun shu yo'nalish kengaytiriladi."

## 7. Credential

Demo:

- Employee: `Ali Valiyev`
- Credential type: `CARD`
- External ID: `EMP001-CARD`
- Active: `true`

Gapirish:

> "Credential xodimni qurilma tanishi uchun kerak. Bu karta, yuz, barmoq izi yoki QR bo'lishi mumkin."

Face ID izohi:

> "Hozir Face eventni simulyatsiya qilish mumkin: DeviceEvent sahifasida `authType=FACE` tanlanadi. Real kamera orqali face matching keyingi bosqichda alohida ulanadi."

## 8. Device

Demo:

- Branch: `Main Office`
- Name: `HIK-001`
- Serial number: `HIK-DEMO-001`
- IP address: `192.168.1.10`
- Port: `80`
- Type: `HIKVISION`
- Connection type: `PUSH`
- Status: `ACTIVE`

Gapirish:

> "Device bu kamera yoki terminal. Qurilma filialga bog'lanadi, shunda event qaysi ofisdan kelgani aniq bo'ladi."

Rotate key:

> "Agar qurilma push event yuborsa, unga API key beriladi. Kalit faqat bir marta ko'rsatiladi, keyin xavfsizlik uchun hash saqlanadi."

## 9. Attendance

Manual check-in demo:

- Employee: `Ali Valiyev`
- Branch: `Main Office`
- Method: `MANUAL`
- Note: `Demo check-in`

Gapirish:

> "Manual check-in test yoki favqulodda holatlar uchun. Real hayotda esa bu event qurilmadan avtomatik keladi."

Check-out demo:

- Employee: `Ali Valiyev`
- Method: `MANUAL`
- Note: `Demo check-out`

Gapirish:

> "Check-out qilinganda tizim ishlangan daqiqalarni hisoblaydi."

## 10. Device Event

Ingest demo:

- Device: `HIK-001`
- External event ID: `EVT-001`
- Employee code: `EMP001`
- Credential value: `EMP001-CARD`
- Direction: `IN`
- Auth type: `CARD` yoki `FACE`
- Raw payload:

```json
{
  "source": "demo",
  "terminal": "HIK-001",
  "employeeCode": "EMP001"
}
```

Gapirish:

> "DeviceEvent qurilmadan kelgan xom signal. Process qilinganda bu signal davomatga aylanadi."

## 11. Reports

Gapirish:

> "Hisobotlarda rahbar umumiy, kunlik, oylik, xodim yoki filial bo'yicha davomatni ko'radi. Excel export orqali ma'lumotni tashqi hujjat sifatida olish mumkin."

Ko'rsatish:

1. Summary report
2. Daily report
3. Employee report
4. Excel export

## 12. Closing

Gapirish:

> "WorkPulse kompaniya uchun davomat jarayonini markazlashtiradi: xodim, filial, qurilma, check-in/check-out va hisobotlar bitta tizimda. MVP hozir admin panel, manual attendance, device event processing va report export flow'larini qamrab oladi. Keyingi bosqichda real Hikvision integration va real Face ID matching qo'shiladi."

