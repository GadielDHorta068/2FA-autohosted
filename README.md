# twofactorauth (2FA API con Spring Boot)

API REST para habilitar y verificar 2FA (TOTP) con generación de QR y códigos de recuperación.

## Tecnologías
- Java 11+
- Spring Boot 2.7.x
- Spring Web, Spring Data JPA
- H2 (dev) / PostgreSQL (prod)
- totp-spring-boot-starter (dev.samstevens.totp)
- springdoc-openapi-ui (Swagger)

## Requisitos
- Java 11 o superior (se compiló y probó con Java 17)
- Maven 3.8+

## Configuración rápida (dev)
1. Variables de entorno recomendadas:
```bash
export ENCRYPTION_KEY='fP9uZ3sVq1Xb7DhK'  # 16 chars para AES-128
```
2. Configuración en `src/main/resources/application.properties` (ya incluida):
```properties
server.port=8081
spring.datasource.url=jdbc:h2:mem:twofactorauth;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
# Clave de cifrado (puede referenciar variable de entorno)
encryption.key=${ENCRYPTION_KEY}
```

## Construir y ejecutar
```bash
mvn clean package -DskipTests
java -jar target/twofactorauth-0.0.1-SNAPSHOT.jar
```
Por defecto expone la API en `http://localhost:8081`.

## Endpoints
- POST `/api/2fa/enable` (application/json)
  - Request: `{ "username": "usuario" }`
  - Response: `{ "qrCode": "data:image/png;base64,...", "recoveryCodes": ["xxxx-....", ...] }`
- POST `/api/2fa/verify` (application/json)
  - Request: `{ "username": "usuario", "code": "123456" }`
  - Response: `{ "verified": true|false }`

## Probar con Postman
1. POST `http://localhost:8081/api/2fa/enable`
   - Headers: `Content-Type: application/json`
   - Body: `{ "username": "demo" }`
   - Copia el valor `qrCode` (data URI) y pégalo en el navegador para ver el QR, o usa Postman Visualizer:
```js
const data = pm.response.json();
pm.visualizer.set(`
  <div style="font-family: system-ui, sans-serif">
    <h3>QR para escanear</h3>
    <img src="{{qr}}" style="max-width: 280px; border:1px solid #ddd; padding:6px; border-radius:6px;" />
  </div>
`, { qr: data.qrCode });
```
2. Escanea el QR en tu app TOTP (Google Authenticator, Authy, 1Password...).
3. POST `http://localhost:8081/api/2fa/verify`
   - Headers: `Content-Type: application/json`
   - Body: `{ "username": "demo", "code": "123456" }` (usa el código actual de tu app)
   - Debe devolver `{ "verified": true }` si es válido.

## Swagger y H2 Console
- Swagger UI: `http://localhost:8081/swagger-ui/index.html` (si está habilitado)
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- H2 Console: `http://localhost:8081/h2-console` (JDBC URL `jdbc:h2:mem:twofactorauth`, user `sa`)

## Seguridad
- En desarrollo puedes dejar la seguridad desactivada (actualmente se excluyó la autoconfiguración). Para producción:
  - Quita la exclusión de `SecurityAutoConfiguration` y define un `SecurityFilterChain` que permita `/api/2fa/**`, `/v3/api-docs/**`, `/swagger-ui/**` y proteja el resto.
  - Configura CORS, CSRF según tus necesidades.
  - No expongas la H2 console.

## Persistencia y cifrado
- El secreto TOTP se almacena cifrado (AES). La clave se inyecta desde `encryption.key`.
- Los `recoveryCodes` se guardan cifrados en base de datos y se devuelven en claro solo al habilitar 2FA.
- Cambiar `encryption.key` rompe el descifrado de secretos previos; planifica una rotación controlada si es necesario.

## PostgreSQL (opcional prod)
Ejemplo de propiedades:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/twofactorauth
spring.datasource.username=twofa
spring.datasource.password=tu_password
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL10Dialect
```

## Estructura del proyecto
- `controller` → Endpoints REST
- `service` → Lógica de negocio (TOTP, QR, cifrado)
- `entity` → Entidades JPA (`User`, `RecoveryCode`)
- `repository` → Repositorios Spring Data
- `dto` → Modelos de request/response

## Desarrollo
- Formato/estilo: Java 11+, Lombok (`@Data`, etc.)
- Ejecutar en caliente (opcional): `mvn spring-boot:run`

## Notas
- Mantén `ENCRYPTION_KEY` fuera del repositorio (variables de entorno/secret manager).
- Considera añadir rate limiting y protección anti-brute force en `/verify`.
- Para producción, añade auditoría/observabilidad (logs estructurados, métricas). 

## Despliegue a producción (PostgreSQL)

### 1) Preparar base de datos
```bash
sudo -u postgres psql -c "CREATE DATABASE twofactorauth;"
sudo -u postgres psql -c "CREATE USER twofa WITH ENCRYPTED PASSWORD 'tu_password_segura';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE twofactorauth TO twofa;"
```

### 2) Perfil de producción
Crea `src/main/resources/application-prod.properties`:
```properties
server.port=8080

spring.datasource.url=jdbc:postgresql://localhost:5432/twofactorauth
spring.datasource.username=twofa
spring.datasource.password=tu_password_segura
spring.datasource.hikari.maximum-pool-size=10

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL10Dialect
spring.jpa.open-in-view=false

# Deshabilitar herramientas de dev
spring.h2.console.enabled=false
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false

# Clave AES (no la hardcodees; usa secret/variable de entorno)
encryption.key=${ENCRYPTION_KEY}
```

### 3) Seguridad mínima (recomendado)
Crea `src/main/java/com/argy/twofactorauth/config/SecurityConfig.java`:
```java
package com.argy.twofactorauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf().disable()
			.authorizeRequests()
			.antMatchers("/api/2fa/**").permitAll()
			.anyRequest().denyAll();
		return http.build();
	}
}
```
Ajusta las reglas según tus necesidades (CORS, CSRF, authn/authz, rate limiting, etc.).

### 4) Construir artefacto
```bash
mvn clean package -DskipTests
```

### 5) Ejecutar con perfil prod
```bash
export SPRING_PROFILES_ACTIVE=prod
export ENCRYPTION_KEY='cambia_esta_clave_de_16_chars'
java -jar target/twofactorauth-0.0.1-SNAPSHOT.jar
```

### 6) Verificación rápida
- POST `http://<host>:8080/api/2fa/enable` con `{ "username": "demo" }` → 200 y devuelve `qrCode`/`recoveryCodes`.
- POST `http://<host>:8080/api/2fa/verify` con `{ "username": "demo", "code": "123456" }` → `{ "verified": true|false }`.

### Recomendaciones de producción
- Usa migraciones (Flyway/Liquibase) en lugar de `ddl-auto`.
- Gestiona `ENCRYPTION_KEY` en un secret manager (no en archivos).
- No expongas `/h2-console`; si usas Swagger en prod, protégelo.
- Añade observabilidad (logs estructurados, métricas, tracing) y un proxy con HTTPS (Nginx/Caddy).
- Backup/rotación de clave: cambiar `ENCRYPTION_KEY` invalida secretos previamente cifrados; planifica migración/rotación. 