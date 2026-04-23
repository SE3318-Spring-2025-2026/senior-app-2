# Performance Analytics & Charts Dashboard - Dokumentasyon

## 📋 İçindekiler
1. [Genel Bakış](#genel-bakış)
2. [Başlarken](#başlarken)
3. [Komponentler](#komponentler)
4. [API Endpoints](#api-endpoints)
5. [Veri Formatları](#veri-formatları)
6. [Dışa Aktarma](#dışa-aktarma)
7. [Testler](#testler)
8. [Sorun Giderme](#sorun-giderme)

---

## Genel Bakış

Performance Analytics Dashboard, öğretim üyelerinin ve danışmanların öğrenci ve grup performansını görsel olarak analiz etmesine olanak sağlayan bir araçtır. Dashboard, zaman serisine göre performans trendlerini ve detaylı performans metriklerini sunmaktadır.

### Temel Özellikler:
- 📊 **Radar Grafikleri**: Öğrenci/grup performans metriklerini görselleştir
- 📈 **Trend Çizelgeleri**: Semester boyunca skor ilerlemesini takip et
- 🎯 **Gelişmiş Filtreleme**: Öğrenci, grup ve tarih aralığına göre filtrele
- 📥 **PDF/CSV Dışa Aktarma**: Raporları danışman toplantıları için indir
- 📱 **Responsive Tasarım**: Tüm cihazlarda çalışır

---

## Başlarken

### 1. Kütüphaneleri Kurma

```bash
npm install recharts jspdf html2canvas date-fns
```

### 2. Route Ekleme (App.jsx)

Dashboard'a erişmek için aşağıdaki rotayı kullanın:
```
/panel/analytics
```

Route zaten App.jsx'e eklenmiştir.

### 3. Layout Navigasyonuna Link Ekleme (Opsiyonel)

Kullanıcıların dashboard'a kolayca erişmesi için Layout.jsx'de navigation'a link ekleyebilirsiniz:

```jsx
<a href="/panel/analytics">📊 Analytics</a>
```

---

## Komponentler

### 1. RadarChart (`components/RadarChart.jsx`)

Öğrenci veya grup performans metriklerini radar grafiği ile gösterir.

**Props:**
```tsx
interface RadarChartProps {
  data: Array<{ metric: string; value: number }>;
  studentName: string;
  loading: boolean;
}
```

**Kullanım:**
```jsx
<PerformanceRadarChart
  data={[
    { metric: 'Doğruluk', value: 85 },
    { metric: 'Hız', value: 75 },
    { metric: 'Kalite', value: 90 }
  ]}
  studentName="Ahmet Yılmaz"
  loading={false}
/>
```

### 2. TrendlineChart (`components/TrendlineChart.jsx`)

Zaman serisine göre performans trendlerini gösterir.

**Props:**
```tsx
interface TrendlineChartProps {
  data: Array<{ date: string; score: number; trend?: number }>;
  title: string;
  loading: boolean;
  type?: 'line' | 'area';
}
```

**Kullanım:**
```jsx
<TrendlineChart
  data={[
    { date: '2025-01-01', score: 60 },
    { date: '2025-01-08', score: 75 }
  ]}
  title="Performans Trendi"
  loading={false}
  type="line"
/>
```

### 3. FilterPanel (`components/FilterPanel.jsx`)

Filtreleme kontrolleri sağlar.

**Props:**
```tsx
interface FilterPanelProps {
  students: Array<{ id: string; name: string; studentId: string }>;
  groups: Array<{ id: string; name: string }>;
  onFilterChange: (filters: FilterState) => void;
  loading: boolean;
}
```

**Filtreleme Seçenekleri:**
- Öğrenci seçimi
- Grup seçimi
- Tarih aralığı seçimi

---

## API Endpoints

Backend'de aşağıdaki endpoints'ler implementasyon yapılmalıdır:

### 1. Öğrenci Performansı
```
GET /api/analytics/students/{studentId}/performance
```

**Response:**
```json
{
  "metrics": [
    { "metric": "Doğruluk", "value": 85 },
    { "metric": "Hız", "value": 75 },
    { "metric": "Kalite", "value": 90 }
  ],
  "summary": {
    "averageScore": 83,
    "maxScore": 95,
    "minScore": 70,
    "improvement": 15
  }
}
```

### 2. Grup Performansı
```
GET /api/analytics/groups/{groupId}/performance
```

**Response:**
```json
{
  "metrics": [
    { "metric": "İşbirliği", "value": 88 },
    { "metric": "Zaman Yönetimi", "value": 80 }
  ],
  "summary": {
    "averageScore": 84,
    "memberCount": 4,
    "projectCount": 2
  }
}
```

### 3. Performans Trendleri
```
GET /api/analytics/performance-trends?studentId=X&groupId=Y&startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
```

**Response:**
```json
[
  { "date": "2025-01-01", "score": 60 },
  { "date": "2025-01-08", "score": 75 },
  { "date": "2025-01-15", "score": 85 }
]
```

### 4. Kullanılabilir Öğrenciler
```
GET /api/analytics/available-students
```

**Response:**
```json
[
  { "id": "1", "name": "Ahmet Yılmaz", "studentId": "20230001" },
  { "id": "2", "name": "Fatih Demir", "studentId": "20230002" }
]
```

### 5. Kullanılabilir Gruplar
```
GET /api/analytics/available-groups
```

**Response:**
```json
[
  { "id": "1", "name": "Grup A" },
  { "id": "2", "name": "Grup B" }
]
```

---

## Veri Formatları

### Öğrenci Performans Veri Formatı

```ts
interface StudentPerformance {
  metrics: Array<{
    metric: string;  // "Doğruluk", "Hız", "Kalite", vb.
    value: number;   // 0-100
  }>;
  summary: {
    averageScore: number;
    maxScore: number;
    minScore: number;
    improvement: number;  // % değişim
  };
}
```

### Trend Veri Formatı

```ts
interface TrendDataPoint {
  date: string;        // ISO 8601 format: YYYY-MM-DD
  score: number;       // 0-100
  trend?: number;      // Optional: trend line value
}
```

### Filtre Veri Formatı

```ts
interface FilterState {
  studentId: string;   // Boş string = tüm öğrenciler
  groupId: string;     // Boş string = tüm gruplar
  startDate: string;   // YYYY-MM-DD veya null
  endDate: string;     // YYYY-MM-DD veya null
}
```

---

## Dışa Aktarma

### PDF Rapor İndirme

```js
import { exportToPDF } from '../services/exportService';

await exportToPDF('analytics-dashboard', {
  fileName: 'Raporum.pdf',
  title: 'Performans Analitik Raporu',
  metadata: {
    'Rapor Tarihi': new Date().toLocaleDateString('tr-TR'),
    'Öğrenci': 'Ahmet Yılmaz'
  }
});
```

### CSV Veri İndirme

```js
import { exportToCSV } from '../services/exportService';

const data = [
  { date: '2025-01-01', score: 60 },
  { date: '2025-01-08', score: 75 }
];

exportToCSV(data, 'trend-verileri.csv');
```

### Grafik Olarak PNG İndirme

```js
import { exportChartAsImage } from '../services/exportService';

await exportChartAsImage('trendline-chart', 'grafik.png');
```

---

## Testler

### Test Dosyaları

- `components/RadarChart.test.jsx` - Radar grafik testleri
- `components/TrendlineChart.test.jsx` - Trend grafik testleri
- `components/FilterPanel.test.jsx` - Filtre paneli testleri
- `services/exportService.test.js` - Dışa aktarma hizmeti testleri
- `pages/PerformanceAnalytics.test.jsx` - Ana sayfa testleri

### Testleri Çalıştırma

```bash
npm run test
```

### Spesifik Test Dosyasını Çalıştırma

```bash
npm run test RadarChart.test.jsx
```

### Testleri Coverage İle Çalıştırma

```bash
npm run test -- --coverage
```

---

## Sorun Giderme

### "Veri bulunamadı" Hatası

**Sorun:** Dashboard'da hiç veri görünmüyor.

**Çözüm:**
1. Backend API endpoints'lerinin çalışıp çalışmadığını kontrol edin
2. API response'ları doğru formatta olduğundan emin olun
3. Tarayıcı konsolundan hata mesajlarını kontrol edin

### PDF İndirme Çalışmıyor

**Sorun:** "PDF dışa aktarırken hata oluştu" mesajı görülüyor.

**Çözüm:**
1. `html2canvas` ve `jspdf` paketlerinin yüklü olduğundan emin olun
2. Tarayıcının pop-up engelleme ayarlarını kontrol edin
3. Tarayıcı konsolundan hata detaylarını inceleyin

### Grafikler Görünmüyor

**Sorun:** Komponentler yükleniyor ama grafikler boş.

**Çözüm:**
1. `recharts` paketinin yüklü olduğundan emin olun
2. Veri formatının doğru olduğundan emin olun
3. Tarayıcıda JavaScript'in etkinleştirildiğini kontrol edin

### Tarih Seçimi Çalışmıyor

**Sorun:** Tarih picker açılmıyor veya tarihleri seçemiyorum.

**Çözüm:**
1. `react-datepicker` paketinin yüklü olduğundan emin olun
2. CSS dosyasının yüklü olduğundan emin olun
3. Tarayıcı konsolundan hata mesajlarını kontrol edin

---

## Backend Implementasyon Örneği (Spring Boot)

```java
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
  
  @GetMapping("/students/{studentId}/performance")
  public StudentPerformanceDTO getStudentPerformance(@PathVariable Long studentId) {
    // Implementation
  }
  
  @GetMapping("/groups/{groupId}/performance")
  public GroupPerformanceDTO getGroupPerformance(@PathVariable Long groupId) {
    // Implementation
  }
  
  @GetMapping("/performance-trends")
  public List<TrendDataPointDTO> getPerformanceTrends(
    @RequestParam(required = false) Long studentId,
    @RequestParam(required = false) Long groupId,
    @RequestParam(required = false) LocalDate startDate,
    @RequestParam(required = false) LocalDate endDate
  ) {
    // Implementation
  }
}
```

---

## İletişim ve Destek

Sorularınız veya sorunlarınız için proje ekibiyle iletişime geçin.

---

**Son Güncelleme:** 23 Nisan 2025
**Versiyon:** 1.0
