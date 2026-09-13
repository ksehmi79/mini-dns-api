# Mini DNS API

A Spring Boot REST API that simulates realistic DNS record management and resolution.

The application supports A and CNAME records, CNAME chaining, circular reference detection, TTL-based expiration, asynchronous cleanup, validation, and consistent API error handling.

## Tech Stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- Hibernate
- H2 In-Memory Database
- Jakarta Bean Validation
- Spring Scheduler
- JUnit 5
- Mockito
- MockMvc
- SpringDoc OpenAPI / Swagger UI
- Maven

## Running the Application

### Requirements

Java 21 is required.

Check your Java version:

```bash
java -version
```

### Run Tests

On Windows:

```bash
.\mvnw clean test
```

Current test result:

```text
Tests run: 19
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### Start the Application

```bash
.\mvnw spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

## API Documentation

Swagger UI is available after starting the application:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## API Endpoints

### Create a DNS Record

```http
POST /api/dns
```

Example A record:

```json
{
  "type": "A",
  "hostname": "example.com",
  "value": "192.168.1.1"
}
```

Multiple A records can exist for the same hostname:

```json
{
  "type": "A",
  "hostname": "example.com",
  "value": "192.168.1.2"
}
```

Example A record with TTL:

```json
{
  "type": "A",
  "hostname": "temporary.example.com",
  "value": "10.0.0.1",
  "ttl": 60
}
```

Example CNAME:

```json
{
  "type": "CNAME",
  "hostname": "www.example.com",
  "value": "example.com"
}
```

Successful creation returns:

```text
201 Created
```

### Resolve a Hostname

```http
GET /api/dns/{hostname}
```

Example:

```http
GET /api/dns/example.com
```

Example response:

```json
{
  "hostname": "example.com",
  "resolvedIps": [
    "192.168.1.1",
    "192.168.1.2"
  ],
  "recordType": "A",
  "pointsTo": null
}
```

CNAME records are followed until an A record is found.

For example:

```text
www.example.com
        |
        v
alias.example.com
        |
        v
example.com
        |
        v
192.168.1.1
```

### Get Records for a Hostname

```http
GET /api/dns/{hostname}/records
```

Example:

```http
GET /api/dns/example.com/records
```

This returns the active DNS records associated with the hostname.

### Delete a DNS Record

```http
DELETE /api/dns/{hostname}?type=A&value=192.168.1.1
```

Successful deletion returns:

```text
204 No Content
```

## DNS Rules

### A Records

A hostname can have multiple A records.

For example:

```text
example.com -> 192.168.1.1
example.com -> 192.168.1.2
```

Duplicate hostname/IP combinations are rejected.

### CNAME Records

A hostname can have only one CNAME record.

A CNAME cannot coexist with an A record for the same hostname.

For example, this is not allowed:

```text
example.com -> A -> 192.168.1.1
example.com -> CNAME -> other.com
```

### CNAME Chaining

CNAME records are resolved recursively until an A record is reached.

Example:

```text
www.example.com
    -> alias.example.com
    -> example.com
    -> 192.168.1.1
```

### Circular CNAME Detection

Circular CNAME references are detected and rejected.

Example:

```text
a.com -> b.com
b.com -> c.com
c.com -> a.com
```

The final record is rejected with:

```text
409 Conflict
```

Cycle detection is also performed during resolution as an additional safety measure.

## TTL Expiration

DNS records can optionally contain a TTL value in seconds.

Example:

```json
{
  "type": "A",
  "hostname": "temporary.example.com",
  "value": "10.0.0.1",
  "ttl": 60
}
```

When a TTL is provided, an expiration timestamp is calculated:

```text
expiresAt = createdAt + TTL
```

Expired records are excluded from DNS resolution immediately, even if the background cleanup process has not physically removed them from the database yet.

## Asynchronous TTL Cleanup

Spring Scheduler is used for asynchronous cleanup of expired DNS records.

The cleanup task runs periodically:

```java
@Scheduled(fixedRate = 60000)
```

This provides two levels of TTL handling:

1. **Logical expiration** — expired records are immediately ignored during DNS queries.
2. **Physical cleanup** — a scheduled background task periodically removes expired records from the database.

This asynchronous TTL cleanup was selected to satisfy the senior-level asynchronous processing requirement.

## Validation

The application validates:

- Hostnames
- IPv4 addresses
- Required request fields
- Positive TTL values
- Duplicate A records
- Multiple CNAME records
- A/CNAME conflicts
- Self-referencing CNAME records
- Circular CNAME chains

## Error Handling

The API uses consistent HTTP status codes.

| Situation | HTTP Status |
| --- | --- |
| Record created | 201 Created |
| Successful query | 200 OK |
| Successful deletion | 204 No Content |
| Invalid input | 400 Bad Request |
| Record not found | 404 Not Found |
| DNS record conflict | 409 Conflict |

Malformed JSON requests return `400 Bad Request`.

## Architecture

The project follows a layered architecture:

```text
Controller
    |
    v
Service
    |
    v
Repository
    |
    v
Database
```

Additional components handle:

- DTOs
- Validation
- Exception handling
- TTL scheduling

### Controller Layer

Handles HTTP requests and responses.

### Service Layer

Contains the DNS business logic, including record creation, DNS resolution, conflict detection, deletion, CNAME processing, and TTL filtering.

### Repository Layer

Provides persistence through Spring Data JPA.

### Validation

Hostname and IPv4 validation are separated from the main business logic.

### Scheduler

Runs the asynchronous TTL cleanup process independently from incoming API requests.

## Testing

Automated testing uses JUnit 5, Mockito, MockMvc, and Spring Boot Test.

The test suite currently contains 19 passing tests covering:

- A record creation
- Multiple A records
- Duplicate record rejection
- Invalid IPv4 addresses
- Invalid hostnames
- CNAME conflicts
- CNAME chaining
- Circular CNAME detection
- DNS resolution
- Record deletion
- Missing records
- TTL expiration behavior
- HTTP 201 responses
- HTTP 400 responses
- HTTP 404 responses
- HTTP 409 responses
- HTTP 204 responses

Current result:

```text
Tests run: 19
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## Design Decisions

### H2 Database

An H2 in-memory database was selected so the assessment can be run without requiring an external database.

The repository abstraction allows the persistence layer to be migrated to another relational database later with minimal impact on the DNS business logic.

### Layered Architecture

Business logic is kept in the service layer rather than the controller, improving separation of concerns, maintainability, and testability.

### TTL Design

TTL expiration does not depend entirely on the scheduled cleanup job.

Expired records are filtered when DNS records are queried, ensuring an expired record cannot be returned during the interval between scheduled cleanup executions.

### Circular Reference Protection

CNAME cycles are checked both when records are created and while resolving hostnames, providing additional protection against circular references.

## AI Tool Usage

ChatGPT was used as a development assistant during this assessment.

AI assistance included:

- Discussing application architecture
- Reviewing DNS business rules
- Suggesting edge cases and test scenarios
- Assisting with portions of the implementation
- Troubleshooting development issues
- Reviewing error handling
- Assisting with test development
- Assisting with documentation

All AI-assisted suggestions were reviewed, understood, tested, and adapted before being included in the final implementation.

The related AI conversation/documentation will be included with the submission as requested in the assessment instructions.