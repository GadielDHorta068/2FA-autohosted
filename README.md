## twofactorauth (API 2FA con Spring Boot)

API REST para habilitar/verificar 2FA (TOTP), generación de QR y gestión de códigos de recuperación (hashed). No requiere frontend: es solo API.

### Stack
- Java 11+ (runtime 17 en Docker)
- Spring Boot 2.7.x (Web, Data JPA, Security, Validation, Actuator)
- H2 (dev) / PostgreSQL (prod)
- `dev.samstevens.totp` (TOTP/QR)
- OpenAPI/Swagger UI

### Seguridad y prácticas
- Secreto TOTP cifrado con AES-GCM (IV aleatorio + tag de autenticación).
- `username` único y validado.
- Códigos de recuperación guardados con hash BCrypt; solo se muestran una vez al habilitar.
- Endpoint de verificación de código de recuperación marca one-shot como usado.
- Validación de entrada con Bean Validation y manejo global de errores.
- Actuator con healthcheck para Docker.

### Requisitos locales
- JDK 11+ y Maven 3.8+ (o usa Docker para construir/ejecutar)

### Configuración (dev)
Variables de entorno recomendadas:
```bash
set ENCRYPTION_KEY=pon_una_clave_secreta
```
`src/main/resources/application.properties` ya está preparado para usar `ENCRYPTION_KEY` y levantar en 8080 con H2.

### Construir y ejecutar (local)
```bash
mvn clean package -DskipTests
java -jar target/twofactorauth-0.0.1-SNAPSHOT.jar
```
API en `http://localhost:8080`.

### Docker
```bash
docker build -t twofactorauth:latest .
docker run -e ENCRYPTION_KEY=tu_clave -p 8080:8080 twofactorauth:latest
```
Compose con PostgreSQL:
```bash
docker compose up -d
```

### Endpoints
- POST `/api/2fa/enable`
  - Request: `{ "username": "usuario" }`
  - Response: `{ "qrCode": "data:image/png;base64,...", "recoveryCodes": ["...", ...] }`
- POST `/api/2fa/verify`
  - Request: `{ "username": "usuario", "code": "123456" }`
  - Response: `{ "verified": true|false }`
- POST `/api/2fa/verify-recovery/{username}`
  - Body (text/plain o JSON string): código de recuperación
  - Response: `{ "verified": true|false }` y marca el código como usado si coincide
- POST `/api/2fa/rotate/{username}`
  - Regenera el secreto TOTP, devuelve nuevo `qrCode` y nuevos `recoveryCodes`. Útil para rotación periódica o compromisos.
- POST `/api/2fa/disable/{username}`
  - Inhabilita 2FA: elimina el secreto y limpia códigos de recuperación. Respuesta 204.

Swagger UI: `http://localhost:8080/swagger-ui/index.html`
H2 Console (dev): `http://localhost:8080/h2-console`
Health: `http://localhost:8080/actuator/health`

### Rate limiting
- Los endpoints de verificación (`/verify` y `/verify-recovery/{username}`) tienen límite por `username` + IP (p. ej., 5 req/min). Si se supera, responde con 400/429.

### Producción (sugerencias)
- Usa PostgreSQL y desactiva H2/Swagger si no es necesario.
- Gestiona `ENCRYPTION_KEY` en un secret manager.
- Activa autenticación (p. ej., JWT) y CORS según tu caso. El `SecurityFilterChain` permite `/api/2fa/**` y `actuator/health` por defecto y deniega el resto.
- Añade rate limiting por usuario/IP para `/verify` (base `Bucket4j` incluida).
- Migraciones con Flyway/Liquibase; `ddl-auto=validate`.

### Docker Compose (prod-like)
1) En PowerShell (Windows), define la clave de cifrado para el contenedor:
```powershell
$env:ENCRYPTION_KEY = "pon_una_clave_segura"
```
2) Levanta servicios (app + PostgreSQL):
```powershell
docker compose up -d --build
```
3) Verifica health:
```powershell
curl http://localhost:8080/actuator/health
```
4) Prueba los endpoints (ver sección Postman o usa `curl`).

### Notas de diseño
- AES-GCM protege confidencialidad e integridad de secretos.
- Recovery codes hashed con BCrypt; verificación vía `matches`, nunca se re-muestran.
- Respuestas de error consistentes via `@ControllerAdvice`.