# Manual de API — 2FA Autohosted

Este manual explica cómo levantar la API por primera vez y cómo consumir sus endpoints desde aplicaciones de terceros.

## 1) Requisitos
- Docker y Docker Compose (recomendado)
- Opcional dev: JDK 11+ y Maven 3.8+

## 2) Arranque rápido (Docker Compose)
1. Define variables de entorno (PowerShell):
```powershell
$env:ENCRYPTION_KEY = "pon_una_clave_segura"
$env:JWT_SECRET = "my_jwt_secret"  # opcional, protege endpoints fuera de /api/2fa/**
```
2. Levanta la app + DB:
```powershell
docker compose up -d --build
```
3. Healthcheck:
```powershell
curl http://localhost:8080/actuator/health
```
4. Swagger (si habilitado): `http://localhost:8080/swagger-ui/index.html`

Notas:
- La app usa PostgreSQL del propio Compose
- Flyway crea el esquema base automáticamente
- El HEALTHCHECK del contenedor usa `/actuator/health`

## 3) Configuración relevante
- `ENCRYPTION_KEY`: clave AES-GCM para cifrar secreto TOTP
- `JWT_SECRET`: activa validación JWT HS256 para rutas autenticadas
- `CORS_ALLOWED_ORIGINS`: orígenes permitidos (por defecto `http://localhost:3000`)

## 4) Modelo de datos y seguridad
- Secreto TOTP cifrado con AES-GCM (IV aleatorio + tag de autenticación)
- Códigos de recuperación guardados como hash BCrypt; se marcan `used` al consumirse (one‑shot)
- `username` único y validado

## 5) Endpoints (Base URL: http://localhost:8080)

### POST /api/2fa/enable
- Body JSON: `{ "username": "usuario" }`
- 200: `{ "qrCode": "data:image/png;base64,...", "recoveryCodes": ["...", ...] }`
- Notas: `recoveryCodes` solo se muestran una vez

### POST /api/2fa/verify
- Body JSON: `{ "username": "usuario", "code": "123456" }`
- 200: `{ "verified": true|false }`
- Rate limit por `username+IP` (p. ej. 5 req/min). Si se excede, error 400

### POST /api/2fa/verify-recovery/{username}
- Body: text/plain con el código de recuperación
- 200: `{ "verified": true|false }`; el código se marca como usado si es válido (one‑shot)
- Rate limit aplica (429 si se excede)

### POST /api/2fa/rotate/{username}
- Regenera secreto, devuelve nuevo `qrCode` y `recoveryCodes`
- 200: mismo formato que `/enable`

### POST /api/2fa/disable/{username}
- Inhabilita 2FA del usuario (borra secreto y códigos)
- 204 No Content

## 6) Errores
- Validación: 400 `{ "code": "validation_error", "errors": { "field": "mensaje" } }`
- Genéricos: 400 `{ "code": "bad_request", "message": "..." }`

## 7) CORS y JWT
- Permitido sin auth: `/api/2fa/**`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`
- Resto autenticado si defines `JWT_SECRET`
- Para exigir auth también en `/api/2fa/**`, ajusta `SecurityConfig`

## 8) Ejemplos de uso

### 8.1 curl
```bash
curl -s -X POST http://localhost:8080/api/2fa/enable \
  -H "Content-Type: application/json" \
  -d '{"username":"demo"}'

curl -s -X POST http://localhost:8080/api/2fa/verify \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","code":"123456"}'

curl -s -X POST http://localhost:8080/api/2fa/verify-recovery/demo \
  -H "Content-Type: text/plain" \
  --data-raw 'xxxx-xxxx-xxxx-xxxx'

curl -s -X POST http://localhost:8080/api/2fa/rotate/demo

curl -i -s -X POST http://localhost:8080/api/2fa/disable/demo
```

### 8.2 Postman (rápido)
- Crea colección con requests:
  - POST http://localhost:8080/api/2fa/enable (JSON body `{ "username": "demo" }`)
  - POST http://localhost:8080/api/2fa/verify (JSON body `{ "username": "demo", "code": "123456" }`)
  - POST http://localhost:8080/api/2fa/verify-recovery/demo (text/plain con un recovery code)
  - POST http://localhost:8080/api/2fa/rotate/demo
  - POST http://localhost:8080/api/2fa/disable/demo
- Visualizer para QR (en Tests):
```js
const data = pm.response.json();
pm.visualizer.set('<img src="{{qr}}" style="max-width:280px"/>', { qr: data.qrCode });
```

### 8.3 JavaScript (fetch)
```javascript
await fetch('http://localhost:8080/api/2fa/enable', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'demo' })
}).then(r => r.json());
```

### 8.4 Java (WebClient)
```java
WebClient client = WebClient.create("http://localhost:8080");
Enable2FAResponse resp = client.post().uri("/api/2fa/enable")
  .contentType(MediaType.APPLICATION_JSON)
  .bodyValue(Map.of("username", "demo"))
  .retrieve().bodyToMono(Enable2FAResponse.class).block();
```

### 8.5 Python (requests)
```python
import requests
r = requests.post('http://localhost:8080/api/2fa/enable', json={'username': 'demo'})
print(r.json())
```

## 9) Operación
- Health: `GET /actuator/health`
- Logs: stdout del contenedor
- Rate limiting: 5 req/min por `username+IP` en `/verify` y `/verify-recovery`

## 10) Buenas prácticas
- Gestiona `ENCRYPTION_KEY`/`JWT_SECRET` en Secret Manager
- Deshabilita Swagger/H2 en prod si no son necesarios
- HTTPS con reverse proxy y cabeceras de seguridad
