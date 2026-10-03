# Reservo — Setup Guide

Restaurant reservation system: **Spring Boot** (REST API + **Thymeleaf** pages) · **React** (Vite) · **PostgreSQL** · **MongoDB** · **RabbitMQ**.

This guide takes you from a fresh clone to a running app, and shows how to look inside the databases with **pgAdmin** and **MongoDB** tools.

---

## 0. What runs where

| Piece | URL / port | Started by |
|---|---|---|
| Spring Boot API + Thymeleaf pages | http://localhost:8080 | **IntelliJ** (or Docker `full` profile) |
| React app (dev server) | http://localhost:5173 | `npm run dev` |
| PostgreSQL | `localhost:5433` (db/user/password: `reservo`) — 5433, not 5432, so it never clashes with a PostgreSQL installed on Windows | Docker |
| **pgAdmin** (Postgres UI) | http://localhost:5050 | Docker |
| MongoDB | `localhost:27017` (db: `reservo`, no auth in dev) | Docker |
| **mongo-express** (Mongo UI) | http://localhost:8081 | Docker |
| RabbitMQ | AMQP `localhost:5672` | Docker |
| RabbitMQ management UI | http://localhost:15672 (`guest` / `guest`) | Docker |
| Mailpit (fake inbox for emails) | http://localhost:8025 | Docker |
| Swagger UI (API docs) | http://localhost:8080/swagger-ui.html | IntelliJ |

**Seeded demo accounts** (created on first start by `DataSeeder`):

| Role | Email | Password |
|---|---|---|
| Admin | `admin@reservo.local` | `Admin@123` |
| Staff | `staff@reservo.local` | `Staff@123` |
| Customer | `customer@reservo.local` | `Customer@123` |

Three restaurants (La Trattoria, Sakura Sushi, Savanna Grill) with tables and menus are seeded too.

---

## 1. Prerequisites

- **JDK 17** (IntelliJ: *File → Project Structure → SDK*; the project is set to `temurin-17`)
- **Node.js 20+** and npm
- **Docker Desktop** — **start it and wait until it says "Engine running"** before the next step
- **IntelliJ IDEA** (Community or Ultimate)

---

## 2. Start the infrastructure (Docker)

From the repository root (the folder containing `docker-compose.yml`):

```bash
docker compose up -d
```

First run downloads the images (a few minutes). Then check everything is healthy:

```bash
docker compose ps
```

You should see `reservo-postgres`, `reservo-mongo`, `reservo-rabbitmq` as **healthy**, plus `pgadmin`, `mongo-express`, `mailpit` running.

Useful Docker commands:

| What | Command |
|---|---|
| See logs of one service | `docker compose logs -f postgres` |
| Stop (keep data) | `docker compose down` |
| Stop **and wipe all data** (fresh start) | `docker compose down -v` |
| Restart one service | `docker compose restart rabbitmq` |

> Defaults work out of the box. To change passwords/ports, copy `.env.example` to `.env` and edit it.

---

## 3. pgAdmin (PostgreSQL)

