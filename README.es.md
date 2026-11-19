# Backend API  Spring Boot (JWT + Google OAuth + Tasks)

Reimplementación en **Spring Boot 4.1.2 / Java 17** del backend original en FastAPI.
La API conserva el mismo contrato que consume el frontend de Next.js
(`nextjs-frontend-jwt-api-backend-integration-tasks`): mismas rutas, mismos
nombres de campos, mismos códigos de estado y mismos mensajes de error.

- Puerto: **8080**
- Base de datos: **SQLite** (`app.db` en la raíz del proyecto)
- Autenticación: **JWT HS256** con Bearer token + **Google OAuth 2.0 / OpenID Connect**
- Documentación interactiva: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>



Same project in other backend:

[Flask](https://github.com/diegoperea20/flask-rest-api-with-jwt-google-oauth-tasks)

[Django](https://github.com/diegoperea20/django-google-authentication-api-tasks)

Frontend:

[Nextjs](https://github.com/diegoperea20/nextjs-frontend-jwt-api-backend-integration-tasks)


---

## Requisitos

- **Java 17** o superior (`java -version`)
- No hace falta instalar Maven: el proyecto incluye el wrapper (`mvnw.cmd` en Windows, `./mvnw` en Linux/macOS)

## Arranque rápido

```bash
# 1. (Opcional) crear tu configuración local
copy .env.example .env        # Windows
# cp .env.example .env        # Linux/macOS

# 2. Arrancar
.\mvnw.cmd spring-boot:run    # Windows
# ./mvnw spring-boot:run      # Linux/macOS
```

La aplicación queda disponible en <http://localhost:8080>.
La primera ejecución crea `app.db` y aplica la migración `V1__init.sql`
automáticamente con Flyway.

### Compilar y ejecutar el JAR

```bash
.\mvnw.cmd clean package
java -jar target/backendapi-0.0.1-SNAPSHOT.jar
```

### Tests

```bash
.\mvnw.cmd test
```

Los tests de integración usan H2 en memoria, por lo que no tocan `app.db`.
El rate limiting se desactiva en el perfil `test`.

---

## Configuración del frontend

En el proyecto de Next.js, el rewrite de `/api/:path*` apunta a `BACKEND_URL`.
Crea/modifica `.env.local`:

```env
BACKEND_URL=http://localhost:8080
```

No se necesita configurar CORS en el frontend cuando las llamadas pasan por el
proxy de Next.js; aun así el backend incluye CORS para llamadas directas
(configurable con `CORS_ORIGINS`, separado por comas).

### Google Cloud Console

En el cliente OAuth 2.0 de tipo **Web application**:

- **Authorized redirect URI**: `http://localhost:8080/authorize/google`

El login se inicia en `GET /api/login/google`, Google redirige a
`/authorize/google`, el backend valida el `id_token`, crea/actualiza el usuario y
redirige al frontend a `/auth/google/callback?code=...`. El frontend canjea ese
código temporal en `POST /api/auth/google/exchange` y guarda el JWT.

Si no configuras `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`, la aplicación
arranca igualmente: `GET /api/login/google` redirige al callback del frontend
con `?error=auth_failed` en lugar de romper el arranque.

---

## Variables de entorno

Se leen de variables de entorno del sistema o de un archivo `.env` en la raíz.
El archivo se interpreta con sintaxis dotenv (compatible con el `.env` de
FastAPI): admite comillas simples/dobles, comentarios con `#` y el prefijo
`export`. Tiene la prioridad más baja, de modo que las variables de entorno del
sistema y los archivos `application*.properties` siempre ganan.

```
SECRET_KEY=your-secret-key-here-change-in-production
JWT_SECRET_KEY=your-jwt-secret-key-here-change-in-production
DATABASE_URL=sqlite:///app.db
GOOGLE_CLIENT_ID= your-google-client-id-here
GOOGLE_CLIENT_SECRET= your-google-client-secret-here
GOOGLE_REDIRECT_URI=http://localhost:8080/authorize/google
CORS_ORIGINS=http://localhost:3000
FRONTEND_URL=http://localhost:3000
```


> **Formato de `DATABASE_URL`:** se acepta tanto el formato SQLAlchemy que
> usaba FastAPI como el formato JDBC; la traducción es automática:
>
> | FastAPI (SQLAlchemy) | Spring Boot (JDBC) |
> |---|---|
> | `sqlite:///./app.db` | `jdbc:sqlite:./app.db` |
> | `sqlite:////ruta/absoluta/app.db` | `jdbc:sqlite:/ruta/absoluta/app.db` |
>
> Por eso puedes reutilizar el `.env` del backend FastAPI sin cambios.

---

## Endpoints

Autenticación: `Authorization: Bearer <access_token>` salvo en las rutas públicas.
Los límites de peticiones son **por IP y por minuto**, equivalentes a los de
`slowapi` en FastAPI.

| Método | Ruta | Auth | Límite | Descripción |
|---|---|:---:|:---:|---|
| `GET` | `/api/health` | No | — | Estado de la API y la base de datos |
| `POST` | `/api/register` | No | 5/min | Registro; devuelve JWT y usuario (**HTTP 200**, como FastAPI) |
| `POST` | `/api/login` | No | 10/min | Login con usuario/contraseña |
| `GET` | `/api/login/google` | No | — | Inicia el flujo OAuth con Google |
| `GET` | `/authorize/google` | No | — | Callback de Google (lo llama Google) |
| `POST` | `/api/auth/google/exchange` | No | 10/min | Canjea el código temporal por el JWT |
| `GET` | `/api/protected` | Sí | — | Ruta de prueba protegida |
| `GET` | `/api/user/profile` | Sí | — | Perfil del usuario autenticado |
| `GET` | `/api/stats` | Sí | — | Total de usuarios y códigos OAuth activos |
| `POST` | `/api/tasks` | Sí | 20/min | Crea una tarea |
| `GET` | `/api/tasks` | Sí | — | Lista **todas** las tareas (ver notas de compatibilidad) |
| `GET` | `/api/tasks/user/{user}` | Sí | — | Tareas del usuario autenticado |
| `GET` | `/api/tasks/{taskId}?user=` | Sí | — | Tarea por id |
| `PUT` | `/api/tasks/{taskId}` | Sí | 20/min | Actualiza una tarea |
| `DELETE` | `/api/tasks/{taskId}` | Sí | 20/min | Elimina una tarea y la devuelve |
| `DELETE` | `/api/tasks/user/{user}/deleteall` | Sí | 5/min | Elimina todas las tareas del usuario |
| `GET` | `/api/tasks/countsames/{user}` | Sí | — | Cuenta usuarios con títulos en común |
| `GET` | `/api/tasks/countsame/{user}` | Sí | — | Emails de otros usuarios con títulos en común |

### Ejemplos

```bash
# Registro
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"demo@example.com\",\"password\":\"secret123\"}"

# Login
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"demo@example.com\",\"password\":\"secret123\"}"

# Perfil
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN"

# Crear tarea
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"user\":\"demo@example.com\",\"title\":\"Comprar pan\",\"description\":\"Integral\"}"

# Títulos compartidos con otros usuarios
curl http://localhost:8080/api/tasks/countsames/demo@example.com \
  -H "Authorization: Bearer $TOKEN"
```

### Formas de respuesta

```jsonc
// register / login / exchange
{
  "message": "Login exitoso",
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "demo@example.com",
    "created_at": "2026-11-18T23:10:46.434325",
    "last_login": "2026-11-18T23:10:46.434325"
  }
}

// task
{
  "id": 10,
  "user": "demo@example.com",
  "title": "Comprar pan",
  "description": "Integral",
  "created_at": "2026-11-18T23:12:01.123456"
}

// GET /api/tasks/countsames/{user}
{ "results": [ { "Number of titles": 2, "title": "Comprar pan" } ] }

// GET /api/tasks/countsame/{user}
{ "results": [ { "title": "Comprar pan", "emails": ["otro@example.com"] } ] }

// DELETE /api/tasks/user/{user}/deleteall (sin tareas)
{ "message": "No hay tareas para eliminar", "tasks": [] }
```

Los errores usan un formato compatibles con ambos clientes:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "El username ya existe",
  "error": "El username ya existe"
}
```

`error` es el campo que lee el frontend de Next.js; `detail` se mantiene para
clientes que venían del backend FastAPI.

---

## Base de datos y migraciones

- `app.db` es el archivo SQLite por defecto.
- `src/main/resources/db/migration/V1__init.sql` crea `users` y `tasks`.
- `spring.flyway.baseline-on-migrate=true` permite abrir una base creada por el
  backend FastAPI: si ya existen tablas, Flyway marca el baseline y no vuelve a
  crearlas.
- Las tablas y columnas son las mismas que generaba SQLAlchemy
  (`users.id`, `users.username`, `users.password_hash`, `users.created_at`,
  `users.last_login`; `tasks.id`, `tasks.user`, `tasks.title`,
  `tasks.description`, `tasks.created_at`).
- Las contraseñas son BCrypt. Los hashes `$2b$` generados por Python
  (`bcrypt`/`passlib`) se pueden verificar sin migración.
- Las fechas se guardan y serializan en **UTC** (`datetime.utcnow().isoformat()`
  en FastAPI).

Para reiniciar la base de datos en desarrollo: detén la aplicación y elimina
`app.db`; se volverá a crear al arrancar.

---

## Estructura del proyecto

```
src/main/java/com/backend/backendapi
├── config/        AppProperties, SecurityConfig, CORS, OpenAPI
├── domain/        Entidades JPA User y Task
├── dto/           Records de petición/respuesta
├── repository/    Repositorios Spring Data JPA
├── security/      JWT, filtros, OAuth2, códigos temporales, rate limit
├── service/       Reglas de negocio (AuthService, TaskService)
├── web/           Controladores REST y manejo global de errores
└── BackendapiApplication.java
```

Flujo de una petición autenticada:

```
RateLimitFilter -> JwtAuthenticationFilter -> Spring Security -> Controller -> Service -> Repository
```

Detalles de implementación:

- JWT **stateless**: no se crean sesiones HTTP.
- El `state` de OAuth2 se guarda en un caché en memoria con TTL en lugar de la
  `HttpSession`, para que el flujo funcione aunque el login pase por el proxy de
  Next.js (la cookie de sesión quedaría en `localhost:3000` y el callback llega a
  `localhost:8080`).
- Los códigos temporales de Google son de **un solo uso** y expiran a los 5
  minutos (Caffeine).
- Rate limiting con **Bucket4j** y cubos en caché acotado (Caffeine).
- Errores con `ProblemDetail` (RFC 9457) + propiedad `error`.



---

## Seguridad y producción

Antes de desplegar:

- Define un `JWT_SECRET_KEY` largo y aleatorio (mínimo 32 caracteres).
- Configura `FRONTEND_URL` y `CORS_ORIGINS` con tu dominio real (HTTPS).
- Usa HTTPS y actualiza `GOOGLE_REDIRECT_URI` en Google Console.
- Restringe el acceso a `/swagger-ui/**` y `/v3/api-docs/**` o deshabilítalos:
  ```properties
  springdoc.api-docs.enabled=false
  springdoc.swagger-ui.enabled=false
  ```
- Mantén `RATE_LIMIT_ENABLED=true`.
- Haz copias de seguridad de `app.db` o migra a un motor cliente-servidor si
  necesitas varias instancias.


----

### Youtube links tutorials

[curso spring java](https://www.youtube.com/watch?v=pQGxVgaNM-o&list=PL2Z95CSZ1N4EWw14HZ0NFD3woFcn6uiCm&index=2)

[descargar java](https://youtu.be/BTZDdf4sw5E)

----


### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Diego Ivan Perea Montealegre**

- GitHub: [@diegoperea20](https://github.com/diegoperea20)

---

Created by [Diego Ivan Perea Montealegre](https://github.com/diegoperea20)
