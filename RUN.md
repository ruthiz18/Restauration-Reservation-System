# Reservo — Where & How to Run Everything

Project folder: `C:\Users\HP\Downloads\innovation webtech final project`

```
innovation webtech final project/
├── docker-compose.yml   ← run Docker commands here (project root)
├── backend/             ← Spring Boot (run from IntelliJ, or `mvn` here)
├── frontend/            ← React (run `npm` here)
└── http/reservo.http    ← API requests for IntelliJ
```

You need **3 things running**, in this order:

| # | What | Where to run it | Port |
|---|---|---|---|
| 1 | Docker services (MongoDB, RabbitMQ, Mailpit, admin tools) | terminal in the **project root** | various |
| 2 | Backend (Spring Boot) | **IntelliJ** → `ReservoApplication` | 8080 |
| 3 | Frontend (React) | terminal in the **`frontend`** folder | 5173 |

PostgreSQL is your **local Windows PostgreSQL 18** (port 5432, user `postgres`) — it is already running as a Windows service.

---

## 0. One-time setup (skip if already done)

1. **Docker Desktop** installed and started (wait for "Engine running").
2. **Create the database** (once) in your local PostgreSQL:
   ```bash
   psql -h localhost -p 5432 -U postgres -c "CREATE DATABASE reservo"
   ```
   (or in pgAdmin: right-click *Databases → Create → Database…* → name `reservo`)
3. **Frontend packages** (once):
   ```bash
   cd frontend
   npm install
   ```
4. **IntelliJ**: *File → Open…* → choose the project folder → *Load Maven Project* (bottom-right balloon) → make sure the SDK is **17** and *Settings → Build → Compiler → Annotation Processors → Enable annotation processing* is ticked.

The backend connects to PostgreSQL with `postgres` / `imena1234` (set in `backend/src/main/resources/application.yml`). If your password differs, set `DB_PASSWORD` in the IntelliJ run configuration instead of editing the file.

---

## 1. Start the Docker services

**Where:** a terminal opened in the project root (IntelliJ → *Terminal* tab, or PowerShell: `cd "C:\Users\HP\Downloads\innovation webtech final project"`).

```bash
docker compose up -d
docker compose ps
```

Wait until `reservo-mongo` and `reservo-rabbitmq` show **healthy**.

> This also starts a Docker PostgreSQL on port **5433**. The app does **not** use it (it uses your local one on 5432) — ignore it, or leave it for pgAdmin's pre-registered server.

---

## 2. Start the backend

**Option A — IntelliJ (recommended)**

1. Open `backend/src/main/java/com/reservo/ReservoApplication.java`
2. Click the green ▶ next to `main` → **Run 'ReservoApplication'**
3. Wait for `Started ReservoApplication in … seconds`

To make emails appear in Mailpit: *Run → Edit Configurations → ReservoApplication → Environment variables* → add `EMAIL_ENABLED=true`, then re-run.

**Option B — terminal**

```bash
cd backend
mvn spring-boot:run
```

**Check it works:** http://localhost:8080/actuator/health → `{"status":"UP"}`

---

## 3. Start the frontend

**Where:** a terminal in the `frontend` folder (IntelliJ *Terminal*, then `cd frontend`).

```bash
cd frontend
npm run dev
```

Open **http://localhost:5173**

---

## 4. All the URLs

| What | URL | Login |
|---|---|---|
| **The app (React)** | http://localhost:5173 | see accounts below |
| Server-rendered pages (Thymeleaf) | http://localhost:8080/ | — |
| API docs (Swagger) | http://localhost:8080/swagger-ui.html | — |
| Backend health | http://localhost:8080/actuator/health | — |
| pgAdmin (Docker PostgreSQL) | http://localhost:5050 | `admin@example.com` / `admin` |
| Mongo browser (mongo-express) | http://localhost:8081 | — |
| RabbitMQ dashboard | http://localhost:15672 | `guest` / `guest` |
| Mailpit (email inbox) | http://localhost:8025 | — |

**Demo accounts** (created automatically on the first backend start):

| Role | Email | Password |
|---|---|---|
| Admin | `admin@reservo.local` | `Admin@123` |
| Staff | `staff@reservo.local` | `Staff@123` |
| Customer | `customer@reservo.local` | `Customer@123` |

**Looking at your real database (local PostgreSQL):** open pgAdmin on Windows → *Servers → PostgreSQL 18 → Databases → reservo → Schemas → public → Tables*. (The pgAdmin in Docker, port 5050, shows the unused Docker Postgres.)

---

## 5. Run the tests

**Where:** IntelliJ → right-click `backend/src/test` → **Run 'All Tests'**, or:

```bash
cd backend
mvn test
```

They use an in-memory database and need no Docker.

---

## 6. Stop everything

| What | How |
|---|---|
| Frontend | click in its terminal → `Ctrl + C` |
| Backend | IntelliJ ■ Stop button (or `Ctrl + C` if run in a terminal) |
| Docker services (keep data) | `docker compose down` |
| Docker services **and wipe their data** | `docker compose down -v` |

---

## 7. Optional: run the whole stack in Docker instead

No IntelliJ or npm needed. **Stop the IntelliJ backend first** (port 8080 clash).

```bash
docker compose --profile full up --build
```

| URL | What |
|---|---|
| http://localhost:3000 | React app (nginx) |
| http://localhost:8080 | Backend + Thymeleaf pages |

This mode uses the **Docker PostgreSQL** internally (fresh empty database, demo data re-seeded) — not your local one.

---

## 8. Quick troubleshooting

| Problem | Fix |
|---|---|
| `docker compose` says it can't connect to the engine | Start Docker Desktop and wait for "Engine running". |
| Backend: `password authentication failed for user "postgres"` | Wrong password in `application.yml` — set `DB_PASSWORD` in the run configuration. |
| Backend: `database "reservo" does not exist` | Do step 0.2 (create the database). |
| Backend: connection refused to Mongo / RabbitMQ | `docker compose ps` — start them with `docker compose up -d`. |
| Port 8080 already in use | An old backend is still running — stop it (IntelliJ ■) or end the `java` process. |
| React shows "Cannot reach the server" | Backend isn't running yet (step 2). |
| No emails in Mailpit | Add `EMAIL_ENABLED=true` to the run configuration and restart the backend. |
| Red getters/builders in IntelliJ | Enable annotation processing (step 0.4), then *Build → Rebuild Project*. |

More detail (pgAdmin, MongoDB, Google sign-in, requirements map): see **SETUP.md**.
