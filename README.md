# Backend API  Spring Boot (JWT + Google OAuth + Tasks)

Reimplementation in **Spring Boot 4.1.2 / Java 17** of the original FastAPI backend.
The API keeps the same contract consumed by the Next.js frontend
(`nextjs-frontend-jwt-api-backend-integration-tasks`): same routes, same
field names, same status codes, and same error messages.

- Port: **8080**
- Database: **SQLite** (`app.db` in the project root)
- Authentication: **JWT HS256** with Bearer token + **Google OAuth 2.0 / OpenID Connect**
- Interactive documentation: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>



Same project in other backend:

[Flask](https://github.com/diegoperea20/flask-rest-api-with-jwt-google-oauth-tasks)

[Django](https://github.com/diegoperea20/django-google-authentication-api-tasks)

Frontend:

[Nextjs](https://github.com/diegoperea20/nextjs-frontend-jwt-api-backend-integration-tasks)


---

## Requirements

- **Java 17** or higher (`java -version`)
- Maven does not need to be installed: the project includes the wrapper (`mvnw.cmd` on Windows, `./mvnw` on Linux/macOS)

## Quick start

```bash
# 1. (Optional) create your local configuration
copy .env.example .env        # Windows
# cp .env.example .env        # Linux/macOS

# 2. Start the application
.\mvnw.cmd spring-boot:run    # Windows
# ./mvnw spring-boot:run      # Linux/macOS
```

The application is available at <http://localhost:8080>.
The first run creates `app.db` and applies the `V1__init.sql` migration
automatically with Flyway.

### Build and run the JAR

```bash
.\mvnw.cmd clean package
java -jar target/backendapi-0.0.1-SNAPSHOT.jar
```

### Tests

```bash
.\mvnw.cmd test
```

The integration tests use in-memory H2, so they do not touch `app.db`.
Rate limiting is disabled in the `test` profile.

---

## Frontend configuration

In the Next.js project, the `/api/:path*` rewrite points to `BACKEND_URL`.
Create/modify `.env.local`:

```env
BACKEND_URL=http://localhost:8080
```

CORS does not need to be configured in the frontend when calls go through the
Next.js proxy; even so, the backend includes CORS for direct calls
(configurable with `CORS_ORIGINS`, comma-separated).

### Google Cloud Console

In the OAuth 2.0 client of type **Web application**:

- **Authorized redirect URI**: `http://localhost:8080/authorize/google`

The login starts at `GET /api/login/google`, Google redirects to
`/authorize/google`, the backend validates the `id_token`, creates/updates the user, and
redirects to the frontend at `/auth/google/callback?code=...`. The frontend exchanges that
temporary code at `POST /api/auth/google/exchange` and stores the JWT.

If you do not configure `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`, the application
still starts: `GET /api/login/google` redirects to the frontend callback
with `?error=auth_failed` instead of failing at startup.

---

## Environment variables

They are read from system environment variables or from a `.env` file in the root.
The file is parsed with dotenv syntax (compatible with the FastAPI `.env`):
it supports single/double quotes, `#` comments, and the `export` prefix.
It has the lowest priority, so system environment variables and the
`application*.properties` files always win.

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


> **`DATABASE_URL` format:** both the SQLAlchemy format used by FastAPI and the
> JDBC format are accepted; the translation is automatic:
>
> | FastAPI (SQLAlchemy) | Spring Boot (JDBC) |
> |---|---|
> | `sqlite:///./app.db` | `jdbc:sqlite:./app.db` |
> | `sqlite:////absolute/path/app.db` | `jdbc:sqlite:/absolute/path/app.db` |
>
> That is why you can reuse the FastAPI backend's `.env` without changes.

---

## Endpoints

Authentication: `Authorization: Bearer <access_token>` except on public routes.
Request limits are **per IP and per minute**, equivalent to those of
`slowapi` in FastAPI.

| Method | Route | Auth | Limit | Description |
|---|---|:---:|:---:|---|
| `GET` | `/api/health` | No | — | API and database status |
| `POST` | `/api/register` | No | 5/min | Registration; returns JWT and user (**HTTP 200**, like FastAPI) |
| `POST` | `/api/login` | No | 10/min | Username/password login |
| `GET` | `/api/login/google` | No | — | Starts the Google OAuth flow |
| `GET` | `/authorize/google` | No | — | Google callback (called by Google) |
| `POST` | `/api/auth/google/exchange` | No | 10/min | Exchanges the temporary code for the JWT |
| `GET` | `/api/protected` | Yes | — | Protected test route |
| `GET` | `/api/user/profile` | Yes | — | Authenticated user's profile |
| `GET` | `/api/stats` | Yes | — | Total users and active OAuth codes |
| `POST` | `/api/tasks` | Yes | 20/min | Creates a task |
| `GET` | `/api/tasks` | Yes | — | Lists **all** tasks (see compatibility notes) |
| `GET` | `/api/tasks/user/{user}` | Yes | — | Authenticated user's tasks |
| `GET` | `/api/tasks/{taskId}?user=` | Yes | — | Task by id |
| `PUT` | `/api/tasks/{taskId}` | Yes | 20/min | Updates a task |
| `DELETE` | `/api/tasks/{taskId}` | Yes | 20/min | Deletes a task and returns it |
| `DELETE` | `/api/tasks/user/{user}/deleteall` | Yes | 5/min | Deletes all of the user's tasks |
| `GET` | `/api/tasks/countsames/{user}` | Yes | — | Counts users with titles in common |
| `GET` | `/api/tasks/countsame/{user}` | Yes | — | Emails of other users with titles in common |

### Examples

```bash
# Register
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"demo@example.com\",\"password\":\"secret123\"}"

# Login
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"demo@example.com\",\"password\":\"secret123\"}"

# Profile
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN"

# Create task
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"user\":\"demo@example.com\",\"title\":\"Buy bread\",\"description\":\"Whole wheat\"}"

# Titles shared with other users
curl http://localhost:8080/api/tasks/countsames/demo@example.com \
  -H "Authorization: Bearer $TOKEN"
```

### Response shapes

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
  "title": "Buy bread",
  "description": "Whole wheat",
  "created_at": "2026-11-18T23:12:01.123456"
}

// GET /api/tasks/countsames/{user}
{ "results": [ { "Number of titles": 2, "title": "Buy bread" } ] }

// GET /api/tasks/countsame/{user}
{ "results": [ { "title": "Buy bread", "emails": ["other@example.com"] } ] }

// DELETE /api/tasks/user/{user}/deleteall (no tasks)
{ "message": "No hay tareas para eliminar", "tasks": [] }
```

Errors use a format compatible with both clients:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "El username ya existe",
  "error": "El username ya existe"
}
```

`error` is the field read by the Next.js frontend; `detail` is kept for
clients that came from the FastAPI backend.

---

## Database and migrations

- `app.db` is the default SQLite file.
- `src/main/resources/db/migration/V1__init.sql` creates `users` and `tasks`.
- `spring.flyway.baseline-on-migrate=true` allows opening a database created by the
  FastAPI backend: if the tables already exist, Flyway marks the baseline and does not
  create them again.
- The tables and columns are the same ones SQLAlchemy generated
  (`users.id`, `users.username`, `users.password_hash`, `users.created_at`,
  `users.last_login`; `tasks.id`, `tasks.user`, `tasks.title`,
  `tasks.description`, `tasks.created_at`).
- Passwords are BCrypt. The `$2b$` hashes generated by Python
  (`bcrypt`/`passlib`) can be verified without migration.
- Dates are stored and serialized in **UTC** (`datetime.utcnow().isoformat()`
  in FastAPI).

To reset the database in development: stop the application and delete
`app.db`; it will be recreated on startup.

---

## Project structure

```
src/main/java/com/backend/backendapi
├── config/        AppProperties, SecurityConfig, CORS, OpenAPI
├── domain/        JPA entities User and Task
├── dto/           Request/response records
├── repository/    Spring Data JPA repositories
├── security/      JWT, filters, OAuth2, temporary codes, rate limit
├── service/       Business rules (AuthService, TaskService)
├── web/           REST controllers and global error handling
└── BackendapiApplication.java
```

Flow of an authenticated request:

```
RateLimitFilter -> JwtAuthenticationFilter -> Spring Security -> Controller -> Service -> Repository
```

Implementation details:

- **Stateless** JWT: no HTTP sessions are created.
- The OAuth2 `state` is stored in an in-memory cache with a TTL instead of the
  `HttpSession`, so the flow works even when the login goes through the Next.js
  proxy (the session cookie would stay on `localhost:3000` and the callback arrives at
  `localhost:8080`).
- Google temporary codes are **single-use** and expire after 5
  minutes (Caffeine).
- Rate limiting with **Bucket4j** and buckets in a bounded cache (Caffeine).
- Errors with `ProblemDetail` (RFC 9457) + an `error` property.



---

## Security and production

Before deploying:

- Define a long, random `JWT_SECRET_KEY` (at least 32 characters).
- Configure `FRONTEND_URL` and `CORS_ORIGINS` with your real domain (HTTPS).
- Use HTTPS and update `GOOGLE_REDIRECT_URI` in Google Console.
- Restrict access to `/swagger-ui/**` and `/v3/api-docs/**` or disable them:
  ```properties
  springdoc.api-docs.enabled=false
  springdoc.swagger-ui.enabled=false
  ```
- Keep `RATE_LIMIT_ENABLED=true`.
- Back up `app.db` or migrate to a client-server engine if
  you need multiple instances.


### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Diego Ivan Perea Montealegre**

- GitHub: [@diegoperea20](https://github.com/diegoperea20)

---

Created by [Diego Ivan Perea Montealegre](https://github.com/diegoperea20)
