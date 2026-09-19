# Restaurant Reservation System — Assignment 2 (Spring Boot REST API)

Phase 1 of this project implemented Customer and MenuItem with JSF + Hibernate.
Assignment 2 rebuilds the system as a **Spring Boot REST API** with full CRUD over
**three entities** taken from the initial entity list in the Phase 1 documentation:

| # | Entity | Why it was chosen |
|---|--------|-------------------|
| 1 | **Customer** | The person making the booking (BR-01 to BR-05). |
| 2 | **RestaurantTable** | The resource being booked — capacity and status. |
| 3 | **Reservation** | The link between the two, and where the real business rules live. |

These three were chosen together because they form the core booking transaction of the
system: without all three there is no reservation, and the interesting rules
(double-booking, capacity, delete guards) only exist where they meet.

---

## 1. Technology

- Java 17, Spring Boot 3.3.4
- Spring Web (REST), Spring Data JPA (Hibernate 6), Bean Validation (Hibernate Validator)
- PostgreSQL (default) or H2 in-memory (for demonstrating without a database install)
- Maven, packaged as an executable JAR

## 2. Architecture

```
controller/   REST endpoints, HTTP status codes, @Valid on every request body
service/      business rules and transactions  <-- all rules enforced here
repository/   Spring Data JPA interfaces + JPQL search queries
entity/       JPA entities with Bean Validation constraints (second line of defence)
dto/          request and response objects, so entities are never exposed directly
exception/    custom exceptions + @RestControllerAdvice for uniform JSON errors
```

Validation happens twice, exactly as BR-09 requires: on the incoming DTO
(`@Valid` → HTTP 400 with a field-by-field error map) and again on the entity
before Hibernate writes it.

## 3. Running the project

### Option A — PostgreSQL (matches Phase 1 configuration)

```sql
CREATE DATABASE restaurant_db;
```

Check the credentials in `src/main/resources/application.properties`, then:

```bash
mvn clean spring-boot:run
```

### Option B — H2 in memory (no database install needed)

```bash
mvn clean spring-boot:run -Dspring-boot.run.profiles=h2
```

Tables are created automatically (`ddl-auto=update` on PostgreSQL, `create-drop` on H2).
The API starts on **http://localhost:8080**.

## 4. Endpoints

### Customer — `/api/v1/customers`

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/customers` | Create (BR-01) |
| GET | `/api/v1/customers` | List all, or search with `?keyword=` across name, email, phone (BR-03) |
| GET | `/api/v1/customers/{id}` | Read one |
| PUT | `/api/v1/customers/{id}` | Update (BR-04) |
| DELETE | `/api/v1/customers/{id}` | Delete (BR-05) |

### RestaurantTable — `/api/v1/tables`

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/tables` | Create |
| GET | `/api/v1/tables` | List, filter with `?status=` `?minCapacity=` `?keyword=` |
| GET | `/api/v1/tables/{id}` | Read one |
| PUT | `/api/v1/tables/{id}` | Update |
| DELETE | `/api/v1/tables/{id}` | Delete |