1. Open **http://localhost:5050**.
2. Log in with **`admin@example.com`** / **`admin`**.
   (pgAdmin refuses addresses ending in `.local`, which is why this isn't `@reservo.local`.)
3. In the left tree open **Servers → Reservo (Docker)**. It is pre-registered from `docker/pgadmin/servers.json`.
4. When asked for a password type **`reservo`** and tick **Save Password**.
5. Browse: **Reservo (Docker) → Databases → reservo → Schemas → public → Tables**.

> The tables (`users`, `restaurants`, `dining_tables`, `reservations`) are created by Hibernate **when the backend first starts** (`ddl-auto: update`). If the list is empty, start the backend (section 5) then right-click *Tables → Refresh*.

Run queries: right-click the `reservo` database → **Query Tool**, e.g.

```sql
SELECT id, full_name, email, role, provider FROM users;

SELECT r.id, rest.name, r.reservation_date, r.start_time, r.party_size, r.status
FROM reservations r JOIN restaurants rest ON rest.id = r.restaurant_id
ORDER BY r.reservation_date DESC, r.start_time;

SELECT rest.name, count(t.id) AS tables, sum(t.capacity) AS seats
FROM restaurants rest JOIN dining_tables t ON t.restaurant_id = rest.id
GROUP BY rest.name;
```

Useful for your report: right-click a table → **ERD For Table** / **Generate ERD** gives you the physical schema diagram, and *Properties → Constraints / Indexes* shows keys and indexes.

**Registering the server by hand** (if you ever need to): *Register → Server…*

- *General → Name*: anything
- *Connection*: Host **`postgres`** (pgAdmin runs inside Docker, so use the service name — **not** `localhost`), Port `5432`, Maintenance DB `reservo`, Username `reservo`, Password `reservo`.
- If you use a *desktop* pgAdmin installed on Windows instead, the host is **`localhost`** and the port is **`5433`**.

---

## 4. MongoDB

MongoDB stores **menus**, **reviews** and the **notification audit log** (collections `menus`, `reviews`, `notification_logs`). Collections appear once data is written — `menus` is filled by the seeder on first backend start.

### Option A — mongo-express (already running)

Open **http://localhost:8081** → click the **`reservo`** database → browse collections.

### Option B — MongoDB Compass (desktop app, nicer for reports)

Download Compass from mongodb.com, then connect with:

```
mongodb://localhost:27017/reservo
```

### Option C — command line (`mongosh` inside the container)

```bash
docker exec -it reservo-mongo mongosh reservo
```

```js
show collections
db.menus.find().pretty()
db.reviews.find()
db.notification_logs.find().sort({ createdAt: -1 }).limit(5)
db.reviews.getIndexes()          // shows the unique (restaurantId, userId) index
```

> Dev MongoDB has **no authentication**. Never expose port 27017 to the internet like this.

---

## 5. Run the backend in IntelliJ

1. **File → Open…** and choose the **repository root** (the folder with `docker-compose.yml`, `backend/`, `frontend/`).
   - If IntelliJ shows a *"Maven projects need to be imported"* balloon, click **Load Maven Project**.
   - Otherwise: right-click `backend/pom.xml` → **Add as Maven Project**.
2. **Project SDK**: *File → Project Structure → Project* → select **17** (Temurin 17).
3. **Lombok**: *Settings → Build, Execution, Deployment → Compiler → Annotation Processors* → tick **Enable annotation processing**. (Lombok plugin is bundled in recent IntelliJ versions.) If you see red "cannot find symbol" on getters/builders, this is the cause.
4. Open `backend/src/main/java/com/reservo/ReservoApplication.java` and click the green ▶ next to `main` → **Run**.
5. Wait for `Started ReservoApplication`. Check:
   - http://localhost:8080/actuator/health → `{"status":"UP"}`
   - http://localhost:8080/ → the **Thymeleaf** restaurant directory
   - http://localhost:8080/swagger-ui.html → interactive API docs

All connection settings default to the Docker containers from step 2 (see `backend/src/main/resources/application.yml`), so **no environment variables are needed** for local development.

**Optional environment variables** (Run → *Edit Configurations…* → *Environment variables*, `;`-separated):

| Variable | Purpose |
|---|---|
| `EMAIL_ENABLED=true` | Actually send email through Mailpit (otherwise emails are only logged/simulated). Then read them at http://localhost:8025 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Enable "Continue with Google" (see section 8) |
| `JWT_SECRET` | Override the token signing key (use 32+ characters) |

**Run the tests:** right-click `backend/src/test` → **Run 'All Tests'** (or `cd backend && mvn test`). The tests use an in-memory H2 database and mock MongoDB/RabbitMQ, so they run even without Docker.

**Try the API from IntelliJ:** open `http/reservo.http`, choose the **dev** environment (top-right of the editor) and click the green ▶ next to any request. Run the three *login* requests first — they store the tokens the others use.

---

## 6. Run the React frontend

In IntelliJ's **Terminal** tab (or any terminal):

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` to `http://localhost:8080`, so the backend must be running.

Things to try:

1. **Customer**: sign in as `customer@reservo.local` → open a restaurant → **Book a table** → 3-step wizard (date & guests → time → confirm) → success screen. Find it later under **My reservations**.
2. **Staff**: sign in as `staff@reservo.local` → you land on the **Dashboard** (today's stats, reservations, live table status). Use the restaurant picker in the top bar. Then:
   - **Reservations** → *Day* view is a calendar (one column per table); *List* view has filters. Click a booking to open its details page and move it `PENDING → CONFIRMED → SEATED → COMPLETED` (or cancel / no-show).
   - **Tables** → floor view with live status (available / reserved / occupied / pending).
   - **Menu** → edit the restaurant's menu (stored in MongoDB).
3. Check the **side effects of RabbitMQ**:
   - http://localhost:15672 → *Queues* → `notification.email` / `notification.sms` (message rates tick up)
   - http://localhost:8025 (Mailpit) → the HTML email rendered by Thymeleaf *(needs `EMAIL_ENABLED=true`)*
   - Admin → **Notifications** page (rows stored in MongoDB `notification_logs`)
4. **Admin**: sign in as `admin@reservo.local` → extra **Administration** items in the sidebar: restaurants, users & roles, notification log. Admins can also add/deactivate tables on the **Tables** page.
5. **RBAC**: as a customer, try opening `/admin/users` → blocked in the UI *and* the API returns `403`.

Production build: `npm run build` (output in `frontend/dist`).

---

### Restaurant background pictures

Each restaurant gets a background scene based on its **cuisine** (grill/meat/African → flames & skewers, Japanese/sushi → sushi & rising sun, Italian → pizza & pasta, anything else → candle-lit dining room). The staff sidebar is tinted to match the selected restaurant.

The scenes are drawn illustrations in `frontend/public/images/art/*.svg` (and `backend/src/main/resources/static/img/*.svg` for the Thymeleaf pages). To use **real photos** instead, just add files — no code changes:

| File name | Used for |
|---|---|
| `grill.jpg`, `sushi.jpg`, `italian.jpg`, `dining.jpg` | cards, restaurant headers, dashboard banner |
| `hero.jpg` | home page + login page background |

Put them in `frontend/public/images/` (React app) and `backend/src/main/resources/static/img/` (Thymeleaf pages; no `hero.jpg` there). Use photos you have the rights to. Cuisine → scene rules live in `frontend/src/themes.js` and `backend/.../web/Themes.java`.

---

## 7. Thymeleaf pages (server-rendered)

Served directly by Spring Boot, no React involved:

| URL | Page |
|---|---|
| http://localhost:8080/ | Restaurant directory with search + pagination |
| http://localhost:8080/restaurants/1 | Restaurant detail with menu (MongoDB) and rating |

They link to the React app (`app.frontend-url`) for the actual booking. Thymeleaf also renders the **HTML reservation emails**: `backend/src/main/resources/templates/email/reservation.html`.

Templates live in `backend/src/main/resources/templates/`, CSS in `static/css/site.css`.

---

## 8. Google sign-in (OAuth2) — optional

Email/password login works without this. To enable **Continue with Google**:

1. Go to https://console.cloud.google.com → create/select a project → **APIs & Services → OAuth consent screen** (External, add yourself as a test user).
2. **Credentials → Create credentials → OAuth client ID → Web application**.
3. **Authorized redirect URI**: `http://localhost:8080/login/oauth2/code/google`
4. Copy the client ID and secret into the IntelliJ run configuration as `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, then restart the backend.
5. On the React login page click **Continue with Google**. After Google approves, the backend redirects to `http://localhost:5173/oauth2/callback#token=…` and the app signs you in.

Never commit the secret to Git — keep it in the run configuration or in `.env`.

---

## 9. Run the whole stack in Docker (demo mode)

Builds the backend and frontend images and runs everything in containers:

```bash
docker compose --profile full up --build
```

| URL | What |
|---|---|
| http://localhost:3000 | React app (nginx, proxies `/api` to the backend) |
| http://localhost:8080 | Backend + Thymeleaf pages |

In this mode `EMAIL_ENABLED` is `true` and mail goes to Mailpit (http://localhost:8025).
Stop IntelliJ's copy of the backend first — both want port 8080.

---

## 10. Troubleshooting

| Symptom | Fix |
|---|---|
| `error during connect … dockerDesktopLinuxEngine` | Docker Desktop isn't running. Start it and wait for "Engine running". |
| `port is already allocated` / `address already in use` on 5433, 27017, 5672… | Another Postgres/Mongo/RabbitMQ is running locally. Stop it, or change the left-hand port in `docker-compose.yml` **and** set the matching env var (`DB_URL`, `MONGO_URI`, `RABBIT_PORT`). |
| `password authentication failed for user "reservo"` | The backend reached a *different* PostgreSQL (e.g. the Windows service on 5432). Make sure `DB_URL` points at port **5433** (the default) — check with `docker compose ps`. |
| Backend fails: `Connection refused … 5433` / `27017` / `5672` | Containers not up or not healthy yet: `docker compose ps`, then `docker compose logs <service>`. |
| pgAdmin: *"email address is invalid"* | Don't use a `.local` address in `PGADMIN_EMAIL`. |
| pgAdmin can't connect to the server | Inside pgAdmin (Docker) the host is `postgres`, not `localhost`. |
| Tables missing in pgAdmin | Start the backend once, then *Refresh*. |
| Red getters/builders in IntelliJ | Enable annotation processing (section 5, step 3), then *Build → Rebuild Project*. |
| `Cannot reach the server` in the React app | Backend not running, or not on port 8080. |
| Login says invalid credentials after `docker compose down -v` | The database was wiped; the seeder recreates the demo users on the next backend start — restart the backend. |
| Google button shows an error page | `GOOGLE_CLIENT_ID/SECRET` not set, or the redirect URI doesn't match exactly (section 8). |
| Emails not showing in Mailpit | Set `EMAIL_ENABLED=true` and restart the backend; confirm the queue is consumed at http://localhost:15672. |
| Want a clean slate | `docker compose down -v`, then `docker compose up -d` and restart the backend. |

---

## 11. Git workflow (project requirement 9)

```bash
git checkout -b feature/<short-name>
git add -A
git commit -m "feat: <what you did>"
git push -u origin feature/<short-name>
```

Then open a **pull request into `main`** on GitHub/GitLab when the feature is complete. Use small, meaningful commits (`feat:`, `fix:`, `docs:`, `test:`). `.gitignore` already excludes `target/`, `node_modules/`, `dist/` and `.env`.

---

## 12. Where each project requirement lives

| Requirement | Implementation |
|---|---|
| Responsive front end (React) | `frontend/src` — sidebar dashboard for staff/admin, top-bar site for customers; collapses to a mobile menu. Brand name/tagline live in `src/brand.js` |
| Thymeleaf | `backend/src/main/resources/templates` + `web/PageController.java` |
| Spring Boot layered architecture | `web` → `service` → `repo` → `domain` (+ `event`, `security`, `config`) |
| Relational DB (PostgreSQL) | `domain/*` JPA entities: users, restaurants, dining_tables, reservations |
| Non-relational DB (MongoDB) | `mongo/*`: menus, reviews, notification_logs |
| Secure auth + OAuth2 | JWT (`security/JwtService`), Google OAuth2 (`OAuth2SuccessHandler`), BCrypt |
| RBAC | `Role` enum (CUSTOMER/STAFF/ADMIN), `@PreAuthorize` on controllers, `Protected` routes in React |
| RabbitMQ (email/SMS) | `config/RabbitConfig`, `event/*` (publisher, listeners, topic exchange `reservo.events`) |
| Performance | Caffeine caching, pagination, DB indexes, gzip, pessimistic lock against double booking |
| Testing (bonus) | `backend/src/test` — unit + integration tests |
| DevOps (bonus) | `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` + nginx |
