# Ticket Booking Backend

A REST backend for booking movie seats, built with Spring Boot and PostgreSQL.

The core idea: **two people can never book the same seat.** Each booking runs in a single
database transaction that locks the requested seat rows with `SELECT ... FOR UPDATE`
(pessimistic locking), so concurrent requests for the same seat are serialized and exactly
one of them wins. A multi-threaded test proves it.

## Tech stack

- Java 21, Spring Boot 4.2 (Web MVC, Data JPA, Validation)
- PostgreSQL (H2 in-memory for tests)
- Lombok
- Maven (wrapper included)

## Data model

| Entity  | Fields                                                            | Notes                                             |
|---------|-------------------------------------------------------------------|---------------------------------------------------|
| Show    | id, movieName, venue, startTime                                   |                                                   |
| Seat    | id, show, seatNumber (e.g. `A1`), price, status                   | `AVAILABLE` / `BOOKED`; unique (show_id, seat_number) |
| Booking | id, customerName, show, seats, totalAmount, status, createdAt     | `CONFIRMED` / `CANCELLED`                         |

Seats belong directly to a show. A booking's seats are stored in a `booking_seats` join table,
so a cancelled booking still shows which seats it held.

## API

| Method   | Endpoint             | Description                                                   | Success |
|----------|----------------------|---------------------------------------------------------------|---------|
| `POST`   | `/shows`             | Create a show and auto-generate its seats (default rows A–E × 10) | 201 |
| `GET`    | `/shows`             | List shows, ordered by start time                             | 200     |
| `GET`    | `/shows/{id}/seats`  | Seat map with status                                          | 200     |
| `POST`   | `/bookings`          | Book seats (all or nothing)                                   | 201     |
| `GET`    | `/bookings/{id}`     | View a booking                                                | 200     |
| `DELETE` | `/bookings/{id}`     | Cancel a booking and free its seats                           | 200     |

### Examples

Create a show (`rows` and `seatsPerRow` are optional, default 5 × 10):

```http
POST /shows
Content-Type: application/json

{
  "movieName": "Oppenheimer",
  "venue": "Galaxy Cinemas - Screen 2",
  "startTime": "2026-12-20T18:30:00",
  "seatPrice": 300.00,
  "rows": 5,
  "seatsPerRow": 10
}
```

Book seats:

```http
POST /bookings
Content-Type: application/json

{
  "customerName": "Tenzin",
  "showId": 1,
  "seatIds": [1, 2]
}
```

```json
{
  "id": 1,
  "customerName": "Tenzin",
  "showId": 1,
  "movieName": "Interstellar",
  "seats": [
    { "id": 1, "seatNumber": "A1", "price": 250.00 },
    { "id": 2, "seatNumber": "A2", "price": 250.00 }
  ],
  "totalAmount": 500.00,
  "status": "CONFIRMED",
  "createdAt": "2026-11-23T21:17:02.565466Z"
}
```

### Errors

Errors use the standard [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem detail format.

| Status | When                                                                               |
|--------|------------------------------------------------------------------------------------|
| 400    | Validation failure (with a per-field `errors` map), malformed JSON, non-numeric id |
| 404    | Show, booking or seat not found (including a seat that belongs to another show)    |
| 409    | Seat already booked, booking already cancelled                                     |

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Seats [A2] are already booked"
}
```

## How booking works

`BookingService.book` runs in one `@Transactional` method, so either every requested seat is
booked or none is:

1. Lock the requested seats with `@Lock(PESSIMISTIC_WRITE)`, **ordered by id**.
2. If any seat is missing or already `BOOKED`, throw an exception and the whole transaction rolls back.
3. Create the booking, set the seats to `BOOKED` and compute the total.

A second request for the same seat blocks on the row lock until the first transaction commits,
then sees the seat as `BOOKED` and gets a 409. Locking rows in id order means two requests for
overlapping seats (for example `[A1, A2]` and `[A2, A1]`) always lock in the same order and
cannot deadlock.

Cancelling is the reverse: lock the booking (so two cancels cannot both succeed), lock its seats
in id order, set them back to `AVAILABLE` and mark the booking `CANCELLED`.

## Getting started

### Prerequisites

- JDK 21 or newer
- PostgreSQL running locally

### Database

Create the database once:

```sql
CREATE DATABASE ticket_booking;
```

The connection is configured in `src/main/resources/application.yml` and can be overridden with
environment variables (keep real passwords out of the repository):

| Variable      | Default                                           |
|---------------|---------------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/ticket_booking` |
| `DB_USERNAME` | `postgres`                                        |
| `DB_PASSWORD` | `postgres`                                        |

Hibernate creates the tables on startup (`ddl-auto: update`), then `data.sql` seeds two sample
shows with 50 seats each. The seed script is idempotent, so restarts do not duplicate data.

### Run

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The API is available at `http://localhost:8080`.

## Tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database, so no setup is needed.

- **`BookingConcurrencyTest`**: 10 threads try to book the same seat at the same moment and
  exactly 1 succeeds. A second test races `[A1, A2]` against `[A2, A1]` to show there is no
  deadlock and the booking stays all-or-nothing. With the `@Lock` removed, all 10 threads
  "succeed", so the test does catch double booking.
- **`TicketBookingApiTest`**: end-to-end tests of all six endpoints, including all-or-nothing
  booking, cancel and rebook, and the 400 / 404 / 409 error responses.

### API tests (Postman)

`postman/` holds a Postman collection that tests the running server end to end: all six
endpoints, the 400 / 404 / 409 error responses, and two live concurrency races in which 10
simultaneous bookings compete for the same seats and exactly one wins.

Start the server, then run it with the [Postman CLI](https://learning.postman.com/docs/postman-cli/postman-cli-overview/):

```bash
postman collection run "postman/collections/Ticket Booking API" \
  -e postman/environments/Local.environment.yaml
```

The collection creates its own show on each run, so it can be run repeatedly.

### Concurrency test on PostgreSQL

To run the concurrency test against PostgreSQL instead of H2:

```bash
./mvnw test -Dtest=BookingConcurrencyTest \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/ticket_booking \
  -Dspring.datasource.username=postgres -Dspring.datasource.password=postgres \
  -Dspring.jpa.hibernate.ddl-auto=update
```
