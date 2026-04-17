# Fake Vigo Skole
Denne tjenesten er en fake Vigo Skole for bruk til eksperimentering i prosjektet
sammenehengende tjenester for barn og unge (SAMT-BU).

## Eksempel HTTP request
```bash
curl -i \
  -X POST http://localhost:8080/api/submissions/graduating-students \
  -H 'Authorization: Bearer local-test-token' \
  -H 'Content-Type: application/ld+json' \
  --data @src/test/resources/curl/graduating-students-example-payload.json
```
