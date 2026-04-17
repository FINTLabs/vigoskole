# Fake Vigo Skole
Denne tjenesten er en fake Vigo Skole for bruk til eksperimentering i prosjektet
sammenehengende tjenester for barn og unge (SAMT-BU).

## Eksempel HTTP request (JSON-LD)
```bash
curl -i \
  -X POST http://localhost:8080/api/submissions/graduating-students \
  -H 'Authorization: Bearer local-test-token' \
  -H 'Content-Type: application/ld+json' \
  -H 'Accept: application/ld+json' \
  --data @src/test/resources/curl/graduating-students-example-payload.json
```

## OpenAPI dokumentasjon
OpenAPI JSON er tilgjengelig på `/api-docs` og Swagger UI på `/swagger-ui`.
