# Backend Analytics API Implementasyon Rehberi

Bu dosya, Performance Analytics Dashboard için backend API endpoints'lerinin nasıl implementasyon yapılacağını açıklar.

## 📋 Implementasyon Yapılacak Endpoints

### 1. Öğrenci Performansı Metrikleri

**Endpoint:** `GET /api/analytics/students/{studentId}/performance`

**Spring Boot Implementasyon Örneği:**

```java
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final StudentService studentService;
    private final SubmissionService submissionService;
    
    @GetMapping("/students/{studentId}/performance")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADVISOR')")
    public ResponseEntity<StudentPerformanceDTO> getStudentPerformance(
            @PathVariable Long studentId) {
        
        // Öğrenci metrikleri hesapla
        double accuracy = calculateAccuracy(studentId);
        double speed = calculateSpeed(studentId);
        double quality = calculateQuality(studentId);
        
        List<MetricDTO> metrics = List.of(
            new MetricDTO("Doğruluk", accuracy),
            new MetricDTO("Hız", speed),
            new MetricDTO("Kalite", quality)
        );
        
        // Özet istatistikleri hesapla
        SummaryDTO summary = new SummaryDTO(
            calculateAverageScore(studentId),
            calculateMaxScore(studentId),
            calculateMinScore(studentId),
            calculateImprovement(studentId)
        );
        
        return ResponseEntity.ok(
            new StudentPerformanceDTO(metrics, summary)
        );
    }
    
    private double calculateAccuracy(Long studentId) {
        // Yapılan hatalar vs toplam teslimler oranı
        List<Submission> submissions = submissionService.findByStudentId(studentId);
        long totalSubmissions = submissions.size();
        long correctSubmissions = submissions.stream()
            .filter(s -> s.getGrade() >= 80)
            .count();
        return totalSubmissions > 0 ? (correctSubmissions * 100.0) / totalSubmissions : 0;
    }
    
    private double calculateSpeed(Long studentId) {
        // Teslim süresi metriği (kısa = iyi)
        List<Submission> submissions = submissionService.findByStudentId(studentId);
        if (submissions.isEmpty()) return 0;
        
        OptionalDouble avgDaysToSubmit = submissions.stream()
            .mapToLong(s -> ChronoUnit.DAYS.between(s.getDeliverable().getDueDate(), s.getSubmittedAt()))
            .average();
        
        // Daha az gün = daha yüksek skor
        return Math.max(0, 100 - avgDaysToSubmit.orElse(0) * 2);
    }
    
    private double calculateQuality(Long studentId) {
        // Ortalama skor
        List<Grade> grades = gradeService.findByStudentId(studentId);
        return grades.isEmpty() ? 0 : 
            grades.stream().mapToDouble(Grade::getScore).average().orElse(0);
    }
    
    private double calculateAverageScore(Long studentId) {
        List<Grade> grades = gradeService.findByStudentId(studentId);
        return grades.isEmpty() ? 0 :
            grades.stream().mapToDouble(Grade::getScore).average().orElse(0);
    }
    
    private double calculateMaxScore(Long studentId) {
        return gradeService.findByStudentId(studentId).stream()
            .mapToDouble(Grade::getScore)
            .max()
            .orElse(0);
    }
    
    private double calculateMinScore(Long studentId) {
        return gradeService.findByStudentId(studentId).stream()
            .mapToDouble(Grade::getScore)
            .min()
            .orElse(0);
    }
    
    private double calculateImprovement(Long studentId) {
        List<Grade> grades = gradeService.findByStudentId(studentId);
        if (grades.size() < 2) return 0;
        
        grades.sort(Comparator.comparing(Grade::getCreatedAt));
        double firstScore = grades.get(0).getScore();
        double lastScore = grades.get(grades.size() - 1).getScore();
        
        return lastScore - firstScore;
    }
}
```

### 2. Grup Performansı Metrikleri

**Endpoint:** `GET /api/analytics/groups/{groupId}/performance`

```java
@GetMapping("/groups/{groupId}/performance")
@PreAuthorize("hasAnyRole('PROFESSOR', 'ADVISOR')")
public ResponseEntity<GroupPerformanceDTO> getGroupPerformance(
        @PathVariable Long groupId) {
    
    Group group = groupService.findById(groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    
    // Grup metrikleri hesapla
    double collaboration = calculateCollaboration(group);
    double timeManagement = calculateTimeManagement(group);
    
    List<MetricDTO> metrics = List.of(
        new MetricDTO("İşbirliği", collaboration),
        new MetricDTO("Zaman Yönetimi", timeManagement)
    );
    
    // Özet istatistikleri
    SummaryDTO summary = new SummaryDTO(
        calculateGroupAverageScore(group),
        group.getMembers().size(),
        group.getProjects().size()
    );
    
    return ResponseEntity.ok(new GroupPerformanceDTO(metrics, summary));
}

private double calculateCollaboration(Group group) {
    // Grup üyeleri arasında eşit katılım
    // ... implementasyon
    return 85.0;
}

private double calculateTimeManagement(Group group) {
    // Zamanında teslimat oranı
    // ... implementasyon
    return 80.0;
}

private double calculateGroupAverageScore(Group group) {
    return group.getMembers().stream()
        .flatMap(member -> gradeService.findByStudentId(member.getId()).stream())
        .mapToDouble(Grade::getScore)
        .average()
        .orElse(0);
}
```

### 3. Performans Trendleri

**Endpoint:** `GET /api/analytics/performance-trends`

