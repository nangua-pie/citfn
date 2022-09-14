# CITFN

A front end and back end separated forum, built as a modular monolith.

- **Backend** — Spring Boot 2.7.0 on JDK 11, MariaDB via MyBatis-Plus, Redis,
  WebSocket with STOMP for private messaging.
- **Frontend** — Vue 3 with Vuetify 3 beta, Vite, Pinia and Vue Router.


## Layout

```text
backend/    Spring Boot application, Flyway migrations under src/main/resources/db/migration
frontend/   Vue 3 single page application
deploy/     Nginx configuration
docker-compose.yml
```

## Requirements

| Tool | Version |
|---|---|
| JDK | 11 |
| Maven | 3.8.5 |
| Node.js | 16.15.1 LTS |
| npm | 8.11.0 |
| MariaDB | 10.6.8 |
| Redis | 6.2.x |

## Running locally

### Database

```sql
CREATE DATABASE forum CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Flyway applies the schema on first start. Configuration lives in
`backend/src/main/resources/application.yml` and `application-dev.yml`; real
secrets belong in `application-local.yml`, which is git ignored.

The minimum settings are the JWT secret and the database credentials:

```properties
forum.jwt.secret=<at least 32 bytes>
spring.datasource.username=forum
spring.datasource.password=<password>
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080` and the API docs are at
`http://localhost:8080/doc.html`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies `/api`, `/files` and
`/ws` to the backend, so the browser stays same-origin and no CORS preflight is
needed.

## Docker Compose

Copy the example environment file and fill in the secrets:

```bash
cp .env.example .env
docker compose up --build
```

This starts MariaDB, Redis, MinIO, the API and Nginx on port 80.

## Tests

```bash
cd backend && mvn test
cd frontend && npm run build
```

## Private messaging

One to one chat. MariaDB is the only source of truth; Redis holds online state,
unread counters, rate limits and the recent conversation cache.

WebSocket connects at `/ws` with the access token as an `accessToken` query
parameter, clients send to `/app/im.send` and each user subscribes to
`/user/queue/im`. Events are `NEW_MESSAGE`, `MESSAGE_READ`, `MESSAGE_RECALLED`,
`UNREAD_COUNT`, `USER_ONLINE` and `USER_OFFLINE`.

History uses an id cursor rather than offset paging:

```http
GET /api/im/conversations/{conversationId}/messages?beforeId=9520&size=30
```

Sending applies the checks in order: authenticated caller, receiver exists,
sender not muted or banned, receiver has not blocked the sender, payload within
limits, then persist, update both conversation rows, bump the unread counter and
push if the receiver is online.