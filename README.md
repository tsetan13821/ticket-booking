# Ticket Booking

A ticket booking service built with Spring Boot.

## Tech stack

- Java 21
- Spring Boot 4.2 (Web MVC, Data JPA, Validation)
- PostgreSQL
- Lombok
- Maven (wrapper included)

## Getting started

### Prerequisites

- JDK 21
- A running PostgreSQL instance

### Configuration

Set the database connection in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ticket_booking
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Build and run

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

### Run tests

```bash
./mvnw test
```