```java
@GetMapping("/performance-trends")
@PreAuthorize("hasAnyRole('PROFESSOR', 'ADVISOR')")
public ResponseEntity<List<TrendDataPointDTO>> getPerformanceTrends(
        @RequestParam(required = false) Long studentId,
        @RequestParam(required = false) Long groupId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    
    LocalDate actualStartDate = startDate != null ? startDate : LocalDate.now().minusMonths(4);
    LocalDate actualEndDate = endDate != null ? endDate : LocalDate.now();
    
    List<TrendDataPointDTO> trendData = new ArrayList<>();
    
    if (studentId != null) {
        trendData = calculateStudentTrend(studentId, actualStartDate, actualEndDate);
    } else if (groupId != null) {
        trendData = calculateGroupTrend(groupId, actualStartDate, actualEndDate);
    } else {
        trendData = calculateOverallTrend(actualStartDate, actualEndDate);
    }
    
    return ResponseEntity.ok(trendData);
}

private List<TrendDataPointDTO> calculateStudentTrend(
        Long studentId, LocalDate startDate, LocalDate endDate) {
    
    List<Grade> grades = gradeService.findByStudentIdAndDateRange(
        studentId, startDate, endDate);
    
    return grades.stream()
        .collect(Collectors.groupingBy(
            grade -> grade.getCreatedAt().toLocalDate(),
            Collectors.averagingDouble(Grade::getScore)
        ))
        .entrySet().stream()
        .map(entry -> new TrendDataPointDTO(
            entry.getKey().toString(),
            entry.getValue()
        ))
        .sorted(Comparator.comparing(TrendDataPointDTO::getDate))
        .collect(Collectors.toList());
}

private List<TrendDataPointDTO> calculateGroupTrend(
        Long groupId, LocalDate startDate, LocalDate endDate) {
    
    Group group = groupService.findById(groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    
    List<Grade> grades = new ArrayList<>();
    for (User member : group.getMembers()) {
        grades.addAll(gradeService.findByStudentIdAndDateRange(
            member.getId(), startDate, endDate));
    }
    
    return grades.stream()
        .collect(Collectors.groupingBy(
            grade -> grade.getCreatedAt().toLocalDate(),
            Collectors.averagingDouble(Grade::getScore)
        ))
        .entrySet().stream()
        .map(entry -> new TrendDataPointDTO(
            entry.getKey().toString(),
            entry.getValue()
        ))
        .sorted(Comparator.comparing(TrendDataPointDTO::getDate))
        .collect(Collectors.toList());
}
```

### 4. Kullanılabilir Öğrenciler

**Endpoint:** `GET /api/analytics/available-students`

```java
@GetMapping("/available-students")
@PreAuthorize("hasAnyRole('PROFESSOR', 'ADVISOR')")
public ResponseEntity<List<StudentListDTO>> getAvailableStudents() {
    List<User> students = userService.findByRole(Role.STUDENT);
    
    return ResponseEntity.ok(students.stream()
        .map(student -> new StudentListDTO(
            student.getId(),
            student.getFullName(),
            student.getStudentId()
        ))
        .sorted(Comparator.comparing(StudentListDTO::getName))
        .collect(Collectors.toList())
    );
}
```

### 5. Kullanılabilir Gruplar

**Endpoint:** `GET /api/analytics/available-groups`

```java
@GetMapping("/available-groups")
@PreAuthorize("hasAnyRole('PROFESSOR', 'ADVISOR')")
public ResponseEntity<List<GroupListDTO>> getAvailableGroups() {
    List<Group> groups = groupService.findAll();
    
    return ResponseEntity.ok(groups.stream()
        .map(group -> new GroupListDTO(
            group.getId(),
            group.getName()
        ))
        .sorted(Comparator.comparing(GroupListDTO::getName))
        .collect(Collectors.toList())
    );
}
```

## 📊 DTO Sınıfları

```java
// StudentPerformanceDTO.java
@Data
@AllArgsConstructor
public class StudentPerformanceDTO {
    private List<MetricDTO> metrics;
    private SummaryDTO summary;
}

// GroupPerformanceDTO.java
@Data
@AllArgsConstructor
public class GroupPerformanceDTO {
    private List<MetricDTO> metrics;
    private SummaryDTO summary;
}

// MetricDTO.java
@Data
@AllArgsConstructor
public class MetricDTO {
    private String metric;
    private double value;
}

// SummaryDTO.java
@Data
@AllArgsConstructor
public class SummaryDTO {
    private double averageScore;
    private double maxScore;  // veya memberCount/projectCount
    private double minScore;  // veya memberCount
    private double improvement; // veya projectCount
}

// TrendDataPointDTO.java
@Data
@AllArgsConstructor
public class TrendDataPointDTO {
    private String date;  // YYYY-MM-DD
    private double score;
}

// StudentListDTO.java
@Data
@AllArgsConstructor
public class StudentListDTO {
    private Long id;
    private String name;
    private String studentId;
}

// GroupListDTO.java
@Data
@AllArgsConstructor
public class GroupListDTO {
    private Long id;
    private String name;
}
```

## 🔒 Güvenlik Notları

1. **Authorization**: Tüm endpoints'lerde `@PreAuthorize` kullanın
2. **Veri Erişimi**: Öğretim üyeleri sadece kendi öğrencilerinin/gruplarının verilerine erişebilmeli
3. **Rate Limiting**: Sık API çağrıları için rate limiting ekleyin
4. **Caching**: Performans için sonuçları cache'leyin

## 🧪 Test Örneği

```java
@SpringBootTest
class AnalyticsControllerTest {
    
    @MockBean
    private StudentService studentService;
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testGetStudentPerformance() throws Exception {
        Long studentId = 1L;
        
        mockMvc.perform(get("/api/analytics/students/{studentId}/performance", studentId)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metrics").isArray());
    }
}
```

---

**Tahmini Implementasyon Süresi:** 2-3 gün
