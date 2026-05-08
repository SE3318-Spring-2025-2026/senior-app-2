# TODO

## Task: Core AI validation module (Process 5.3)

- [ ] Implement strict OpenAI “Senior Technical Auditor” system prompt
- [ ] Add strict JSON parsing + DTO for validator output (accuracyScore + evidence/discrepancies)
- [ ] Add 30-second timeout wrapper; map timeout to HTTP 504 via GlobalExceptionHandler
- [ ] Extend ComparisonDtos to include accuracyScore
- [ ] Wire validator into ComparisonService (replace placeholder AI feedback)
- [ ] Add OpenAI config to application.properties (model/api-key/timeout)
- [ ] Add unit tests for JSON parse and timeout->504 exception
- [ ] Run `mvn test` in backend