### Reservation — `/api/v1/reservations`

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/reservations` | Create |
| GET | `/api/v1/reservations` | List, filter with `?customerId=` `?tableId=` `?date=` `?status=` |
| GET | `/api/v1/reservations/{id}` | Read one |
| PUT | `/api/v1/reservations/{id}` | Update |
| PATCH | `/api/v1/reservations/{id}/status` | Move through the lifecycle |
| DELETE | `/api/v1/reservations/{id}` | Delete |

## 5. Business rules implemented

Carried over from the Phase 1 documentation:

- **BR-01 / BR-04 / BR-05** — create, update and delete a customer.
- **BR-02** — two customers may not share an email address (checked case-insensitively on create *and* update).
- **BR-03** — one keyword searches first name, last name, email and phone.
- **BR-09** — every payload is validated at the DTO layer and again at the entity layer.
- **BR-10** — every response uses the same envelope: `success`, `message`, `data`, `timestamp`.

Added for the two new entities:

| Rule | Description | HTTP |
|---|---|---|
| R-1 | The customer and the table must both exist | 404 |
| R-2 | No booking in the past | 422 |
| R-3 | Booking time must fall inside opening hours (09:00–23:00, configurable) | 422 |
| R-4 | Party size may not exceed the table's capacity | 422 |
| R-5 | An `OUT_OF_SERVICE` table cannot be booked | 422 |
| R-6 | **No double booking** — active reservations on the same table may not overlap the 120-minute dining slot | 422 |
| R-7 | The same customer may not hold two overlapping reservations | 422 |
| R-8 | Status changes follow the lifecycle: `PENDING → CONFIRMED → SEATED → COMPLETED`, with `CANCELLED` / `NO_SHOW` as alternative ends | 422 |
| R-9 | A reservation in a final state can no longer be edited | 422 |
| — | Table numbers are unique | 409 |
| — | A customer or table with active reservations cannot be deleted | 422 |
| — | A table's capacity cannot be reduced below a party already booked on it | 422 |
| — | Table status (`AVAILABLE` / `RESERVED` / `OCCUPIED`) is kept in step with its bookings automatically | — |

The dining slot length and opening hours are configuration, not hard-coded:

```properties
restaurant.reservation.slot-duration-minutes=120
restaurant.reservation.opening-time=09:00
restaurant.reservation.closing-time=23:00
```

## 6. HTTP status codes used

| Code | Meaning in this API |
|---|---|
| 200 | Successful read, update or delete |
| 201 | Resource created |
| 400 | Validation failure — the response `data` is a map of field → message |
| 404 | Resource not found |
| 409 | Uniqueness conflict (duplicate email, duplicate table number) |
| 422 | Request was well formed but breaks a business rule |
| 500 | Unexpected error |

## 7. Testing with Postman

1. Start the application.
2. In Postman: **Import** → `postman/Restaurant-Reservation-API.postman_collection.json`
   (optionally also `postman/Restaurant-Reservation.postman_environment.json`).
3. Run the four folders **in order**, or use the Collection Runner on the whole collection.

The collection has **46 requests** and needs no manual editing:

- A collection-level pre-request script sets `{{futureDate}}` to seven days ahead, so
  bookings never fail the "no past dates" rule.
- Create requests store `{{customerId}}`, `{{tableId}}`, `{{reservationId}}` and so on
  into collection variables through test scripts, and later requests reuse them.
- Every request carries a test asserting the expected status code, so the Runner shows
  a pass/fail summary for the whole API.

Folder layout:

| Folder | What it demonstrates |
|---|---|
| 1. Setup and Customer CRUD | Create, read, search, update — plus the duplicate-email (409) and validation (400) failures |
| 2. RestaurantTable CRUD | Create, filter by status and capacity, update — plus duplicate number and bad capacity |
| 3. Reservation CRUD and business rules | The happy path, then double booking, over-capacity, past date, closed hours, invalid status transitions and the edit lock |
| 4. Cross-entity rules and cleanup | Delete guards between the three entities, then removal of the test data |

### Sample request

`POST http://localhost:8080/api/v1/reservations`

```json
{
  "customerId": 1,
  "tableId": 1,
  "reservationDate": "2026-10-05",
  "reservationTime": "19:00",
  "partySize": 4,
  "notes": "Window seat if possible"
}
```

Success (201):

```json
{
  "success": true,
  "message": "Reservation created successfully",
  "data": {
    "id": 1,
    "customerId": 1,
    "customerName": "Ruth Izukondi",
    "customerPhone": "0788123456",
    "tableId": 1,
    "tableNumber": "T1",
    "tableCapacity": 6,
    "reservationDate": "2026-10-05",
    "reservationTime": "19:00",
    "partySize": 4,
    "status": "PENDING",
    "notes": "Window seat if possible"
  },
  "timestamp": "2026-09-18T10:15:30.123"
}
```

Double booking on the same table (422):

```json
{
  "success": false,
  "message": "Table T1 is already booked around 20:00 on 2026-10-05. Each booking holds the table for 120 minutes.",
  "timestamp": "2026-09-18T10:16:02.551"
}
```

Validation failure (400):

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "Enter a valid email address",
    "firstName": "First name is required",
    "phone": "Phone must contain 10 digits and start with 07"
  },
  "timestamp": "2026-09-18T10:16:44.010"
}
```

## 8. Date and time formats

- Dates: `yyyy-MM-dd` (e.g. `2026-10-05`)
- Times: `HH:mm`, 24-hour (e.g. `19:00`)
- Enum values are sent as text: `PENDING`, `CONFIRMED`, `SEATED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`,
  and `AVAILABLE`, `RESERVED`, `OCCUPIED`, `OUT_OF_SERVICE`.
